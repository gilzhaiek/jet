
package com.reconinstruments.messagecenter.frontend;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;

import com.reconinstruments.messagecenter.R;

/**
 * <code>UIUtils</code> provides some common functions for rendering UI.
 */
public class UIUtils {

    public static final String GROUP_PHONE = "MISSED CALLS";
    public static final String GROUP_TEXTS = "TEXTS";
    public static final String GROUP_FACEBOOK = "FACEBOOK";

    /**
     * convert full name to first name + last initial
     * 
     * @param fullname
     */
    public static String lastInitial(String fullname) {
        String res = "";
        if(fullname.contains("(") || fullname.contains(")") || fullname.contains("+")){ // phone format
            return fullname;
        }
        String names[] = fullname.split(" ");
        if (names.length > 1) {
            res = names[0] + " " + names[1];
        }else{
            res = fullname; // single word
        }
        return res;
    }

    /**
     * convert a date data into a proper format comparing to the current
     * timestamp
     * 
     * @param date
     * @return
     */
    public static String formatTimestamp(Date date) {
        String timestamp = "";
        long duration = System.currentTimeMillis() - date.getTime();
        if (duration / 1000 < 60) {
            timestamp = "1 min ago";
        } else if (duration / (60 * 1000) < 60) {
            timestamp = duration / (60 * 1000) + " mins ago";
        } else if (duration / (60 * 1000) < 2 * 60) {
            timestamp = "1 hour ago";
        } else if (duration / (60 * 60 * 1000) < 24) {
            timestamp = duration / (60 * 60 * 1000) + " hours ago";
        } else if (duration / (60 * 60 * 1000) < 48) {
            timestamp = "Yesterday";
        } else {
            timestamp = (new SimpleDateFormat("MMM dd, yyyy")).format(date);
        }
        return timestamp;
    }


    public static String dateFormat(Date date) {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
    }

    public static Drawable getDrawableFromAPK(PackageManager pacMan, String apk, int iconRes) {
        if (iconRes != 0) {
            try {
                Resources res = pacMan.getResourcesForApplication(apk);
                return res.getDrawable(iconRes);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
