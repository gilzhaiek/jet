package com.reconinstruments.applauncher.transcend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.location.Location;
import android.os.Build;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.format.Time;

import android.util.Log;

/**
 *  class <code>ReconEventHandler</code> is responsible for writing
 *  event files.
 */
public class ReconEventHandler {
    public static final String TAG = "ReconEventHandler";
    private static final String CONFIG_FOLDER = "ReconApps";
    private static final String ID_FOLDER = "ReconApps";
    private static final String EVENT_FOLDER = "ReconApps/TripData";
    private ReconTranscendService mRTS;
    private ReconTimeManager mTimeManager;
    private static final int EPOCH_TIME_FIELD_LENGTH = 4; // duplicate from log
    private static final int UTC_FIELD_LENGTH = 3;
    private static final int CHRONO_FIELD_LENGTH = 3;
    private static final int TYPE_EVENT_FIELD_LENGTH = 1;
    private static final int JUMP_AIR_TIME_FIELD_LENGTH = 2;
    private static final int JUMP_DISTANCE_FIELD_LENGTH = 2;
    private static final int JUMP_HEIGHT_FIELD_LENGTH = 2;
    private static final int JUMP_DROP_FIELD_LENGTH = 2;
    private static final int SPORTS_ACTIVITY_FIELD_LENGTH = 1;
    private static final int CHRONO_START_EVENT = 0x02;
    private static final int CHRONO_STOP_EVENT = 0x03;
    private static final int CHRONO_DELETE_EVENT = 0x00;
    private static final int CHRONO_FLAG_EVENT = 0x01;
    public static final int SPORTS_ACTIVITY_START_EVENT = 0x01;
    public static final int SPORTS_ACTIVITY_PAUSE_EVENT = 0x02;
    public static final int SPORTS_ACTIVITY_RESUME_EVENT = 0x03;
    public static final int SPORTS_ACTIVITY_FINISH_EVENT = 0x00;
    public static final int SPORTS_ACTIVITY_DISCARD_EVENT = 0x04;
    private int  posAfterGenericEvent = 7; 
    public ReconEventHandler(ReconTranscendService rts){
	mRTS = rts;
	mTimeManager= mRTS.mTimeMan;
    }
    public void writeEventHeaderFile(){
	// Note that this function overrides the existing Event file.
	// ReconTimeManager should find you a good daynumber such that
	// no prior dayfile is there (We don't care about event file
	// with no corresponding day file)
	int mDayNo = mTimeManager.getDayNo();
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, EVENT_FOLDER+"/EVENT"+mDayNo/10+""+mDayNo%10+".RIB");
	try {
	    (file.getParentFile()).mkdirs();
	    OutputStream os = new FileOutputStream(file,false);//Don't append
	    byte[] data = new byte[] {(byte)0x09,	       // Length
				      (byte)(0xC0 + 40),       // epoch time
				      (byte)(0x80 + 0),	       // utc flag
				      (byte)(0x00 + 17), // event type
				      (byte)(0x80 + 16), // chrono
				      (byte)(0x40+19), // jump air
				      (byte)(0x40+20), // jump distance
				      (byte)(0x40 + 21), // jump height
				      (byte)(0x40 + 22), // jump drop
				      (byte)(0x00 + 23)	 // sports activity
	    };
	    os.write(data);
	    os.close();
		        
	} catch (IOException e) {
	    // Unable to create file, likely because external storage is
	    // not currently mounted.
	    //Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
	    Log.w("ExternalStorage", "Error writing " + file, e);
	}
    }

    private static int TOTAL_RECORD_SIZE  = EPOCH_TIME_FIELD_LENGTH+
	UTC_FIELD_LENGTH + TYPE_EVENT_FIELD_LENGTH +
	    CHRONO_FIELD_LENGTH + JUMP_AIR_TIME_FIELD_LENGTH +
	    JUMP_DISTANCE_FIELD_LENGTH + JUMP_HEIGHT_FIELD_LENGTH +
	    JUMP_DROP_FIELD_LENGTH + SPORTS_ACTIVITY_FIELD_LENGTH;

    public Time getTime() {
	Time utcTime = new Time();
	utcTime.switchTimezone(Time.TIMEZONE_UTC);
	Location loc = mRTS.getReconLocationManager().getLocation();
	if (loc != null) utcTime.set(loc.getTime());
	return utcTime;
    }

    private byte[] generateGenericEvent() {
	return generateGenericEvent(getTime());
    }
    private byte[] generateGenericEvent(Time utcTime){
	// Generates a generic event record with all the fields. the
	// output is given to a specific event record handler, say
	// jump, and the fields get updated and then saved to file
	byte[] event_entry_all = new byte [TOTAL_RECORD_SIZE] ;
	// put everything to zero just to be sure:
	for (int i=0; i<TOTAL_RECORD_SIZE;i++){
	    event_entry_all[i]  = 0;
	}
	int pos = 0;
	// epoch time
	long tmpLong = utcTime.toMillis(true)/1000;
	event_entry_all[pos++] = (byte)((tmpLong >> 24)&(0XFF));
	event_entry_all[pos++] = (byte)((tmpLong >> 16)&(0XFF));
	event_entry_all[pos++] = (byte)((tmpLong >> 8)&(0XFF));
	event_entry_all[pos++] = (byte)((tmpLong)&(0XFF));

	// utc stuff
	event_entry_all[pos++] = (byte) (0x00+utcTime.hour);
	event_entry_all[pos++] = (byte) (utcTime.minute);
	event_entry_all[pos++] = (byte) (utcTime.second);
	Log.d("Event UTC",""+utcTime.hour+" "+utcTime.minute+" "+utcTime.second);
	posAfterGenericEvent = pos;
	return event_entry_all;
    }

    private void writeEventEntryAllToFile(byte[] event_entry_all) {
	writeEventEntryAllToFile(event_entry_all, true);
    }

    private void writeEventEntryAllToFile(byte[] event_entry_all,boolean careAboutGps){
	// Saves the event record to file
	int day = mTimeManager.getDayNo();
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, EVENT_FOLDER +"/EVENT"+day/10+""+day%10+".RIB");
	if (!file.exists()){
	    mRTS.startANewDay(true); // resets stats as well
	    day = mTimeManager.getDayNo(); // update day
	    file = new File(path, EVENT_FOLDER +"/EVENT"+day/10+""+day%10+".RIB");
	}
	try {
	    (file.getParentFile()).mkdirs();
	    FileOutputStream os = new FileOutputStream(file,true);//Append data
	    if (mRTS.getAndroidLocationListener().mHasHadGPSfix || !careAboutGps){
		// only write if has had gps fix
		    os.write(event_entry_all);
	    }
	    else {		// no gps fix no file
		Log.d(TAG,"No Gps fix, no writing to file");
	    }
	    os.close();

	} catch (IOException e) {
	    // Unable to create file, likely because external storage is
	    // not currently mounted.
	    //Toast.makeText(getApplicationContext(), "Failed to write to external storage", Toast.LENGTH_SHORT).show();
	    Log.w("ExternalStorage", "Error writing " + file, e);
	}
    }
    public void writeStartChronoEvent(){
	int pos = posAfterGenericEvent;
	int tmpInt = 0;
	byte [] event_entry_all = generateGenericEvent();
	event_entry_all[pos++] = (byte)(0x00); // Chrono: Not jump

	tmpInt = (CHRONO_START_EVENT << 22);//Means Start event;
	tmpInt += (mRTS.getReconChronoManager().getCurrentTrialIndex() << 8);//Timer Number
	tmpInt += ((mTimeManager.getUTCTimems())/10)%100;//centi seconds;
	
	Log.d("WriteStartChrono",""+tmpInt);
	event_entry_all[pos++] = (byte)(((tmpInt/256)/256)%256);//MSB
	event_entry_all[pos++] = (byte)((tmpInt/256)%256);
	event_entry_all[pos++] = (byte)(tmpInt%256);//LSB
	writeEventEntryAllToFile(event_entry_all);
    }


    public void writeLapChronoEvent(){
	
		
    }
    public void writeStopChronoEvent(){
	int pos = posAfterGenericEvent;
	int tmpInt = 0;
	byte [] event_entry_all = generateGenericEvent();
	event_entry_all[pos++] = (byte)(0x00); // Chrono: Not jump

	tmpInt = (CHRONO_STOP_EVENT << 22);//Means Stop event;
	tmpInt += (mRTS.getReconChronoManager().getCurrentTrialIndex() << 8);//Timer Number
	tmpInt += ((mTimeManager.getUTCTimems())/10)%100;//centi seconds;
	Log.d("WriteStopChrono",""+tmpInt);
	event_entry_all[pos++] = (byte)(((tmpInt/256)/256)%256);//MSB
	event_entry_all[pos++] = (byte)((tmpInt/256)%256);
	event_entry_all[pos++] = (byte)(tmpInt%256);//LSB

	writeEventEntryAllToFile(event_entry_all);
		
    }

    public void writeDeleteChronoEvent(){
	int pos = posAfterGenericEvent;
	int tmpInt = 0;
	byte [] event_entry_all = generateGenericEvent();
	event_entry_all[pos++] = (byte)(0x00); // Chrono: Not jump

	tmpInt = (CHRONO_DELETE_EVENT << 22);//Means delete event;
	tmpInt += (mRTS.getReconChronoManager().getCurrentTrialIndex() << 8);//Timer Number
	tmpInt += ((mTimeManager.getUTCTimems())/10)%100;//centi seconds;
	Log.d("WriteDeleteChrono",""+tmpInt);
	event_entry_all[pos++] = (byte)(((tmpInt/256)/256)%256);//MSB
	event_entry_all[pos++] = (byte)((tmpInt/256)%256);
	event_entry_all[pos++] = (byte)(tmpInt%256);//LSB

	writeEventEntryAllToFile(event_entry_all);
		
    }

    public void writeFlagChronoEvent(){
	int pos = posAfterGenericEvent;
	int tmpInt = 0;
	byte [] event_entry_all = generateGenericEvent();
	event_entry_all[pos++] = (byte)(0x00); // Chrono: Not jump

	tmpInt = (CHRONO_FLAG_EVENT << 22);//Means flag event;
	tmpInt += (mRTS.getReconChronoManager().getCurrentTrialIndex() << 8);//Timer Number
	tmpInt += ((mTimeManager.getUTCTimems())/10)%100;//centi seconds;
	Log.d("WriteFlagChrono",""+tmpInt);
	event_entry_all[pos++] = (byte)(((tmpInt/256)/256)%256);//MSB
	event_entry_all[pos++] = (byte)((tmpInt/256)%256);
	event_entry_all[pos++] = (byte)(tmpInt%256);//LSB

	writeEventEntryAllToFile(event_entry_all);
    }

    public void writeJumpEvent() {
	// debug message:
	Log.d(TAG,"writeJumpEvent");

	int pos = posAfterGenericEvent;
	int tmpInt = 0;
	byte [] event_entry_all = generateGenericEvent();
	event_entry_all[pos++] = (byte)(0x01); // Jump: Not chrono

	// move pointer to begining of jump fields
	pos = EPOCH_TIME_FIELD_LENGTH + UTC_FIELD_LENGTH + TYPE_EVENT_FIELD_LENGTH +
	    CHRONO_FIELD_LENGTH;
	
	// Get the last jump from the jump manager
	ReconJump lastJump = mRTS.getReconJumpManager().getLastJump();

	// General approach: The fields of jump should be recorded as
	// unsigned short. So we convert them to int and then take the
	// two least significant bytes in big endian fashion.  If the
	// value of the field is invalid we put the max number of
	// unsigned short minus 2: 0xfffe

	// air
	tmpInt = (lastJump.mAir == ReconJump.INVALID_AIR)? 0xfffe: lastJump.mAir;
	event_entry_all[pos++] = (byte)((tmpInt << 16 )>> 24);//MSB-2
	event_entry_all[pos++] = (byte)((tmpInt << 24 )>> 24);//LSB

	// distance
	tmpInt = (lastJump.mDistance == ReconJump.INVALID_DISTANCE)?
	    0xfffe : (int)(lastJump.mDistance*10);
	event_entry_all[pos++] = (byte)((tmpInt << 16 )>> 24);//MSB-2
	event_entry_all[pos++] = (byte)((tmpInt << 24 )>> 24);//LSB

	// height
	tmpInt = (lastJump.mHeight == ReconJump.INVALID_HEIGHT)?
	    0xfffe : (int)((lastJump.mHeight+0.05)*10);
	event_entry_all[pos++] = (byte)((tmpInt << 16 )>> 24);//MSB-2
	event_entry_all[pos++] = (byte)((tmpInt << 24 )>> 24);//LSB

	// drop
	tmpInt = (lastJump.mDrop == ReconJump.INVALID_DROP)?
	    0xfffe : (int)((lastJump.mDrop+0.05)*10);
	event_entry_all[pos++] = (byte)((tmpInt << 16 )>> 24);//MSB-2
	event_entry_all[pos++] = (byte)((tmpInt << 24 )>> 24);//LSB
		

	// Write all to the file:
	writeEventEntryAllToFile(event_entry_all);

    }

    public void writeSportsActivityEvent(int action, int sports_activity_type) {
	writeSportsActivityEvent(action, sports_activity_type, getTime(),true);
    }
    public void writeSportsActivityEvent(int action, int sports_activity_type,
					 Time time, boolean careAboutGps) {
	int pos = posAfterGenericEvent;
	int tmpInt = 0;
	byte [] event_entry_all = generateGenericEvent(time);
	event_entry_all[pos++] = (byte)(0x02); // sports activity
	// Go to 
	pos = EPOCH_TIME_FIELD_LENGTH+UTC_FIELD_LENGTH + TYPE_EVENT_FIELD_LENGTH +
	    CHRONO_FIELD_LENGTH + JUMP_AIR_TIME_FIELD_LENGTH +
	    JUMP_DISTANCE_FIELD_LENGTH + JUMP_HEIGHT_FIELD_LENGTH +
	    JUMP_DROP_FIELD_LENGTH; // That's where sports_Activiy
				    // starts
	tmpInt = ((action & 0xff) << 5);
	tmpInt += sports_activity_type;
	event_entry_all[pos++] = (byte)(tmpInt&0xff);
	writeEventEntryAllToFile(event_entry_all,careAboutGps);

    }
    public void writeIDFile(){
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

	    // version info
	    int tmpInt = VersionInfo.SVN_VERSION_NUMBER;
	    data[pos++] = (byte)((tmpInt << 0 )>> 24);//MSB
	    data[pos++] = (byte)((tmpInt << 8 )>> 24);//MSB-1
	    data[pos++] = (byte)((tmpInt << 16 )>> 24);//MSB-2
	    data[pos++] = (byte)((tmpInt << 24 )>> 24);//LSB

	    // serialnumber
	    try {
		tmpInt = Integer.parseInt(Build.SERIAL);
	    }
	    catch (NumberFormatException e) {
		tmpInt = 0;
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
	
    public void writeConfigFile(){
	File path = Environment.getExternalStorageDirectory();
	File file = new File(path, CONFIG_FOLDER+"/"+"CONFIG.RIB");
	try {
	    (file.getParentFile()).mkdirs();
	    OutputStream os = new FileOutputStream(file,false);//Don't append
	    byte[] data = new byte[] {(byte)(0x06),(byte)0xfb,(byte)0xfa,(byte)0x39,
				      (byte)0x38,(byte)0x37,(byte)0x36,(byte)0x00,
				      (byte)0x00,(byte)0xde,(byte)0x2c,(byte)0x00,
				      (byte)0x00,(byte)0xef,(byte)0x10,(byte)0x00};
	    os.write(data);
	    os.close();
		        
	} catch (IOException e) {
	    // Unable to create file, likely because external storage is
	    // not currently mounted.
	    //Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
	    Log.w("ExternalStorage", "Error writing " + file, e);
	}
		
	
    }
}
