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
import android.view.Gravity;
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
import com.reconinstruments.dashlauncher.music.SimplifiedMusicHelper.MusicInterface;
import com.reconinstruments.dashmusic.DashLauncherApp;
import com.reconinstruments.dashmusic.MusicActivity;
import com.reconinstruments.dashmusic.R;


/**
 *  <code>SimplifiedMusicControllerActivity</code> is invokded when a
 *  thirparty app wants to launch a quick music contro screen. The
 *  <code>SimplifiedMusicControllerActivity</code> is not a stateful
 *  controller as is only meant for easy music volume controll and
 *  switch. Becasue it is simple it doesn't require much boilerplate
 *  code or interaction with other components of the system.
 *
 */
public class SimplifiedMusicControllerActivity extends Activity implements MusicInterface {

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
    SimplifiedMusicHelper musicHelper;

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
	getWindow().setGravity(Gravity.CENTER);
	getWindow().setBackgroundDrawableResource(android.R.color.transparent); //removes window border
	setContentView(R.layout.music_controller_simplified);
	setupViews();
	createdUptime = SystemClock.uptimeMillis();
	musicHelper = new SimplifiedMusicHelper(this);
    }

    private boolean buttonReleased = false;
    @Override
    public void onResume() {
	super.onResume();
	buttonReleased = false;
	
    }

    public boolean onKeyLongPressed(){
	return false;
    }
	
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (!buttonReleased) return true;
	switch (keyCode) {
	case KeyEvent.KEYCODE_ENTER:
	case KeyEvent.KEYCODE_DPAD_CENTER:
	    pressView(playPauseIcon);
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
	return super.onKeyDown(keyCode, event);
    }
	
    public boolean onKeyUp(int keyCode, KeyEvent event) {
	int volume;
	if (!buttonReleased) {
	    buttonReleased = true;
	    return true;
	}
	switch (keyCode) {
	case KeyEvent.KEYCODE_ENTER:
	case KeyEvent.KEYCODE_DPAD_CENTER:
	    musicHelper.updatePlayer(new MusicMessage(MusicMessage.Action.TOGGLE_PAUSE));
	    unpressView(playPauseIcon);
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
		SimplifiedMusicHelper.playerInfo.volume = ((float)volume-1)/10;
		if(volumeBar != null)
		    volumeBar.invalidate();
	    }
	    musicHelper.updatePlayer(new MusicMessage(new PlayerInfo(null,null,SimplifiedMusicHelper.playerInfo.volume,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.VOLUME_DOWN));
	    unpressView(volDownIcon);
	    return true;
	case KeyEvent.KEYCODE_DPAD_UP:
	    volume = MusicHelper.getVolumeInt();
	    if(volume<10){
		MusicHelper.playerInfo.volume = ((float)volume+1)/10;
		SimplifiedMusicHelper.playerInfo.volume = ((float)volume+1)/10;
		if(volumeBar != null)
		    volumeBar.invalidate();
	    }
	    musicHelper.updatePlayer(new MusicMessage(new PlayerInfo(null,null,MusicHelper.playerInfo.volume,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.VOLUME_UP));
	    unpressView(volUpIcon);
	    return true;
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
	
	
    @Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(0, R.anim.dock_bottom_exit);
		super.onBackPressed();
	}

	public void showPlayerInfo(PlayerInfo playerInfo) {
	return;
    }

    public void showSong(ReconSong song) {
	return;
    }
}
