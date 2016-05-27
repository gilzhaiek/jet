package com.reconinstruments.navigation.navigation.dal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.reconinstruments.navigation.navigation.MapManager;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

public class MapManagerDAL {

	static final String MMP_FILE_EXT = ".smp"; // Map Manager Preference
	static final String TAG = "MapManagerDAL";  
	
	static final String MMP_LAST_RESORT_NAME="LastResortName";
	static final String MMP_LAST_RESORT_ID="LastResortID";
	static final String MMP_LAST_RESORT_COUNTRY_ID="LastResortCountryID";
	 
	protected static String GetParcelFileName()
	{
		return "map_mngr" + MMP_FILE_EXT;
	}	
	
	public static void Load(Context context, MapManager mapManager) {
		File file =   new File(context.getFilesDir(), GetParcelFileName());
		
		//make sure that the file exists and is readable
		if( file.exists() && file.canRead() )
		{
			int fileSize = (int)file.length();
			Log.d(TAG,"File size is "+fileSize);
			
			if ( fileSize > 0 )
			{
			    try {
			    	FileInputStream fileInputStream = context.openFileInput(GetParcelFileName());
			    	byte [] byteArray = new byte[fileSize];
			    	
			    	fileInputStream.read(byteArray);
			    	fileInputStream.close();
			    	
			    	final Parcel parcel = Parcel.obtain();
			    	parcel.unmarshall(byteArray,0,fileSize);

			    	parcel.setDataPosition(0);
			    	Bundle bundle = parcel.readBundle();
			    	
			    	try { // Last Resort
			    		mapManager.setLastResortName(bundle.getString(MMP_LAST_RESORT_NAME));
			    		mapManager.setLastResortID(bundle.getInt(MMP_LAST_RESORT_ID));
			    		mapManager.setLastResortCountryID(bundle.getInt(MMP_LAST_RESORT_COUNTRY_ID));
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

    protected static Bundle GenerateBandle(MapManager mapManager)
    {
    	Bundle bundle = new Bundle();
		
		{ // Last Resort
			if(mapManager.getActiveResort() == null)
			{
				bundle.putString(MMP_LAST_RESORT_NAME, "");
				bundle.putInt(MMP_LAST_RESORT_ID, -1);
				bundle.putInt(MMP_LAST_RESORT_COUNTRY_ID, -1);
			}
			else
			{
				bundle.putString(MMP_LAST_RESORT_NAME, mapManager.getLastResortName());
				bundle.putInt(MMP_LAST_RESORT_ID, mapManager.getLastResortID());
				bundle.putInt(MMP_LAST_RESORT_COUNTRY_ID, mapManager.getLastResortCountryID());
			}
		}
		
		return bundle;
    }
		
	public static void Save(Context context, MapManager mapManager )
	{	
		Log.d(TAG,"Saving...");
				
		Parcel parcel = Parcel.obtain();
		Bundle bundle = GenerateBandle(mapManager);
		bundle.writeToParcel(parcel, 0);
		
		try {
			FileOutputStream fileOutputStream =  context.openFileOutput( GetParcelFileName(), Context.MODE_PRIVATE);
			
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
