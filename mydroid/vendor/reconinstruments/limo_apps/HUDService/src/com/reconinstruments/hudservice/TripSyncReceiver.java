package com.reconinstruments.hudservice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

public class TripSyncReceiver extends BroadcastReceiver {

	String SEND_TRIPS_REQUEST_INTENT = "TRIP_SYNC_REQUEST";
	String RECEIVE_TRIPS_REQUEST_INTENT = "HUD_TRIP_TRANSFER";
	String DELETE_TRIPS_REQUEST_INTENT = "TRIP_FILE_DELETE_REQUEST";
	String SUCCESS_TRIPS_REQUEST_INTENT = "SUCCESS_TRIP_SYNC";
	String FAILED_TRIPS_REQUEST_INTENT = "FAILED_TRIP_SYNC";
	/** Failed, but continue the sync with remaining files (intent is poorly named) */
	String FAILED_CONTINUE_TRIPS_REQUEST_INTENT = "RESTART_TRIP_SYNC";
	String HUD_CONN_STATE = "HUD_STATE_CHANGED";
	String mPath = "com.reconinstruments.tripsync.TripSyncService";

	static String TAG = "TripSyncReceiver";

	ArrayList<String> mDayQueue = new ArrayList<String>();
	ArrayList<String> mEventQueue = new ArrayList<String>();

	boolean syncying = false;
	boolean connected = false;
	
	long limitTimeModified = 24*60*60*1000;
	HUDService mTheService = null;
	public TripSyncReceiver(HUDService mService){
		super();
		mTheService = mService;
	}
	
	@Override
	public void onReceive(Context c, Intent intent) {
		if(intent.getAction().equals(SEND_TRIPS_REQUEST_INTENT)){
		    turnOffSyncying(); // turn off the previous sync task.
		    sendTripsToClient(c);
		}
		else if(intent.getAction().equals(DELETE_TRIPS_REQUEST_INTENT)){
			Bundle b = intent.getExtras();
			// deletes trip from storage if files are older than X
			if(b!=null){
				HUDConnectivityMessage msg = new HUDConnectivityMessage(b.getByteArray("message"));
				parseDeleteMsg(msg.getData());
			}

			if(mDayQueue.size()==mEventQueue.size()){
				// only if queues are the same size we can retry
				if(mDayQueue.size()!=0)
					sendTripToClient(c);
				else
					sendSuccessSync(c); // if they are empty then we are completed
			}else{
				sendFailedSync(c); // queues are not the same size
				Log.w(TAG,"Queues have different size. Failed, not sending files.");
			}
		}else if(intent.getAction().equals(FAILED_TRIPS_REQUEST_INTENT)){
			turnOffSyncying();
		}else if(intent.getAction().equals(FAILED_CONTINUE_TRIPS_REQUEST_INTENT)){
			if(mDayQueue.size()==mEventQueue.size()){
				// only if queues are the same size we can retry
				if(mDayQueue.size()!=0)
					sendTripToClient(c);
				else
					sendSuccessSync(c); // if they are empty then we are completed
			}else{
				sendFailedSync(c); // queues are not the same size
			}
		}
		else if(intent.getAction().equals(HUD_CONN_STATE)){
			Bundle b = intent.getExtras();

			if(b!=null){
				int state = b.getInt("state");

				if(connected&&state==0){
					connected = false;
					turnOffSyncying();

					// send STOP SYNC
				}
				else if(state==2)
					connected = true;
			}
		}else if(intent.getAction().equals(mPath)){
			// find out if successful
			boolean result = intent.getBooleanExtra("result", false);
			if(result){
				Log.d(TAG,"message was successfully sent");
			}
			else{
				turnOffSyncying();
				Log.w(TAG,"message was NOT sent");
			}
		}

	}

	public void startListening(Context c){
		// Sync process intents
		c.registerReceiver(this, new IntentFilter(SEND_TRIPS_REQUEST_INTENT));
		c.registerReceiver(this, new IntentFilter(DELETE_TRIPS_REQUEST_INTENT));
		c.registerReceiver(this, new IntentFilter(FAILED_TRIPS_REQUEST_INTENT));
		c.registerReceiver(this, new IntentFilter(FAILED_CONTINUE_TRIPS_REQUEST_INTENT));
		// Connection and file transfer intents
		c.registerReceiver(this, new IntentFilter(HUD_CONN_STATE));
		c.registerReceiver(this, new IntentFilter(mPath));
	}
	public void stopListening(Context c){
		c.unregisterReceiver(this);
	}

	private void sendSuccessSync(Context c){
		turnOffSyncying();
		sendObject(c, SUCCESS_TRIPS_REQUEST_INTENT, writeXmlSuccessMsg());
	}

	private void sendFailedSync(Context c){
		turnOffSyncying();
		sendObject(c, FAILED_TRIPS_REQUEST_INTENT, writeXmlFailedMsg());
	}

	private void sendObject(Context c, String intent, String msg){
		Log.d(TAG,"Sending object msg to client app");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");

		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(intent);
		cMsg.setRequestKey(0);
		cMsg.setSender(mPath);

		cMsg.setData(msg.getBytes());

		i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());

		c.sendBroadcast(i);
	}

	private String writeXmlSuccessMsg(){	
		return writeSimpleXmlMsg(SUCCESS_TRIPS_REQUEST_INTENT);
	}
	private String writeXmlFailedMsg(){	
		return writeSimpleXmlMsg(FAILED_TRIPS_REQUEST_INTENT);
	}
	private String writeSimpleXmlMsg(String s){
		return "<recon intent=\""+s+"\"/>";
	}
	private void sendTripsToClient(Context c){

		mDayQueue.clear();
		mEventQueue.clear();

		Log.d(TAG,"sending all trips to client");
		ArrayList<String> names = getFilenamesList();
		for(int i =0 ; i<names.size(); i++){
			String filename = names.get(i);
			Log.d(TAG,"filename: "+filename);
			if(filename.contains("DAY")){
				// we want to get the corresponding event file and put both in the corresponding queues
				// but we can just assume that the file exists
				String eventName = filename.replace("DAY", "EVENT");
				if(names.contains(eventName)){
					mDayQueue.add(filename);
					mEventQueue.add(eventName);
				}else{
					Log.w(TAG,"Day file found but Event file missing!");
				}
			}else{
				Log.w(TAG,"File found is not a trip DAY file");
			}
		}
		// send first files on queue to client
		sendTripToClient(c);
	}

	private void sendTripToClient(Context c){

		if(mDayQueue.size()==mEventQueue.size()&&mDayQueue.size()!=0){
			sendDayFile(c);
			sendEventFile(c);
		}else{
			if(mDayQueue.size()==0){
				Log.d(TAG,"day queue is empty, no file to send");
				sendSuccessSync(c);
			}
			if(mDayQueue.size()!=mEventQueue.size())
				Log.d(TAG,"queues are not the same size");
			
		}
	}

	private void sendDayFile(Context c){
		sendFile(c, getDayFileForClient());
	}
	private void sendEventFile(Context c){
		sendFile(c, getEventFileForClient());
	}

	private void sendFile(Context c, Pair<String,byte[]> pair){
		Log.d(TAG,"Sending "+pair.first+" to client app");

		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(RECEIVE_TRIPS_REQUEST_INTENT);
		cMsg.setRequestKey(0);
		cMsg.setSender("com.reconinstruments.tripsync.TripSyncService");
		cMsg.setData(writeFile(pair.second).getBytes());
		cMsg.setInfo(pair.first+"|"+performOwnChecksum(c,pair.first));

		mTheService.push(cMsg, HUDConnectivityService.Channel.FILE_CHANNEL);
	}

	// this removes the file from queue
	private Pair<String,byte[]> getDayFileForClient(){

		byte[] bytesFile = null;
		String filename = "";
		if(mDayQueue.size()!=0){
			//
			filename = mDayQueue.get(0);
			mDayQueue.remove(0);
			bytesFile = getBytesForFile(filename);
		}else{
			Log.d(TAG,"no new day files to send");
		}
		return new Pair<String,byte[]>(filename, bytesFile);
	}

	// this removes the file from queue
	private Pair<String,byte[]> getEventFileForClient(){
		byte[] bytesFile = null;
		String filename = "";
		if(mEventQueue.size()!=0){
			//
			filename = mEventQueue.get(0);
			mEventQueue.remove(0);
			bytesFile = getBytesForFile(filename);
		}else{
			Log.d(TAG,"no new event files to send");
		}
		return new Pair<String,byte[]>(filename, bytesFile);
	}

	private static byte[] getBytesForFile(String filename){

		File sdCardRoot = Environment.getExternalStorageDirectory();
		//		File yourDir = new File(sdCardRoot, "ReconApps/TripData/");
		File file = new File(sdCardRoot, "ReconApps/TripData/"+filename);


		int size = (int) file.length();
		byte[] bytes = new byte[size];
		try {
			BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
			buf.read(bytes, 0, bytes.length);
			buf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(TAG,"file size in bytes: "+size);
		return bytes;
	}

	private ArrayList<String> getFilenamesList(){
		ArrayList<String> names = new ArrayList<String>();
		File sdCardRoot = Environment.getExternalStorageDirectory();
		File yourDir = new File(sdCardRoot, "ReconApps/TripData/");
		if(yourDir == null ||  yourDir.listFiles() == null) return names;
		for (File f : yourDir.listFiles()) {
			if (f.isFile())
				names.add(f.getName());
		}
		return names;
	}

	private void parseDeleteMsg(byte[] array){
		String xml = new String(array);
		String dayFile = "";
		String eventFile = "";
		String dayMD5 = "";
		String eventMD5 = "";

		try {
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList nodes = doc.getElementsByTagName("recon");
			Node rootNode = nodes.item(0);

			Node firstChild = rootNode.getFirstChild();	
			NamedNodeMap deleteAttr = firstChild.getAttributes(); 
			Node nNode = deleteAttr.getNamedItem("dayName");
			dayFile = nNode.getNodeValue();
			nNode = deleteAttr.getNamedItem("eventName");
			eventFile = nNode.getNodeValue();
			nNode = deleteAttr.getNamedItem("dayMD5");
			dayMD5 = nNode.getNodeValue();
			nNode = deleteAttr.getNamedItem("eventMD5");
			eventMD5 = nNode.getNodeValue();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		deleteFiles(dayFile, dayMD5, eventFile, eventMD5);
	}

	private boolean deleteFiles(String dayFile, String dayMd, String eventFile, String eventMd){
		if(dayFile.equals("")||eventFile.equals(""))
			return false;

		File path = Environment.getExternalStorageDirectory();
		path = new File(path,"ReconApps/TripData/");
		// make desired directories
		if (!path.mkdirs())
		{
			Log.d(TAG,"Parent directories were not created. Possibly since they already exist.");
		}

		// delete file if it exists
		File dayFileToDel = new File(path.getAbsolutePath(), dayFile);
		File eventFileToDel = new File(path.getAbsolutePath(), eventFile);

		Log.d(TAG,"Path to delete day file: "+dayFileToDel.getAbsolutePath());
		boolean dayDeleted = false;
		long modifiedTime = 0;
		if(dayFileToDel.exists()){
			modifiedTime = System.currentTimeMillis()-dayFileToDel.lastModified();
			if(modifiedTime>limitTimeModified){
				Log.d(TAG,"day file exists");
				dayDeleted = dayFileToDel.delete();
			}else{
				Log.d(TAG,"file exists but is not old enough to be deleted");
				return false;
			}
		}
		boolean eventDeleted = false;
		if(eventFileToDel.exists()){
			Log.d(TAG,"event file exists");
			eventDeleted = eventFileToDel.delete();
		}
		
		
		
		if(dayDeleted)
			Log.d(TAG,dayFile+" file was succesfully deleted");
		else
			Log.d(TAG,"No "+dayFile+" file found to delete");

		if(eventDeleted)
			Log.d(TAG,eventFile+" file was succesfully deleted");
		else
			Log.d(TAG,"No "+eventFile+" file found to delete");
		return dayDeleted&&eventDeleted;
	}

	private void turnOffSyncying(){
		syncying = false;
		mDayQueue.clear();
		mEventQueue.clear();
	}

	private String performOwnChecksum(Context c, String filename) {
		File sdCardRoot = Environment.getExternalStorageDirectory();
		//		File yourDir = new File(sdCardRoot, "ReconApps/TripData/");
		File file = new File(sdCardRoot, "ReconApps/TripData/"+filename);
		String checksum = "";
		if(file.exists()){
			Log.d(TAG,"path to look for DB file: "+file.getAbsolutePath());
			checksum = md5(file.getAbsolutePath(),false);
		}
		return checksum;
	}

	/** read a file and calculate md5 from the byte array 
	/*
	 * @param ignoreSQLFileChangeCounter set this to ignore 4 bytes that change every time in a sqlite file to prevent them from being seen as different files
	 */
	private static String md5(String path,boolean ignoreSQLFileChangeCounter){	
		FileInputStream fileStream;
		try
		{
			fileStream = new FileInputStream(path);
			byte[] fileBuffer = readByteArray(fileStream,0);

			// set bytes 24-28 to 0 to ignore the file change counter bytes in a sqlite db
			// prevent identical databases from appearing different and thus triggering a file transfer
			// @see http://sqlite.org/fileformat.html 
			if(ignoreSQLFileChangeCounter){
				for(int i=24;i<28;i++){
					fileBuffer[i] = 0;
				}
			}

			return md5(fileBuffer,0);
		} catch (FileNotFoundException e)
		{
			Log.d(TAG, "Tried to get checksum on missing file: "+path);
			return "";
		} catch (ArrayIndexOutOfBoundsException e)
		{
			Log.d(TAG, "Tried to get checksum on empty file: "+path,e);
			return "";
		}
	}
	/** generate an md5 checksum from a byte array
	 offset allows md5 to be calculated ignoring the beginning of the data */
	private static String md5(byte[] array,int offset) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			//digest.update(array);
			digest.update(array, offset, array.length-offset);
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i=0; i<messageDigest.length; i++)
				hexString.append(String.format("%02x", messageDigest[i]&0xff));
			String md5 = hexString.toString();
			Log.d(TAG,"message digest length: "+messageDigest.length);
			Log.d(TAG,"md5 gen: "+md5);
			return md5;

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static byte[] readByteArray(InputStream input, int size)
	{		
		try{
			if(size==0) size = input.available();
			byte[] data = new byte[size];
			input.read(data);
			return data;
		}
		catch(IOException e){
			Log.d(TAG, "failed to read byte array",e);
			return null;}		
	}
	
	public String writeFile(byte[] fileArray){

		String filename = performChecksum(fileArray)+".tmp";
		String path = "";
		//
		String parentFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmp/";
		// make desired directories
		
		File file = new File(parentFolderPath);
		if (!file.mkdirs())
		{
			Log.d(TAG,"Parent directories were not created. Possibly since they already exist.");
		}
		
		// delete file if it exists
		String file_path = file.getAbsolutePath()+"/"+filename;
		Log.d(TAG,"temporary path: "+file_path);
		File fileToDel = new File(file_path);
		boolean deleted = false;
		if(fileToDel.exists())
			deleted = fileToDel.delete();

		if(deleted)
			Log.d(TAG,"file was succesfully deleted");
		else{Log.d(TAG,"no file found to delete");}


		// write to file
		file = new File(file_path);


		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			Log.w(TAG,"caught exception opening buffer: "+e);
			e.printStackTrace();
			return path;
		}
		try {
			bos.write(fileArray);
			bos.flush();
			bos.close();
			path = file.getAbsolutePath();
		} catch (IOException e) {
			Log.w(TAG,"caught exception closing file : "+e);
			e.printStackTrace();
			return path;
		}

		//
		return path;
	}
	
	private String performChecksum(byte[] c) {
		String localSum = md5(c,0);
		return localSum;
	}


}
