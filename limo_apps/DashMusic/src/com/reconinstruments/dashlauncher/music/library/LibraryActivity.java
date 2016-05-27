package com.reconinstruments.dashlauncher.music.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.TextView;

import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.connect.music.ReconMediaData.ReconSong;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.dashlauncher.music.MusicHelper.MusicInterface;
import com.reconinstruments.dashmusic.DashLauncherApp;
import com.reconinstruments.dashmusic.R;

public class LibraryActivity extends FragmentActivity{

	private static final String TAG = "LibraryActivity";
	private Fragment[] listFragments;
	//private SmartphoneConnector connector;
	//private MusicHelper musicHelper;
	//private FrameLayout fragmentFrame;
	//private TextView errorTV;
	private TextView playlistTab;
	private TextView artistTab;
	private TextView songTab;
	
	private int selection = Integer.MAX_VALUE/2;

	private String hudConnStatus = "HUD_STATE_CHANGED";

	private String[] tabNames = {"PLAYLISTS", "ARTISTS", "SONGS"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.music_library_main);

		
		//tabGallery = (Gallery) findViewById(R.id.tab_gallery);
		//tabGallery.setFocusable(false);
		playlistTab = (TextView) findViewById(R.id.playlist_tab);
		artistTab = (TextView) findViewById(R.id.artist_tab);
		songTab = (TextView) findViewById(R.id.song_tab);

		if(/*musicHelper.iosMode()*/ false){//FIXME: It seemed
					  //wrong. Library should not
					  //be called when in iosMode

			//tabGallery.setAdapter(new InfiniteStringAdapter(this, new String[]{"Playlists", "Songs"}));
			listFragments = new Fragment[] {new LibraryListFragment(MusicListType.PLAYLISTS), 
					new LibraryListFragment(MusicListType.SONGS)};
		}
		else{

			//tabGallery.setAdapter(new InfiniteStringAdapter(this, tabNames));
			listFragments = new Fragment[] {new LibraryListFragment(MusicListType.PLAYLISTS), 
					new LibraryListFragment(MusicListType.ARTISTS), 
					new LibraryListFragment(MusicListType.SONGS)};
		}

		//tabGallery.setSelection(selection+1);
		setSelection(selection+1);

		registerReceiver(phoneConnectionReceiver, new IntentFilter(ConnectHelper.MSG_STATE_UPDATED));
		registerReceiver(phoneConnectionReceiver, new IntentFilter(hudConnStatus));
	}

	public void onDestroy() {
		Log.d(TAG,"onDestroy()");
		super.onDestroy();
		unregisterReceiver(phoneConnectionReceiver);
	}

	public void onResume() {
		super.onResume();
		DashLauncherApp.getInstance().setInMusicApp(true);
	}
	public void onPause() {
		super.onPause();
		DashLauncherApp.getInstance().setInMusicApp(false);
	}
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_UP) {
			int newSelection;
			int last = selection%listFragments.length;
			switch(keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				newSelection = selection-1;
				//tabGallery.setSelection(newSelection, true);
				setSelection(newSelection);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				newSelection = selection+1;
				//tabGallery.setSelection(newSelection, true);
				setSelection(newSelection);
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				((LibraryListFragment)listFragments[last]).performCircularity(false);
				//NOTIFY list fragment
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
				((LibraryListFragment)listFragments[last]).performCircularity(true);
				//NOTIFY list fragment
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	public void onBackPressed() {
		super.onBackPressed();

		this.overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
	}

	public void setSelection(int newSelection) {
		if(newSelection==selection) return;

		int last = selection%listFragments.length;
		int next = newSelection%listFragments.length;

		Log.v(TAG, "position: " + next + ", prev: " + last);

		fixTabHeaders(last,next);
		
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_NONE);

		if(last >= 0) {
			ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
			ft.remove(listFragments[last]);
		}

		ft.add(R.id.details, listFragments[next]);		
		ft.commit();

		selection = newSelection;
	}
	
	private void fixTabHeaders(int last, int next){
		int white = getResources().getColor(R.color.selected_tab_color);
		int dark = getResources().getColor(R.color.unselected_tab_color);
		
		if(last==0){
			// playlists
			playlistTab.setTextColor(dark);
		}else if(last==1){
			// artists
			artistTab.setTextColor(dark);
		}else if(last==2){
			// songs
			songTab.setTextColor(dark);
		}
		
		if(next==0){
			// playlists
			playlistTab.setTextColor(white);
		}else if(next==1){
			// artists
			artistTab.setTextColor(white);
		}else if(next==2){
			// songs
			songTab.setTextColor(white);
		}
	}

	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "action: " + intent.getAction());

			if (intent.getAction().equals(ConnectHelper.MSG_STATE_UPDATED)) {
				boolean isConnected = intent.getBooleanExtra("connected", false);
				Log.v(TAG, isConnected ? "Phone Connected" : "Phone Not Connected");

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
