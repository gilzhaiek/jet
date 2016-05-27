package com.reconinstruments.dashlauncher.music.library;

import java.io.Serializable;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.SongInfo;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.modlivemobile.music.ReconMediaData;

public class MusicListActivity extends ListActivity {

	private static final String TAG = "MusicListActivity";
	

	private MusicCursorAdapter mListAdapter;
	private TextView errorTV, titleTV;
	private ListView listView;
	private MusicListType listType;
	//private int mode;
	private String srcId;
	//private MusicListCursor cursor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		
		Bundle extras = getIntent().getExtras();
		//mode = extras.getInt("mode");
		srcId = extras.getString("id");
		listType = (MusicListType) extras.getSerializable("listType");
		
		setContentView(R.layout.music_library_list_layout);
		
		// Set title for the view
		titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText(extras.getString("title"));
		titleTV.setVisibility(View.VISIBLE); // Set this visible since this layout has this widget gone by default
		
		errorTV = (TextView) findViewById(android.R.id.empty);

		//getMusicListType(mode);
		
		// Get instance of the music cursor
		//cursor = MusicDBFrontEnd.getCursor(this, listType, (int) srcId);
		
		// Create adapter from cursor
		/*mListAdapter = new SimpleCursorAdapter(this,
				R.layout.music_library_list_item,
				cursor,
				LibraryListFragment.getColumnNames(listType), // column names we want from cursor
				new int[] { R.id.maintext });*/
		
		mListAdapter = new MusicCursorAdapter(this,listType, srcId,R.layout.music_library_list_item,new int[] { R.id.maintext,R.id.mainimage },"");
		
		setListAdapter(mListAdapter);
		
		// Set up onClick for list items
		listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//Log.v(TAG, "id: " + id + " , cursor: " + cursor.getLong(0));
				
				ReconMediaData data = mListAdapter.getMedia(position);
				String selId = data.getId();

				if(selId.equals(MusicCursorAdapter.NO_MEDIA_ID)) return;
				
				if(selId.equals(MusicCursorAdapter.SHUFFLE_ID)){					
					SongInfo list = new SongInfo("-1", listType, srcId);
					startActivity(new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("shuffle",MusicHelper.SongInfoToBundle(list)));
					return;
				}
				Intent nextIntent = null;
				switch(listType) {
				// If songs are being shown, clicking should start playing them
				case ARTIST_SONGS:
				case PLAYLIST_SONGS:
				case ALBUM_SONGS:
					SongInfo song = new SongInfo(selId, listType, srcId);
					nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("song", MusicHelper.SongInfoToBundle(song));
					break;
					
				// If albums are being shown, a new activity showing their contents should be brought up.
				case ALBUMS:
					nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.library.MusicListActivity.class);
					if(selId.equals("-1")){//artist songs
						nextIntent.putExtra("listType", MusicListType.ARTIST_SONGS);
						nextIntent.putExtra("id", srcId);
					} else {
						nextIntent.putExtra("listType", MusicListType.ALBUM_SONGS);
						nextIntent.putExtra("id", selId);
						String title = (String) ((TextView)view.findViewById(R.id.maintext)).getText();
						nextIntent.putExtra("title", title);
					}
					break;
				default:
					return;
				}
				
				startActivity(nextIntent);
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});
		
		registerReceiver(phoneConnectionReceiver, new IntentFilter(BTCommon.MSG_STATE_UPDATED));
	}
	
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		Log.d(TAG, "onNewIntent");
		setIntent(intent);
	}


	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(TAG, "onResume");
		Bundle extras = getIntent().getExtras();
		
		if(extras.containsKey("title"))
			titleTV.setText(extras.getString("title"));
		srcId = extras.getString("id");
		listType = (MusicListType) extras.getSerializable("listType");
		
		mListAdapter.updateCursor(listType, srcId);
		MusicHelper.setInMusicApp(true);
	}
	@Override
	protected void onPause()
	{
		super.onPause();
		MusicHelper.setInMusicApp(false);
	}
	@Override
	protected void onStart()
	{
		super.onStart();
		Log.d(TAG, "onStart");
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(phoneConnectionReceiver);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}
	
	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BTCommon.MSG_STATE_UPDATED)) {
				boolean isConnected = intent.getBooleanExtra("connected", false);
				if(!isConnected)
					finish();
			}
		}
	};
}
