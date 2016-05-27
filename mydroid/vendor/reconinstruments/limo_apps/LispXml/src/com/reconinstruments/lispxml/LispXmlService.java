package com.reconinstruments.lispxml;
import android.app.Service;
import java.io.*;
import android.os.FileObserver;
import android.os.Environment;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Base64;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;

public class LispXmlService extends Service {
    public static final String TAG = "LispXmlService";
    public static final String LONG_TERM_ACTION =
	"com.reconinstruments.lispxml.LispXmlService";
    public static final String BT_ACTION =
	"com.reconinstruments.lispxml.LispXmlService.BT";
    @Override
    public void onCreate() {
        super.onCreate(); 
	mSeedFileObserver.startWatching();
    }
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
	if (intent != null && LONG_TERM_ACTION.equals(intent.getAction())) {
	    return START_STICKY;
	}
	else if (intent!= null && BT_ACTION.equals(intent.getAction())) {
	    final Intent myintent = intent;
	    new Thread(new Runnable() {
		    public void run() {
			btRunProgram(myintent);
		    }
		}).start();
	    return START_NOT_STICKY;
	}
	else {
	    return START_NOT_STICKY;
	}
    }
    public IBinder onBind(Intent intent) {
	Log.d(TAG,"onBind");
	return new LocalBinder();
    }
    public class LocalBinder extends Binder {
	LispXmlService getService() {
	    return LispXmlService.this;
	}
    }
    public void onDestroy() {
	Log.d(TAG,"onDestroy");
        mSeedFileObserver.stopWatching();
        super.onDestroy();
    }

    private final static String PATH_TO_WATCH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ReconApps/LispXML/Input/";

    // Observe the folder into which the seed file is going to be
    // copied:
    FileObserver mSeedFileObserver =
        new FileObserver(PATH_TO_WATCH, FileObserver.MODIFY) {
            @Override
            public void onEvent(int event, String file) {
                Log.v(TAG,"Input File created or modified "+file);
                if (file != null) {
                    File f = new File(PATH_TO_WATCH+"/"+file);
		    try {
			String program = LispXmlActivity.readFileAsString(f);
			File outputFile = getOutputFile(f);
			Log.v(TAG,"outputFileName is "+outputFile.getName());
			String output = runProgram(LispXmlService.this, program);
			//Log.v(TAG,output);
			writeStringToFile(output,outputFile);
		    }
		    catch (Exception e) {
			e.printStackTrace();
		    }
                }
            }
        };

    static String runProgram (Context context, String program) {
	LispXmlParser p = new LispXmlParser(context, program);
	p.loadModule(new IOModule(p)); // enable IO
	p.loadModule(new MathModule(p)); // enable Math
	p.loadModule(new SettingsModule(p)); // enable Settings access
	p.loadModule(new AndroidModule(p)); // enable Android activity ...  access
	p.loadModule(new LocationModule(p)); // Location stuff
	String output = p.elementToString(p.execute());
	return output;
    }

    public static void writeStringToFile(String txt, File file)
	throws java.io.IOException {
	writeToFile(txt,file,false,false);
    }
    public static void writeStringToFile(String txt, File file, boolean isappend)
	throws java.io.IOException{
	writeToFile(txt,file,isappend,false);
    }
    public static void writeToFile(String txt, File file, boolean isappend,boolean isB64)
	throws java.io.IOException {
	PrintStream out = null;
	OutputStream out2 = null;
	try {
	    if (!isB64) {
		out = new PrintStream(new FileOutputStream(file.getAbsolutePath(),isappend));
		out.print(txt);
	    }
	    else {		// is b64
		byte[] data = Base64.decode(txt,Base64.DEFAULT);
		out2 = new FileOutputStream(file.getAbsolutePath(),isappend);
		out2.write(data);
	   } 
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (out != null)out.close();
	    if (out2 != null)out2.close();
	}
    }


    private final static String OUTPUT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ReconApps/LispXML/Output/";
    public File getOutputFile(File inputFile) {
	inputFile.mkdirs();
	return new File(OUTPUT_PATH, inputFile.getName()+".out");
    }

    private void btRunProgram(final Intent intent) {
	byte [] message = intent.getByteArrayExtra("message");
	if (message == null) {
	    Log.v(TAG,"bad message");
	    return;
	}
	HUDConnectivityMessage msg =
	    new HUDConnectivityMessage(message);
	String program = new String(msg.getData());
	String result = runProgram(this,program);
	// Send it back:
	Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");
	String returnAddress = msg.getSender();
	int requestKey = msg.getRequestKey();
	HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
	cMsg.setIntentFilter(returnAddress);
	cMsg.setRequestKey(requestKey);
	//cMsg.setSender(sBT_RETURN); // We don't really need the confirmation
	cMsg.setData(result.getBytes());
	i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());
	sendBroadcast(i);
    }
    
}
