package com.reconinstruments.dashlauncher.livestats;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.reconinstruments.applauncher.transcend.ReconTranscendService;
import com.reconinstruments.dashlauncher.DashLauncherApp;
import com.reconinstruments.dashlauncher.HUDServiceHelper;
import com.reconinstruments.dashlauncher.MODServiceConnection;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector;
import com.reconinstruments.dashlauncher.connect.SmartphoneConnector.DeviceType;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconChronoWidget4x1;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconDashboardHashmap;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconDashboardWidget;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconWidgetHolder;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;
import com.reconinstruments.modlivemobile.music.MusicDBFrontEnd;
import com.reconinstruments.modlivemobile.music.ReconMediaData.ReconSong;
import com.reconinstruments.modservice.ReconMODServiceMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CustomDashFragment extends Fragment {

	public static final String TAG = "CustomDashFragment";
	//private String layoutXML = "";
	private ReconDashboardLayout layout;

	private MODServiceConnection mMODConnection;
	private Messenger mMODConnectionMessenger;
	private MusicBroadcastReceiver mMusicReceiver;

	private Bundle mFullInfoBundle = null; // Most recent full info bundle
	
	private static String currentSong = null;

	public CustomDashFragment() {
		super();
	}
	
	public static CustomDashFragment newInstance(String layoutXML) {
		CustomDashFragment c = new CustomDashFragment();
		
		// Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("layout", layoutXML);
        c.setArguments(args);

        return c;
	}
	
	public void onResume() {
		super.onResume();

		// Set up connection to MOD Service
		mMODConnection = new MODServiceConnection(this.getActivity()
				.getApplicationContext());
		mMODConnectionMessenger = new Messenger(new MODServiceHandler());
		mMODConnection.addReceiver(mMODConnectionMessenger);
		mMODConnection.doBindService();
		
		// Listen for music broadcasts
		mMusicReceiver = new MusicBroadcastReceiver();
		this.getActivity().registerReceiver(mMusicReceiver, new IntentFilter(XMLMessage.MUSIC_MESSAGE));
		this.getActivity().registerReceiver(mMusicReceiver, new IntentFilter(XMLMessage.SONG_MESSAGE));
		
		if(SmartphoneConnector.lastDevice() == SmartphoneConnector.DeviceType.ANDROID) {
			// Update music player state
			MusicMessage musicMsg = new MusicMessage(MusicMessage.Action.GET_PLAYER_STATE);
			
//			HUDServiceHelper.getInstance(DashLauncherApp.getInstance().getApplicationContext()).broadcastMessage(DashLauncherApp.getInstance(), musicMsg.toXML());
			
			BTCommon.broadcastMessage(DashLauncherApp.getInstance(), musicMsg.toXML());
		} 
		currentSong = getCurrentSongString(MusicHelper.getSong(getActivity()));
	}

	public void onPause() {
		super.onPause();

		mMODConnection.doUnBindService();
		this.getActivity().unregisterReceiver(mMusicReceiver);
	}
	
	public String getLayout() {
		String layoutXML = getArguments() != null ? getArguments().getString("layout") : "";
		return layoutXML;
	}

	public boolean hasChronoWidget() {
		return layout.hasChrono();
	}

	public void toggleChrono() {
		Log.v(TAG, "toggleChrono");
		if (hasChronoWidget()) {
		    if (!(mFullInfoBundle != null &&
			 mFullInfoBundle.getBundle("CHRONO_BUNDLE") != null)) {
			return;
		    }
		    if (!mFullInfoBundle.getBundle("CHRONO_BUNDLE").getBoolean("IsRunning")) {
				Log.v(TAG, "new trial");
				try {
					Message msg = Message.obtain(null,
							ReconTranscendService.MSG_CHRONO_START_NEW_TRIAL,
							0, 0);
					msg.replyTo = mMODConnectionMessenger;
					mMODConnection.sendMessage(msg);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			} else {
				Log.v(TAG, "stop trial");
				try {
					Message msg = Message.obtain(null,
							ReconTranscendService.MSG_CHRONO_STOP_TRIAL, 0, 0);
					msg.replyTo = mMODConnectionMessenger;
					mMODConnection.sendMessage(msg);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return buildViewFromXML(getLayout());
	}

	private View buildViewFromXML(String xml) {
		ReconDashboardHashmap dashboardHashmap = new ReconDashboardHashmap();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			Document doc = db.parse(is);

			// get layout element
			Node layoutNode = doc.getElementsByTagName("layout").item(0);
			String id = layoutNode.getAttributes().getNamedItem("layout_id")
					.getNodeValue();
			layout = new ReconDashboardLayout(id, this.getActivity()
					.getApplicationContext());

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
					int widgetId = dashboardHashmap.WidgetHashMap
							.get(widgetName);
					ReconDashboardWidget widget = ReconDashboardWidget
							.spitReconDashboardWidget(widgetId, this
									.getActivity().getApplicationContext());

					// Add widget to widget container of the layout object
					layout.mAllWidgets.add(widget);
				}
			}

			layout.populate();

			return layout.mTheBigView;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private void updateFields(Bundle fullInfoBundle) {
		for (ReconDashboardWidget r : layout.mAllWidgets) {
			r.updateInfo(fullInfoBundle);
		}
	}

	class MODServiceHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ReconMODServiceMessage.MSG_RESULT:
				if (msg.arg1 == ReconMODServiceMessage.MSG_GET_FULL_INFO_BUNDLE) {
					Bundle data = msg.getData();

					mFullInfoBundle = data;
					
					// put song info in
					if(SmartphoneConnector.lastDevice() == SmartphoneConnector.DeviceType.IOS) {
						
						mFullInfoBundle.putString("CURRENT_SONG", currentSong);
						
					} else if(SmartphoneConnector.lastDevice() == SmartphoneConnector.DeviceType.ANDROID) {
						mFullInfoBundle.putString("CURRENT_SONG", currentSong);
					} else {
						currentSong = null;
						mFullInfoBundle.putString("CURRENT_SONG", currentSong);
					}
					
					updateFields(mFullInfoBundle);
				}
				break;

			default:
				super.handleMessage(msg);
			}
		}

	}
	
	class MusicBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(XMLMessage.MUSIC_MESSAGE)) {
				Bundle bundle = intent.getExtras();
				String message = bundle.getString("message");
				
				ReconSong song = MusicHelper.getSong(context, intent.getAction(), message);
				
				currentSong = getCurrentSongString(song);

				updateFields(mFullInfoBundle);
			}
		}
	}
	public String getCurrentSongString(ReconSong song){
		if(song!=null) {
			return song.artist + " - " + song.title;
		} else {
			return null;
		}
	}

	class ReconDashboardLayout {
		public ReconDashboardHashmap dashboardhash;
		public View mTheBigView;
		public ArrayList<ReconDashboardWidget> mAllWidgets;
		public String id;

		public ReconDashboardLayout(String layout_id, Context c) {
			dashboardhash = new ReconDashboardHashmap();
			id = layout_id;
			mAllWidgets = new ArrayList<ReconDashboardWidget>();
			// Let's inflate the baby:
			LayoutInflater inflater = (LayoutInflater) c
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTheBigView = inflater.inflate(
					dashboardhash.LayoutHashMap.get(layout_id), null);
		}

		public void populate() {
			// Goes through all
			for (int i = 0; i < mAllWidgets.size(); i++) {
				((ReconWidgetHolder) mTheBigView
						.findViewById(dashboardhash.PlaceholderMap.get(i)))
						.addView(mAllWidgets.get(i));

			}
		}

		public void updateInfo(Bundle fullInfo) {
			for (int i = 0; i < mAllWidgets.size(); i++) {
				mAllWidgets.get(i).updateInfo(fullInfo);
			}
		}

		public boolean hasChrono() {
			for (Object w : mAllWidgets) {
				if (w instanceof ReconChronoWidget4x1)
					return true;
			}
			return false;
		}

	}

}
