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

public class SimplifiedMusicHelper {
    private static final String TAG = "SimplifiedMusicHelper";

    public interface MusicInterface{
	public void showPlayerInfo(PlayerInfo playerInfo);
	public void showSong(ReconSong song);
    }
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

    // For dump music contorl
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
    public SimplifiedMusicHelper(final MusicInterface context){
	this.context = context;
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

    // handles a delayed message to check whether iphone database can be loaded
    Handler libraryLoadedHandler = new Handler(){
	    @Override
	    public void handleMessage(Message msg)
	    {
		super.handleMessage(msg);
		checkLibrary();
	    }
	};

    public void checkLibrary(){
    }
	
    public void showOverlay(Error error){
	MusicHelper.logFunctionName(TAG);
    }
    public void hideOverlay(){
	MusicHelper.logFunctionName(TAG);
    }
    public SongInfo getRandomSong(MusicListType srcType,String srcId){
	return null;
    }
    public void updatePlayer(MusicMessage message){
	MusicHelper.logFunctionName(TAG);
	updateLocalState(message);
	sendMessage();
    }

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
	queuedMessage.info = new PlayerInfo(null,null, null, null, null, null, null);
	queuedMessage.action = null;
    }

    public void sendMusicMessage(MusicMessage musicmessage) {
	MusicHelper.logFunctionName(TAG);
	Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");
	HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
	cMsg.setIntentFilter(XMLMessage.MUSIC_MESSAGE);
	cMsg.setRequestKey(0);
	cMsg.setSender("com.reconinstruments.mobilesdk.mediaplayer.MusicHelper");
	byte[] data = musicmessage.toXML().getBytes();
	cMsg.setData(data);
	//Log.e(TAG,"size of music status message: "+cMsg.ToByteArray().length);
	i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());
	getActivity().sendBroadcast(i);

    }

    public static int getVolumeInt(){
	// song only mode, (IOS)
	if(playerInfo==null)return 0;

	// set default volume to 0.5f
	if(playerInfo.volume==null)playerInfo.volume = 0.5f;

	return (int) Math.round(playerInfo.volume*10);
    }

    public static void gotPlayerInfo(PlayerInfo newInfo)
    {
	MusicHelper.logFunctionName(TAG);
	if (playerInfo != null)
	    playerInfo.update(newInfo);
	song = null;
    }

    public boolean songLoaded(){
	MusicHelper.logFunctionName(TAG);
	return (playerInfo!=null&&(playerInfo.state==PlayerState.PLAYING||playerInfo.state==PlayerState.PAUSED)&&playerInfo.song!=null);
    }
    

    public static final void logFunctionName(String tag) {
	Log.v(tag, "Method name: "+Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}
