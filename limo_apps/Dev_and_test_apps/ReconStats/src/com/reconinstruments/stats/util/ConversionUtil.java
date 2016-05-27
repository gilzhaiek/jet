package com.reconinstruments.stats.util;

public class ConversionUtil {
	
	private static final double METERS_FEET_RATIO = 3.2808399;
	private static final double KM_MILE_RATIO = 0.621371192;
	private static final double METERS_MILE_RATIO = 1609.344;
	
	public static double metersToFeet(double meters) {
		return meters * METERS_FEET_RATIO;
	}
	
	public static double metersToMiles(double meters) {
		return meters / METERS_MILE_RATIO;
	}
	
	public static double kmsToMiles(double kms) {
		return kms * KM_MILE_RATIO;
	}
	
	public static int celciusToFahrenheit(int Tc) {
		return (int)( (9.0/5)*Tc )+32;
	}
}
