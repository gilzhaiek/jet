package com.reconinstruments.dashlauncher.radar;


import android.content.Context;
import android.provider.Settings;

public class ReconSettingsUtil
{
	static private final String RECON_UNIT_SETTING = "ReconUnitSetting";
	static public final int RECON_UINTS_METRIC = 0;
	static public final int RECON_UINTS_IMPERIAL = 1;
	static public final String RECON_UNIT_SETTING_CHANGED= "RECON_UNIT_SETTING_CHANGED";
	static public final String UNIT_SETTING = "unitSetting";
	
	 /*
	  * Utility function for checking the unit settings
	  * return 0 for Metric; 1 for Imperial
	  */
	 static public int getUnits( Context context )
	 {
	     if (context == null) { // Hack: Ali at the very begining context is null
		 return RECON_UINTS_METRIC;
	     }
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
	 static protected int getUnitsWrite( Context context )
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
}