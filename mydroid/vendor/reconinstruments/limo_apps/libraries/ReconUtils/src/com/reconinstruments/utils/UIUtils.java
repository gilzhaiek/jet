package com.reconinstruments.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.content.Context;

public class UIUtils {
    public static Intent sButtonHoldIntent = new Intent("com.reconinstruments.inputs.BUTTON_HOLD_BEAVIOUR_CHANGED");

    private static Map<Integer, Typeface> fontTypefaces = new HashMap<Integer, Typeface>();
    
	public static String dateFormat(Date date){
		return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
	}
	public static Drawable getDrawableFromAPK(PackageManager pacMan,String apk,int iconRes){
		if(iconRes!=0){
			try{
				Resources res = pacMan.getResourcesForApplication(apk);
				return res.getDrawable(iconRes);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}catch (NotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

    public static void setButtonHoldShouldLaunchApp(Context context,boolean behav) {
	//context.removeStickyBroadcast(sButtonHoldIntent);
	sButtonHoldIntent.putExtra("shouldLaunchApp",behav);
	//context.sendStickyBroadcast(sButtonHoldIntent);
	context.sendBroadcast(sButtonHoldIntent);

    }
    
    public static Typeface getFontFromRes(Context context, int resource)
    {
        Typeface tf = fontTypefaces.get(resource);
        if(tf != null){ // if tf is in the cache, return it from the cache
            return tf;
        }
        
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(resource);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        String outPath = context.getCacheDir() + "/tmp" + resource + ".raw";
        try
        {
            byte[] buffer = new byte[is.available()];
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outPath));

            int l = 0;
            while ((l = is.read(buffer)) > 0)
                bos.write(buffer, 0, l);

            bos.close();

            tf = Typeface.createFromFile(outPath);

            // clean up
            new File(outPath).delete();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        
        fontTypefaces.put(resource, tf); //put this tf into the cache to reuse it later
        
        return tf;
    }
}
