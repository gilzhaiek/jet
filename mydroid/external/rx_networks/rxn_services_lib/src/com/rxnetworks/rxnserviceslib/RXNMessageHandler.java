package com.rxnetworks.rxnserviceslib;

import org.xml.sax.Attributes; 
import org.xml.sax.SAXException; 
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.rxnetworks.device.DeviceId;
import com.rxnetworks.rxnserviceslib.request.GetPGPSSeedMessage;
import com.rxnetworks.rxnserviceslib.request.GetSNTPTimeMessage;
import com.rxnetworks.rxnserviceslib.request.MslXybridRequest;
import com.rxnetworks.sntp.SNTPTime;

public class RXNMessageHandler extends DefaultHandler 
{

    // =========================================================== 
    // Fields 
    // =========================================================== 
	private boolean dataAccessAllowed = false;
	
    private boolean command_msg = false;
    @SuppressWarnings("unused")
	private boolean response_msg = false;
    private boolean name_tag = false;
    private boolean data_tag = false;
    
    private String command = "";
    private String tag_value;
    
    private final SocketServer socket;    
    private final RXNetworksService service;
    
    private GetPGPSSeedMessage seedRequest;
    private GetSNTPTimeMessage sntpRequest;
    private MslXybridRequest xybridRequest;
    
    public static final String TAG = "RXNetworksService.RXNMessageHandler";
     
	public RXNMessageHandler(RXNetworksService service, SocketServer socket)
	{
		this.service = service;
		this.socket = socket;
	}   
    // =========================================================== 
    // Getter & Setter 
    // =========================================================== 


    // =========================================================== 
    // Methods 
    // =========================================================== 
    @Override 
    public void startDocument() throws SAXException 
    { 

    } 

    @Override 
    public void endDocument() throws SAXException { 
         // Nothing to do 
    } 

    @Override
    public void error(SAXParseException e)
    {
    	Log.d(TAG, "Error: " + e.toString());
    }
    
    @Override
    public void fatalError(SAXParseException e)
    {
    	Log.d(TAG, "Fatal error: " + e.toString());
    }

    /** Gets be called on opening tags like: 
     * <tag> 
     * Can provide attribute(s), when xml was like: 
     * <tag attribute="attributeValue">*/ 
    @Override 
    public void startElement(String namespaceURI, String localName, 
              String qName, Attributes atts) throws SAXException { 
		 if (localName.equalsIgnoreCase("RXN_Command")) 
         { 
        	 this.command_msg = true;  
         }
         else if(localName.equalsIgnoreCase("RXN_Response"))
         {
	         this.response_msg = true;    	 
         }
         else if(localName.equalsIgnoreCase("name"))
         {
        	this.name_tag = true; 	 
         }
         else if(localName.equalsIgnoreCase("data"))
         {
        	this.data_tag = true;
           	if(command.matches("Get_PGPS_Seed"))
        	{
          		Log.i(TAG, "Request to get PGPS seed received.");
    			seedRequest = new GetPGPSSeedMessage();         		
        	}
          	else if(command.matches("Get_SNTP_Time"))
          	{
          		Log.i(TAG, "Request to get SNTP time received.");
          		sntpRequest = new GetSNTPTimeMessage();
          	}
            else if (command.matches("get_xybrid_response"))
            {
                Log.i(TAG, "Request to get xybrid response");
                xybridRequest = new MslXybridRequest();
            }
         } 
    }
     
    /** Gets be called on closing tags like: 
     * </tag> */ 
    @Override 
    public void endElement(String namespaceURI, String localName, String qName) 
              throws SAXException 
    {     	
        if (localName.equalsIgnoreCase("RXN_Command")) 
        {
       	
       	 	this.command_msg = false;
       	 	if(command.equalsIgnoreCase("Get_PGPS_Seed"))
       	 	{
				SendPGPSSeed messageToNative = new SendPGPSSeed(socket);
       	 		PGPSSeedHTTPRequest httpRequest = new PGPSSeedHTTPRequest(seedRequest.host, seedRequest.port, seedRequest.httpRequest);
       	 		httpRequest.getSeedData(messageToNative.getOutputStream());
				messageToNative.xmlSocketSend();
				Log.i(TAG, "PGPS Data Sent.");
       	 	}
       	 	else if(command.equalsIgnoreCase("Get_SNTP_Time"))
       	 	{
       	 		SNTPTime sntp = new SNTPTime(sntpRequest.host, sntpRequest.port);
       	 		sntp.getTime();
       	 		SendSNTPTime messageToNative = new SendSNTPTime(socket);
       	 		
       	 		/* If able to obtain clock */
       	 		if(sntp.currentTime != 0)
       	 		{
       	 			messageToNative.xmlSocketSend(sntp.currentTime, sntp.clockOffset);
       	 			Log.i(TAG, "SNTP Data Sent.");
       	 		}
       	 		else
       	 		{
       	 			Log.i(TAG, "No SNTP Data to Send.");
       	 		}
       	 	}
       	 	else if(command.equalsIgnoreCase("Get_Device_ID"))
       	 	{
       	 		SendDeviceID messageToNative = new SendDeviceID(socket);
       	 		messageToNative.xmlSocketSend(DeviceId.getDeviceIMEI(), DeviceId.getDeviceIMSI());
       	 		Log.i(TAG, "Device IMEI: " + DeviceId.getDeviceIMEI() + " and IMSI: " + DeviceId.getDeviceIMSI() +" Sent.");
       	 	}
       	 	else if(command.equalsIgnoreCase("Send_Data_Access_Status"))
       	 	{
       	 		SendDataAccessStatus messageToNative = new SendDataAccessStatus(socket);
       	 		messageToNative.xmlSocketSend(dataAccessAllowed);
       	 		Log.i(TAG, "Data Access Status:" +dataAccessAllowed+" Sent.");
       	 	}
			else if (command.equalsIgnoreCase("get_xybrid_response")) {
				Log.i(TAG, command);
				if (xybridRequest.rt || xybridRequest.bce)
				{
					Log.d(TAG, "Handling Xybrid request");
					service.handleXybridRequest(xybridRequest);
				}
				
				if (xybridRequest.synchro)
				{
					Log.d(TAG, "Handling Synchro request");
					service.handleSynchroRequest(xybridRequest);
				}
			} 

       	 	this.command = "";
        }
        else if(localName.equalsIgnoreCase("RXN_Response"))
        {
	         this.response_msg = false;    	 
        }
        else if(localName.equalsIgnoreCase("name"))
        {
       	 this.name_tag = false;
        }
        else if(localName.equalsIgnoreCase("data"))
        {
       	 this.data_tag = false;
        }
        
    	if(command.equalsIgnoreCase("Get_PGPS_Seed"))
    	{
    		if(this.data_tag)
    		{
    			if(localName.equalsIgnoreCase("host"))
    			{
    				seedRequest.host = tag_value;
    			}
    			else if(localName.equalsIgnoreCase("port"))
    			{
    				seedRequest.port = Integer.parseInt(tag_value);
    			}
    			else if(localName.equalsIgnoreCase("request"))
    			{
    				seedRequest.httpRequest = tag_value;
    			}    			    			
    		}
    	}
    	else if(command.equalsIgnoreCase("Get_SNTP_Time"))
    	{
    		if(this.data_tag)
    		{
    			if(localName.equalsIgnoreCase("host"))
    			{
    				sntpRequest.host = tag_value;
    			}
    			else if(localName.equalsIgnoreCase("port"))
    			{
    				sntpRequest.port = Integer.parseInt(tag_value);
    			}
    		}
    	}
    	else if(command.equalsIgnoreCase("Send_Data_Access_Status"))
    	{
    		if(localName.equalsIgnoreCase("data"))
    		{
    			dataAccessAllowed = Boolean.parseBoolean(tag_value);
    			Log.i(TAG, "Data Access Status:" +dataAccessAllowed);
    		}
    	}
        else if (command.equalsIgnoreCase("Terminate_RXNServices"))
        {
        	Log.i(TAG, "Terminate request received.  Shutting down.");
        	service.stopSelf();
        	socket.stop();
        	System.exit(3);
        }
        else if (command.equalsIgnoreCase("get_xybrid_response"))
        {
            if (this.data_tag)
            {
                if (localName.equalsIgnoreCase("Host"))
                {
                    xybridRequest.host = tag_value;
                }
                else if (localName.equalsIgnoreCase("MaxIndex"))
                {
                    xybridRequest.maxIndex = Integer.parseInt(tag_value);
                }
                else if (localName.equalsIgnoreCase("Port"))
                {
                    xybridRequest.port = Integer.parseInt(tag_value);
                }
                else if (localName.equalsIgnoreCase("VendorID"))
                {
                    xybridRequest.vendorId = tag_value;
                }
                else if (localName.equalsIgnoreCase("vendorSalt"))
                {
                    xybridRequest.vendorSalt = tag_value;
                }
                else if (localName.equalsIgnoreCase("Include_WiFi"))
                {
                    xybridRequest.useWifi = Boolean.parseBoolean(tag_value);
                }
                else if (localName.equalsIgnoreCase("Include_Cell"))
                {
                    xybridRequest.useCell = Boolean.parseBoolean(tag_value);
                }
                else if (localName.equalsIgnoreCase("GPSWeek"))
                {
                    xybridRequest.gpsWeek = Integer.parseInt(tag_value);
                }
                else if (localName.equalsIgnoreCase("rt"))
                {
                	xybridRequest.rt = Boolean.parseBoolean(tag_value);
                }
                else if (localName.equalsIgnoreCase("bce"))
                {
               		xybridRequest.bce = !tag_value.equalsIgnoreCase("none");
            		xybridRequest.setFiltered(tag_value.equalsIgnoreCase("filtered"));
                }
                else if (localName.equalsIgnoreCase("synchro"))
                {
                	xybridRequest.synchro = Boolean.parseBoolean(tag_value);
                }
                else if (localName.equalsIgnoreCase("Donate"))
                {
                	xybridRequest.donate = Boolean.parseBoolean(tag_value);
                }
            }
        }
    } 
     
    /** Gets be called on the following structure: 
     * <tag>characters</tag> */ 
    @Override 
   public void characters(char ch[], int start, int length) 
   {
    	if(this.command_msg)
    	{
    		if(this.name_tag)
    		{
    			command = new String(ch, start, length);
    		}
    		
	    	if(!command.equalsIgnoreCase(null))
	    	{
	    		if(this.data_tag)
	    		{
	    			tag_value = new String(ch, start, length).trim();
	    		}
    		}
    	}
   } 
}
