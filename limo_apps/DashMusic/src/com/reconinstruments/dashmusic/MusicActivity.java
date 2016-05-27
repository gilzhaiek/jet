package com.reconinstruments.dashmusic;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.messages.MusicMessage.PlayerState;
import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.music.MusicDBFrontEnd;
import com.reconinstruments.connect.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.connect.music.ReconMediaData.ReconSong;
import com.reconinstruments.dashelement1.ColumnElementActivity;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.dashlauncher.music.MusicHelper.MusicInterface;
import com.reconinstruments.dashmusic.R.color;

//connected activity default state is disconnected
public class MusicActivity extends ColumnElementActivity implements SmartphoneInterface, MusicInterface{
	public static final String TAG = "MusicActivity";

	public static final boolean DEBUG = true;

	private TextView leftOptionTv;
	private TextView titleTv;
	private View noSong,songInfo;
	private TextView songNameTV, artistNameTV, albumNameTV;

	SmartphoneConnector connector;
	MusicHelper musicHelper;
	MusicActivitySyncer musicSyncer;


	public void onCreate(Bundle savedInstanceState) {
	    //MusicHelper.logFunctionName(TAG);
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");

		this.setContentView(R.layout.music_main2);
		noSong = (View) findViewById(R.id.no_song);
		songInfo = (View) findViewById(R.id.song_info);

		songNameTV = (TextView) findViewById(R.id.song_name);
		artistNameTV = (TextView) findViewById(R.id.artist_name);
		albumNameTV = (TextView) findViewById(R.id.album_name);

		// this is the only text view that needs change
		titleTv = (TextView) findViewById(R.id.music_main_title);
		leftOptionTv = (TextView) findViewById(R.id.music_main_shuffle_text);
		View shuffle = findViewById(R.id.music_main_shuffle_songs);
		View lib = findViewById(R.id.music_main_library);
		TextView libTv = (TextView) findViewById(R.id.music_main_library_text);

		// the order of connector helper is important
		connector = new SmartphoneConnector(this);
		musicHelper = new MusicHelper(this);

		setUpMusicSyncReceivers();

	}
	private void setUpMusicSyncReceivers() {
	    //MusicHelper.logFunctionName(TAG);
		musicSyncer = new MusicActivitySyncer(this);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

	}


	@Override
	public void onPause()
	{
		super.onPause();
	}

	public void onResume() {
	    	    //MusicHelper.logFunctionName(TAG);
		super.onResume();

		// JIRA: MODLIVE-688 Music app reads "No Library detected" when MOD Live connectes with Engage Android
		Log.d(TAG, "Check to see if the Overlay should be removed.");
		updateLibrary();
		if (musicHelper.iosMode()){
		    hideLibraryOptionAndMore();
		}
		// if (connector.isConnected()) {
		//     Log.v(TAG,"connected");
		//     if (musicHelper.iosMode()) {
		// 	connector.showOverlay();
		//     }

		// }
		// else{
		//     Log.v(TAG,"disconnected");
		//     connector.onDisconnect();
		// }

		// End of JIRA: MODLIVE-688

	}

	public void onDestroy() {
	    	    //MusicHelper.logFunctionName(TAG);
		super.onDestroy();
		connector.onDestroy();
		musicSyncer.onDestroy(this);
		musicHelper.onDestroy();
	}

    public boolean onKeyUp(int keyCode, KeyEvent event) {
	//MusicHelper.logFunctionName(TAG);

	// Need a separate logic for
	if (musicHelper.iosMode()) {// iOS device connected
	    if (keyCode==KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER) {
		openController();
		return true;
	    }
	    else {
		return super.onKeyUp(keyCode, event);
	    }
	}
	else if (connector.isConnected()) { // none iOS device connected
		if(!musicHelper.songLoaded()) {
		    // screen where connection exists but no song playing
		    if (event.getAction() == KeyEvent.ACTION_UP&&(keyCode==KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER)&&!musicHelper.checkIfLibraryEmpty()&&!musicSyncer.isSyncying()) {
			SongInfo list = new SongInfo("-1",MusicListType.SONGS,"-1");
			Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class).putExtra("shuffle",MusicHelper.SongInfoToBundle(list));
			startActivity(nextIntent);
			overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
			return true;
		    }
		    else if(event.getAction() == KeyEvent.ACTION_UP&&keyCode==KeyEvent.KEYCODE_DPAD_DOWN&&!musicSyncer.isSyncying()) {
			openLibrary();
			return true;
		    }
		    else if(event.getAction() == KeyEvent.ACTION_UP&&(keyCode==KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER)&&musicHelper.checkIfLibraryEmpty()) {
			Toast.makeText(MusicActivity.this, "Empty database", Toast.LENGTH_SHORT).show();
			return true;
		    }
		}
		// other screen where connection exists and song info is showing
		if (event.getAction() == KeyEvent.ACTION_UP&&(keyCode==KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER)&&!musicHelper.checkIfLibraryEmpty()&&!musicSyncer.isSyncying()){
		    openController();
		    return true;
		}
		else if(event.getAction() == KeyEvent.ACTION_UP&&keyCode==KeyEvent.KEYCODE_DPAD_DOWN&&!musicSyncer.isSyncying()){
		    openLibrary();
		    return true;
		}
		else if(event.getAction() == KeyEvent.ACTION_UP&&(keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_DPAD_DOWN|| keyCode == KeyEvent.KEYCODE_ENTER)&&musicSyncer.isSyncying()){
		Toast.makeText(MusicActivity.this, "Have not finished syncing the library", Toast.LENGTH_SHORT).show();
		return true;
		}
	}
	else{			// No device connected
	    // not connected
	    if (event.getAction() == KeyEvent.ACTION_UP&&keyCode==KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER) {
		Settings.System.putString(getContentResolver(), "DisableSmartphone", "false");
		startActivityForResult(new Intent("com.reconinstruments.connectdevice.CONNECT"),connector.CONNECT_REQUEST_CODE);
	    }
	}

	// 
	return super.onKeyUp(keyCode, event);

    }

	@Override
	public void onBackPressed() {
		goBack();
	}

	public void onConnect()
	{
	    	    //MusicHelper.logFunctionName(TAG);
		musicHelper.onConnect();
		musicSyncer.setSyncMode(true);
		if(!musicHelper.iosMode()){
			getClientState();
		} else {
		    // We Hide the library options
		    hideLibraryOptionAndMore();
		}
	}
	public void onDisconnect()
	{
	    //MusicHelper.logFunctionName(TAG);
	    musicHelper.onDisconnect();
	    
	}
	public void showPlayerInfo(PlayerInfo playerInfo) {
	    	    //MusicHelper.logFunctionName(TAG);
		if(musicHelper.songLoaded()){
			noSong.setVisibility(View.GONE);
			songInfo.setVisibility(View.VISIBLE);
			leftOptionTv.setText("MUSIC CONTROLS");
			if(playerInfo.state==PlayerState.PLAYING){
				titleTv.setText("NOW PLAYING");
				songNameTV.setTextColor(Color.WHITE);
				artistNameTV.setTextColor(Color.WHITE);
				albumNameTV.setTextColor(Color.WHITE);
			}
			else{
				titleTv.setText("MUSIC PAUSED");
				int grayInt = getResources().getColor(R.color.reconGrayText);
				songNameTV.setTextColor(grayInt);
				artistNameTV.setTextColor(grayInt);
				albumNameTV.setTextColor(grayInt);
			}
			showSong(MusicDBFrontEnd.getSongFromId(this, playerInfo.song.songId));

		} else {
			noSong.setVisibility(View.VISIBLE);
			songInfo.setVisibility(View.GONE);
			leftOptionTv.setText("SHUFFLE SONGS");
			titleTv.setText("MUSIC PLAYER");
		}
	}
	public void showSong(ReconSong song)
	{
	    	    //MusicHelper.logFunctionName(TAG);
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

	public void openController(){
	    	    //MusicHelper.logFunctionName(TAG);
		Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.MusicControllerActivity.class);
		startActivity(nextIntent);
		overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
	}
	
	
	public void openLibrary(){
	    	    //MusicHelper.logFunctionName(TAG);
		Intent nextIntent = new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.music.library.LibraryActivity.class);
		startActivity(nextIntent);
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
	}
	public void updateLibrary(){
	    	    //MusicHelper.logFunctionName(TAG);
		musicHelper.checkLibrary();
	}
	
	public void getClientState(){
	    	    //MusicHelper.logFunctionName(TAG);
		musicHelper.getState();
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

	public View getNoConnectOverlay()
	{
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		return inflater.inflate(R.layout.activity_pre_pair_dash, null); 
	}
	public View getNoConnectSetupButton(View overlay)
	{
		return overlay.findViewById(R.id.setup_item);
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

        @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    switch(keyCode) {
	    case KeyEvent.KEYCODE_DPAD_DOWN:
		return false;
	    }
	return super.onKeyDown(keyCode,event);
	}

    public void hideLibraryOptionAndMore() {
	findViewById(R.id.music_main_library).setVisibility(View.INVISIBLE);
	findViewById(R.id.music_main_library_text).setVisibility(View.INVISIBLE);
	leftOptionTv.setText("MUSIC CONTROLS");
	noSong.setVisibility(View.INVISIBLE);
    }

}
