package com.reconinstruments.qa;

//import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStreamReader;
import java.io.OutputStream;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
//import android.widget.TextView;

public class QatoolsActivity extends Activity {
	
	//Variables
	private Handler mHandler = new Handler();
	private Process exceptionLogger;
	//Runnable
	private Runnable StopLogcatDump = new Runnable() {
	    public void run() 
	    {
	    	Log.w("Logcat Logger","Stooooooooooooooop");
	    	exceptionLogger.destroy();               
	    }
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        checkroot();
        final Button button_Root = (Button) findViewById(R.id.Root);
        button_Root.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rooting();          
            }
        });
        final Button button_log = (Button) findViewById(R.id.LogDump);
        button_log.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logcatdump();
            }
        });
    }
    
    void checkroot()
    {
    	File srcFile = new File("/default.prop");
    	Button button = (Button) findViewById(R.id.Root);
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
	    	    	Log.d("rooting",s);
	    	    	boolean check=false;
	    	    	String strs[] = s.split("\n");
	    	    	for (int i = 0; i < strs.length; i++)
	    	    	      if (strs[i].matches("ro.secure=1")==true){check=true;};
	    	    	
	    	    	if (check == true){
	    	    		button.setText("Root");
	    	    	}
	    	    	else if (check == false){
	    	    		button.setText("UnRoot");
		    	    }
	    	    		
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
    void rooting(){
    	File srcFile = new File("/default.prop");
    	InputStream inTape = null;
    	OutputStream outTape = null;
    	int numBytes;
    	String Message=null;
    	byte[] mBytes = new byte[(int)srcFile.length()];

    	    try {
				inTape = new FileInputStream(srcFile);
			    numBytes  = inTape.read(mBytes);
	    	    inTape.close();
	    	    if (numBytes > -1){
	    	    	String s = new String(mBytes);
	    	    	Log.d("rooting",s);
	    	    	boolean check=false;
	    	    	String strs[] = s.split("\n");
	    	    	for (int i = 0; i < strs.length; i++)
	    	    	      if (strs[i].matches("ro.secure=1")==true){check=true;};
	    	    	
	    	    	if (check == true){
	    	    	s=  s.replaceAll("ro.secure=1", "ro.secure=0");
	    	    	Message = "A reboot is needed. The unit will reboot to finish the root process";
	    	    	Log.d("rooter",s);
	    	    	}
	    	    	else if (check == false){
		    	    	s=  s.replaceAll("ro.secure=0", "ro.secure=1");
		    	    	Message = "A reboot is needed. The unit will reboot to finish the unroot process";
		    	    	Log.d("unrooter",s);
		    	    }
	    	    		
	    	    	outTape = new FileOutputStream(srcFile,false);
	    	    	outTape.write(s.getBytes());
	    	    	outTape.close();
	    	    	AlertBox("WARNING",Message);
	    	    }
	    	    
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }
    void Logcatdump(){
        String filename = null;     
        String directory = null;     
        String fullPath = null;     
        String externalStorageState = null;      // The directory will depend on if we have external storage available to us or not     
        try{
        filename = String.valueOf(System.currentTimeMillis()) + ".log";         
        	externalStorageState = Environment.getExternalStorageState();          
        	if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {             
        		if(android.os.Build.VERSION.SDK_INT <= 7) {                 
        			directory = Environment.getExternalStorageDirectory().getAbsolutePath();             
        			} else {                 
        				directory = getExternalFilesDir(null).getAbsolutePath();
        				}         
        		} else {             
        			directory = getFilesDir().getAbsolutePath();
        				}          
        	fullPath = directory + File.separator + filename;
        	Log.w("Logcat Logger", fullPath);
        	mHandler.postDelayed(StopLogcatDump, 10000);
        	/*Process*/ exceptionLogger = Runtime.getRuntime().exec("logcat -f " + fullPath);
    } catch (Exception e) {         
		Log.e("ProfilerService", e.getMessage());
		} 
    }
    
    void AlertBox(String Title,String Message){
    	AlertDialog deleteAlert = new AlertDialog.Builder(this).create();
    	deleteAlert.setTitle(Title);
    	deleteAlert.setMessage(Message);
    	deleteAlert.setButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              try
              {
            	  /*Process process = */Runtime.getRuntime().exec("reboot");
              }
              catch (IOException e) {}
              
              }
           }
    	);
    	deleteAlert.show();
    }
}