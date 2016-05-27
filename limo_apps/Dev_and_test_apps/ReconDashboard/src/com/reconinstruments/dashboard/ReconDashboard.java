package com.reconinstruments.dashboard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.applauncher.transcend.ReconTranscendService;
import com.reconinstruments.dashboard.hashmaps.ReconDashboardHashmap;
import com.reconinstruments.dashboard.layouts.ReconDashboardLayout;
import com.reconinstruments.dashboard.widgets.FontSingleton;
import com.reconinstruments.dashboard.widgets.ReconDashboardWidget;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.dto.message.*;
import com.reconinstruments.modlivemobile.dto.message.MusicActionMessage.MusicAction;
import com.reconinstruments.modlivemobile.dto.message.MusicActionMessage.MusicActionBundle;
import com.reconinstruments.modlivemobile.dto.message.StatusChangeMessage.PlayerState;
import com.reconinstruments.modlivemobile.dto.message.StatusChangeMessage.StatusChangeBundle;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd;
import com.reconinstruments.modlivemobile.music.ReconMediaData.ReconSong;

public class ReconDashboard extends Activity {
	/** Called when the activity is first created. */

	public static final String TAG = "ReconDashboard";
	
	private ArrayList<ReconDashboardLayout> mAllLayouts; // All layouts that we cycle through
	private int mCurrentLayoutindex;// = 0; //Index of the layout
	private ReconDashboardHashmap mHashmap;
	private BroadcastReceiver mBroadcastReceiver;
    private boolean TOAST_DEBUG = false;
	private static final String defaultXml = "<Recon-Dashboard-Layouts>"
			+ "<layout layout_id=\"1\"><widget name=\"spd_4x3\"/><widget name=\"alt_2x2\"/><widget name=\"chr_4x1\"/><widget name=\"tmp_2x2\"/></layout>"
			+ "<layout layout_id=\"3\"><widget name=\"spd_4x2\"/><widget name=\"maxspd_2x2\"/><widget name=\"chr_4x1\"/><widget name=\"vrt_2x2\"/><widget name=\"dst_4x1\"/></layout>"
			+ "<layout layout_id=\"1\"><widget name=\"spd_4x3\"/><widget name=\"air_2x2\"/><widget name=\"ply_4x1\"/><widget name=\"tmp_2x2\"/></layout>"
			+ "<layout layout_id=\"6\"><widget name=\"spd_6x4\"/></layout>"
			+ "<layout layout_id=\"4\"><widget name=\"spd_4x2\"/><widget name=\"alt_2x2\"/><widget name=\"vrt_2x2\"/><widget name=\"dst_2x2\"/><widget name=\"tmp_2x2\"/></layout>"
			+ "</Recon-Dashboard-Layouts>";

	private Bundle mFullInfoBundle;
	private String currentSong;
	private Bundle mHRBundle;
	
	private float vertical = -1;
	
	private Toast statsToast; // a single toast instance for all stats updates
	
	/**
	 * This part pertains to stuff need to connect to transcend service
	 */
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ReconTranscendService.MSG_RESULT:
				//String song = null;
				//if(mFullInfoBundle != null)
				//	song = mFullInfoBundle.getString("CURRENT_SONG");
				
				Bundle incomingFullInfoBundle = (Bundle) msg.getData();
				
				// Check if vertical pop up should be thrown
				float newVertical = incomingFullInfoBundle.getBundle("VERTICAL_BUNDLE").getFloat("Vert");
				
				if(vertical == -1)
					vertical = newVertical;
				
				// Throw pop up every 300ft or 100m
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					if(Math.abs(ConversionUtil.metersToFeet((double) vertical) - ConversionUtil.metersToFeet((double) newVertical)) > 300) { 
						Intent i = new Intent("RECON_MOD_BROADCAST_VERTICAL");
						
						i.putExtra("Bundle", incomingFullInfoBundle.getBundle("VERTICAL_BUNDLE"));
						i.putExtra("WhichOne", "Vert");
						
						ReconDashboard.this.sendBroadcast(i);
						
						vertical = newVertical;
					}
				} else {
					if(Math.abs(vertical - newVertical) > 100) {
						Intent i = new Intent("RECON_MOD_BROADCAST_VERTICAL");
					
						i.putExtra("Bundle", incomingFullInfoBundle.getBundle("VERTICAL_BUNDLE"));
						i.putExtra("WhichOne", "Vert");
					
						ReconDashboard.this.sendBroadcast(i);
						
						vertical = newVertical;
					}
				}
				
				mFullInfoBundle = incomingFullInfoBundle;
				mFullInfoBundle.putString("CURRENT_SONG", currentSong);
				updateEverything();
				break;

			case ReconTranscendService.MSG_RESULT_CHRONO:
				mFullInfoBundle.putBundle("CHRONO_BUNDLE", (Bundle) msg.getData());
				updateEverything();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);

			// As part of the sample, tell the user what happened.
			//Ali: commented out the toast
			// Toast.makeText(ReconDashboard.this, "Remote Service connected",
			// 		Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;

			// As part of the sample, tell the user what happened.
			Toast.makeText(ReconDashboard.this, "Remote Service Disconnected",
					Toast.LENGTH_SHORT).show();
		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		bindService(
				new Intent("RECON_MOD_SERVICE"),				
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		// Toast.makeText(this,"Binding.",1000).show();
		
		//make sure Polar Service is running
		startService(new Intent("POLAR_HR_SERVICE"));
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			// Toast.makeText(ReconDashboard.this, "Disconnected", 1000).show();
		}
	}

	public void requestUpdateFullInfo() {
		try {

			// Give it some value as an example.
			Message msg = Message.obtain(null,
					ReconTranscendService.MSG_GET_FULL_INFO_BUNDLE, 0, 0);
			msg.replyTo = mMessenger;
			mService.send(msg);
		} catch (RemoteException e) {
			// In this case the service has crashed before we could even
			// do anything with it; we can count on soon being
			// disconnected (and then reconnected if it can be restarted)
			// so there is no need to do anything here.
		} catch (Exception e) {
			//Log.e("ReconDashboard", e.toString());
		}
	}

	public void updateEverything() {
		if (mFullInfoBundle != null)
			if(mHRBundle != null)
				mFullInfoBundle.putBundle("POLAR_BUNDLE", mHRBundle);
			mAllLayouts.get(mCurrentLayoutindex).updateInfo(mFullInfoBundle);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		
		mAllLayouts = new ArrayList<ReconDashboardLayout>();
		mHashmap = new ReconDashboardHashmap();
		mBroadcastReceiver = new DashboardBroadcastReceiver();
		
		// Initialize stats toast
		statsToast = new Toast(this);
		statsToast.setDuration(Toast.LENGTH_LONG);
		statsToast.setGravity(Gravity.FILL, 0,0);
		
		// Register BroadcastReceiver
		registerReceiver(mBroadcastReceiver, new IntentFilter("RECON_MOD_BROADCAST_LOCATION"));
		registerReceiver(mBroadcastReceiver, new IntentFilter("RECON_MOD_BROADCAST_JUMP"));
		registerReceiver(mBroadcastReceiver, new IntentFilter("RECON_MOD_BROADCAST_VERTICAL"));
		registerReceiver(mBroadcastReceiver, new IntentFilter("RECON_MOD_BROADCAST_SPEED"));
		registerReceiver(mBroadcastReceiver, new IntentFilter("RECON_MOD_BROADCAST_TEMPERATURE"));
		registerReceiver(mBroadcastReceiver, new IntentFilter("RECON_MOD_BROADCAST_ALTITUDE"));
		registerReceiver(mBroadcastReceiver, new IntentFilter("POLAR_BROADCAST_HR"));
		// Bluetooth related messages
		registerReceiver(mBroadcastReceiver, new IntentFilter(XMLMessage.MUSIC_RESPONSE_MESSAGE));
		registerReceiver(mBroadcastReceiver, new IntentFilter(BTCommon.MSG_STATE_UPDATED));
		
		
		// Load saved state
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mCurrentLayoutindex = mPrefs.getInt("current_layout_index", 0);
		Log.d("ReconDashboard", "Current Layout Index: " + Integer.toString(mCurrentLayoutindex));

		loadLayoutXML();
		
		// Set the big view of the first layout in the list as the default view
		// Otherwise show default no layout
		if (mAllLayouts.size() >= 1) {
			try {
				setContentView(mAllLayouts.get(mCurrentLayoutindex).mTheBigView);
			} catch(Exception e) {
				mCurrentLayoutindex = 0; 
				setContentView(mAllLayouts.get(mCurrentLayoutindex).mTheBigView);
			}
		} else {
			setContentView(R.layout.no_dash_layout);
		}
		
		// Bind to the service for requests:
		doBindService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		/* Request current song */
		Intent myi = new Intent();
		myi.setAction(BTCommon.GEN_MSG);
		MusicActionMessage maMsg = new MusicActionMessage();
		String message = maMsg.compose(new MusicActionBundle(MusicAction.GET_PLAYER_STATE));
		myi.putExtra("message", message);
		this.sendBroadcast(myi);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.v(TAG, "onPause");

		// Save dashboard view index
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt("current_layout_index", mCurrentLayoutindex); // value to
																	// store
		editor.commit();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");

		doUnbindService();
		unregisterReceiver(mBroadcastReceiver);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (mAllLayouts.size() < 1)
    	{return false;}
    	
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	mCurrentLayoutindex = (mCurrentLayoutindex + 1) % mAllLayouts.size();
        	Log.d("ReconDashboard", "mCurrentLayoutindex: " + Integer.toString(mCurrentLayoutindex));
            setContentView(mAllLayouts.get(mCurrentLayoutindex).mTheBigView);
            requestUpdateFullInfo();
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	mCurrentLayoutindex = (mCurrentLayoutindex + mAllLayouts.size() - 1) % mAllLayouts.size();
        	Log.d("ReconDashboard", "mCurrentLayoutindex: " + Integer.toString(mCurrentLayoutindex));
            setContentView(mAllLayouts.get(mCurrentLayoutindex).mTheBigView);
            requestUpdateFullInfo();
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        	Intent myIntent = new Intent(this, ReconDashboardStats.class);
        	myIntent.putExtra("FULL_INFO_BUNDLE", mFullInfoBundle);
            startActivityForResult(myIntent, 0);
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        	Intent myIntent = new Intent(this, ReconDashboardStats.class);
        	myIntent.putExtra("FULL_INFO_BUNDLE", mFullInfoBundle);
            startActivityForResult(myIntent, 0);
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
        	finish();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        	//Check to see if screen has chrono on it
        	if(mAllLayouts.get(mCurrentLayoutindex).hasChrono()) {
        		//Log.d("ReconDashboard", "Chrono toggled");
        		
        		if (!mFullInfoBundle.getBundle("CHRONO_BUNDLE").getBoolean("IsRunning")) {
        			try {
            			Message msg = Message.obtain(null,ReconTranscendService.MSG_CHRONO_START_NEW_TRIAL, 0, 0);
                        msg.replyTo = mMessenger;
                        mService.send(msg);
            		} catch(Exception e) {
            			
            		}
        		} else {
        			try {
            			Message msg = Message.obtain(null,ReconTranscendService.MSG_CHRONO_STOP_TRIAL, 0, 0);
                        msg.replyTo = mMessenger;
                        mService.send(msg);
            		} catch(Exception e) {
            			
            		}
        		}
        	}
        		 
        }
        else {
        	return false;
        }
    
        return true;
    }
	
	private void loadLayoutXML() {
		// Load user defined xml
		try {
			String userXML = getUserDashboardXml();
			parseReconLayoutsXML(userXML);
			Log.d(TAG, "Successfully loaded custom dash xml");
		}
		
		// If user defined xml fails use default layout
		// and replace the user xml with the default xml
		catch (Exception e) {
			Log.e(TAG, "Invalid customizable dashboard xml file, using default");
			
			// Load default configuration
			try {
				parseReconLayoutsXML(defaultXml);
			} catch(Exception e2) {
				Log.e(TAG, "Error loading default dashboard view",e);
				finish();
			}
			
			// Replace invalid user layout
			try {
				File usrXmlFile = new File("/mnt/storage/ReconApps/Dashboard/user_dashboard.xml");
				
				//Copy old layout to new file
				if(usrXmlFile.exists())
					copy(usrXmlFile.getAbsolutePath(), "/mnt/storage/ReconApps/Dashboard/user_dashboard_invalid.xml");
				
				boolean erased = usrXmlFile.delete();
				if(!erased) Log.e(TAG, "Couldn't replace customizable dash xml");
				
				FileWriter fWriter = new FileWriter(usrXmlFile);
				fWriter.write(defaultXml);
				fWriter.close();
				Log.d(TAG, "Replaced customizable dash xml with default layout");
			} catch(IOException ioe) {
				Log.e(TAG, "Couldn't replace customizable dash xml", ioe);
			}
			
			mCurrentLayoutindex = 0;
		}
	}
	
	private String getUserDashboardXml() throws Exception{
		try {
			StringBuffer fileData = new StringBuffer(100);
	        BufferedReader reader = new BufferedReader(new FileReader(new File("/mnt/storage/ReconApps/Dashboard/user_dashboard.xml")));
	        char[] buf = new char[1024];
	        int numRead=0;
	        while((numRead=reader.read(buf)) != -1){
	            fileData.append(buf, 0, numRead);
	        }
	        reader.close();
	        
	        return fileData.toString();
		} catch(Exception e) {
			Log.e("ReconDashboard", e.toString());
			throw e;
		}
	}


	/**
	 * The functions in this part pertain to reading XML, parsing and
	 * constructing customized dashes
	 */
	public void parseReconLayoutsXML(String rawxml) throws Exception {
		/*
		 * Parses recon layout xml. Generates appropriate views and layouts and
		 * puts them in data structures. (It calls auxiliary functions)
		 */
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(rawxml));
			Document doc = db.parse(is);

			NodeList layoutNodes = doc.getElementsByTagName("layout");
			analyzeLayoutList(layoutNodes);
			
			// If there is are no layouts in this xml
			if(mAllLayouts.size() < 1)
				throw new Exception();

		} catch (Exception e) {
			throw e;
		}
	}

	public void analyzeLayoutList(NodeList layouts) {
		Node layoutNode;
		for (int i = 0; i < layouts.getLength(); i++) {
			layoutNode = layouts.item(i);
			analyzeSingleLayout(layoutNode);
		}
	}

	public void analyzeSingleLayout(Node layoutNode) {
		String id = layoutNode.getAttributes().getNamedItem("layout_id")
				.getNodeValue();

		// Create Recon layout object
		ReconDashboardLayout layout = new ReconDashboardLayout(id, this);

		// Add it to the list of layouts of the activity
		mAllLayouts.add(layout);

		// Get all the widgets of the layout from XML
		NodeList widgetList = layoutNode.getChildNodes();

		String widgetName;
		for (int i = 0; i < widgetList.getLength(); i++) {

			// Get the next widget from widget list
			Node w = widgetList.item(i);
			if (w.hasAttributes()) { // make sure it is a valid one
				// Has attributes

				widgetName = w.getAttributes().getNamedItem("name")
						.getNodeValue();

				// Generate ReconDashboardWidget Object from widgetName
				ReconDashboardWidget widget = ReconDashboardWidget
						.spitReconDashboardWidget(mHashmap.WidgetHashMap
								.get(widgetName), this);

				// Add widget to widget container of the layout object
				layout.mAllWidgets.add(widget);
			}
		}
		layout.populate();
	}

	public static void copy(String from, String to) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			InputStream inFile = new FileInputStream(from);
			in = new BufferedInputStream(inFile);
			OutputStream outFile = new FileOutputStream(to);
			out = new BufferedOutputStream(outFile);
			while (true) {
				int data = in.read();
				if (data == -1) {
					break;
				}
				out.write(data);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}

	class DashboardBroadcastReceiver extends BroadcastReceiver {

		double altPopupThresImp = 1500; // ft
		double altPopupThres = 500; // m
		double vertPopupThresImp = 1500; // ft
		double vertPopupThres = 500; // m
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			/* LOCATION BROADCASTS */
			if (action.equals("RECON_MOD_BROADCAST_LOCATION")) {
				ReconDashboard.this.requestUpdateFullInfo();
				
			}
			else if(action.equals(BTCommon.MSG_STATE_UPDATED)) {
				boolean connected = intent.getBooleanExtra("bt_connected", false);
				if(connected){
					MusicActionMessage musicAction = new MusicActionMessage();
					String msg = musicAction.compose(new MusicActionBundle(MusicAction.GET_PLAYER_STATE));
					BTCommon.broadcastMessage(ReconDashboard.this, msg);
				} else {
					ReconDashboard.this.currentSong = null;
				}
			} 
			else if(action.equals(XMLMessage.MUSIC_RESPONSE_MESSAGE)) {
				Bundle bundle = intent.getExtras();
				String message = bundle.getString("message");
				
				StatusChangeMessage scm = new StatusChangeMessage();
				StatusChangeBundle scb = (StatusChangeBundle) scm.parse(message);
				
				if(scb.state==PlayerState.PLAYING){
					ReconSong song = MusicDBFrontEnd.getSongFromId(ReconDashboard.this, scb.songId);
					if(song!=null)
						ReconDashboard.this.currentSong = song.artist + " - " + song.title;
					else
						ReconDashboard.this.currentSong = null;
				} else {
					ReconDashboard.this.currentSong = null;
				}
			}
			
			else if(action.equals("RECON_MOD_BROADCAST_VERTICAL")) {
				Bundle bundle = intent.getExtras();
				Bundle vertBundle = bundle.getBundle("Bundle");
				Double vert = (double) vertBundle.getFloat(bundle.getString("WhichOne"));
				Double previousVert = (double) vertBundle.getFloat("PreviousVert");
				
				// Determine if popup warranted
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					if(!( (ConversionUtil.metersToFeet(vert) / vertPopupThresImp) 
							> (ConversionUtil.metersToFeet(previousVert) / vertPopupThresImp) ))
						return;
				} else {
					if(!(( vert / vertPopupThres) > (previousVert / vertPopupThres)))
						return;
				}
				
				String units = "m";
				
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					units = "ft";
					vert = ConversionUtil.metersToFeet(vert);
				} else {
					
				}
				
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(0);
				
				Drawable icon = getResources().getDrawable(R.drawable.popup_vert);
				statsToast("VERTICAL", df.format(vert), units, icon, 0x148cc8);
			}
			
			else if(action.equals("RECON_MOD_BROADCAST_JUMP")) {
				Bundle bundle = intent.getExtras();
				Bundle latestJump = bundle.getBundle("Bundle");
				// Ali: Change  of API
				//				Bundle airBundle = bundle.getBundle("Bundle");

				// ArrayList<Bundle> jumps = airBundle.getParcelableArrayList("Jumps");//change  of API now the only single jump is sent
				
				//				Bundle latestJump = jumps.get(jumps.size() - 1);

				if(latestJump == null) return;
				
				Double time = ((double) latestJump.getInt("Air")) / 1000;
				
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(1);
				df.setMinimumFractionDigits(1);
				
				Drawable icon = getResources().getDrawable(R.drawable.popup_jump);
				statsToast("JUMP AIRTIME", df.format(time), "sec", icon, 0xBE3C96);
			}
						
			else if(action.equals("RECON_MOD_BROADCAST_SPEED")) {
				Bundle bundle = intent.getExtras();
				Bundle speedBundle = bundle.getBundle("Bundle");
				Double speed = (double) speedBundle.getFloat(bundle.getString("WhichOne"));
				
				// If speed less than 30kph, don't show
				if (speed < 30)
					return;
				
				String units = "km\n/h";
				
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					units = "mph";
					speed = ConversionUtil.kmsToMiles(speed);
				}
				
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(0);
				
				Drawable icon = getResources().getDrawable(R.drawable.popup_speed);
				statsToast("MAX SPEED", df.format(speed), units, icon, 0x64B446);
			}
						
			else if(action.equals("RECON_MOD_BROADCAST_TEMPERATURE")) {
				Bundle bundle = intent.getExtras();
				Bundle tempBundle = bundle.getBundle("Bundle");
				int temperature = tempBundle.getInt(bundle.getString("WhichOne"));
				
				String units = "\u00B0" + "C";
				
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					units = "\u00B0" + "F";
					temperature = ConversionUtil.celciusToFahrenheit(temperature);
				}
				
				String title = "";
				if(bundle.getString("WhichOne").equals("MinTemperature")) {
					title = "MIN TEMP TODAY";
				} else if (bundle.getString("WhichOne").equals("MaxTemperature")) {
					title = "MAX TEMP TODAY";
				} else if (bundle.getString("WhichOne").equals("AllTimeMaxTemperature")) {
					title = "ALL TIME MAX TEMP";
				} else if (bundle.getString("WhichOne").equals("AllTimeMinTemperature")) {
					title = "ALL TIME MIN TEMP";
				} else {
					title = "TEMPERATURE";
				}
				
				Drawable icon = getResources().getDrawable(R.drawable.popup_temp);
				statsToast(title, Integer.toString(temperature) + units, null, icon, 0x32C8A0);
			}
			
			else if(action.equals("RECON_MOD_BROADCAST_ALTITUDE")) {
				Bundle bundle = intent.getExtras();
				Bundle altBundle = bundle.getBundle("Bundle");
				double altitude = (double) altBundle.getFloat(bundle.getString("MaxAlt"));
				double prevAltitude = (double) altBundle.getFloat("PreviousMaxAlt");
				
				// determine if popup warranted
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					if(!( (ConversionUtil.metersToFeet(altitude) / altPopupThresImp) 
							> (ConversionUtil.metersToFeet(prevAltitude) / altPopupThresImp) ))
						return;
				} else {
					if(!(( altitude / altPopupThresImp) > (prevAltitude / altPopupThresImp)))
						return;
				}
				
				String units = "m";
				
				if(ReconSettingsUtil.getUnits(ReconDashboard.this) == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
					units = "ft";
					altitude = ConversionUtil.metersToFeet(altitude);
				}
				
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(0);
				df.setMinimumFractionDigits(0);
				
				Drawable icon = getResources().getDrawable(R.drawable.popup_alt);
				statsToast("MAX ALTITUDE", df.format(altitude), units, icon, 0xFF8C14);
			}
			
			else if(action.equals("POLAR_BROADCAST_HR")) {
				Bundle hrBundle = intent.getExtras().getBundle("POLAR_BUNDLE");
				//Log.d(TAG, "HR: " + hrBundle.getInt("AvgHR"));
				mHRBundle = hrBundle;
			}
			
		}
		
	}
	
	public void statsToast(String title, String message, String message2, Drawable icon, int backgroundColor) {
		// Create Toast View
		LayoutInflater inflater = (LayoutInflater) ReconDashboard.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View jumpToast = inflater.inflate(R.layout.pop_up, null);
		
		// Set popup color
		View mainContainer = jumpToast.findViewById(R.id.mainContainer);
		GradientDrawable dialogShape = (GradientDrawable) getResources().getDrawable(R.drawable.dialog_shape);
		dialogShape.setColor(backgroundColor + 0x44000000);
		dialogShape.setStroke(5, backgroundColor + 0xFF000000);
		mainContainer.setBackgroundDrawable(dialogShape);
		
		View titleContainer = jumpToast.findViewById(R.id.titleContainer);
		GradientDrawable titleShape = (GradientDrawable) getResources().getDrawable(R.drawable.dialog_top_shape);
		titleShape.setColor(backgroundColor + 0xFF000000);
		titleContainer.setBackgroundDrawable(titleShape);
		
		// Set icon
		ImageView iconImageView = (ImageView) mainContainer.findViewById(R.id.dialogImg);
		iconImageView.setImageDrawable(icon);
		
		// Set text
		TextView titleTextView = (TextView) jumpToast.findViewById(R.id.dialogTitle);
		TextView messageTextView = (TextView) jumpToast.findViewById(R.id.dialogText);
		TextView message2TextView = (TextView) jumpToast.findViewById(R.id.dialogTextTwo);
		titleTextView.setText(title);
		messageTextView.setText(message);
		message2TextView.setText(message2);
		
		// Set Typeface
		Typeface tf = FontSingleton.getInstance(ReconDashboard.this).getTypeface();
		titleTextView.setTypeface(tf);
		messageTextView.setTypeface(tf);
		message2TextView.setTypeface(tf);
		
		//Show message 2 if appropriate
		message2TextView.setVisibility(View.VISIBLE);
		
		// Launch Toast
		//Toast toastView = new Toast(ReconDashboard.this);
		statsToast.setView(jumpToast);
		statsToast.show();
	}
}
