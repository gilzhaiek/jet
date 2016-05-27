package com.reconinstruments.dashlauncher.music.library;

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

import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.connect.music.ReconMediaData;
import com.reconinstruments.connect.music.ReconMediaData.ReconSong;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.dashlauncher.music.MusicHelper.MusicInterface;
import com.reconinstruments.dashmusic.DashLauncherApp;
import com.reconinstruments.dashmusic.R;

public class MusicListActivity extends ListActivity{

	private static final String TAG = "MusicListActivity";
	

	private MusicCursorAdapter mListAdapter;
	private TextView errorTV, titleTV;
	private ListView listView;
	private MusicListType listType;
	//private int mode;
	private String srcId;
	//private MusicListCursor cursor;
	private String hudConnStatus = "HUD_STATE_CHANGED";
	
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
				String selName = data.getName();
				Log.d(TAG,"id selected: "+selId);
				
				// HACK for playlist songs. otherwise we cannot catch when user presses no song item
				if(selId.equals(MusicCursorAdapter.NO_MEDIA)||selId.contains("No songs")) return;
				
				// hack, because playlist songs have the _id we are interested in the second column we also compare with name which will be the actual id
				if(selId.equals(MusicCursorAdapter.SHUFFLE_ID)||selId.equals("SHUFFLE")){	
					SongInfo list = new SongInfo("-1", listType, srcId);
					startActivity(new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("shuffle",MusicHelper.SongInfoToBundle(list)));
					Log.d(TAG,"performing toggle shuffle in client app");
					return;
				}
				Intent nextIntent = null;
				switch(listType) {
				// If songs are being shown, clicking should start playing them
				case ARTIST_SONGS:
					// if(selId.equals("-3")){// shuffle
					// 	nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.library.MusicListActivity.class);
					// 	nextIntent.putExtra("listType", MusicListType.ARTIST_SONGS);
					// 	nextIntent.putExtra("id", srcId);
					// }
				case PLAYLIST_SONGS:
				case ALBUM_SONGS:
					SongInfo song = new SongInfo(selId, listType, srcId);
					nextIntent = new Intent(getApplicationContext(), 
							com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("song", MusicHelper.SongInfoToBundle(song));
					break;
					
				// If albums are being shown, a new activity showing their contents should be brought up.
				case ARTIST_ALBUMS:
					nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.library.MusicListActivity.class);
					if(selId.equals(MusicCursorAdapter.ALL_SONGS)){//artist songs
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
		
		registerReceiver(phoneConnectionReceiver, new IntentFilter(ConnectHelper.MSG_STATE_UPDATED));
		registerReceiver(phoneConnectionReceiver, new IntentFilter(hudConnStatus));
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
		listView.setSelection(0);
		DashLauncherApp.getInstance().setInMusicApp(true);
	}
	@Override
	protected void onPause()
	{
		super.onPause();
		DashLauncherApp.getInstance().setInMusicApp(false);
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
		Log.d(TAG,"onDestroy()");
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
			if (intent.getAction().equals(ConnectHelper.MSG_STATE_UPDATED)) {
				boolean isConnected = intent.getBooleanExtra("connected", false);
				if(!isConnected)
					finish();
			}
			else if (intent.getAction().equals(hudConnStatus)) {
				boolean connected = intent.getExtras().getInt("state")==2;
				Log.d(TAG,"connected "+connected);
				if(!connected)
					finish();
			}
		}
	};
}
