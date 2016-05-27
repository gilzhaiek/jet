package com.reconinstruments.dashlauncher.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
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

import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.connect.messages.MusicMessage;
import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.messages.MusicMessage.PlayerState;
import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.messages.MusicMessage.Type;
import com.reconinstruments.connect.messages.SongMessage;
import com.reconinstruments.connect.messages.XMLMessage;
import com.reconinstruments.connect.music.MusicDBContentProvider;
import com.reconinstruments.connect.music.MusicDBFrontEnd;
import com.reconinstruments.connect.music.MusicDBFrontEnd.MusicListType;
import com.reconinstruments.connect.music.ReconMediaData.ReconSong;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.dashlauncher.connect.SmartphoneInterface;
import com.reconinstruments.dashmusic.R;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;

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

		IOS = iosMode();
	}

	public void getState(){
		//		if (MusicActivity.DEBUG) Log.d(TAG, "getState() called : " + (hasLibrary?"DATABASE LOADED":"NO DATABASE"));
		if(hasLibrary){
			updatePlayer(new MusicMessage(MusicMessage.Action.GET_PLAYER_STATE));	
		}
	}

	public static ReconSong getSong(Context context){
	    	    MusicHelper.logFunctionName(TAG);
		if(playerInfo!=null&&playerInfo.song!=null){
			return MusicDBFrontEnd.getSongFromId(context, playerInfo.song.songId);
		}
		return null;
	}

	// helper for dashboard
	public static ReconSong getSong(Context context,String action,String xml){
	    	    MusicHelper.logFunctionName(TAG);
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
	MusicHelper.logFunctionName(TAG);
	SmartphoneConnector ctor = ((SmartphoneInterface) getActivity()).getConnector();	
	if (iosMode()) {	// Means CONNECTED to iPhone
	    // just hide it right off the bat
	    hideOverlay();
	    return;
	} else if (!ctor.isConnected()) {
	    Log.v(TAG,"Nothing is connected");
	    ctor.showNewConnectOverlay();
	    return;	    	// Keeps the overlayon
	}
	MusicDBContentProvider.openOrCreateDatabase(getActivity());
	Cursor cursor = getActivity().getContentResolver().query(MusicDBContentProvider.CONTENT_URI, null, null, null, null);
	hasLibrary = cursor!=null;

	if(hasLibrary){
	    cursor.close();
			
	    getState();

	    // JIRA: MODLIVE-688 Music app reads "No Library detected" when MOD Live connectes with Engage Android
	    Log.d(TAG, "Music Library is not empty, the Overlay is going to be hided.");
	    // End of JIRA: MODLIVE-688

	    hideOverlay();
	}
	else {
	    IBLEService bleService = ctor.bleService;
	    try	{
		Log.v(TAG,"connection state is "+ctor.isIPhoneConnected(bleService));
		Log.v(TAG,"can send xml state is "+bleService.ifCanSendXml());
		if(ctor.isIPhoneConnected(bleService) && bleService.ifCanSendXml()){
		    hideOverlay();
		} 
	    }
	    catch (RemoteException e){
		e.printStackTrace();
		ctor.showNewConnectOverlay();
	    }
	}
    }
	
	// checks if we have an empty DB
	public boolean checkIfLibraryEmpty(){
	    	    MusicHelper.logFunctionName(TAG);
		MusicDBContentProvider.openOrCreateDatabase(getActivity());
		SQLiteDatabase database = MusicDBContentProvider.getDatabase();
		Cursor cursor = null;
		
		if(database!=null)
			cursor = database.query(MusicDBContentProvider.MEDIA_TABLE, null, null, null, null, null, null);
		
		if (cursor!=null){
			boolean empty = cursor.getCount()==0;
			cursor.close();
			Log.d(TAG,"library is empty? "+empty);
			return empty;}
		else{
			Log.d(TAG,"cursor is null, no library");
			return false;
		}
	}
	
	public void showOverlay(Error error){
	    	    MusicHelper.logFunctionName(TAG);
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
	    	    MusicHelper.logFunctionName(TAG);
		if(overlay!=null){
		    Log.v(TAG,"overlay is not null");
			mainView.removeView(overlay);
			overlay = null;
			error = Error.NONE;
		}
		else {
		    Log.v(TAG,"overlay is null");
		}
		Log.v(TAG,"trying connector overlay too");
		SmartphoneConnector ctor = ((SmartphoneInterface) getActivity()).getConnector();
		ctor.hideOverlay();
	}
	public SongInfo getRandomSong(MusicListType srcType,String srcId){
	    	    MusicHelper.logFunctionName(TAG);
		Cursor c = MusicDBFrontEnd.getCursor(getActivity(), srcType, srcId);
		if(c!=null){
			int randomSong = (int) ((Math.random()*c.getCount())-1);
			if(randomSong==0)
				randomSong=1;
			if(c.moveToPosition(randomSong)){
				String songId;
				if(srcType.equals(MusicDBFrontEnd.MusicListType.PLAYLIST_SONGS))
					songId = ((ReconSong)MusicDBFrontEnd.getPlaylistSong.read(c)).id;
				else
					songId = ((ReconSong)MusicDBFrontEnd.getSong.read(c)).id;
				return new SongInfo(songId,srcType,srcId);
			}
			c.close();
		} 
		return null;
	}
	public void updatePlayer(MusicMessage message){
	    	    MusicHelper.logFunctionName(TAG);
		updateLocalState(message);
		tryAndSendMessage();
	}
	public void tryAndSendMessage(){
		SmartphoneConnector ctor = ((SmartphoneInterface) getActivity()).getConnector();
		IBLEService bleService = ctor.bleService;
		try
		{	
			if(ctor.isIPhoneConnected(bleService)){
				if(bleService.ifCanSendXml()){
					hideOverlay();
					if(bleService.ifCanSendMusicXml()){
						sendMessage();
					} else {
						bleService.incrementFailedPushCounter();
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
	    	    MusicHelper.logFunctionName(TAG);

		// START_SONG overrides other actions, ie don't do anything else while waiting to start a song
		if(queuedMessage.action==null||!(queuedMessage.action==MusicMessage.Action.START_SONG))
			queuedMessage.action = message.action;

		if(message.info!=null){
			queuedMessage.info.update(message.info);
			if (playerInfo != null) {
			    playerInfo.update(queuedMessage.info);
			}

			// hack. don't send state, only actions
			queuedMessage.info.state = null;
		}
		if(playerInfo!=null)
			context.showPlayerInfo(playerInfo);
	}
	public void sendMessage(){
	    	    MusicHelper.logFunctionName(TAG);
		sendHandler.removeMessages(0);
		
		//ConnectHelper.broadcastXML(getActivity(), queuedMessage.toXML());
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");
		
		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(XMLMessage.MUSIC_MESSAGE);
		cMsg.setRequestKey(0);
		cMsg.setSender("com.reconinstruments.mobilesdk.mediaplayer.MusicHelper");
		byte[] data = queuedMessage.toXML().getBytes();
		cMsg.setData(data);
		//Log.e(TAG,"size of music status message: "+cMsg.ToByteArray().length);
		i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());
		getActivity().sendBroadcast(i);
		
		// if we sent a next / prev song the current song is unknown
//		if(queuedMessage.action==MusicMessage.Action.NEXT_SONG||queuedMessage.action==MusicMessage.Action.PREVIOUS_SONG){
//			playerInfo.song = new SongInfo("",null,"");
//		}
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
	public boolean iosMode(){
	    	    MusicHelper.logFunctionName(TAG);
	    //TODO This is wrong. FIXME: need to ask the service

	    SmartphoneConnector ctor = ((SmartphoneInterface) getActivity()).getConnector();
	    IBLEService bleService = ctor.bleService;
	    return ctor.isIPhoneConnected(bleService);
	}

	public static void gotPlayerInfo(PlayerInfo newInfo)
	{
	    	    MusicHelper.logFunctionName(TAG);
		    if (playerInfo != null)
			playerInfo.update(newInfo);
		song = null;
	}
	public static void gotSong(ReconSong newSong)
	{
	    	    MusicHelper.logFunctionName(TAG);
		song = newSong;
		playerInfo = null;
	}
	// returns if ready
	public void onConnect()
	{
	    	    	    MusicHelper.logFunctionName(TAG);

			    //		Log.d(TAG, "onConnect()");
		IOS = iosMode();
		SmartphoneConnector ctor = ((SmartphoneInterface) getActivity()).getConnector();
		if(!ctor.isIPhoneConnected(ctor.bleService)&&!IOS){


			// if we have never checked the library
			//			if (hasLibrary==null)
			//				checkLibrary();
			// rather just check all the time
			checkLibrary();

			if(!hasLibrary) showOverlay(Error.NOLIBRARY);
		}
	}
	public void onDisconnect() {
	    MusicHelper.logFunctionName(TAG);
		if(!IOS){
			playerInfo = new PlayerInfo(PlayerState.NOT_INIT,null,0.5f,null,null,null,null);
			context.showPlayerInfo(playerInfo);
		}
	}
	public boolean isReady(){
	    	    MusicHelper.logFunctionName(TAG);
		return hasLibrary!=null&&hasLibrary;//&&playerInfo!=null;
	}
	public boolean songLoaded(){
	    	    MusicHelper.logFunctionName(TAG);
	    return (playerInfo!=null&&(playerInfo.state==PlayerState.PLAYING||playerInfo.state==PlayerState.PAUSED)&&playerInfo.song!=null);
	}
    
    // This broadcast receiver is for song change etc.
	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			if(intent.getAction().equals(XMLMessage.MUSIC_MESSAGE)){
				Bundle b = intent.getExtras();
				if(b!=null){
					//byte[] data = b.getByteArray("message");
					HUDConnectivityMessage cMsg = new HUDConnectivityMessage(b.getByteArray("message"));
					String s = new String(cMsg.getData());
					//Log.e(TAG,"string :"+s);
					
					if(s!=null){
						MusicMessage message = new MusicMessage(s);
						if(message.type==MusicMessage.Type.STATUS){
							Log.d(TAG, "got music status: "+s);

							gotPlayerInfo(message.info);

							context.showPlayerInfo(playerInfo);
						}
					}
				}
			} 
			else if (intent.getAction().equals(XMLMessage.SONG_MESSAGE)) {
				Bundle b = intent.getExtras();
				if (b!= null) {
				    HUDConnectivityMessage cMsg = new HUDConnectivityMessage(b.getByteArray("message"));
				    String msg = new String(cMsg.getData());
				    song = SongMessage.getSong(msg);
				    gotSong(song);
				    context.showSong(song);
				}
				else {
				    Log.w(TAG,"SONG_MESSAGE has no data.");
				}
			}
			else {
			    Log.w(TAG,"Got unknwon message type with action" + intent.getAction());
			}
		}
	};
	public void onDestroy()
	{

		getActivity().unregisterReceiver(phoneConnectionReceiver);
	}

	public boolean IOSKeyUp(int keyCode, KeyEvent event)
	{
	    	    MusicHelper.logFunctionName(TAG);
		switch (keyCode) {
		case KeyEvent.KEYCODE_ENTER:
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
	private void bleCommandWithCheck(byte b) {
		try {
			if (((SmartphoneInterface) getActivity()).getConnector().bleService != null) {
				((SmartphoneInterface) getActivity()).getConnector().bleService.sendControlByte(b);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

    public static final void logFunctionName(String tag) {
	Log.v(tag, "Method name: "+Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}
