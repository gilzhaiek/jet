
package com.reconinstruments.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import android.util.Log;

public class SystemPropUtil {

    private static final String TAG = "SystemPropUtil";

    /*
     * Returns the value of the system property
     * @param <b>String name</b> name of the property
     * @return <b>String value</b> value of the property, if not defined, 1 is returned for now
     */
    public static String getSystemProp(String name) {
        InputStream inputstream = null;
        try {
            inputstream = Runtime.getRuntime().exec(new String[] {
                    "/system/bin/getprop", name
            }).getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String propval = "";
        try {
            propval = new Scanner(inputstream).useDelimiter("\\A").next();
            // Log.d(TAG, propval);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "SystemProperty - " + name + " : " + propval);
        return propval;
    }

    /**
     *
     * Sets the system property. Note that DashWarning is system user, so there's only a certain type of
     * system property it can set. See system/core/init/property_service.c.
     *
     * @param key - name of the property
     * @param val - value of the property to set
     */
    public static void setSystemProp(String key, String val) {
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/setprop", key, val});
            Log.d(TAG, "Set system property: " + key + " : " + val);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
