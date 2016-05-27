package com.reconinstruments.dashlauncher.settings;


import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class ReconSettingsUtil
{
	static private final String RECON_UNIT_SETTING = "ReconUnitSetting";
	static public final int RECON_UINTS_METRIC = 0;
	static public final int RECON_UINTS_IMPERIAL = 1;
	static public final String RECON_UNIT_SETTING_CHANGED= "RECON_UNIT_SETTING_CHANGED";
	static public final String UNIT_SETTING = "unitSetting";
	
	// auto time variables
	static public final String RECON_SET_TIME_GPS = "ReconSetTimeGPS";
	static public final int RECON_SET_TIME_GPS_OFF = 0;
	static public final int RECON_SET_TIME_GPS_ON = 1;
	
    // sync time with smartphone variables
    static public final String RECON_SYNC_TIME_SMARTPHONE = "ReconSyncTimeWithSmartPhone";
    static public final int RECON_SYNC_TIME_OFF = 0;
    static public final int RECON_SYNC_TIME_ON = 1;
	
	 /*
	  * Utility function for checking the unit settings
	  * return 0 for Metric; 1 for Imperial
	  */
	 static public int getUnits( Context context )
	 {
		 try
		 {
			 int setting = Settings.System.getInt( context.getContentResolver(), RECON_UNIT_SETTING );
			 //int setting = Settings.System.getInt( null, RECON_UNIT_SETTING );
			 //setting: 0: Metric; 1: Imperial
			 return setting;
		 }
		 catch( Settings.SettingNotFoundException e )
		 {
			 //otherwise, the system setting has not been created yet
			 //let's create the system setting, and set the default unit 
			 //to be Metric			 
			 return RECON_UINTS_METRIC;
			 
		 }
		 
	 }
	 
	 /*
	  * Utility function for checking the unit settings
	  * If not existed create the system setting
	  * return 0 for Metric; 1 for Imperial
	  * call this function need WRITE_SETTING permission\
	  * thus it is designed as protected(locally used only)
	  */
	 public static int getUnitsWrite( Context context )
	 {
		 try
		 {
			 int setting = Settings.System.getInt( context.getContentResolver(), RECON_UNIT_SETTING );
			 //int setting = Settings.System.getInt( null, RECON_UNIT_SETTING );
			 //setting: 0: Metric; 1: Imperial
			 return setting;
		 }
		 catch( Settings.SettingNotFoundException e )
		 {
			 //otherwise, the system setting has not been created yet
			 //let's create the system setting, and set the default unit 
			 //to be Metric			
			 Settings.System.putInt( context.getContentResolver(), RECON_UNIT_SETTING, RECON_UINTS_METRIC  );
			 return RECON_UINTS_METRIC;
			 
		 }
		 
	 }
	 
	 /*
	  * Set the Units setting: 0 for Metric; Non-zero for Imperial
	  */
	 static protected void setUnits( Context context, int setting )
	 {
		 Settings.System.putInt( context.getContentResolver(), RECON_UNIT_SETTING, setting == RECON_UINTS_METRIC ? RECON_UINTS_METRIC : RECON_UINTS_IMPERIAL );		 
	 }
	 
	 static protected void setSyncTimeWithSmartPhone( Context context, boolean syncTimeOn){
         Settings.System.putInt( context.getContentResolver(), RECON_SYNC_TIME_SMARTPHONE, syncTimeOn ? RECON_SYNC_TIME_ON : RECON_SYNC_TIME_OFF);
         if(syncTimeOn){
             setTimeAuto(context, false);
         }
	 }
	 
	 static public boolean getSyncTimeWithSmartPhone( Context context){
         int syncTimeWithSmartPhone;
        try {
            syncTimeWithSmartPhone = Settings.System.getInt( context.getContentResolver(), RECON_SYNC_TIME_SMARTPHONE);
            return syncTimeWithSmartPhone == RECON_SYNC_TIME_ON;
        } catch (SettingNotFoundException e) {
            Settings.System.putInt( context.getContentResolver(), RECON_SYNC_TIME_SMARTPHONE, RECON_SYNC_TIME_OFF);
            return false;
        }
	 }
	 
	 /*
	  * Set time automatically
	  */
	 static protected void setTimeAuto( Context context, boolean useGPSTime) {
		 Settings.System.putInt( context.getContentResolver(), RECON_SET_TIME_GPS, useGPSTime ? RECON_SET_TIME_GPS_ON : RECON_SET_TIME_GPS_OFF);
	 }
	 
	 /*
	  * Is time automatically set by GPS?
	  */
	 static public boolean getTimeAuto( Context context) {
		 int gpsSetting;
		try {
			gpsSetting = Settings.System.getInt( context.getContentResolver(), RECON_SET_TIME_GPS);
			return gpsSetting == RECON_SET_TIME_GPS_ON;
		} catch (SettingNotFoundException e) {
			//if we can't get it, it hasn't been set. Set to on
			Settings.System.putInt( context.getContentResolver(), RECON_SET_TIME_GPS, RECON_SET_TIME_GPS_ON);
			return true;
		}
	 }
}