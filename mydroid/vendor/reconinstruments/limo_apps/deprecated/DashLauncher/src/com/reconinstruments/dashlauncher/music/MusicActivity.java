package com.reconinstruments.dashlauncher.music;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import com.reconinstruments.dashlauncher.BoardActivity;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashlauncher.music.MusicHelper.MusicInterface;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.PlayerInfo;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.PlayerState;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.SongInfo;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.modlivemobile.music.ReconMediaData.ReconSong;

//connected activity default state is disconnected
public class MusicActivity extends BoardActivity implements SmartphoneInterface, MusicInterface{
	public static final String TAG = "MusicActivity";
	
	public static final boolean DEBUG = true;

	private View firstItem, libraryItem;
	private View noSong,songInfo;
	private TextView songNameTV, artistNameTV, albumNameTV, playerStateTV;

	private View iPhoneItem;
	SmartphoneConnector connector;
	MusicHelper musicHelper;

    protected boolean mIsOakley = false;
    protected static final String SECRECT_FILE = "/OakleyFile"; 

    protected void setIsOakley () {
		File OakleyFile = new File(SECRECT_FILE);
		mIsOakley = OakleyFile.exists();
    }

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");

		this.setContentView(R.layout.music_main);

		noSong = (View) findViewById(R.id.no_song);
		songInfo = (View) findViewById(R.id.song_info);

		playerStateTV = (TextView) findViewById(R.id.player_state);
		songNameTV = (TextView) findViewById(R.id.song_name);
		artistNameTV = (TextView) findViewById(R.id.artist_name);
		albumNameTV = (TextView) findViewById(R.id.album_name);

		firstItem = findViewById(R.id.music_main_item);
		libraryItem = findViewById(R.id.library_item);
		iPhoneItem = findViewById(R.id.iphone_button);

		firstItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectPlayerButton();
			}
		});
		firstItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		iPhoneItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class);
				startActivity(nextIntent);
				overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
			}
		});
		iPhoneItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		libraryItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openLibrary();
			}
		});
		libraryItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		firstItem.setOnLongClickListener(musicShortcut);
		iPhoneItem.setOnLongClickListener(musicShortcut);
		libraryItem.setOnLongClickListener(musicShortcut);
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		musicHelper = new MusicHelper(this);
		connector = new SmartphoneConnector(this);
	}


	@Override
	protected void onPause()
	{
		super.onPause();
	}

	public void onResume() {
		super.onResume();
		
		// JIRA: MODLIVE-688 Music app reads "No Library detected" when MOD Live connectes with Engage Android
		Log.d(TAG, "Check to see if the Overlay should be removed.");
		musicHelper.checkLibrary();
		// End of JIRA: MODLIVE-688
		
		Log.d(TAG, "onResume()");
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");

		connector.onDestroy();
		musicHelper.onDestroy();
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {

		/*if(connector.isConnected()&&MusicHelper.iosMode()) {
			if (event.getAction() == KeyEvent.ACTION_UP&&keyCode==KeyEvent.KEYCODE_DPAD_CENTER){
				Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class);
				startActivity(nextIntent);
				overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
				return true;
			}
		}*/
		return super.onKeyUp(keyCode, event);
	}

	public void onConnect()
	{
		musicHelper.onConnect();
		if(!MusicHelper.iosMode()){
			musicHelper.getState();
			firstItem.setFocusable(true);
			libraryItem.setFocusable(true);
			firstItem.requestFocus();
			firstItem.setSelected(true);	
			firstItem.setVisibility(View.VISIBLE);
			libraryItem.setVisibility(View.VISIBLE);
			iPhoneItem.setVisibility(View.GONE);
		} else {
			firstItem.setVisibility(View.GONE);
			libraryItem.setVisibility(View.GONE);
			iPhoneItem.setFocusable(true);
			iPhoneItem.setVisibility(View.VISIBLE);
		}
	}
	public void onDisconnect()
	{
		musicHelper.onDisconnect();
		if(!MusicHelper.iosMode()){
			firstItem.setFocusable(false);
			firstItem.setVisibility(View.VISIBLE);
			libraryItem.setFocusable(false);
			libraryItem.setVisibility(View.VISIBLE);
			iPhoneItem.setVisibility(View.GONE);
		} else {
			firstItem.setVisibility(View.GONE);
			libraryItem.setVisibility(View.GONE);
			iPhoneItem.setFocusable(false);
			iPhoneItem.setVisibility(View.VISIBLE);
		}
	}

	public void showPlayerInfo(PlayerInfo playerInfo) {	
		if(musicHelper.songLoaded()){
			noSong.setVisibility(View.GONE);
			songInfo.setVisibility(View.VISIBLE);

			if(playerInfo.state==PlayerState.PLAYING)
				playerStateTV.setText("Music Playing");
			else if(playerInfo.state==PlayerState.PAUSED)
				playerStateTV.setText("Music Paused");

			showSong(MusicDBFrontEnd.getSongFromId(this, playerInfo.song.songId));

		} else {
			noSong.setVisibility(View.VISIBLE);
			songInfo.setVisibility(View.GONE);
		}
	}
	public void showSong(ReconSong song)
	{
		//ios mode
		if(songInfo==null) return;

		if(song!=null){
			artistNameTV.setText((song.artist != null) ? song.artist : "");
			albumNameTV.setText((song.album != null) ? song.album : "");
			songNameTV.setText((song.title != null) ? song.title : "");
		} else {
			Log.d(TAG, "song is null!");
			artistNameTV.setText("");
			albumNameTV.setText("");
			songNameTV.setText("");	
		}
	}

	public void openLibrary(){
		Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.library.LibraryActivity.class);
		startActivity(nextIntent);
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
	}
	public void selectPlayerButton(){
		if(musicHelper.isReady()){
			if(musicHelper.songLoaded()){
				Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class);
				startActivity(nextIntent);
				overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
			} else {
				SongInfo list = new SongInfo("-1",MusicListType.SONGS,"-1");
				Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("shuffle",MusicHelper.SongInfoToBundle(list));
				startActivity(nextIntent);
				overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
			}
		}
	}

	public boolean requiresAndroid(){
		return false;
	}
	public View getNoConnectOverlay()
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		return inflater.inflate(R.layout.activity_pre_pair_dash, null); 
	}
	public View getNoConnectSetupButton(View overlay)
	{
		return overlay.findViewById(R.id.setup_item);
	}
	public View getNoConnectNoShowButton(View overlay)
	{
		return overlay.findViewById(R.id.no_show_item);
	}
	public View getAndroidOverlay()
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View overlay = (View) inflater.inflate(R.layout.activity_android_overlay, null);
		if (mIsOakley) {
		    TextView conn = (TextView) findViewById(R.id.activity_android_overlay_text);
		    conn.setText(R.string.android_overlay_oakley);
		}
		return overlay;
	}
	public View getIOSOverlay()
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		return inflater.inflate(R.layout.activity_ios_overlay, null);
	}
	public View getIOSConnectButton(View overlay)
	{
		return overlay.findViewById(R.id.connect);
	}
}
