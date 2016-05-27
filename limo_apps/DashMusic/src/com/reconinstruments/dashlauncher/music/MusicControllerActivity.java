package com.reconinstruments.dashlauncher.music;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;

import com.reconinstruments.connect.messages.MusicMessage;
import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.messages.MusicMessage.PlayerState;
import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.music.MusicDBFrontEnd;
import com.reconinstruments.connect.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.connect.music.ReconMediaData.ReconSong;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashlauncher.music.MusicHelper.MusicInterface;
import com.reconinstruments.dashmusic.DashLauncherApp;
import com.reconinstruments.dashmusic.MusicActivity;
import com.reconinstruments.dashmusic.R;

public class MusicControllerActivity extends Activity implements SmartphoneInterface, MusicInterface {

	protected static final String TAG = "MusicControllerActivity";

	private TextView songNameTV, artistNameTV, albumNameTV;
	private ImageView playPauseIcon;
	private ImageView nextIcon;
	private ImageView prevIcon;
	private ImageView volUpIcon;
	private ImageView volDownIcon;
	private VolumeBarView volumeBar;
	//private int songId = -1;
	private RelativeLayout controllerLayout;

	private LinearLayout songInfoLayout;
	private LinearLayout noSongLayout;
	private ImageView shuffleIcon;
	private ImageView loopIcon;

	public long createdUptime;
	SmartphoneConnector connector;
	MusicHelper musicHelper;

    private boolean wasIOS = false;

    private void setupViews() {
	songInfoLayout = (LinearLayout) findViewById(R.id.song_info);
	noSongLayout = (LinearLayout) findViewById(R.id.no_song);

	playPauseIcon = (ImageView) findViewById(R.id.play_pause_icon);
	nextIcon = (ImageView) findViewById(R.id.next_icon);
	prevIcon = (ImageView) findViewById(R.id.prev_icon);
	volUpIcon = (ImageView) findViewById(R.id.volup_icon);
	volDownIcon = (ImageView) findViewById(R.id.voldown_icon);
		
	shuffleIcon = (ImageView) findViewById(R.id.shuffle_icon);
	loopIcon = (ImageView) findViewById(R.id.loop_icon);

	controllerLayout = (RelativeLayout) findViewById(R.id.controller);
	//controllerLayout.setBackgroundDrawable(new ControllerDrawable());

	songNameTV = (TextView) findViewById(R.id.song_name);
	artistNameTV = (TextView) findViewById(R.id.artist_name);
	albumNameTV = (TextView) findViewById(R.id.album_name);

	volumeBar = (VolumeBarView) findViewById(R.id.volume_seekbar);
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_controller_main);

		setupViews();
		createdUptime = SystemClock.uptimeMillis();
		
		// the order of connector helper is important
		connector = new SmartphoneConnector(this);
		musicHelper = new MusicHelper(this);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		
		
	}

    private void layoutSwap() {
	if (musicHelper.iosMode() && !wasIOS) {
	    setContentView(R.layout.music_controller_main_ble_ios);
	    setupViews();
	} else if (!musicHelper.iosMode() && wasIOS) {
	    setContentView(R.layout.music_controller_main);
	    setupViews();
	}
	wasIOS = musicHelper.iosMode();

    }
	public void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		DashLauncherApp.getInstance().setInMusicApp(true);
		//musicHelper.getState();
		showSong(MusicHelper.getSong(this));
		layoutSwap();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		DashLauncherApp.getInstance().setInMusicApp(false);
	}
	public void onDestroy() {
		Log.d(TAG,"onDestroy()");
		super.onDestroy();
		connector.onDestroy();
		musicHelper.onDestroy();
	}
	public void onConnect()
	{	
		musicHelper.onConnect();
		layoutSwap();
		if(!musicHelper.iosMode()) {
	
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
		} else {	// in ios Mode
			playPauseIcon.setImageResource(R.drawable.music_icon_play_pause);
			noSongLayout.setVisibility(View.GONE);
			songInfoLayout.setVisibility(View.VISIBLE);
		}
	}
	public void onDisconnect(){
		// since we do not have a proper overlay for this activity when there is no connection
		finish();
		
		// TODO: uncomment old code to display no connection overlay
//		musicHelper.onDisconnect();
//		if(!MusicHelper.iosMode()){
//			songInfoLayout.setVisibility(View.GONE);
//			noSongLayout.setVisibility(View.GONE);
//		}
		
	}
	// Override default android activity transitions for when this activity is hidden/dismissed
	@Override
	public void onBackPressed() {
	    //super.onBackPressed();
		Log.d(TAG,"on back pressed");
		launchMusicActivity();
	}
	
	private void launchMusicActivity(){
		Intent intent = new Intent(MusicControllerActivity.this, MusicActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
		finish();
	}
	
	public boolean onKeyLongPressed(){
		return false;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				pressView(null);
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				pressView(prevIcon);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				pressView(nextIcon);
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				pressView(volDownIcon);
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
				pressView(volUpIcon);
				return true;
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    MusicHelper.logFunctionName(TAG);
		// ignore keys pressed before the activity was created (long press)
		if(event.getDownTime()<createdUptime){
			return false;
		}

		// Handle iOS Case
		if(musicHelper.iosMode()) {
		    Log.v(TAG,"iosMode()");
		    if(!musicHelper.IOSKeyUp(keyCode, event)) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
			    launchMusicActivity();
				return true;
			}
		    }
		    return super.onKeyUp(keyCode, event);
		}
		else {//Normal mode
		    if (keyCode == KeyEvent.KEYCODE_BACK) {
			Log.v(TAG,"just go back don't even think");
			launchMusicActivity();
			return true;
		    }
		}

		Log.v(TAG,"noneiosMode()");
		if(!musicHelper.isReady()||!connector.isConnected())  {
		    launchMusicActivity();
			return super.onKeyUp(keyCode, event);
		}

		if(!musicHelper.songLoaded()){
			if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER){
				SongInfo song = musicHelper.getRandomSong(MusicListType.SONGS, "-1");
				PlayerInfo newInfo = new PlayerInfo(PlayerState.PLAYING,song, null, null, true, null, null);
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.START_SONG,newInfo));
				//musicHelper.gotPlayerInfo(newInfo);
				return true;
			}
			else if(keyCode==KeyEvent.KEYCODE_BACK){
				launchMusicActivity();
				return true;
			}
		}
		else if (event.getAction() == KeyEvent.ACTION_UP) {
			int volume;
			switch (keyCode) {
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:

				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.TOGGLE_PAUSE));
				unpressView(null);
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.PREVIOUS_SONG));
				unpressView(prevIcon);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.NEXT_SONG));
				unpressView(nextIcon);
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:

				volume = MusicHelper.getVolumeInt();
				if(volume>0){
					MusicHelper.playerInfo.volume = ((float)volume-1)/10;
					if(volumeBar != null)
					volumeBar.invalidate();
				}
				musicHelper.updatePlayer(new MusicMessage(new PlayerInfo(null,null,MusicHelper.playerInfo.volume,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.VOLUME_DOWN));

				//musicHelper.sendMusicMessage(new MusicMessage(MusicMessage.Action.VOLUME_DOWN).toXML());
				unpressView(volDownIcon);
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:

				volume = MusicHelper.getVolumeInt();
				if(volume<10){
					MusicHelper.playerInfo.volume = ((float)volume+1)/10;
					if(volumeBar != null)
					volumeBar.invalidate();
				}
				musicHelper.updatePlayer(new MusicMessage(new PlayerInfo(null,null,MusicHelper.playerInfo.volume,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.VOLUME_UP));

				//musicHelper.sendMusicMessage(new MusicMessage(MusicMessage.Action.VOLUME_UP).toXML());
				unpressView(volUpIcon);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	private void unpressView(View v){
		if(v==null)
			return;
		
		v.clearFocus();
		v.invalidate();
	}
	private void pressView(View v){
		if(v==null)
			return;
		
		if(!v.isPressed())
		{
		v.requestFocus();
		v.setPressed(true);
		v.invalidate();
		}
	}
	
	
	public void showPlayerInfo(PlayerInfo playerInfo) {
	    MusicHelper.logFunctionName(TAG);
		Log.v(TAG, "showPlayerInfo");
		if (musicHelper.iosMode()) {//HACK
		    noSongLayout.setVisibility(View.GONE);
		    songInfoLayout.setVisibility(View.VISIBLE);
		    return;
		}

		if(!musicHelper.songLoaded()) {
			songInfoLayout.setVisibility(View.GONE);
			noSongLayout.setVisibility(View.VISIBLE);
			return;
		} else {
			songInfoLayout.setVisibility(View.VISIBLE);
			noSongLayout.setVisibility(View.GONE);
			if(playerInfo.loop==null)
				loopIcon.setVisibility(View.GONE);
			else if(playerInfo.loop)
				loopIcon.setVisibility(View.VISIBLE);
			else
				loopIcon.setVisibility(View.GONE);
			
			if(playerInfo.shuffle==null)
				shuffleIcon.setVisibility(View.GONE);
			else if(playerInfo.shuffle)
				shuffleIcon.setVisibility(View.VISIBLE);
			else
				shuffleIcon.setVisibility(View.GONE);
			

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
		//controllerLayout.invalidate();
	}
	public View getNoConnectOverlay(){
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		return inflater.inflate(R.layout.activity_pre_pair_dash, null); 
	}

	public View getNoConnectSetupButton(View overlay){
		return overlay.findViewById(R.id.setup_item);
	}

	public void showSong(ReconSong song)
	{
	    MusicHelper.logFunctionName(TAG); 
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
		//controllerLayout.invalidate();
	}

	@Override
	public SmartphoneConnector getConnector() {
		return connector;
	}

	@Override
	public TextView getNoConnectSetupTitle(View overlay) {
		return (TextView) overlay.findViewById(R.id.setup_text);
	}
	
	@Override
	public TextView getNoConnectSetupInfo(View overlay) {
		return (TextView) overlay.findViewById(R.id.setup_info);
	}

	@Override
	public TextView getNoConnectSetupConnectText(View overlay) {
		return (TextView) overlay.findViewById(R.id.setup_music_button_text);
	}
}
