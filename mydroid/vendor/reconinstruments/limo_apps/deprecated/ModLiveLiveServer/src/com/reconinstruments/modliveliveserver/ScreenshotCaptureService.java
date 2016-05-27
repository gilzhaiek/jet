package com.reconinstruments.modliveliveserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class ScreenshotCaptureService extends Service {

	// debugging
	private static final String TAG = "ScreenshotBroadcastService";
	private static final Boolean D = true;
	
	// image size
	public static final int FRAME_BUFFER_SIZE = (428 * 240) * 2;
	private Bitmap screenshot = Bitmap.createBitmap(428, 240, Bitmap.Config.RGB_565);
	
	private CaptureServiceReceiver mReceiver = new CaptureServiceReceiver();
	private ScreenshotHandler ssHandler = new ScreenshotHandler();
    private boolean RUNNING = false;
    
    // Lookup tables for fast 5bit and 6bit to 8 bit conversions
    private int[] fiveToEightBitTable = new int[32];
    private int[] sixToEightBitTable = new int[64];
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        
        registerReceiver(mReceiver, new IntentFilter("MOD_LIVE_LIVE_START"));
        registerReceiver(mReceiver, new IntentFilter("MOD_LIVE_LIVE_STOP"));
        registerReceiver(mReceiver, new IntentFilter("MOD_LIVE_LIVE_IS_RUNNING"));
        
        for(int i=0; i<32; i++) {
        	fiveToEightBitTable[i] = i * 255 / 31;
        }
        
        for(int i=0; i<64; i++) {
        	sixToEightBitTable[i] = i * 255 / 63;
        }
        
        startRecording();
        
        return START_STICKY;
    }
	
	@Override
	public void onDestroy() {
		stopRecording();
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	
	public boolean isRunning() {
		return RUNNING;
	}
	
	public void startRecording() {
		if(!RUNNING) {
			RUNNING = true;
        	ssHandler.sendMessageDelayed(new Message(), 0); // Take a screenshot
        	
        	Intent myi = new Intent();
    		myi.setAction("MOD_LIVE_LIVE_RUNNING");
    		Context c = getApplicationContext();
    		c.sendBroadcast(myi);
		}
	}
	
	public void stopRecording() {
		RUNNING = false;
		
		Intent myi = new Intent();
		myi.setAction("MOD_LIVE_LIVE_STOPPED");
		Context c = getApplicationContext();
		c.sendBroadcast(myi);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
		ScreenshotCaptureService getService() {
            return ScreenshotCaptureService.this;
        }
    }
	
	class CaptureServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals("MOD_LIVE_LIVE_START")) {
				startRecording();
			}
			
			else if (action.equals("MOD_LIVE_LIVE_STOP")) {
				stopRecording();
			}
			
			else if (action.equals("MOD_LIVE_LIVE_IS_RUNNING")) {
				Intent myi = new Intent();
	    		myi.setAction("MOD_LIVE_LIVE_RUNNING");
	    		Context c = getApplicationContext();
	    		c.sendBroadcast(myi);
			}
		}
		
	}
	
	class ScreenshotHandler extends Handler {
    	public void handleMessage(Message msg) {
    		//long startTime = SystemClock.elapsedRealtime();
    		if(!RUNNING)
    			return;
    		
    		try {
	    		// Capture framebuffer
    			File frameBuffer = new File("/dev/graphics/fb0");
	    		FileInputStream fInStr = new FileInputStream(frameBuffer);
	    		
	    		byte[] frame = new byte[FRAME_BUFFER_SIZE];
	    		fInStr.read(frame, 0, FRAME_BUFFER_SIZE);
	    		fInStr.close();
	    		
	    		int[] pixels = new int[FRAME_BUFFER_SIZE / 2];
	    		for(int i=0; i < FRAME_BUFFER_SIZE; i+=2) {
	    			pixels[i / 2] = rgb565torgb256(frame[i+1], frame[i]);
	    		}
	    		
	    		screenshot.setPixels(pixels, 0, 428, 0, 0, 428, 240);
	    		
	    		
	    		File file = new File("/mnt/storage/ReconApps/ModLiveLive/ss_canvas.jpg");
	    		file.setWritable(true, false);
	    		file.getParentFile().mkdirs();
	    		//file.createNewFile();
	    		FileOutputStream out = new FileOutputStream(file);
	    		screenshot.compress(Bitmap.CompressFormat.JPEG, 80, out);
	    		out.close();
	    		
	    		if(file.exists())
	    			Runtime.getRuntime().exec("mv /mnt/storage/ReconApps/ModLiveLive/ss_canvas.jpg /mnt/storage/ReconApps/ModLiveLive/ss.jpg");
	    		else
	    			Log.e("TAG", "file just written no longer exists?");
	    		
    		} catch (Exception e) {
    			Log.e("ModLiveLive", e.toString());
    			e.printStackTrace();
    		}
    		
    		//Log.v(TAG, "Screenshot took: " + (SystemClock.elapsedRealtime() - startTime));
    		
    		// Take another screenshot in 100s
    		ssHandler.sendMessageDelayed(new Message(), 100);
        }
    }
	/***********************************************************
	 * Not used, but good reference
	 ***********************************************************/
	
	@SuppressWarnings("unused")
	private Bitmap loadBitmap() throws Exception {
		File path = Environment.getExternalStorageDirectory();
		File file = new File(path, "ReconApps/ModLiveLive/ss.dat");
		FileInputStream fInStr = new FileInputStream(file);
		
		byte[] frame = new byte[FRAME_BUFFER_SIZE];
		fInStr.read(frame, 0, FRAME_BUFFER_SIZE);
		
		Bitmap screenshot = Bitmap.createBitmap(428, 240, Bitmap.Config.RGB_565);
		
		int[] pixels = new int[FRAME_BUFFER_SIZE / 2];
		
		for(int i=0; i < FRAME_BUFFER_SIZE; i+=2) {
			pixels[i / 2] = rgb565torgb256(frame[i+1], frame[i]);
		}
		
		screenshot.setPixels(pixels, 0, 428, 0, 0, 428, 240);
		return screenshot;
	}
    
    private int rgb565torgb256(byte b1, byte b2) {
    	byte red = (byte) (((b1 & 0xF8) >> 3) & 0x1f);
    	byte green = (byte) (((b1 & 0x07) << 3) + ((b2 & 0xE0) >> 5));
    	byte blue = (byte) (b2 & 0x1F);
    	
    	return Color.argb(0xFF, fiveToEightBitTable[red], sixToEightBitTable[green], fiveToEightBitTable[blue]);
    }
}
