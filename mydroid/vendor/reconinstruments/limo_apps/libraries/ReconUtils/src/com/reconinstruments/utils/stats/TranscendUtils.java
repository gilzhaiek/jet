package com.reconinstruments.utils.stats;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.reconinstruments.utils.BundleUtils;

import android.os.Environment;
import android.os.Message;
import android.os.Parcel;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Describe class <code>TranscendUtils</code> here. Code to
 * communicate with Transcend Service
 *
 */
public class TranscendUtils {
    public static final String BROADCAST_COMMAND =
            "com.reconinstruments.applauncher.transcend.BROADCAST_COMMAND";
    public static final String FULL_INFO_UPDATED =
            "com.reconinstruments.applauncher.transcend.FULL_INFO_UPDATED";
    public static final void sendCommandToTranscendService(Context c, int command) {
        sendCommandToTranscendService(c, command,0);
    }

    public static final  void sendCommandToTranscendService(Context c, int command, int arg1) {
        try {
            Message msg = Message.obtain(null,command,arg1, 0);
            msg.replyTo = null; // there is no any message returned back
            Intent i = new Intent(BROADCAST_COMMAND);
            i.putExtra("command",msg);
            c.sendBroadcast(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static final Bundle getFullInfoBundle(Context context) {
        Intent intent = context.registerReceiver(null,new IntentFilter(TranscendUtils.FULL_INFO_UPDATED));
        if (intent != null) {
            return (Bundle)intent.getParcelableExtra("FullInfo");
        }
        return null;
    }

    public static String getLastTripDataFileName(int sportType){
        return "simpleLatLng" + sportType + ".csv";
    }

    public  static final String SUMMARY_FOLDER = "ReconApps/TripData";

    // Bundle dumping/reading
    //////////////////////////////////////////

    public static final String bundleFilePathFromType(int aType) {
        return SUMMARY_FOLDER+"/summary_"+aType+".raw";
    }

    public static final File bundleFileFromType(int aType) {
        File path = Environment.getExternalStorageDirectory();
        return new File(path, bundleFilePathFromType(aType));
    }

    public static void dumpFullInfoBundleIntoExternalStorage(Bundle b, int aType) {
        File file = bundleFileFromType(aType);
        BundleUtils.writeBundleToFile(b, file);
    }

    public static Bundle readSummaryDumpIntoBundle(int aType) {
        File file = bundleFileFromType(aType);
        return BundleUtils.readBundleFromFile(file);
    }




    public static String parseElapsedTime(long elapsedTime, boolean secondsString) {
        int hours = (int) elapsedTime / 3600000;
        elapsedTime -= hours * 3600000;
        int minutes = (int) elapsedTime / 60000;
        elapsedTime -= minutes * 60000;
        int seconds = (int) elapsedTime / 1000;
        elapsedTime -= seconds * 1000;
        int hundredthSeconds = (int) elapsedTime / 10;

        if (secondsString) {
            String f = "";
            if (hours > 0)
                f += String.format("%02d", hours) + ":";
            return f + String.format("%02d", minutes) + ":"
                    + String.format("%02d", seconds);
        }
        return "." + String.format("%02d", hundredthSeconds);
    }
}