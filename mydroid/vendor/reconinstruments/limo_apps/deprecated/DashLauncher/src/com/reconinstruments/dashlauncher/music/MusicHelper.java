package com.reconinstruments.dashlauncher.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.DashLauncherApp;
import com.reconinstruments.dashlauncher.HUDServiceHelper;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage;
import com.reconinstruments.modlivemobile.dto.message.SongMessage;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.PlayerInfo;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.PlayerState;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.SongInfo;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage.Type;
import com.reconinstruments.modlivemobile.music.MusicDBContentProvider;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.modlivemobile.music.ReconMediaData.ReconSong;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath;

public class MusicHelper {
	private static final String TAG = "MusicHelper";

	public interface MusicInterface{
		public void showPlayerInfo(PlayerInfo playerInfo);

		public void showSong(ReconSong song);
	}


	// HACK bundlizer
	public static final Bundle SongInfoToBundle(SongInfo si) {
		Bundle b = new Bundle();
		b.putString("songId",si.songId);
		b.putSerializable("srcType",si.srcType);
		b.putString("srcId",si.srcId);
		return b;
	}
	public static final SongInfo BundleToSongInfo(Bundle b) {
		SongInfo si = new SongInfo(b.getString("songId"),(MusicListType)b.getSerializable("srcType"),b.getString("srcId"));
		return si;
	}

	//End hack

	public enum Error{
		NONE,
		CANTXML,
		NOLIBRARY,
		NOSTATE,
		BLEERROR
	};
	String[] errorMessages = new String[]{
			"",
			"Waiting for File Transfer to finish",
			"No music detected",
			"Waiting for player",
			"Messaging service not ready"
	};
	public String getErrorMessage(Error error){
		return errorMessages[error.ordinal()];
	}

	public static MusicMessage queuedMessage = new MusicMessage(new PlayerInfo(null,null,null,null,null,null, null),Type.CONTROL);
	public static PlayerInfo playerInfo = new PlayerInfo(PlayerState.NOT_INIT,null,null,null, null, null, null);

	// just for ios
	public static ReconSong song = null;

	public static Boolean hasLibrary = null;

	Error error = Error.NONE;	
	FrameLayout mainView = null;	
	View overlay = null;	
	MusicInterface context;

	static Boolean IOS = null;


	public Activity getActivity(){
		return (Activity)context;
	}
	public MusicHelper(final MusicInterface context){
		this.context = context;
		this.mainView = (FrameLayout) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

		getActivity().registerReceiver(phoneConnectionReceiver, new IntentFilter(XMLMessage.MUSIC_MESSAGE));
		getActivity().registerReceiver(phoneConnectionReceiver, new IntentFilter(XMLMessage.SONG_MESSAGE));
		getActivity().registerReceiver(phoneConnectionReceiver, new IntentFilter(BTCommon.MSG_FILE_FINISH));

		IOS = (SmartphoneConnector.lastDevice()==DeviceType.IOS);
	}

	public void getState(){
	    //		if (MusicActivity.DEBUG) Log.d(TAG, "getState() called : " + (hasLibrary?"DATABASE LOADED":"NO DATABASE"));
		if(hasLibrary){
			updatePlayer(new MusicMessage(MusicMessage.Action.GET_PLAYER_STATE));	
		}
	}

	public static ReconSong getSong(Context context){
		if(playerInfo!=null&&playerInfo.song!=null){
			return MusicDBFrontEnd.getSongFromId(context, playerInfo.song.songId);
		}
		return song;
	}

	// helper for dashboard
	public static ReconSong getSong(Context context,String action,String xml){
		if(action.equals(XMLMessage.MUSIC_MESSAGE)){
			MusicMessage message = new MusicMessage(xml);
			if(message.type==MusicMessage.Type.STATUS&&message.info.song!=null){

				if(message.info.state!=PlayerState.PLAYING) return null;

				return MusicDBFrontEnd.getSongFromId(context, message.info.song.songId);
			}
		}
		else if(action.equals(XMLMessage.SONG_MESSAGE)){
			return SongMessage.getSong(xml);
		}
		return null;
	}

	// handles a delayed message to check whether iphone database can be loaded
	Handler libraryLoadedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			//			if (MusicActivity.DEBUG) Log.d(TAG, "libraryLoadedHandler -> handleMessage " + (hasLibrary?"DATABASE LOADED":"NO DATABASE"));
			checkLibrary();
		}
	};

	public void checkLibrary(){

		MusicDBContentProvider.openOrCreateDatabase(getActivity());
		Cursor cursor = getActivity().getContentResolver().query(MusicDBContentProvider.CONTENT_URI, null, null, null, null);
		hasLibrary = cursor!=null;
		
		if(hasLibrary){
			getState();
			
			// JIRA: MODLIVE-688 Music app reads "No Library detected" when MOD Live connectes with Engage Android
			Log.d(TAG, "Music Library is not empty, the Overlay is going to be hided.");
			// End of JIRA: MODLIVE-688
			
			hideOverlay();
		}
	}
	public void showOverlay(Error error){
		if(overlay!=null){
			if(this.error!=error){
				hideOverlay();
			}
		}
		if(overlay==null){
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
			overlay = inflater.inflate(R.layout.music_phone_waiting, null); 

			mainView.addView(overlay, new LinearLayout.LayoutParams(mainView.getLayoutParams().width, mainView.getLayoutParams().height));
			TextView messageTV = (TextView)overlay.findViewById(R.id.text);
			messageTV.setText(getErrorMessage(error));
			this.error = error;
		}
	}
	public void hideOverlay(){
		if(overlay!=null){
			mainView.removeView(overlay);
			overlay = null;
			error = Error.NONE;
		}
	}
	public SongInfo getRandomSong(MusicListType srcType,String srcId){
		Cursor c = MusicDBFrontEnd.getCursor(getActivity(), srcType, srcId);
		if(c!=null){
			int randomSong = (int) ((Math.random()*c.getCount())-1);
			if(c.moveToPosition(randomSong)){
				String songId = ((ReconSong)MusicDBFrontEnd.getSong.read(c)).id;
				return new SongInfo(songId,srcType,srcId);
			}
			c.close();
		} 
		return null;
	}
	public void updatePlayer(MusicMessage message){
		updateLocalState(message);
		tryAndSendMessage();
	}
	public void tryAndSendMessage(){
		try
		{	
			if(DashLauncherApp.instance.isIPhoneConnected()){
				if(DashLauncherApp.instance.bleService.ifCanSendXml()){
					hideOverlay();
					if(DashLauncherApp.instance.bleService.ifCanSendMusicXml()){
						sendMessage();
					} else {
						DashLauncherApp.instance.bleService.incrementFailedPushCounter();
						sendHandler.removeMessages(0);
						sendHandler.sendEmptyMessageDelayed(0, 2000);
					}
				} else {
					showOverlay(Error.CANTXML);
				}
			} else {
				sendMessage();
			}
		} catch (RemoteException e){
			e.printStackTrace();
			showOverlay(Error.BLEERROR);
		}
	}
	Handler sendHandler = new Handler(){
		@Override
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			tryAndSendMessage();
		}
	};
	public void updateLocalState(MusicMessage message){

		// START_SONG overrides other actions, ie don't do anything else while waiting to start a song
		if(queuedMessage.action==null||!(queuedMessage.action==MusicMessage.Action.START_SONG))
			queuedMessage.action = message.action;

		if(message.info!=null){
			queuedMessage.info.update(message.info);
			playerInfo.update(queuedMessage.info);

			// hack. don't send state, only actions
			queuedMessage.info.state = null;
		}
		context.showPlayerInfo(playerInfo);
	}
	public void sendMessage(){
		sendHandler.removeMessages(0);
		BTCommon.broadcastMessage(DashLauncherApp.getInstance(), queuedMessage.toXML());
//		HUDServiceHelper.getInstance(DashLauncherApp.getInstance().getApplicationContext()).broadcastMessage(DashLauncherApp.getInstance(), queuedMessage.toXML());
		// if we sent a next / prev song the current song is unknown
		if(queuedMessage.action==MusicMessage.Action.NEXT_SONG||queuedMessage.action==MusicMessage.Action.PREVIOUS_SONG){
			playerInfo.song = new SongInfo("",null,"");
		}
		// clear queue
		queuedMessage.info = new PlayerInfo(null,null, null, null, null, null, null);
		queuedMessage.action = null;
	}

	public static int getVolumeInt(){
		// song only mode, (IOS)
		if(playerInfo==null)return 0;

		// set default volume to 0.5f
		if(playerInfo.volume==null)playerInfo.volume = 0.5f;

		return (int) Math.round(playerInfo.volume*10);
	}
	public static boolean iosMode(){
		if(IOS==null) IOS = (SmartphoneConnector.lastDevice()==DeviceType.IOS);
		return IOS;
	}

	public static void gotPlayerInfo(PlayerInfo newInfo)
	{
		playerInfo.update(newInfo);
		song = null;
	}
	public static void gotSong(ReconSong newSong)
	{
		song = newSong;
		playerInfo = null;
	}
	// returns if ready
	public void onConnect()
	{
		Log.d(TAG, "onConnect()");
		IOS = (SmartphoneConnector.lastDevice()==DeviceType.IOS);
		if(!DashLauncherApp.getInstance().isIPhoneConnected()&&!IOS){
			// if we have never checked the library
//			if (hasLibrary==null)
//				checkLibrary();
			// rather just check all the time
			checkLibrary();
			
			if(!hasLibrary) showOverlay(Error.NOLIBRARY);
		}
	}
	public void onDisconnect() {
		if(!IOS){
			playerInfo = new PlayerInfo(PlayerState.NOT_INIT,null,0.5f,null,null,null,null);
			context.showPlayerInfo(playerInfo);
		}
	}
	public boolean isReady(){
		return hasLibrary!=null&&hasLibrary;//&&playerInfo!=null;
	}
	public boolean songLoaded(){
		return (playerInfo.state==PlayerState.PLAYING||playerInfo.state==PlayerState.PAUSED)&&playerInfo.song!=null;
	}

	public static void setInMusicApp(boolean inMusic){
		try{
			DashLauncherApp.instance.bleService.setInMusicApp(inMusic);
		} catch (RemoteException e){
			e.printStackTrace();
		} catch (NullPointerException n){
			n.printStackTrace();
		}
	}

	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			if(intent.getAction().equals(XMLMessage.MUSIC_MESSAGE)){
				String msg = intent.getStringExtra("message");
				MusicMessage message = new MusicMessage(msg);
				if(message.type==MusicMessage.Type.STATUS){
					Log.d(TAG, "got music status: "+msg);

					gotPlayerInfo(message.info);

					context.showPlayerInfo(playerInfo);
				}
			} else if (intent.getAction().equals(BTCommon.MSG_FILE_FINISH)) {
				if(intent.getStringExtra("error").equals("SAVED")){
					FilePath file = (FilePath) intent.getSerializableExtra("savedFile");
					if(file.path.endsWith("reconmusic.db")){
					    //						if (MusicActivity.DEBUG) Log.d(TAG, "phoneConnectionReceiver:BTCommon.MSG_FILE_FINISHED -> reconmusic.db saved");
						checkLibrary();
					}
					else if(file.path.endsWith("reconmusic.db.gz")){
					    //						if (MusicActivity.DEBUG) Log.d(TAG, "phoneConnectionReceiver:BTCommon.MSG_FILE_FINISHED -> reconmusic.db.gz saved");
						// after a second check to see if the database is there
						libraryLoadedHandler.sendEmptyMessageDelayed(0, 1000);
					}
				}
			} else if (intent.getAction().equals(XMLMessage.SONG_MESSAGE)) {
				String msg = intent.getStringExtra("message");
				song = SongMessage.getSong(msg);
				gotSong(song);

				context.showSong(song);
			}
		}
	};
	public void onDestroy()
	{
		getActivity().unregisterReceiver(phoneConnectionReceiver);
	}

	public static boolean IOSKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:	
			Log.d(TAG, "play/pause");
			bleCommandWithCheck(MUSIC_TOGGLE_PLAY_PAUSE);
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			Log.d(TAG, "prev");
			bleCommandWithCheck(MUSIC_PREVIOUS_TRACK);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			Log.d(TAG, "next");
			bleCommandWithCheck(MUSIC_NEXT_TRACK);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			Log.d(TAG, "vol up");
			bleCommandWithCheck(MUSIC_VOLUME_UP);
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			Log.d(TAG, "vol down");
			bleCommandWithCheck(MUSIC_VOLUME_DOWN);
			return true;
		}
		return false;
	}
	private static final byte MUSIC_NEXT_TRACK = 0x01;
	private static final byte MUSIC_PREVIOUS_TRACK = 0x02;
	private static final byte MUSIC_VOLUME_UP = 0x03;
	private static final byte MUSIC_VOLUME_DOWN = 0x04;
	private static final byte MUSIC_TOGGLE_PLAY_PAUSE = 0x05;
	private static void bleCommandWithCheck(byte b) {
		try {
			if (DashLauncherApp.getInstance().bleService != null) {
				DashLauncherApp.getInstance().bleService.sendControlByte(b);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
