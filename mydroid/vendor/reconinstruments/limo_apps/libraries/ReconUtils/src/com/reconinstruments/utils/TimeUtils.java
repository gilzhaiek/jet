package com.reconinstruments.utils;

//taken from Phone
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

	static SimpleDateFormat time = new SimpleDateFormat("h:mm aa", Locale.US);
	static SimpleDateFormat fullDate = new SimpleDateFormat("MM.dd.yy", Locale.US);
	static SimpleDateFormat weekDay = new SimpleDateFormat("EE", Locale.US);

	static SimpleDateFormat monthDay = new SimpleDateFormat("MMMM dd", Locale.US);
	
	public static String getTimeString(Date date){
		Calendar cal = Calendar.getInstance();
		Date currentDate = cal.getTime();
		long currentDateInMillis = currentDate.getTime();
		long eventDateInMillis = date.getTime();
		
		int diffInDays = (int) Math.floor((currentDateInMillis - eventDateInMillis) / (24 * 60 * 60 * 1000));
	
		if (diffInDays == 0) {
			return time.format(date);
		} else if (diffInDays == 1) {
			return "Yesterday";
		} else if (diffInDays <= 7) {
			return weekDay.format(date);
		} else {
			return fullDate.format(date);
		}
	}
	//like July 10
	public static String getDateString(Date date){
		return monthDay.format(date);
	}
}
