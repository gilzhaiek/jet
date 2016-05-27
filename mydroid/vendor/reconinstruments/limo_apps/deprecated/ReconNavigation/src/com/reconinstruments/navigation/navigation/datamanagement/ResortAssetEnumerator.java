/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.util.Log;

/**
 * This class enumerate available resort shape map
 * assets from a specfic location
 */
public class ResortAssetEnumerator
{
	static final String SHP_EXT = ".shp";
	private String mFolderName;						//which folder of the filesystem to look for all the resort assets
	public ArrayList<String> mAvailableAssets;		//the list of asset base name that are loaded on the device
	
	public ResortAssetEnumerator( String folderName )
	{
		mFolderName = folderName + "/lines";
		mAvailableAssets = new ArrayList<String>( 64 );
	}
	
	public void reset( )
	{
		mAvailableAssets.clear();
	}
	
	/**
	 * Look up the mFolderName for all *.shp files
	 * and record down its base file name to mAvailabeAssets list
	 * @throws FileNotFoundException 
	 */
	public void Enumerate( ) throws FileNotFoundException
	{
		//TODO: figure out how to fill the Available assets list
		//by parsing the key
		File folder = new File( mFolderName );
		if( folder.exists() && folder.isDirectory() )
		{
			//iterate through all existed files *.shp file
			String[] files = folder.list();
			for( String file : files )
			{
				int idx = file.lastIndexOf(SHP_EXT);
				if( idx > 0 )
				{
					//insert the asset string to the list by binary searching
					//since the mAvailableAssets is a sorted list
					String assetName = file.substring(0, idx);					
					boolean inserted = false;
					
					int start = 0;
					int end = mAvailableAssets.size() - 1;
					while( start <= end )
					{
						int mid = (start + end)/2;
						String asset = mAvailableAssets.get(mid);
						int result = assetName.compareTo(asset); 
						if( result < 0 )
						{
							end = mid - 1;
						}
						else if( result > 0 )
						{
							start = mid + 1;
						}
						else
						{
							//duplicated asset, report an error in log
							//but just skip it.
							Log.e("Error", "Find duplicated asset");
							inserted = true;
							break;
						}
					}
					
					if( inserted == false )
					{
						mAvailableAssets.add(start, assetName);
					}
				}
				
			}
			
		}
		else
		{
			Log.e("Error", mFolderName + " is not a existed folder of device file system" );
		}
		
		for( String txt : mAvailableAssets )
		{
			Log.d("Assets:", txt);
		}
	}
	
	//test if an asset with base assetName is available
	//in the device
	public boolean isAvailable( String assetName )
	{
/*
		//binary search the list for the assetName
		int start = 0;
		int end = mAvailableAssets.size() - 1;
		
		while( end >= start )
		{
			int mid = (start+end)/2;
			String curr = mAvailableAssets.get( mid );
			
			int result = assetName.compareTo(curr);
			if( result == 0 )
			{
				return true;
			}
			else if( result < 0 )
			{
				end = mid - 1;
			}
			else
			{
				start = mid + 1;
			}
		}
		return false;
*/
		return true;
	}
}
