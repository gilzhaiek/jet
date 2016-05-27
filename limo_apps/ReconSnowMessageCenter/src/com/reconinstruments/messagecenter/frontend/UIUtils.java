package com.reconinstruments.messagecenter.frontend;

import java.text.DateFormat;
import java.util.Date;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;

public class UIUtils {

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
}
