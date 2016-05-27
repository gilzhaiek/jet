package com.reconinstruments.jumpvisualiser;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

public class ClosedFileObserver extends FileObserver
{
	public static final String TAG = "InstallerFileObserver";
	
	private Context mContext = null;
	private Jump mJump = null;
	
	public ClosedFileObserver(Context context)
	{
	    super(Environment.getExternalStorageDirectory() + Util.PATH, FileObserver.CLOSE_WRITE);
		mContext = context;
	}
	
	public void setJump(Jump jump)
	{
		mJump = jump;
	}

	@Override
	public void onEvent(int event, String path)
	{
		File file = new File( Environment.getExternalStorageDirectory() + Util.PATH + path );
		Log.i("FILE_OBSERVER", "file.name: " + file.getName());
		if( file.exists() && file.isFile() && file.getName().equalsIgnoreCase(Util.NAME))
		{
			Util.uploadJumpImageToFB(mJump, mContext);
			stopWatching();
		}
	}	
}