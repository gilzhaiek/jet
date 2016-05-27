package com.reconinstruments.dashmusic;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.connect.music.MusicDBContentProvider;
import com.reconinstruments.connect.util.FileUtils;
import com.reconinstruments.connect.util.XMLUtils;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;

public class MusicActivitySyncer{

	private static final String TAG = "MusicActivitySyncer";
	static final String remoteChecksumMODLiveRequest = "REMOTE_DB_CHECKSUM_REQUEST"; 
	static final String localChecksumMODLiveRequest = "LOCAL_DB_CHECKSUM_REQUEST";
	static final String uploadLocalDbRequest = "UPLOAD_DB_REQUEST";
	static final String failedMODLiveRequest = "FAILED_DB_REQUEST";
	static final String HUD_CONN_CHANGE = "HUD_STATE_CHANGED";
	static final String mPath = "com.reconinstruments.dashmusic.MusicActivitySyncer"; 
	MusicActivity context;
	String lastChecksumFromClient = "";
	
	private boolean syncying = false;
	
	public Activity getActivity(){
		return (Activity)context;
	}

	public boolean isSyncying(){
		Log.d(TAG,"music activity is syncying? "+syncying);
		return syncying;
	}
	
	public MusicActivitySyncer(final MusicActivity c){
		Log.e(TAG,"constructor()");
		this.context = (MusicActivity) c;
		getActivity().registerReceiver(dbSyncReceiver, new IntentFilter(remoteChecksumMODLiveRequest));
		getActivity().registerReceiver(dbSyncReceiver, new IntentFilter(mPath));
		getActivity().registerReceiver(dbSyncReceiver, new IntentFilter(uploadLocalDbRequest));

		// notify clients that we are awake
		// this is will trigger them to want to sync modlive db
		notifyListeners(c);
	}


	private void notifyListeners(Context c) {
		Log.e(TAG,"notifying listeners from music syncer");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");

		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(localChecksumMODLiveRequest);
		cMsg.setRequestKey(0);
		cMsg.setSender(mPath);
		cMsg.setData(writeXmlForNotifyingClients(performOwnChecksum(c)).getBytes());
		i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());

		c.sendBroadcast(i);
	}

	public void onDestroy(Context c){
		c.unregisterReceiver(dbSyncReceiver);
	}


	BroadcastReceiver dbSyncReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(remoteChecksumMODLiveRequest)){
				syncying = true;
				Log.e(TAG,"received broadcast with intent: "+intent.getAction());
				
				Bundle b = intent.getExtras();

				//HUDConnectivityMessage hMsg = new HUDConnectivityMessage(null, msgByte, null);

				if(b!=null){
					//if(b.containsKey("localChecksum"))
					//Log.e(TAG,"bundle contains localChecksum, we are safe. dont need string manipulation");
					//else{
					//Log.e(TAG,"bundle does not contain localChecksum, string manipul required");}
					byte[] msgBytes = b.getByteArray("message");
					HUDConnectivityMessage hMsg = new HUDConnectivityMessage(msgBytes);
					String sum = parseSimpleMessageLocalChecksum(new String((byte[])hMsg.getData()));
					lastChecksumFromClient = sum;
					String localSum = performOwnChecksum(context);

					boolean match;
					if(localSum!=null){
						match = localSum.equals(sum);
						Log.d(TAG,"checksums match: "+match);
					}
					else
						match = false;

					if(match){
						sendSuccessDbMsg(context);
						((MusicActivity) getActivity()).getClientState();
						// same db, wrap up
						// use hud connectivity msg
					}else{
						sendMismatchDbMsg(context);
						// request file
						// use hud connectivity msg
						//TODO: lock music here because we are going to be uploading the db
					}


				}
			}
			else if(intent.getAction().equals(mPath)){
				Log.d(TAG,"received broadcast with intent: "+intent.getAction());
				boolean result = intent.getBooleanExtra("result", false);
				if(result)
					Log.d(TAG,"message was successfully sent");
				else{
					Log.w(TAG,"message was NOT sent");
				}
			}
			else if(intent.getAction().equals(uploadLocalDbRequest)){
				Log.d(TAG,"received broadcast with intent: "+intent.getAction());
				byte[] msgBytes = intent.getExtras().getByteArray("message");
				HUDConnectivityMessage hMsg = new HUDConnectivityMessage(msgBytes);
				// this will substitute the db file 
				
				boolean fileSubbed = false;
				try{
				fileSubbed = writeNewDbFile(readFile(new String(hMsg.getData())), 
						context.getDatabasePath(MusicDBContentProvider.DATABASE_NAME).getPath());
				}catch(IOException e){
					Log.e(TAG,"error reading from file!!");
				}
				//
				String sum = performOwnChecksum(context);
				boolean checksumMatch = sum.equals(lastChecksumFromClient);
				//Log.d(TAG,"file subbed: " +fileSubbed);
				Log.d(TAG,"last checksum: " +lastChecksumFromClient);
				Log.d(TAG,"checksum from new db: " +sum);
				if(fileSubbed&&checksumMatch){
					sendSuccessDbMsg(context);
					((MusicActivity) getActivity()).updateLibrary();
				}
				else{
					Log.w(TAG,"error happened writing to file");
					sendFailedDbMsg(context);
				}
			}
		}
	};

	private boolean writeNewDbFile(byte[] b, String path) {

		if(path.endsWith(".db")){
			// make desired directories
			String fullPath = path;
			String parentPath = path.replace("reconmusic.db", "");
			File file = new File(parentPath);
			if (!file.mkdirs())
			{
				Log.d(TAG,"Parent directories were not created. Possibly since they already exist.");
			}

			// delete file if it exists
			File fileToDel = new File(path);
			boolean deleted = false;
			if(fileToDel.exists())
				deleted = fileToDel.delete();
			//if(MusicDBContentProvider.getDa)

			if(deleted)
				Log.d(TAG,"DB file was succesfully deleted");
			else{Log.d(TAG,"no DB file found to delete");}


			// write to file
			file = new File(fullPath);


			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(new FileOutputStream(file));
			} catch (FileNotFoundException e) {
				Log.w(TAG,"caught exception opening buffer: "+e);
				e.printStackTrace();
				return false;
			}
			try {
				bos.write(b);
			} catch (IOException e) {
				Log.w(TAG,"caught exception writing to file: "+e);
				e.printStackTrace();
				return false;
			}
			try {
				bos.flush();
			} catch (IOException e) {
				Log.w(TAG,"caught exception flushing buffer: "+e);
				e.printStackTrace();
				return false;
			}
			try {
				bos.close();
			} catch (IOException e) {
				Log.w(TAG,"caught exception closing file : "+e);
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	private String performOwnChecksum(Context c) {
		String localPath = c.getDatabasePath(MusicDBContentProvider.DATABASE_NAME).getPath();
		Log.e(TAG,"path to look for DB file: "+localPath);
		String localSum = FileUtils.md5(localPath,true);
		
		return localSum;
	}

	private String parseSimpleMessageLocalChecksum(String msg) {

		try {
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(msg));

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList nodes = doc.getElementsByTagName("recon");
			Node rootNode = nodes.item(0);

			Node firstChild = rootNode.getFirstChild();	
			NamedNodeMap statusAttr = firstChild.getAttributes(); 
			Node nStatusChecksum = statusAttr.getNamedItem("localChecksum");
			String statusChecksum = nStatusChecksum.getNodeValue();
			return statusChecksum;
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
		return "";
	}

	// send mismatch msg via broadcast
	private void sendMismatchDbMsg(Context c) {
		Log.d(TAG,"sending failure (mismatch) message to client");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");

		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(remoteChecksumMODLiveRequest);
		cMsg.setRequestKey(0);
		cMsg.setSender(mPath);
		cMsg.setData(writeXmlForRemoteChecksumReply("false").getBytes());
		i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());

		c.sendBroadcast(i);
	}

	// send success msg via broadcast
	private void sendSuccessDbMsg(Context c) {
		syncying = false;
		Toast.makeText(c, "Database is ready!", Toast.LENGTH_SHORT).show();
		
		Log.d(TAG,"sending success message to client. db synced!");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");

		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(remoteChecksumMODLiveRequest);
		cMsg.setRequestKey(0);
		cMsg.setSender(mPath);
		cMsg.setData(writeXmlForRemoteChecksumReply("true").getBytes());
		i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());

		c.sendBroadcast(i);
	}
	// send failed msg via broadcast
	private void sendFailedDbMsg(Context c) {
		Log.e(TAG,"sending failure message to client. something went wrong!");
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");

		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(failedMODLiveRequest);
		cMsg.setRequestKey(0);
		cMsg.setSender(mPath);
		cMsg.setData(writeXmlForFailedReply().getBytes());
		i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());

		c.sendBroadcast(i);
	}

	private String writeXmlForNotifyingClients(String localSum){
		ArrayList<BasicNameValuePair> array = new ArrayList<BasicNameValuePair>();
		array.add(new BasicNameValuePair("remoteChecksum",localSum));
		return XMLUtils.composeSimpleMessage(localChecksumMODLiveRequest,"status",array);
	}

	private String writeXmlForRemoteChecksumReply(String match){
		ArrayList<BasicNameValuePair> array = new ArrayList<BasicNameValuePair>();
		array.add(new BasicNameValuePair("synced", match));
		return XMLUtils.composeSimpleMessage(remoteChecksumMODLiveRequest,"status",array);
	}
	private String writeXmlForRemoteChecksumReply(String match, String sum){
		ArrayList<BasicNameValuePair> array = new ArrayList<BasicNameValuePair>();
		array.add(new BasicNameValuePair("remoteChecksum", sum));
		return XMLUtils.composeSimpleMessage(remoteChecksumMODLiveRequest,"status",array);
	}
	private String writeXmlForFailedReply(){
		return "<recon intent=\""+failedMODLiveRequest+"\"/>";
	}
	
	public static byte[] readFile(String path) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(new File(path), "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            Log.d(TAG,"raw file: "+data.length);
            return data;
        } finally {
            f.close();
        }
    }

	void setSyncMode(boolean sync){
		syncying = sync;
	}
}
