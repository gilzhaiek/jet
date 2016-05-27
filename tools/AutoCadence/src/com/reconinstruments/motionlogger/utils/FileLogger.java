package com.reconinstruments.autocadence.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class FileLogger {
    protected static final String TAG = "SensorsFileLogger";
    private final File file = Environment.getExternalStorageDirectory();
    private FileOutputStream fos;

    public static boolean isWriting = false;

    public boolean Activate(String folderName, String fileName){
        Log.d(TAG, file.getPath());
        if(!file.canWrite()){
            Log.d(TAG," NOT Writeable and exit app");
            return false;
        }
        try {
            fos = new FileOutputStream(file.getPath() + "/" + folderName + fileName, false);
        } catch (IOException e) {
            Log.e("TAG","WRITE ERROR");
            return false;
        }
        return true;
    }

    public boolean DeActivate(){
        try {

            if(fos !=null){
                while(isWriting){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                fos.close();
                fos = null;
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "error Closing output stream" + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    public boolean WriteToFile(String result){
        isWriting = true;
        if(fos !=null){
            try {
                //Log.d(TAG,result);
                fos.write(result.getBytes());
                fos.flush();
                isWriting = false;
                return true;
            } catch (IOException e) {
                Log.e(TAG, "error Writing to File" + e.toString());
                e.printStackTrace();
                isWriting = false;
                return false;
            }

        }
        return false;
    }
}
