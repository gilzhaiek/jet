package com.reconinstruments.mapsdk.mapview;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.util.Log;

public class LoadFromResourceUtil {
	
	private static final String TAG = LoadFromResourceUtil.class.getSimpleName();
	
	public static Typeface getFontFromRes(Context context, String filename){
		int id = context.getResources().getIdentifier(filename, "raw",  context.getPackageName());
		if(id == 0) {
			Log.e(TAG, "Could not find raw resource " + filename);
			return null;
		}
		return getFontFromRes(context, id);
	}

	public static Typeface getFontFromRes(Context context, int resource)
	{ 
	    Typeface tf = null;
	    InputStream is = null;
	    try {
	        is = context.getResources().openRawResource(resource);
	    }
	    catch(NotFoundException e) {
	        Log.e(TAG, "Could not find font in resources!");
	        return null;
	    }

	    String outPath = context.getCacheDir() + "/tmp" + System.currentTimeMillis() + ".raw";

	    try
	    {
	        byte[] buffer = new byte[is.available()];
	        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outPath));

	        int l = 0;
	        while((l = is.read(buffer)) > 0)
	            bos.write(buffer, 0, l);

	        bos.close();

	        tf = Typeface.createFromFile(outPath);

	        // clean up
	        new File(outPath).delete();
	    }
	    catch (IOException e)
	    {
	        Log.e(TAG, "Error reading in font!");
	        return null;
	    }

	    Log.d(TAG, "Successfully loaded font.");

	    return tf;      
	}
}
