package com.reconinstruments.dashlauncher.settings;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Typeface;

import com.reconinstruments.dashlauncher.R;

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
	
	static public void resetAllTimeStats()
	{
		if( TRANSEND_SERVICE != null)
			TRANSEND_SERVICE.resetAllTimeStats();
	}
}