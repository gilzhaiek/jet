package com.reconinstruments.root;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class RootMODLiveActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	File srcFile = new File("/default.prop");
    	InputStream inTape = null;
    	OutputStream outTape = null;
    	int numBytes;
    	byte[] mBytes = new byte[(int)srcFile.length()];

    	    try {
				inTape = new FileInputStream(srcFile);
			    numBytes  = inTape.read(mBytes);
	    	    inTape.close();
	    	    if (numBytes > -1){
	    	    	String s = new String(mBytes);
	    	    	Log.d("rooter",s);
	    	    	s=  s.replaceAll("ro.secure=1", "ro.secure=0");
	    	    	Log.d("rooter",s);
	    	    	outTape = new FileOutputStream(srcFile,false);
	    	    	outTape.write(s.getBytes());
	    	    	outTape.close();
	    	    }
	    	    
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	
    }


}