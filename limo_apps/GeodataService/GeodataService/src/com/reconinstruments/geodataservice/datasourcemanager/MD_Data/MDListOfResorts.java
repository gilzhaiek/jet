package com.reconinstruments.geodataservice.datasourcemanager.MD_Data;

import java.util.ArrayList;


public class MDListOfResorts  
{
// constants
	private final static String TAG = "ResortsInfo";

// members	
	protected boolean mIsCorrupted = false;
	public		ArrayList<MDResortInfo>		mResorts = null;
	
// methods
	public MDListOfResorts() {
		mResorts = new ArrayList<MDResortInfo>();
	}
	
	public boolean IsCorrupted() {return mIsCorrupted;}

	public void SetCorrupted(boolean value) {mIsCorrupted = value;}
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof MDListOfResorts; 
	}
	
	public void AddResort(MDResortInfo resortInfo) {
		int index = 0;
		
		if( mResorts.size() != 0 ) {			
			for(MDResortInfo tmpResortInfo : mResorts ) {
				if( tmpResortInfo.mName.compareTo(resortInfo.mName) >= 0 ) {			
					break;
				}
				index++;
			}
		}
		mResorts.add(index, resortInfo );
	}
}
