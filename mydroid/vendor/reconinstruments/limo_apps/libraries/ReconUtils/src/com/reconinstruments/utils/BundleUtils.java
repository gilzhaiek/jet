package com.reconinstruments.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

public class BundleUtils {
    
    private static final String TAG = "BundleUtils";

    public static void writeBundleToFile(Bundle b, File file) {
        FileOutputStream os = null;
        try {
            file.getParentFile().mkdirs();
            os = new FileOutputStream(file, false); // Don't append
        } catch (IOException e) {
            Log.w(TAG,"Cannot open file "+file.getPath());
            return;
        }
        writeBundleToFile(b,os);
    }
    
    public static void writeBundleToFile(Bundle b, FileOutputStream os) {
        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        try {
            os.write(p.marshall());
            os.flush();
            os.close();
        } catch (IOException e) {
            Log.w(TAG, "Error writing bundle", e);
        } finally {
            p.recycle();
        }
    }
    
    public static Bundle readBundleFromFile(File file) {
        int fileSize = (int)file.length();
        final Parcel p = Parcel.obtain();
        if (fileSize > 0 ){
            try {
                FileInputStream f = new FileInputStream(file);
                byte [] byteArray = new byte[fileSize];
                f.read(byteArray);
                p.unmarshall(byteArray,0,fileSize);
                p.setDataPosition(0);
                f.close();
                return p.readBundle();
            }  catch (IOException e){
                Log.e(TAG,"Failed to read bundle from file: "+file.getPath());
                e.printStackTrace();
                return null;
            }
        } else {
            Log.w(TAG,"Failed to read bundle from file: "+file.getPath()+" file empty");
            return null;
        }
    }
}
