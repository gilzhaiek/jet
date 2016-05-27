package com.reconinstruments.dashlauncher;




import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.compass.CompassCalibrationService;
import com.reconinstruments.compass.SensorConfWriter;
import com.reconinstruments.dashlauncher.music.MusicHelper;
import com.reconinstruments.dashlauncher.packagerecorder.ReconPackageRecorder;
import com.reconinstruments.modlivemobile.dto.message.MusicMessage;
import com.reconinstruments.modlivemobile.dto.message.SongMessage;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;
import com.reconinstruments.modlivemobile.music.ReconMediaData.ReconSong;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.content.pm.PackageManager.NameNotFoundException;

public class DashLauncherApp extends Application
{
	private final static String TAG = "DashLauncherApp";
	public IBLEService bleService;
	private BLEServiceConnection bleServiceConnection;

    
    // FIXME: this code is duplicated from transcend service. We have
    // to call make it a cross package call either via jar or
    // something else.  We don't want to launch the entire transcend
    // service because of this functionality. We need all our
    // resources for running the videos. Alternatively we can move ID
    // file out of transcend.
    public void writeIDFile(){
	String ID_FOLDER = "ReconApps";
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, ID_FOLDER+"/"+"ID.RIB");
	try {
	    file.getParentFile().mkdirs();
	    OutputStream os = new FileOutputStream(file,false);//Don't append
	    byte[] data = new byte[] {(byte)0x02,(byte)0xfe,(byte)0xff,(byte)0x00,
				      (byte)(0x00+00),(byte)(0x0d),(byte)(0x5a ),
				      (byte)(0xff), (byte)(0xff), (byte)(0xff), (byte)(0xff)};
	    data[0] = 2;	// 2 fields
	    data[1] = (byte)0xfe;	// version number identifier
	    data[2] = (byte)0xff; 	// serial number identifier
	    int pos = 3;//
	    int version = 0;
	    try {
		PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
		version = pInfo.versionCode;
	    } catch (NameNotFoundException e1) {
		Log.e(this.getClass().getSimpleName(), "Name not found", e1);
	    }
	    // version info
	    int tmpInt = version;
	    data[pos++] = (byte)((tmpInt << 0 )>> 24);//MSB
	    data[pos++] = (byte)((tmpInt << 8 )>> 24);//MSB-1
	    data[pos++] = (byte)((tmpInt << 16 )>> 24);//MSB-2
	    data[pos++] = (byte)((tmpInt << 24 )>> 24);//LSB
	    
	    // serialnumber
	    try {
		tmpInt = Integer.parseInt(Build.SERIAL);
	    } catch (NumberFormatException e ) {
		Log.e(TAG,"Bad service");
	    }
	    Log.d("ID_FILE",tmpInt + "");
	    data[pos++] = (byte)((tmpInt << 0 )>> 24);//MSB
	    data[pos++] = (byte)((tmpInt << 8 )>> 24);//MSB-1
	    data[pos++] = (byte)((tmpInt << 16)>> 24);//MSB-2
	    data[pos++] = (byte)((tmpInt << 24)>> 24);//LSB
	    os.write(data);
	    os.close();
		        
	} catch (IOException e) {
	    // Unable to create file, likely because external storage is
	    // not currently mounted.
	    //Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
	    Log.w("ExternalStorage", "Error writing " + file, e);
	}
    }

	
	public static DashLauncherApp instance;
	@Override
	public void onCreate()
	{
		/*StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        	.detectAll()
        	.penaltyLog()
        	.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        	.detectLeakedSqlLiteObjects()
        	.detectAll()
        	.penaltyLog()
        	.penaltyDeath()
        	.build());*/
		super.onCreate();

		writeIDFile();
		instance = this;
		
		// Write ID file immediately:
		
		// Ali: We run all the services from here.  Later on
		// we can put a better logic so that we don't blindly
		// start all of them:
		startService(new Intent("RECON_INSTALLER_SERVICE"));
		startService(new Intent("RECON_MOD_SERVICE"));
		// We run other services later when video playback is done
		// End of services


		initBLEService();
		
		// Intro Video
		boolean videoPlayed = false;
		boolean alwaysPlayVideo = false;
		try {
			videoPlayed = Settings.System.getInt(this.getContentResolver(), "INTRO_VIDEO_PLAYED", 0) == 1;
			alwaysPlayVideo = Settings.System.getInt(getContentResolver(), "INTRO_VIDEO_ALWAYS_PLAY", 0) == 1;
		} catch(Exception e) {
			Log.e(TAG, e.toString());
		}
		
		Log.v(TAG, "videoPlayed: " + videoPlayed);
		Log.v(TAG, "alwaysPlayVideo: " + alwaysPlayVideo);
		 
		try {
			if (!videoPlayed || alwaysPlayVideo) {
				Intent i = new Intent("RECON_INTRO_VIDEO");
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// Magnetometer update
		SharedPreferences pref = getSharedPreferences(CompassCalibrationService.APP_SHARED_PREFS, Activity.MODE_WORLD_READABLE);
		if(!pref.getBoolean("hasWrittenMagOffsets", false)) { // If mag offsets have not been written
			
			try {
				float magOffsetX = Settings.Secure.getFloat(getContentResolver(), SensorConfWriter.KEY_MAG_OFFSET_X);
				float magOffsetY = Settings.Secure.getFloat(getContentResolver(), SensorConfWriter.KEY_MAG_OFFSET_Y);
				float magOffsetZ = Settings.Secure.getFloat(getContentResolver(), SensorConfWriter.KEY_MAG_OFFSET_Z);
				
				double[] offsets = { (double) magOffsetX, (double) magOffsetY, (double) magOffsetZ };
				
				// Write these offsets to sensors.conf
				Log.v(TAG, "writing saved magnetometer offsets");
				SensorConfWriter.writeMagOffsets(getApplicationContext(), offsets, false);
				
			} catch(Exception e) {
				Log.v(TAG, "could not read previous magnetometer offsets");
			}
			
		} else {
			Log.v(TAG, "mag offsets already up to date");
		}

		registerReceiver(phoneConnectionReceiver, new IntentFilter(XMLMessage.MUSIC_MESSAGE));
		registerReceiver(phoneConnectionReceiver, new IntentFilter(XMLMessage.SONG_MESSAGE));

	}
	
	
	
	@Override
	public void onTerminate()
	{
		super.onTerminate();
		unregisterReceiver(phoneConnectionReceiver);
	}



	/** Static access to a singleton application context */
	public static DashLauncherApp getInstance() {
		return instance;
	}
	public boolean isIPhoneConnected() {
		try{
			return bleService!=null&&bleService.isConnected()&&!bleService.getIsMaster();
		} catch (RemoteException e){
			e.printStackTrace();
			Log.d(TAG, "Remote Exception checking iPhone connected");
			return false;
		}
	}
	void initBLEService() {
		if( bleServiceConnection == null ) {
			bleServiceConnection = new BLEServiceConnection();
			Intent i = new Intent("RECON_BLE_TEST_SERVICE");
			bindService( i, bleServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d( TAG, "bindService()" );
		} 
	}

	void releaseBLEService() {
		if( bleServiceConnection != null ) {
			unbindService( bleServiceConnection );	  
			bleServiceConnection = null;
			Log.d( TAG, "unbindService()" );
		}
	}

	class BLEServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, 
				IBinder boundService ) {
			bleService = IBLEService.Stub.asInterface((IBinder)boundService);
			Log.d(TAG,"onServiceConnected" );
		}

		public void onServiceDisconnected(ComponentName className) {
			bleService = null;
			Log.d( TAG,"onServiceDisconnected" );
		}
	};
	
	
	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			if(intent.getAction().equals(XMLMessage.MUSIC_MESSAGE)){
				
				String msg = intent.getStringExtra("message");
				MusicMessage message = new MusicMessage(msg);
				if(message.type==MusicMessage.Type.STATUS){
					Log.d(TAG, "got music status: "+msg);
					MusicHelper.gotPlayerInfo(message.info);
				}
			} else if (intent.getAction().equals(XMLMessage.SONG_MESSAGE)) {
				String msg = intent.getStringExtra("message");
				
				ReconSong song = SongMessage.getSong(msg);

				MusicHelper.gotSong(song);
			}
		}
	};
    
}
