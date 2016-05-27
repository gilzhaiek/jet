package com.reconinstruments.polarhr.service;

import android.os.Bundle;

public class PolarHRStatus {

	private int battery = -1, hr = -1, hrInterval = -1, minutesUsed = -1, connectionState = -1;
	private String deviceName = "";
	
	// Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device
	
	public static final int BATTERY_DEAD = 0;
	public static final int BATTERY_WEAK = 1;
	public static final int BATTERY_OK = 2;
	public static final int BATTERY_FULL = 3;
	
	public PolarHRStatus() {
		
	}
	
	public void setDeviceName(String name) {
		deviceName = name;
	}
	
	public void setConnectionState(int state) {
		connectionState = state;
	}
	
	public void setBatteryState(int batteryState) {
		battery = batteryState;
	}
	
	// Avg heart rate (beats/min)
	public void setAvgHeartRate(int avgHeartRate) {
		hr = avgHeartRate;
	}
	
	// Interval between heart beats (ms)
	public void setHeartRateInterval(int heartRateInterval) {
		hrInterval = heartRateInterval;
	}
	
	public void setMinutesUsed(int minUsed) {
		minutesUsed = minUsed;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public int getConnectionState() {
		return connectionState;
	}
	
	public int getBatteryState() {
		return battery;
	}
	
	public int getAvgHeartRate() {
		return hr;
	}
	
	public int getHeartRateInterval() {
		return hrInterval;
	}
	
	public int getMinutesUsed() {
		return minutesUsed;
	}
	
	public Bundle getBundle() {
		Bundle b = new Bundle();
		b.putInt("AvgHR", hr);
		b.putString("DeviceName", deviceName);
		b.putInt("ConnectionState", connectionState);
		b.putInt("HRInterval", hrInterval);
		b.putInt("MinutesUsed", minutesUsed);
		
		return b;
	}
	
	public String toString() {
		return "HR: " + Integer.toString(hr) + ", HR_INT: " + hrInterval + ", BAT: " + batteryIntToString(battery) + ", MIN_USED: " + Integer.toString(minutesUsed);
	}
	
	public static String batteryIntToString(int batteryState) {
		switch(batteryState) {
			case BATTERY_DEAD:
				return "Dead Battery";
			case BATTERY_WEAK:
				return "Weak Battery (2.7-2.78 V)";
			case BATTERY_OK:
				return "Normal Battery (2.78-2.9 V)";
			case BATTERY_FULL:
				return "Full Battery";
			default:
				return "Invalid Battery State!";
		}
	}
}
