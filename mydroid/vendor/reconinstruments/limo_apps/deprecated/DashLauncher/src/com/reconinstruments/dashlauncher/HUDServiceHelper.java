package com.reconinstruments.dashlauncher;

import com.reconinstruments.modlivemobile.bluetooth.BTContentProvider;
import com.reconinstruments.modlivemobile.dto.message.TransferRequestMessage;
import com.reconinstruments.modlivemobile.dto.message.TransferRequestMessage.RequestBundle;
import com.reconinstruments.modlivemobile.dto.message.TransferRequestMessage.RequestType;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

public class HUDServiceHelper {

	private static final String TAG = "HUDServiceHelper";
//	private static final String INTENT_CONNECT = "com.reconinstruments.mobilesdk.hudconnectivity.connect";
	private static final String INTENT_COMMAND = "com.reconinstruments.mobilesdk.hudconnectivity.channel.command";
	private static final String INTENT_OBJECT = "com.reconinstruments.mobilesdk.hudconnectivity.channel.object";
	private static final String INTENT_FILE = "com.reconinstruments.mobilesdk.hudconnectivity.channel.file";

	/** this is the broadcast intent that sends a message to the bluetooth service */
	public static final String GEN_MSG = "RECON_SMARTPHONE_CONNECTION_MESSAGE";
	/** this is for telling the bluetooth front end to connect, stop, listen etc. */
	public static final String APP_MSG = "BLUETOOTH_CONTROL_MESSAGE";

	private static HUDServiceHelper mInstance = null;
	
	public enum Channel {
		COMMAND_CHANNEL, OBJECT_CHANNEL, FILE_CHANNEL
	}
	
	/** Message types sent to the FrontEnd from the application */
	public enum AppMSG {
		NONE,START_CONNECT,START_LISTEN,STOP,START_DOWNLOAD, CHECK_ACTION, SYNC_FILE
	}

	private Context mContext;

	public static HUDServiceHelper getInstance(Context context) {
		if (mInstance == null)
			mInstance = new HUDServiceHelper(context);
		return mInstance;
	}

	public HUDServiceHelper(Context context) {
		mContext = context;
	}

	public void push(HUDConnectivityMessage cMsg, Channel channel){
		Intent i = null;
		switch (channel) {
			case COMMAND_CHANNEL:
				i = new Intent(INTENT_COMMAND);
				break;
			case OBJECT_CHANNEL:
				i = new Intent(INTENT_OBJECT);
				break;
			case FILE_CHANNEL:
				i = new Intent(INTENT_FILE);
				break;
			default:
				break;
		}
		if(i != null && cMsg != null){
//			i.putExtra(HUDConnectivityMessage.TAG, cMsg);
			i.putExtra(HUDConnectivityMessage.TAG, cMsg.ToByteArray());
			mContext.sendBroadcast(i);
			Log.d(TAG, "SDK stub sent out the message " + cMsg.toString() + " to " + channel.name());
		}
	}
	
	public void broadcastMessage(Context context,String message){
		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(GEN_MSG);
		cMsg.setRequestKey(0);
		cMsg.setSender(TAG);
		cMsg.setData(message.getBytes());
		push(cMsg, Channel.OBJECT_CHANNEL);
	}
	
	public void pushFile(Context context,FilePath filePath,FilePath destPath){
		if(destPath==null)destPath = filePath;
		RequestBundle req = new RequestBundle(RequestType.FILE_PUSH,filePath,destPath);
		TransferRequestMessage crm = new TransferRequestMessage();
		String str = crm.compose(req);
		broadcastMessage(context,str);
	}
	
	public boolean isConnected(Context context)
	{
		boolean connected = false;
		Cursor cursor = context.getContentResolver().query(BTContentProvider.CONTENT_URI, new String[]{"connected"}, null,null,null);
		if(checkCursor(cursor))
			connected = cursor.getInt(cursor.getColumnIndex("connected"))==1;
		if(cursor!=null) cursor.close();
		return connected;
	}
	
	private boolean checkCursor(Cursor cursor){
		if(cursor!=null&&cursor.moveToFirst())
			return true;
		return false;
	}
}
