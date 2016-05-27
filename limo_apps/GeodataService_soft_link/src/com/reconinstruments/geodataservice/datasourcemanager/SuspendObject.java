package com.reconinstruments.geodataservice.datasourcemanager;

import java.io.File;

public class SuspendObject extends Object {
	public File 	mDestinationFilePath;
	public int	 	mTileID;
	
	public SuspendObject(int tileID, File filepath) {
		mDestinationFilePath = filepath;
		mTileID = tileID;
	}
}
