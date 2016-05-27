package com.reconinstruments.applauncher.transcend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;
import android.os.Build;

public class ReconDataLogHandler {
    // Aggregates for data constantly used
    private ReconTranscendService mRTS;
    private ReconTimeManager mReconTimeManager;
    private ReconLocationManager mReconLocationManager;
    private ReconAltitudeManager mReconAltitudeManager;
    private ReconSpeedManager mReconSpeedManager;
    private ReconTemperatureManager mReconTemperatureManager;

    private static final String TAG = "ReconDataLogger";
    public  static final String LOG_FOLDER = "ReconApps/TripData";
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private boolean mIsFirstEntry = true; //Do not use. TODO: remove
    // Fields info
    private static int EPOCH_TIME_FIELD_LENGTH = 4;
    private static int UTC_FIELD_LENGTH = 3;
    private static int LAT_FIELD_LENGTH = 4;
    private static int LNG_FIELD_LENGTH = 4;
    private static int SPEED_FIELD_LENGTH = 2;
    private static int GPS_ALT_FIELD_LENGTH = 2;
    private static int BAROMETER_FIELD_LENGTH = 2;
    private static int TEMPERATURE_FIELD_LENGTH = 1;
    private static int GPS_PRECISSION_FIELD_LENGTH = 2;
    private static int HEARTRATE_FIELD_LENGTH = 1;
    private static int CADENCE_FIELD_LENGTH = 2;
    private static int POWER_FIELD_LENGTH = 3;
    private static int SENSOR_SPEED_FIELD_LENGTH = 2;
    private static int RIB_ENTRY_LENGTH = EPOCH_TIME_FIELD_LENGTH + UTC_FIELD_LENGTH +
	LAT_FIELD_LENGTH+LNG_FIELD_LENGTH + SPEED_FIELD_LENGTH + GPS_ALT_FIELD_LENGTH +
	BAROMETER_FIELD_LENGTH + TEMPERATURE_FIELD_LENGTH + GPS_PRECISSION_FIELD_LENGTH+
	HEARTRATE_FIELD_LENGTH + CADENCE_FIELD_LENGTH + POWER_FIELD_LENGTH+
	SENSOR_SPEED_FIELD_LENGTH;
    // Incremental RIB push functionality values
    private int RibBufferLogCounter = 0;
    private static int RIB_ENTRIES_BUFFERED_BEFORE_SEND = 10;
    private byte[] rib_buffered = new byte [RIB_ENTRIES_BUFFERED_BEFORE_SEND *
					    RIB_ENTRY_LENGTH];
    //End of incremental RIB push functionality values
    
    private static int INVALID_TEMPERATURE_VALUE = 0;

    private static byte[] DAY_FILE_HEADER = new byte[] {(byte)0x0D, // number of fields
							(byte)(0xC0 + 40), // epoch time 
							(byte)0x80, // utc
							(byte)0xC1, // lat
							(byte)0xC2, // lng
							(byte)0x43, // speed
							(byte)0x44, // gps alt
							(byte)0x45, // pressures
							(byte)0x06, // temperature
							(byte)0x47, // gps precession
							(byte)0x20, // heartrate
							(byte)0x61, // cadence
							(byte)0xA4, // power
							(byte)0x63 // sensor speed

    };
    // Obsolete: Do not use this funxtion
    public void setIsFirstEntry(boolean val){
	mIsFirstEntry = val;
    }
    public void checkSanity() {
        String state  =  Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_NOFS.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
	    Log.d(TAG,"Can read and write media");
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
	    Log.d(TAG,"Cannot read and write");
        }
    }
    public ReconDataLogHandler (ReconTranscendService rts){
	mRTS = rts;
    	mReconTimeManager = rts.getReconTimeManager();
    	mReconLocationManager = rts.getReconLocationManager();
    	mReconAltitudeManager = rts.getReconAltManager();
    	mReconSpeedManager = rts.getReconSpeedManager();
	mReconTemperatureManager = rts.getReconTemperatureManager();
    }
    void writeDayHeaderFile(){
	// Creates the header file for events ansd 
    	Log.d(TAG,"writeDayHeaderFile");
    	int mDayNo = mReconTimeManager.getDayNo();
    	
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, LOG_FOLDER+"/DAY"+mDayNo/10+""+mDayNo%10+".RIB");
	//Toast.makeText(getApplicationContext(), path.toString(), Toast.LENGTH_SHORT).show();
	try {
	    (file.getParentFile()).mkdirs();
	    OutputStream os = new FileOutputStream(file,false);//Don't append
	    os.write(DAY_FILE_HEADER);
	    os.close();
	} catch (IOException e) {
	    // Unable to create file, likely because external storage is
	    // not currently mounted.
	    Log.w("ExternalStorage", "Error writing " + file, e);
	}
	//Pushing it to iOS if necessary too:
    }
    void writeLogEntry() {
    	int mDayNo = mReconTimeManager.getDayNo();
    	Location mLocation = mReconLocationManager.getLocation();
    	Time mUTCTime = new Time();
	 mUTCTime.switchTimezone(Time.TIMEZONE_UTC);
	mUTCTime.set(mLocation.getTime());
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, LOG_FOLDER+"/DAY"+mDayNo/10+""+mDayNo%10+".RIB");
	if (!file.exists()){
	    Log.d(TAG,"File doesn't exist. Try to start a new day");
	    mRTS.startANewDay(true);
	    // update day
	    mDayNo = mReconTimeManager.getDayNo();
	    file = new File(path, LOG_FOLDER+"/DAY"+mDayNo/10+""+mDayNo%10+".RIB");
	}
	byte[] log_entry_all = new byte[RIB_ENTRY_LENGTH];
	int pos = 0;

	// First: the epoch time
	long tmpLong = mUTCTime.toMillis(true)/1000;
	log_entry_all[pos++] = (byte)((tmpLong >> 24)&(0XFF));
	log_entry_all[pos++] = (byte)((tmpLong >> 16)&(0XFF));
	log_entry_all[pos++] = (byte)((tmpLong >> 8)&(0XFF));
	log_entry_all[pos++] = (byte)((tmpLong)&(0XFF));

	// New algorithm for determining the first entry:
	boolean isFirstEntry = (file.length() == DAY_FILE_HEADER.length);
	if (isFirstEntry){//Add date
	    log_entry_all[pos++] = (byte)(0x80 + (mUTCTime.year- 2000) );//date flag and year - 2000
	    log_entry_all[pos++] = (byte) (mUTCTime.month+1);//month is from 0 to 11 originally
	    log_entry_all[pos++] = (byte) (mUTCTime.monthDay);
	    isFirstEntry = false;
	}
	else{
	    log_entry_all[pos++]  = (byte)(0x00 + (mUTCTime.hour) );//date flag set to zero
	    log_entry_all[pos++] = (byte) (mUTCTime.minute);//minute
	    log_entry_all[pos++] = (byte) (mUTCTime.second);//second
	}
	double tmpvar = mLocation.getLatitude();
	byte Sbit = 0;//meaning North
	if (tmpvar < 0)
	    {
		Sbit = (byte)0x80; //meaning south
	    }
	tmpvar = Math.abs(tmpvar);
	log_entry_all[pos++] = (byte)(tmpvar);
		 
	tmpvar = (tmpvar - Math.floor(tmpvar)) * 60;//minutes
	log_entry_all[pos++] = (byte)(Sbit + (int)tmpvar );//N/S bit + Integer part of minutes
		 
	tmpvar = (tmpvar - Math.floor(tmpvar)) * 100;//first two decimal of minutes
	log_entry_all[pos++] = (byte)((int)tmpvar );//two decimal of minutes
		 
	tmpvar = (tmpvar - Math.floor(tmpvar)) * 100;//second two decimal of minutes
	log_entry_all[pos++] = (byte)((int)tmpvar);
		 
	//going to the longtitude filed
	tmpvar = mLocation.getLongitude();
	byte Wbit = 0; //meaning East
	if (tmpvar < 0){
	    Wbit = (byte)0x80;
	}
	tmpvar = Math.abs(tmpvar);
	log_entry_all[pos++] = (byte)tmpvar;

	tmpvar = (tmpvar - Math.floor(tmpvar)) * 60;//minutes
	log_entry_all[pos++] = (byte)(Wbit + (int)tmpvar );//E/W bit + Integer part of minutes

	tmpvar = (tmpvar - Math.floor(tmpvar)) * 100;//first two decimal of minutes
	log_entry_all[pos++] = (byte)((int)tmpvar );//two decimal of minutes

	tmpvar = (tmpvar - Math.floor(tmpvar)) * 100;//second two decimal of minutes
	log_entry_all[pos++] = (byte)((int)tmpvar);
		 
	int tmpInt =(int)(mLocation.getSpeed()*36);//convert to km/h and x 10
	log_entry_all[pos++] = (byte)(tmpInt / 256);//MSB
	log_entry_all[pos++] = (byte)(tmpInt % 256);//LSB 

	if (mLocation.hasAltitude() ) {
	    tmpInt =(int) mLocation.getAltitude();
	} else {
	    tmpInt = (int) ReconAltitudeManager.INVALID_ALT;
	    //That is -10000
	}

	log_entry_all[pos++] = (byte)((tmpInt >> 8)&0xFF);//MSB
	log_entry_all[pos++] = (byte)(tmpInt & 0xFF );//LSB
		 
	//Pressure: According to spec times 50
	tmpInt = (int)(mReconAltitudeManager.getPressure() * 50);
	log_entry_all[pos++] = (byte)(tmpInt / 256);//MSB
	log_entry_all[pos++] = (byte)(tmpInt % 256);//LSB

	//Temperature: According to spec plus 40
	tmpInt = mReconTemperatureManager.getTemperature() + 40;
	if ((tmpInt - 40) != ReconTemperatureManager.INVALID_TEMPERATURE) {
	    log_entry_all[pos++] = (byte)(tmpInt);
	}
	else {			// invalid temperature
	    log_entry_all[pos++] = (byte)INVALID_TEMPERATURE_VALUE;
	}

	// now to GPS precision
	tmpInt = mReconLocationManager.getNumberOfSatellites();//NumSatellites
	tmpInt = (tmpInt << 10);//Move to the left for HDOP
	tmpInt += (int)(mLocation.getAccuracy() + 0.5);
	//HDOP is accuracy divided by 10(typical) (in jet); but in rib
	//format we want hdop times 10 so these cancel eachother and
	//we just report accuray. Essencially in this code I am
	//assuming an HDOP of 1 is equivalent to accuracy of 10m.
	log_entry_all[pos++] = (byte)(tmpInt / 256);//MSB
	log_entry_all[pos++] = (byte)(tmpInt % 256);//LSB

	// Now to heartrate:
	tmpInt = mRTS.getReconHRManager().getHR();
	log_entry_all[pos++] = (byte)(tmpInt % 256);//MSB

	// Now to cadence
	tmpInt = mRTS.getReconCadenceManager().getCadence();
	log_entry_all[pos++] = (byte)(tmpInt / 256);//MSB
	log_entry_all[pos++] = (byte)(tmpInt % 256);//LSB

	// Now to power
	tmpInt = mRTS.getReconPowerManager().getLeftPower()&0xFFF;
	tmpInt = tmpInt << 12;
	tmpInt += mRTS.getReconPowerManager().getRightPower()&0xFFF;
	log_entry_all[pos++] = (byte)((tmpInt >> 16) & (0xFF));//MSB
	log_entry_all[pos++] = (byte)((tmpInt >> 8) & (0xFF)) ;
	log_entry_all[pos++] = (byte)(tmpInt & 0xFF);//LSB

	// Now to sensor speed
	float tmpFloat = mRTS.getReconSpeedManager().getSensorSpeed();
	
	tmpInt = (tmpFloat == ReconSpeedManager.INVALID_SPEED)? 0xFFFF: ((int)(tmpFloat*10.0+0.5))&0xFFFF;
	log_entry_all[pos++] = (byte)(tmpInt / 256);//MSB
	log_entry_all[pos++] = (byte)(tmpInt % 256);//LSB

	
	
	try {
	    (file.getParentFile()).mkdirs();
	    FileOutputStream os = new FileOutputStream(file,true);//Append data
	    if (mLocation != null){
			    
		os.write(log_entry_all);
	    }
	    os.close();
	} catch (IOException e) {
	    // Unable to create file, likely because external storage is
	    // not currently mounted.
	    Log.w("ExternalStorage", "Error writing " + file, e);
	}

	bufferRibsAndSendWhenBufferFull(log_entry_all);
    }
    private void bufferRibsAndSendWhenBufferFull(byte [] log_entry_all) {
	int offset = RibBufferLogCounter * RIB_ENTRY_LENGTH;
	for (int i = 0;i<RIB_ENTRY_LENGTH;i++) {
	    rib_buffered[offset +i] = log_entry_all[i];
	}
	RibBufferLogCounter = (RibBufferLogCounter+1) % RIB_ENTRIES_BUFFERED_BEFORE_SEND;
	if (RibBufferLogCounter == 0) {
	    //Pushing it to ios if necessary
	    //Log.v("ReconDataLogHandler","Sending incremental via BLE");
	    mRTS.mBLE.pushIncrementalRibThroughBLE((byte) 0x01, rib_buffered);
	}
    }
}
