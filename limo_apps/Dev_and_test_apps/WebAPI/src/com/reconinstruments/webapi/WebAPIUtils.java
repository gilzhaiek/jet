package com.reconinstruments.webapi;

import android.util.Base64;

/**
 *Copyright 2012 Recon Instruments
 *All Rights Reserved.
 *
 * @author Patrick Cho
 * 
 *  This helper method class
 */

public class WebAPIUtils {
	
	/**
	 * 
	 * This method decode the String encoded in Base64 format
	 * into a String
	 * 
	 * @param base64String 
	 * 	base64 encoded String
	 * @return decodedBase64String
	 * 	decoded String 
	 */
	public static String decodeBase64String(String base64String){		
		
		byte[] decoded = Base64.decode(base64String, Base64.DEFAULT);
		
		String decodedBase64String = new String(decoded);
		
		return decodedBase64String; 
		
	}
	
	/**
	 * 
	 * This method decode the String encoded in Base64 format
	 * into a byte array
	 * 
	 * @param base64String 
	 * 	base64 encoded String
	 * @return decodedBase64ByteArray
	 * 	decoded byte array 
	 */
	public static byte[] decodeBase64ByteArray(String base64String){		
		
		byte[] decodedBase64ByteArray = Base64.decode(base64String, Base64.DEFAULT);
		
		return decodedBase64ByteArray; 
		
	}

}
