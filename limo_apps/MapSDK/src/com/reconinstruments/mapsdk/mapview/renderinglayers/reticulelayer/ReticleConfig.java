package com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.reconinstruments.mapsdk.R;

public class ReticleConfig {
// Constants
	private static final String TAG = "ReticleConfig";
	private final static String PROFILE_FOLDER = "ReconApps/MapData";

	public enum IconType {
		USER,
		BUDDY,
		SKI_RESORT
	}

	public class ReticleIconDefinition {
		IconType 	mType;
		int			mMaxNum;
		int			mMaxDistInM;
	}

	boolean			mReticleConfigurationNotLoadedCorrectly =false;

	public boolean 		mEnabled = true;
	public HashMap<IconType, ReticleIconDefinition> mIcons = new HashMap<IconType, ReticleIconDefinition>();

	public ReticleConfig(Resources res) {
		loadConfigXML(res);
		if(mReticleConfigurationNotLoadedCorrectly) {
			mEnabled = false;
		}
	}
	
	private void loadConfigXML(Resources res) {
		// create file path
		String fileName;
		XmlPullParser parser = Xml.newPullParser();
		BufferedReader br;
		try {
			File path = Environment.getExternalStorageDirectory();
//			File file = new File(path, PROFILE_FOLDER + "/" + "dsm_configuration.xml"); 
			File file = new File(path, PROFILE_FOLDER + "/" + "reticle_configuration.xml");
			br = new BufferedReader(new FileReader(file));
		    // auto-detect the encoding from the stream
		    parser.setInput(br);

		    boolean done = false;
		    int eventType = parser.getEventType();   // get and process event
		    
		    while (eventType != XmlPullParser.END_DOCUMENT && !done){
		        String name = null;
                
//		        name = parser.getName();
//                if(name == null) name = "null";
//		        Log.e(TAG, "eventType:"+eventType + "-"+ name);
//
		        switch (eventType){
		            case XmlPullParser.START_DOCUMENT:
		                name = parser.getName();
		                break;
		                
		            case XmlPullParser.START_TAG:
		                name = parser.getName();
		                if (name.equalsIgnoreCase("enable")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes < 1) {
		                		Log.e(TAG, "Bad object definition while parsing object in reticle_configuration file.  No attribute for enable tag.");
	               				mReticleConfigurationNotLoadedCorrectly = true;
		                	}
		                	else {
		                		String value = parser.getAttributeValue(0);
		                		if(value.equalsIgnoreCase("true")) 	{ 
		                			mEnabled = true; 
		                		}
		                		else {
		                			mEnabled = false; 
		                		}
		                	}
		                }
		                if (name.equalsIgnoreCase("icon")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes < 3) {
		                		Log.e(TAG, "Bad object definition while parsing object in reticle_configuration file.  Not enough attribute for icon tag.");
	               				mReticleConfigurationNotLoadedCorrectly = true;
		                	}
		                	else {
		                		ReticleIconDefinition newIcon = new ReticleIconDefinition();
		                		
		                		String iconType = parser.getAttributeValue(0);
		                		newIcon.mType = null;
		                		if(iconType.equalsIgnoreCase("user")) newIcon.mType = IconType.USER;
		                		if(iconType.equalsIgnoreCase("buddy")) newIcon.mType = IconType.BUDDY;
		                		if(iconType.equalsIgnoreCase("ski_resort")) newIcon.mType = IconType.SKI_RESORT;
		                		if(newIcon.mType == null) {
		                			mReticleConfigurationNotLoadedCorrectly = true;
			                		Log.e(TAG, "Bad object definition while parsing object in reticle_configuration file.  Unrecognized for icon type: " + iconType);
		                		}
		                		else {
		                			newIcon.mMaxNum = Integer.parseInt(parser.getAttributeValue(1));
		                			newIcon.mMaxDistInM = Integer.parseInt(parser.getAttributeValue(2));
		                		}
		                	}
		                }
		                
		                break;

		            case XmlPullParser.END_TAG:
		                name = parser.getName();
		                break;
		            
		            case XmlPullParser.TEXT:
		                break;
		            }
		        eventType = parser.next();
		        }
		} 
		catch (FileNotFoundException e) {
		    // TODO
			mReticleConfigurationNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (IOException e) {
		    // TODO
			mReticleConfigurationNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (Exception e){
		    // TODO
			mReticleConfigurationNotLoadedCorrectly = true;
			e.printStackTrace();

		}
			
		
	}
	
	
}
