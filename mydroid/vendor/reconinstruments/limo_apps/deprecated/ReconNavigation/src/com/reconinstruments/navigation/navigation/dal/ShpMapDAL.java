package com.reconinstruments.navigation.navigation.dal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

//import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.navigation.navigation.PoInterest;
import com.reconinstruments.navigation.navigation.CustomPoiManager.CustomPoi;
import com.reconinstruments.navigation.navigation.ShpMap;

public class ShpMapDAL {
	static final String SMP_FILE_EXT = ".smp"; // Shape Map Preference
	static final String TAG = "ShpMapDAL";  
	
	static final String SMP_TRACKED_POI_ARRAY="TrackedPOIArray";
	static final String SMP_TRACKED_POI_NAME="Name";
	static final String SMP_TRACKED_POI_TYPE="Type";
	static final String SMP_TRACKED_POI_X_POSITION="XPosition";
	static final String SMP_TRACKED_POI_Y_POSITION="YPosition";
	 
	protected static String GetParcelFileName(int resortID)
	{
		return "shp_map_" + Integer.toString(resortID) + SMP_FILE_EXT;
	}	
	
	protected static void UpdateShpMapPoInterests(ShpMap shpMap, ArrayList<Bundle> trackedPoInterestsBundle)
	{
		for( int poiType = 0; poiType < shpMap.mPoInterests.size();  poiType++)
		{
			for( PoInterest poi : shpMap.mPoInterests.get(poiType) )
			{
				for( Bundle bundle : trackedPoInterestsBundle)
				{
					if(poi.mName.equals(bundle.getString(SMP_TRACKED_POI_NAME)) && poi.getType() == bundle.getInt(SMP_TRACKED_POI_TYPE))
					{
						if(	poi.mPosition.x == bundle.getFloat(SMP_TRACKED_POI_X_POSITION) &&
							poi.mPosition.y == bundle.getFloat(SMP_TRACKED_POI_Y_POSITION))
						{
							poi.setStatus(PoInterest.POI_STATUS_TRACKED);
						}
					}
				}
			}
		}
	}
	
	public static void Load(Context context, int resortID, ShpMap shpMap) {
		File file =   new File(context.getFilesDir(), GetParcelFileName(resortID));
		
		//make sure that the file exists and is readable
		if( file.exists() && file.canRead() )
		{
			int fileSize = (int)file.length();
			Log.d(TAG,"File size is "+fileSize);
			
			if ( fileSize > 0 )
			{
			    try {
			    	FileInputStream fileInputStream = context.openFileInput(GetParcelFileName(resortID));
			    	byte [] byteArray = new byte[fileSize];
			    	
			    	fileInputStream.read(byteArray);
			    	fileInputStream.close();
			    	
			    	final Parcel parcel = Parcel.obtain();
			    	parcel.unmarshall(byteArray,0,fileSize);

			    	parcel.setDataPosition(0);
			    	Bundle bundle = parcel.readBundle();

			    	try
			    	{			    	
			    		ArrayList<Bundle> trackedPoInterestsBundle = bundle.getParcelableArrayList(SMP_TRACKED_POI_ARRAY);			    	
			    		UpdateShpMapPoInterests(shpMap, trackedPoInterestsBundle);
			    	}
			    	catch (Exception e) {}
			    }
			    catch (IOException e){
			    	Log.e(TAG,"Couldn't read User Map Preference" );
			    	e.printStackTrace();
			    }
			}
		}			
	}
	
    protected static Bundle GenerateBundlePoInterest( PoInterest poi ) {
    	Bundle bundle = new Bundle();		
	
    	bundle.putString(SMP_TRACKED_POI_NAME, poi.mName) ;
    	bundle.putInt(SMP_TRACKED_POI_TYPE, poi.getType()) ;
    	bundle.putFloat(SMP_TRACKED_POI_X_POSITION, poi.mPosition.x) ;
    	bundle.putFloat(SMP_TRACKED_POI_Y_POSITION, poi.mPosition.y) ;
    	
    	return bundle;
    }

    protected static Bundle GenerateBandle(int resortID, ShpMap shpMap)
    {
    	Bundle bundle = new Bundle();
		ArrayList<Bundle> trackedPoInterests = new ArrayList<Bundle>();
		
		{ // Tracked PoInterests
			for( int poiType = 0; poiType < shpMap.mPoInterests.size();  poiType++)
			{
				// Buddy, Owner and Pins don't need to be saved here
				if(!( poiType == PoInterest.POI_TYPE_BUDDY  || poiType == PoInterest.POI_TYPE_OWNER || poiType == PoInterest.POI_TYPE_CDP))
				{				
					for( PoInterest poi : shpMap.mPoInterests.get(poiType) )
					{
						// The pins (CDP) are saved someplace else
						if( poi.getStatus() == PoInterest.POI_STATUS_TRACKED )
						{
							trackedPoInterests.add(GenerateBundlePoInterest(poi));
						}				
					}
				}
			}
			bundle.putParcelableArrayList(SMP_TRACKED_POI_ARRAY,trackedPoInterests);
		}    
		
		return bundle;
    }
		
	public static void Save(Context context, int resortID, ShpMap shpMap)
	{	
		Log.d(TAG,"Saving...");
				
		Parcel parcel = Parcel.obtain();
		Bundle bundle = GenerateBandle(resortID, shpMap);
		bundle.writeToParcel(parcel, 0);
		
		try {
			FileOutputStream fileOutputStream = context.openFileOutput( GetParcelFileName(resortID), Context.MODE_PRIVATE);
			
		    Log.d(TAG,"Attempt to marshal and write");
		    fileOutputStream.write(parcel.marshall());
		    Log.d(TAG,"Attempt to flush");
		    fileOutputStream.flush();
		    fileOutputStream.close();
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
}
