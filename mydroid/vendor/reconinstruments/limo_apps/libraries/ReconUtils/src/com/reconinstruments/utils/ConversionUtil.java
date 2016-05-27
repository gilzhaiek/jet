package com.reconinstruments.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


//taken from DashNotification
public class ConversionUtil {
	
	private static final double METERS_FEET_RATIO = 3.2808399;
	public static final double KM_MILE_RATIO = 0.621371192;
	private static final double METERS_MILE_RATIO = 1609.344;
	public static final double FEET_TO_MILES_RATIO = 1.0/5280.0;
    public static final double MILES_TO_FEET_RATIO = 5280.0;
	public static final double METERS_TO_KM_RATIO = 1.0/1000.0;
    public static final double KM_TO_METERS_RATIO = 1000.0;
	public static final double KM_p_H_IN_M_p_S = 3.6;
    public static final double DECIMETERS_IN_METER = 10.0;


	public static final double kgToLBs(double kg) {
		return kg * 11.0 / 5.0;
	}
	public static final int round(double number) {
		return (int)(number+ 0.5);
	}

	public static final double feetToInches(double feet) {
		return feet * 12.0;
	}

	public static final double metersToFeet(double meters) {
		return meters * METERS_FEET_RATIO;
	}
	
	public static final double metersToMiles(double meters) {
		return meters / METERS_MILE_RATIO;
	}
	
	public static final double kmsToMiles(double kms) {
		return kms * KM_MILE_RATIO;
	}
	
	public static final int celciusToFahrenheit(int Tc) {
		return (int)( (9.0/5)*Tc )+32;
	}
	public static final double feetToMiles(double feet){
		return feet*FEET_TO_MILES_RATIO;
	}
	
	public static final double metersToKm(double meters){
		return meters*METERS_TO_KM_RATIO;
	}
	

	public static final String convertUnitString(String rawMessage, boolean metric) {
		
		Pattern pattern;
		Matcher matcher;
		
		// Parse km/h and mph
		pattern = Pattern.compile("<\\-?[0-9\\.,]*\\|km/h>");
		matcher = pattern.matcher(rawMessage);

		DecimalFormat mDecFomat = new DecimalFormat();
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"km/h";
			} else {
				float speed = (float) ConversionUtil.kmsToMiles(Float.parseFloat(values[0]));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(speed)+"mph";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		// Parse m and ft
		pattern = Pattern.compile("<\\-?[0-9\\.,]*\\|m>");
		matcher = pattern.matcher(rawMessage);
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"m";
			} else {
				float length = (float) ConversionUtil.metersToFeet((Float.parseFloat(values[0])));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(length)+"ft";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		// parse celcius and fahrenheit (sp?)
		pattern = Pattern.compile("<\\-?[0-9\\.]*\\|c>");
		matcher = pattern.matcher(rawMessage);
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"C";
			} else {
				float temp = (float) ConversionUtil.celciusToFahrenheit((int)(Float.parseFloat(values[0])));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(temp)+"F";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		// parse distance (km and mi)
		pattern = Pattern.compile("<\\-?[0-9\\.,]*\\|km>");
		matcher = pattern.matcher(rawMessage);
		
		while(matcher.find()) {
			String match = matcher.group();
			
			// Convert
			String[] values = match.replace("<", "").replace(">", "").split("\\|");
			
			String replacement;
			if(metric) {
				replacement = values[0]+"km";
			} else {
				float temp = (float) ConversionUtil.kmsToMiles((Float.parseFloat(values[0])));
				mDecFomat.setMaximumFractionDigits(0);
				replacement = mDecFomat.format(temp)+"mi";
			}
			rawMessage = StringUtils.replace(rawMessage, match, replacement);
		}
		
		return rawMessage;
	}
	
	public static final String speedToPace(float speed, boolean metric){
	    String res;
	    if (metric) {	// The (float) speed is in Km/h
	        long paceInSeconds = (long)(1/speed * 3600.0f);
	        res = secondsToMinutes(paceInSeconds); // per Km
	    } else {
	        long paceInSeconds = (long)(1/speed * (float) METERS_MILE_RATIO*3.6f);
	        res = secondsToMinutes(paceInSeconds); // per Mi
	    }
	    if("59:59".equals(res)){ //invalid value
	        res = "---";
	    }
	    return res;
	}
	
	public static final String secondsToMinutes(long seconds){
	    return secondsToMinutes(seconds, -1);
	}
	
    public static final String secondsToMinutes(long seconds, int nearestVal){
        SimpleDateFormat df = null;
        df = new SimpleDateFormat("mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        if(nearestVal > 0){ 
            return df.format(roundToNearestN(seconds, nearestVal) * 1000);
        }
        return df.format(seconds * 1000); // don't round
    }

	public static long roundToNearestN(long number, int n) {
	    long remainder =number%n;
	    number += (remainder > n/2)? (n - remainder): (-remainder) ;
	    return number;
	}
}
