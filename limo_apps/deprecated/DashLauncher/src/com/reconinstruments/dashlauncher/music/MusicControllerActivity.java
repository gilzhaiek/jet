package com.reconinstruments.dashlauncher.music;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashlauncher.music.MusicHelper.MusicInterface;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.PlayerInfo;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.PlayerState;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.SongInfo;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.modlivemobile.music.ReconMediaData.ReconSong;

public class MusicControllerActivity extends Activity implements SmartphoneInterface, MusicInterface {

	protected static final String TAG = "MusicControllerActivity";

    protected boolean mIsOakley = false;
    protected static final String SECRECT_FILE = "/OakleyFile"; 

    protected void setIsOakley () {
		File OakleyFile = new File(SECRECT_FILE);
		mIsOakley = OakleyFile.exists();
    }

	private TextView songNameTV, artistNameTV, albumNameTV;
	private ImageView playPauseIcon;
	//private int songId = -1;
	private VolumeView mVolumeView;
	private RelativeLayout controllerLayout;

	private LinearLayout songInfoLayout;
	private LinearLayout noSongLayout;

	public long createdUptime;

	SmartphoneConnector connector;
	MusicHelper musicHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		this.setContentView(R.layout.music_controller_main);

		songInfoLayout = (LinearLayout) findViewById(R.id.song_info);
		noSongLayout = (LinearLayout) findViewById(R.id.no_song);


		mVolumeView = (VolumeView) findViewById(R.id.volume);
		playPauseIcon = (ImageView) findViewById(R.id.play_pause_icon);
		controllerLayout = (RelativeLayout) findViewById(R.id.controller);
		controllerLayout.setBackgroundDrawable(new ControllerDrawable());

		songNameTV = (TextView) findViewById(R.id.song_name);
		artistNameTV = (TextView) findViewById(R.id.artist_name);
		albumNameTV = (TextView) findViewById(R.id.album_name);

		createdUptime = SystemClock.uptimeMillis();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		musicHelper = new MusicHelper(this);
		connector = new SmartphoneConnector(this);
	}
	public void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		MusicHelper.setInMusicApp(true);

		showSong(MusicHelper.getSong(this));
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		MusicHelper.setInMusicApp(false);
	}
	public void onDestroy() {
		super.onDestroy();
		connector.onDestroy();
		musicHelper.onDestroy();
	}
	public void onConnect()
	{	
		musicHelper.onConnect();
		if(!MusicHelper.iosMode()){
	
			mVolumeView.setVisibility(View.VISIBLE);
			
			boolean startSong = getIntent().hasExtra("song");
			boolean shuffle = getIntent().hasExtra("shuffle");
			if(startSong){
			    SongInfo song = MusicHelper.BundleToSongInfo(getIntent().getBundleExtra("song"));
				PlayerInfo newInfo = new PlayerInfo(PlayerState.PLAYING,song, null, null, false, null, null);
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.START_SONG,newInfo));
				//musicHelper.gotPlayerInfo(newInfo);
			} 
			else if(shuffle){
			    SongInfo list = MusicHelper.BundleToSongInfo(getIntent().getBundleExtra("shuffle"));
				SongInfo song = musicHelper.getRandomSong(list.srcType, list.srcId);
				PlayerInfo newInfo = new PlayerInfo(PlayerState.PLAYING,song, null, null, true, null, null);
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.START_SONG,newInfo));
				//musicHelper.gotPlayerInfo(newInfo);
			} 
			else if(MusicHelper.hasLibrary){
				musicHelper.getState();
			}
		} else {
			playPauseIcon.setImageResource(R.drawable.music_icon_play_pause);
			noSongLayout.setVisibility(View.GONE);
			songInfoLayout.setVisibility(View.VISIBLE);
		}
	}
	public void onDisconnect(){
		musicHelper.onDisconnect();
		if(!MusicHelper.iosMode()){
			songInfoLayout.setVisibility(View.GONE);
			noSongLayout.setVisibility(View.GONE);
			mVolumeView.setVisibility(View.GONE);
		}
	}
	// Override default android activity transitions for when this activity is hidden/dismissed
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {

		// ignore keys pressed before the activity was created (long press)
		if(event.getDownTime()<createdUptime){
			return false;
		}

		if(connector.isConnected()&&MusicHelper.iosMode()) {
			if(!MusicHelper.IOSKeyUp(keyCode, event))
				return super.onKeyUp(keyCode, event);
		}

		if(!musicHelper.isReady()||!connector.isConnected()) return super.onKeyUp(keyCode, event);

		if(!musicHelper.songLoaded()){
			if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER){
				SongInfo song = musicHelper.getRandomSong(MusicListType.SONGS, "-1");
				PlayerInfo newInfo = new PlayerInfo(PlayerState.PLAYING,song, null, null, true, null, null);
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.START_SONG,newInfo));
				//musicHelper.gotPlayerInfo(newInfo);
				return true;
			}
		}
		else if (event.getAction() == KeyEvent.ACTION_UP) {
			int volume;
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:

				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.TOGGLE_PAUSE));

				/*
				if(MusicHelper.playerInfo.state==PlayerState.PAUSED){
					musicHelper.sendMusicMessage(new MusicMessage(new PlayerInfo(PlayerState.PLAYING,null,null,null,null,null,null),MusicMessage.Type.CONTROL));
					//playPauseIcon.setImageResource(R.drawable.music_icon_play);

					//musicHelper.gotPlayerInfo(new PlayerInfo(PlayerState.PLAYING,null, null, null, null, null, null));

					//MusicHelper.playerInfo.state = PlayerState.PLAYING;
					return true;
				} else if(MusicHelper.playerInfo.state==PlayerState.PLAYING){
					musicHelper.sendMusicMessage(new MusicMessage(new PlayerInfo(PlayerState.PAUSED,null,null,null,null,null,null),MusicMessage.Type.CONTROL));
					//playPauseIcon.setImageResource(R.drawable.music_icon_pause);
					//MusicHelper.playerInfo.state = PlayerState.PAUSED;
				} else{
					// invalid state
					Log.d(TAG, "Invalid player state!");
					musicHelper.getState();
				}*/
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.PREVIOUS_SONG));
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.NEXT_SONG));
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:

				volume = MusicHelper.getVolumeInt();
				if(volume>0){
					MusicHelper.playerInfo.volume = ((float)volume-1)/10;
					//mVolumeView.invalidate();
				}
				musicHelper.updatePlayer(new MusicMessage(new PlayerInfo(null,null,MusicHelper.playerInfo.volume,null,null,null,null),MusicMessage.Type.CONTROL));

				//musicHelper.sendMusicMessage(new MusicMessage(MusicMessage.Action.VOLUME_DOWN).toXML());

				return true;
			case KeyEvent.KEYCODE_DPAD_UP:

				volume = MusicHelper.getVolumeInt();
				if(volume<10){
					MusicHelper.playerInfo.volume = ((float)volume+1)/10;
					//mVolumeView.invalidate();
				}
				musicHelper.updatePlayer(new MusicMessage(new PlayerInfo(null,null,MusicHelper.playerInfo.volume,null,null,null,null),MusicMessage.Type.CONTROL));

				//musicHelper.sendMusicMessage(new MusicMessage(MusicMessage.Action.VOLUME_UP).toXML());

				return true;
			}

		}
		return super.onKeyUp(keyCode, event);
	}

	public void showPlayerInfo(PlayerInfo playerInfo) {
		Log.v(TAG, "showPlayerInfo");

		if(!musicHelper.songLoaded()) {
			songInfoLayout.setVisibility(View.GONE);
			noSongLayout.setVisibility(View.VISIBLE);
			return;
		} else {
			songInfoLayout.setVisibility(View.VISIBLE);
			noSongLayout.setVisibility(View.GONE);

			switch(playerInfo.state){
			case PLAYING:
				playPauseIcon.setImageResource(R.drawable.music_icon_pause);
				break;
			case PAUSED:
				playPauseIcon.setImageResource(R.drawable.music_icon_play);
				break;
			default:
				break;
			}
			showSong(MusicDBFrontEnd.getSongFromId(this, playerInfo.song.songId));
		}
		// for some reason we have to call this to prevent some weird visual issues with the controller layout
		controllerLayout.invalidate();
	}
	public boolean requiresAndroid(){
		return false;
	}
	public View getNoConnectOverlay(){
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		return inflater.inflate(R.layout.activity_pre_pair_music, null); 
	}

	public View getNoConnectSetupButton(View overlay){
		return overlay.findViewById(R.id.setup_item);
	}
	public View getNoConnectNoShowButton(View overlay){
		return null;
	}
	public View getAndroidOverlay(){
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View overlay = (View) inflater.inflate(R.layout.activity_android_overlay, null);
		if (mIsOakley) {
		    TextView conn = (TextView) findViewById(R.id.activity_android_overlay_text);
		    conn.setText(R.string.android_overlay_oakley);
		}

		return overlay;
	}
	public View getIOSOverlay(){
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		return inflater.inflate(R.layout.activity_ios_overlay, null); 
	}
	public View getIOSConnectButton(View overlay){
		return overlay.findViewById(R.id.connect);
	}

	public void showSong(ReconSong song)
	{
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
		// for some reason we have to call this to prevent some weird visual issues with the controller layout
		controllerLayout.invalidate();
	}
}
