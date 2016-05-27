package com.rxnetworks.device;

public class DeviceId 
{
	static String deviceIMEI;
	static String deviceIMSI;
	
	public static void setDeviceId(String sIMEI, String sIMSI)
	{
		deviceIMEI = sIMEI;
		deviceIMSI = sIMSI;
	}
	
	public static String getDeviceIMEI()
	{
		return deviceIMEI;
	}
	public static String getDeviceIMSI()
	{
		return deviceIMSI;
	}
}
