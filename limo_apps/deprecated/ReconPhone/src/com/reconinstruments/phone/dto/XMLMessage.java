package com.reconinstruments.phone.dto;
/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */



/**
 * @author hongzhi wang
 *This class defined the base interface for parsing/composing
 *a XML chunk that are passing through Bluetooth pipe between a Smartphone
 *and Recon Limo Goggle
 */
public abstract class XMLMessage
{
	static public final String BUDDY_INFO_MESSAGE  = "RECON_FRIENDS_LOCATION_UPDATE";
	static public final String CALL_RELAY_MESSAGE = "RECON_CALL_RECEIVE_RELAY";
	static public final String SMS_RELAY_MESSAGE = "RECON_SMS_RECEIVE_RELAY";
	static public final String MUSIC_RESPONSE_MESSAGE = "RECON_MUSIC_RESPONSE";
	static public final String MUSIC_CONTROL_MESSAGE = "RECON_MUSIC_CONTROL";
	static public final String PHONE_CONTROL_MESSAGE = "RECON_PHONE_CONTROL";
	static final boolean DUMP_MESSAGE_FOR_DEBUG = false;
	
	public XMLMessage()
	{
		
	}
	
	/**
	 * Utility function for composing the head element of the message
	 * 
	 */
	protected String composeHead( String intentStr )
	{
		return "<recon intent=\"" + intentStr + "\">";
	}
	
	/**
	 *Utility function for appending the closing element to the message 
	 *
	 */
	protected String appendEnding( String message )
	{
		return message + "</recon>";
	}
	
	/**
	 * given a object that describe the message
	 * Compose a XML chunk that will be sent out
	 * 
	 */
	public abstract String compose( Object messageInfo );
	
	/**
	 * parse a message string which is a XML chunk return an object
	 * 
	 */
	public abstract Object parse( String message );
}