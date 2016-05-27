package com.reconinstruments.dashlauncher.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import com.reconinstruments.dashsettings.R;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.PowerManager;
import android.platform.UBootEnvNative;
import android.util.Log;
import android.widget.Toast;


public class Util
{
	static  Typeface MENU_TYPE_FONT = null;
	static String[] MONTHES_NAME=null;
	static String[] DAYS_NAME = null;
	public static TranscendServiceConnection TRANSEND_SERVICE = null; 
	
	static public Typeface getMenuFont( Context context )
	{
		if( MENU_TYPE_FONT == null )
		{			
			MENU_TYPE_FONT = Typeface.createFromAsset(context.getAssets(), "fonts/Eurostib.ttf" );
		}
		
		return MENU_TYPE_FONT;
	}	
	
	
	static public String getTodayString( Context context )
	{
		int baseYr = Calendar.getInstance().get(Calendar.YEAR);
		int baseDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		int baseMth = Calendar.getInstance().get(Calendar.MONTH);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

		return getFormatedDate( context, day - 1, baseDay, baseMth, baseYr );
	}
	
	
	//expected:
	//dayofWeek: 0-6
	//month: 0-11
	static public String getFormatedDate( Context context, int dayOfWeek, int dayOfMonth, int  month, int year )
	{
		if( MONTHES_NAME == null )
		{
			MONTHES_NAME = context.getResources().getStringArray(R.array.month_name_string);
		}
		
		if( DAYS_NAME == null )
		{
			DAYS_NAME = context.getResources().getStringArray(R.array.weekday_name_string);
		}
		
		String dayOfWeekStr = "";
		switch(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
			case Calendar.SUNDAY: dayOfWeekStr = "Sunday";
				break;
				
			case Calendar.MONDAY: dayOfWeekStr = "Monday";
				break;
				
			case Calendar.TUESDAY: dayOfWeekStr = "Tuesday";
				break;
				
			case Calendar.WEDNESDAY: dayOfWeekStr = "Wednesday";
				break;
				
			case Calendar.THURSDAY: dayOfWeekStr = "Thursday";
				break;
			
			case Calendar.FRIDAY: dayOfWeekStr = "Friday";
				break;
				
			case Calendar.SATURDAY: dayOfWeekStr = "Saturday";
				break;
		}
		
		return dayOfWeekStr + ", " + MONTHES_NAME[month] + " " + dayOfMonth + ", " + year;
	}

	
	static public String getFormatedDate( Context context, int dayOfMonth, int  month, int year )
	{
		if( MONTHES_NAME == null )
		{
			MONTHES_NAME = context.getResources().getStringArray(R.array.month_name_string);
		}
		
		
		return MONTHES_NAME[month] + " " + dayOfMonth + ", " + year;
	}

	static public String getFormatedTime( Context context, int minute, int hr, boolean am )
	{
		String str = "" + hr + " : ";
		
		if( minute < 10 )
			str += "0" + minute;
		else
			str += minute;
		
		if( am )
		{
			str += " AM";
		}
		else
		{
			str += " PM";
		}
		
		return str;
	}
	
	static public void resetStats( )
	{
		if( TRANSEND_SERVICE != null )
		{
			TRANSEND_SERVICE.resetStats();
		}
	}
	
	static public void resetAllTimeStats(Context context)
	{
		if( TRANSEND_SERVICE != null)
			TRANSEND_SERVICE.resetAllTimeStats();
		
		// deletes calls and texts groups from message center
		String select = GrpSchema.COL_URI+"='com.reconinstruments.stats'";
		int count = context.getContentResolver().delete(ReconMessageAPI.GROUPS_URI, select, null);
		Log.d("DashSettings", "deleted "+count+" messages");
	}
	
	/*////////////////////////////////////////////////////
	 *  Firmware Upgrade / Factory Reset Related Variables 
	 *////////////////////////////////////////////////////
	
	public static final String UBOOT_OPT_NAME = "BOOT_OPT";
	public static final String UBOOT_UPDATE_FIRMWARE = "UPDATE_REQ";
	public static final String UBOOT_RESET_FIRMWARE = "FACTORY_REQ";

	public static final File LIMO_UPDATE_ZIP= new File(Environment.getExternalStorageDirectory()+"/update.zip");
	public static final File LIMO_COMMAND_RECOVERY= new File(Environment.getExternalStorageDirectory()+"/recovery/command");
	public static final File LIMO_COMMAND_AUTO_UPDATE= new File(Environment.getExternalStorageDirectory()+"/recovery/autoupdate");
    	
	public static final File JET_UPDATE_BIN_STORAGE	= new File(Environment.getExternalStorageDirectory()+"/ReconApps/cache/update.bin");
	public static final File JET_UPDATE_BIN_CACHE		= new File("/cache/update.bin");
	public static final File JET_COMMAND_RECOVERY		= new File("/cache/recovery/command");
	public static final File JET_COMMAND_RECOVERY_BAK	= new File("/cache/command_bak");
	public static final File JET_RECOVERY_PATH		= new File("/cache/recovery");

	/**
	 * Simple Method for copying file
	 * @param src
	 * @param dst
	 * @throws IOException
	 * @author Patrick Cho
	 */
	public static boolean copy(File src, File dst) {
		InputStream in;
		OutputStream out;
		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(dst);
		    
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();

		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		    
		return true;
	}
	
	/**
	 * Initiates the limo firmware upgrade
	 * 
	 * @return boolean succefully started? only returned when unsuccessful
	 * @author revised by Patrick Cho
	 */
	public static void doLimoFirmwareUpgrade(Context mContext) {
		PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		UBootEnvNative.Set_UBootVar(Util.UBOOT_OPT_NAME, Util.UBOOT_UPDATE_FIRMWARE);
		pm.reboot(null);
	}

	/**
	 * Initiates the jet firmware upgrade
	 * 
	 * @return boolean succefully started? only returned when unsuccessful
	 * @author Patrick Cho
	 */
	public static boolean doJetFirmwareUpgrade(Context mContext) {
		Log.d ("DashSettings","doJetFirmwareUpgrade() called");
		
		if (JET_COMMAND_RECOVERY_BAK.renameTo(JET_COMMAND_RECOVERY))
			Log.d("DashSetting", JET_COMMAND_RECOVERY_BAK + " was renamed to " + JET_COMMAND_RECOVERY);
		
		PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		Log.d ("DashSettings","rebooting the device");
		pm.reboot(null);
		return true; // unreachable haha
	}

}