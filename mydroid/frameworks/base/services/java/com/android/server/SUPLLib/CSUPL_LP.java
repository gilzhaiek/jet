package android.supl;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import android.app.ActivityThread;
import android.net.Uri;

import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;

import android.os.Handler;
import android.content.Context;
import android.content.BroadcastReceiver;

import android.content.Intent;
import android.content.IntentFilter;

import android.telephony.gsm.SmsMessage;

import java.io.UnsupportedEncodingException;
import android.content.res.TypedArray;
import android.app.PendingIntent;

import android.provider.Telephony.Sms.Intents;
public class CSUPL_LP
{

    public static final String TAG = "CSUPL_LP";
	private static Context mContext;

    private static boolean mHandleSMS = false;
    private static boolean mHandleWAP = false;
    private static final int NOTIFICATION_TIME_OUT = 15000;
    private static final int NOTIFICATION_TIME_MAX = 1000000;

    // Intent actions.
    private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String DATA_SMS_ACTION = "android.intent.action.DATA_SMS_RECEIVED";
    private static final String WAP_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static final String SUPL_MSG_ACTION = "android.supl.msg";
    private static final String SUPL_CONFIG_ACTION = "android.supl.SUPL_CONFIG";
    private static final int DATA_SMS_PORT = 7275;

	private static final int CELL_TYPE_GSM = 1;
	private static final int CELL_TYPE_UMTS = 2;

    private static int g_MCC = -1;
    private static int g_MNC = -1;
    private static int g_LAC = -1;
    private static int g_CI = -1;
	private static int g_CiType = -1;
	private static String g_MSISDN = "-1";
	private static int g_HMCC =-1;
	private static int g_HMNC =-1;

    // Native
    private static native boolean PostBodyToQueue(byte[] body);
    private static native boolean NotificationResult(int[] body);

    public static void Init(Context context)
    {
        debugMessage("init():+ CSUPL_LP Ctor.");
        mContext = context;
        Log.d("CSUPL_LP", "init(): Init mContext="+mContext);

        IntentReceiver receiver = new IntentReceiver();
        // Intent Filters
        IntentFilter filter = new IntentFilter();
		filter.addAction(Intents.WAP_PUSH_RECEIVED_ACTION);
		//filter.addAction(Intents.SMS_RECEIVED_ACTION);
		//filter.addAction(Intents.DATA_SMS_RECEIVED_ACTION);
        try {
            filter.addDataType("application/vnd.omaloc-supl-init");
			 debugMessage("init():- CSUPL_LP addDataType");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.w("CSUPL", "[GPS] Malformed SUPL init mime type");
        }

        mContext.registerReceiver(receiver, filter);
		filter = new IntentFilter();
		filter.addAction(Intents.DATA_SMS_RECEIVED_ACTION);
		filter.addDataScheme("sms");
        mContext.registerReceiver(receiver, filter);
		//debugMessage("init():+ CSUPL_LP new IntentFilter");
		filter = new IntentFilter();
		filter.addAction(SUPL_MSG_ACTION);
	    filter.addAction(SUPL_CONFIG_ACTION);

        // Register Broadcast receiver
        //IntentReceiver receiver = new IntentReceiver();
        mContext.registerReceiver(receiver, filter);

        debugMessage("init():- CSUPL_LP Ctor.");
    }

    public static void debugMessage(String msg)
    {
        Log.d(TAG, msg);
    }

    // Gets intents from SUPL BroadcastReceiver.
    public static void update(Intent intent)
    {
		debugMessage("CSUPL: +update");
		int i, j, k;
			int AppId;
			byte[] bAppId;
			
		if (intent.getAction().equals(Intents.DATA_SMS_RECEIVED_ACTION) )
        {
            debugMessage("Action=DATA_SMS_ACTION");
            if (!mHandleSMS)
            {
                return;
            }
            Uri uri = intent.getData();
            if (uri != null)
            {
                int port = intent.getData().getPort();
                if (port == DATA_SMS_PORT)
                {
                    byte[] body = getSmsBodyFromIntent(intent);
                    PostBodyToQueue(body);
                    Toast.makeText(mContext,"CSUPL_LP!\nsendSMS: "
                                   + body.length, Toast.LENGTH_SHORT).show();
                }
            }
        }

		if (intent.getAction().equals(Intents.WAP_PUSH_RECEIVED_ACTION))
        {
            debugMessage("Action=WAP_PUSH_RECEIVED_ACTION");
            if (!mHandleWAP)
            {
            	debugMessage("WAP_PUSH_RECEIVED_ACTION DISABLED");
                return;
            }

			// Get WapHeader
			byte[] WapHeader = (byte[]) intent.getSerializableExtra("header");

			// Print WapHeader
   			for (i=0; i < WapHeader.length; i++)
   			{
   				j = (int)WapHeader[i]&0xFF;
   				debugMessage("WapHeader[" + i + "]=byte[" + WapHeader[i] + "]" + "int[" + j + "]");
    		}

			//boolean bApp = false;

			bAppId = new byte[32];
			
			
			// Prepare AppId array
			for(i = 0; i < 32; i++)
			{
				bAppId[i]=' ';
			};

			// Get AppId
			for(i=0; i<WapHeader.length; i++)
			{
				if( ((int)WapHeader[i] & 0xFF) == 0xAF) 
				{
					if((WapHeader.length-i) > 2) // String
					{
						debugMessage("AppId: String");
						for(j = i+1, k = 0; j < WapHeader.length; j++, k++)
						{
							if(WapHeader[j]!=0)
								bAppId[k]=WapHeader[j];
						}						

					
						// Print AppId
						for(i = 0; i < bAppId.length; i++)
						{
							j = (int)bAppId[i]&0xFF;
							debugMessage("bAppId[" + i + "]=byte[" + bAppId[i] + "]" + "int[" + j + "]");
						}
						
						// Compare AppId
						String sAppId = new String(bAppId);
						debugMessage("sAppId=[" + sAppId + "]");
						debugMessage("sAppId=[" + sAppId.trim() + "]");
						if (sAppId.trim().equals("x-oma-application:ulp.ua"))
						{
							debugMessage("Valid AppId");
							break;
						}
						else
						{
							debugMessage("Error: Invalid AppId");
							return;
						}


						
					}
					else // Hex
					{
						debugMessage("AppId: Hex");
						AppId = (int)WapHeader[i+1] & 0xFF;
						if( AppId != 0x90)
						{
							debugMessage("Error: Invalid Hex AppId = " + AppId);
							return;
						}
						else
						{
							debugMessage("Hex AppId = " + AppId);
							break;
						}						
					}
				}
			}	


            //String mime = intent.getType();
            //if (mime.compareTo("application/vnd.omaloc-supl-init") == 0) 
            {
            	byte[] WapBody = (byte[]) intent.getSerializableExtra("data");

				debugMessage("Action=WAP_PUSH_RECEIVED_ACTION, Got WAP Body");
	
    	 		// Print Data 
    			for (i=0; i < WapBody.length; i++)
    			{
    				debugMessage("WapBody[" + i + "]=[" + WapBody[i] + "]");
	    		}
							
           	 	if (WapBody != null)
            	{
            		debugMessage("Action=WAP_PUSH_RECEIVED_ACTION, PostBodyToQueue");
                	PostBodyToQueue(WapBody);
                }
            }		
		}


        if (intent.getAction().equals(SUPL_MSG_ACTION))
        {
            debugMessage("Action=SUPL_MSG_ACTION");

            boolean verification;
            String reqId;
            String clName;
            int sessionId;
            int timeout;

            // verification
            if (intent.getExtras().containsKey("verification"))
            {
                verification = intent.getExtras().getBoolean("verification");
            }
            else
            {
            	debugMessage("Action=SUPL_MSG_ACTION: failed at verification ->return ");
                return;
            }

            // reqId
            if (intent.getExtras().containsKey("reqId"))
            {
                reqId = intent.getExtras().getString("reqId");
            }
            else
            {
            	debugMessage("Action=SUPL_MSG_ACTION: failed at reqId ->return ");
                return;
            }

            // clNeme
            if (intent.getExtras().containsKey("clNeme"))
            {
                clName = intent.getExtras().getString("clNeme");
            }
            else
            {
            	debugMessage("Action=SUPL_MSG_ACTION: failed at clNeme ->return ");
                return;
            }

            // sessionId
            if (intent.getExtras().containsKey("sessionId"))
            {
                sessionId = intent.getExtras().getInt("sessionId");
            }
            else
            {
            	debugMessage("Action=SUPL_MSG_ACTION: failed at sessionId ->return ");
                return;
            }

            // timeout
            if (intent.getExtras().containsKey("timeout"))
            {
                timeout = intent.getExtras().getInt("timeout");
            }
            else
            {
            	debugMessage("Action=SUPL_MSG_ACTION: failed at timeout ->return ");
                return;
            }

            showSUPLInitDialog(verification, reqId, clName, sessionId, timeout);
        }

        if (intent.getAction().equals(SUPL_CONFIG_ACTION))
        {
            debugMessage("Action=SUPL_CONFIG_ACTION");

            String  StorePath   = intent.getExtras().getString("StorePath");
            String  SLPHost     = intent.getExtras().getString("SLPHost");
            Boolean IsLocalhost = intent.getExtras().getBoolean("IsLocalhost");

            if (StorePath == null || SLPHost == null)
            {
                debugMessage("Error reconfigure.");
                return;
            }

            CNet.SetPath(StorePath);
            CNet.SetImpl("Socket");
            CNet.localhost = IsLocalhost;
            CNet.slphost_port = SLPHost;
            CNet.ReInit();
        }
    } // update(Intent intent)


    // Updates sms/wap receiving settings
    public static void messageListen(boolean allow, int sms_wap)
    {
        debugMessage("+messageListen");
        if (allow)
        {
            if (sms_wap == 1)
            {
                mHandleSMS = true;
            }
            else if (sms_wap == 2)
            {
                mHandleWAP = true;
            }
        }
        else
        {
            if (sms_wap == 1)
            {
                mHandleSMS = false;
            }
            else if (sms_wap == 2)
            {
                mHandleWAP = false;
            }
        }
        debugMessage("-messageListen");
    }

    // Parses SMS intent and return SMS body
    private static final byte[] getSmsBodyFromIntent(Intent intent)
    {
        debugMessage("+getSmsBodyFromIntent");
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        byte[][] pduObjs = new byte[messages.length][];

        for (int i = 0; i < messages.length; i++)
        {
            pduObjs[i] = (byte[]) messages[i];
        }

        if (pduObjs.length == 0)
        {
            debugMessage("-getSmsBodyFromIntent: pduObjs.length == 0");
            return null;
        }
        else if (pduObjs.length == 1)
        {
            debugMessage("-getSmsBodyFromIntent: pduObjs.length == 1");
            return SmsMessage.createFromPdu(pduObjs[0]).getUserData();
        }
        else
        {
            int BodyLength = 0;
            int pos = 0;
            int i = 0;
            int j = 0;

            byte[][] pdus = new byte[pduObjs.length][];
            for (i = 0; i < pdus.length; i++)
            {
                pdus[i] = SmsMessage.createFromPdu(pduObjs[i]).getUserData();
                BodyLength += pdus[i].length;
            }
            byte [] msgs = new byte [BodyLength];
            for (i = 0; i < pdus.length; i++)
            {
                for (j = 0; j < pdus[i].length; j++)
                {
                    msgs[pos++] = pdus[i][j];
                }
            }
            debugMessage("-getSmsBodyFromIntent");
            return msgs;
        }
    }

    // Shows SUPL Init notification dialog.
    public static void showSUPLInitDialog(final boolean verification,
                                          byte[] requestorId,
                                          int encId,
                                          byte[] clientName,
                                          int encCn,
                                          final int sessionId,
                                          int timeout)
    {
        debugMessage("+showSUPLInitDialog");
        debugMessage("showSUPLInitDialog:verification=" + verification);
        debugMessage("showSUPLInitDialog:requestorId=" + requestorId);
        debugMessage("showSUPLInitDialog:clientName=" + clientName);
        debugMessage("showSUPLInitDialog:sessionId=" + sessionId);
        debugMessage("showSUPLInitDialog:timeout=" + timeout);

        String reqId = "Empty";
        String clNeme = "Empty";
        int notificTimeOut = NOTIFICATION_TIME_OUT;

        if (timeout > 0 && timeout < NOTIFICATION_TIME_MAX)
        {
            notificTimeOut = timeout;
        }

            if (requestorId != null && requestorId.length != 0)
            {
	        reqId = new String(requestorId);
            }

            if (clientName != null && clientName.length != 0)
            {
            clNeme = new String(clientName);
        }
        
		
        Intent MsgIntent = new Intent(SUPL_MSG_ACTION);
        MsgIntent.putExtra("reqId", reqId);
        MsgIntent.putExtra("clNeme", clNeme);
        MsgIntent.putExtra("verification", verification);
        MsgIntent.putExtra("sessionId", sessionId);
        MsgIntent.putExtra("timeout", notificTimeOut);

        debugMessage("showSUPLInitDialog: sendBroadcast");
        mContext.sendBroadcast(MsgIntent);
    }

    // Shows SUPL Init notification dialog.
    private static void showSUPLInitDialog(final boolean verification,
                                           String requestorId,
                                           String clientName,
                                           final int sessionId,
                                           int timeout)
    {
        debugMessage("+showSUPLInitDialog2");
        debugMessage("verification=" + verification);
        debugMessage("requestorId=" + requestorId);
        debugMessage("clientName=" + clientName);
        debugMessage("sessionId=" + sessionId);
        debugMessage("timeout=" + timeout);

        int notificTimeOut = timeout;

        String[] elem = { "Always allow" };
        boolean[] elem_flag = { false };

        debugMessage("showSUPLInitDialog2: check0");

                final TimerAlertDialog td = new TimerAlertDialog(mContext, notificTimeOut);

                debugMessage("showSUPLInitDialog2: check1");
                if (verification == true)
                {
                    debugMessage("showSUPLInitDialog: Verification");
                    td.setTitle("SUPL Information!")
					.setMessage("SUPL INIT message received! \n" + 
                    	"Requestor Id : " + requestorId + "\n" +
						"Client Name  : " + clientName + "\n") 
                    .setPositiveButton("Allow",
                                       new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog,
                                            int whichButton)
                        {
                            // result if user click Allow button
                            td.cancel_flag = true;
                            returnSUPLInitDialogResult(1, sessionId);
                        }
                    })
                    .setNegativeButton("Deny",
                                       new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog,
                                            int whichButton)
                        {
                            // result if user click Deny button
                            td.cancel_flag = true;
                            returnSUPLInitDialogResult(2, sessionId);
                        }
                    });


					 td.setOnCancelListener(new DialogInterface.OnCancelListener()
                	 {
	                    public void onCancel(DialogInterface dialog)
	                    {
	                    	debugMessage("showSUPLInitDialog: onCancel");
	                        if (!td.cancel_flag)
	                        {
	                            td.cancel_flag = true;
								debugMessage("showSUPLInitDialog: cancel_flag set to True");
	                            if (verification)
	                            {
	                                returnSUPLInitDialogResult(3, sessionId);
	                            }
	                            else
	                            {
	                                returnSUPLInitDialogResult(3, sessionId);
	                            }
	                        }
	                    }
                	}).show();
					 
                }
                else
                {


                    debugMessage("showSUPLInitDialog: No Verification");
                    td.setTitle("SUPL Information!")
					.setMessage("SUPL INIT message received! \n" + 
                    	"Requestor Id : " + requestorId + "\n" +
																	"Client Name  : " + clientName + "\n")
					
					
							.setPositiveButton("OK",
										   new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
												int whichButton)
							{
								// result if user click Allow button
								td.cancel_flag = true;
								returnSUPLInitDialogResult(1, sessionId);
							}
						})
						 .create();
	
						debugMessage("showSUPLInitDialog: No Verification, after Button OK");

						td.setOnCancelListener(new DialogInterface.OnCancelListener()
		                {
		                    public void onCancel(DialogInterface dialog)
		                    {
		                    	debugMessage("showSUPLInitDialog: onCancel -> Do nothing");
		                    }
		                }).show();
                }
                
	            debugMessage("TimerAlertDialog: show()");
				
    }


    // Returns result of SUPL Init notification dialog.
    private static void returnSUPLInitDialogResult(int result, int sessionId)
    {
        int body[] = new int[2];
        body[0] = result;
        body[1] = sessionId;
        NotificationResult(body);
    }

    /** Returns subscriber identificator (IMSI).
      * @return IMSI
      */
    public static byte[] getSubscriberId()
    {
        Log.d(TAG, " getSubscriberId: Entering \n");

        String strSID = null;

        TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            strSID = tm.getSubscriberId();
        }

        if (null == strSID)
            strSID = "310260000000000"; // fixed value for tests

        Log.d(TAG, " getSubscriberId: strSID is " + strSID);
        byte[] val = new byte[strSID.length()];
        for (int i = 0; i < strSID.length(); i++)
        {
             val[i] = Byte.parseByte(strSID.substring(i, i + 1));//(byte) Integer.parseInt(strSID.substring(i, i + 1));
        }

        return val;
    }

	/** Returns MSISDN **/
	public static byte[] getMSISDN()
	{
		Log.e(TAG, " getMSISDN: Entering \n");
		String phoneNumber;
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

		phoneNumber = tm.getLine1Number();
		if (null == phoneNumber)
			phoneNumber = g_MSISDN; 
        Log.d(TAG, " ===>>> getMSISDN is " + phoneNumber);
		byte[] val = new byte[phoneNumber.length()];
        for (int i = 0; i < phoneNumber.length(); i++)
        {
            val[i] = (byte) Integer.parseInt(phoneNumber.substring(i, i + 1));
        }

        return val;

    }
	public static int[] getHSlp() 
	{
		int[]  MccMnc = { g_HMCC, g_HMNC };
		Integer  hmcc = -1, hmnc = -1;
		String SimOperator;

		Log.d(TAG, "getHSlp: g_HMCC " + g_HMCC);
		Log.d(TAG, "getHSlp: g_HMNC " + g_HMNC);		

		// Update  MCC and MNC values if available from Telephony layer
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

		Log.d(TAG,"getHSlp : getSimOperator - Get MCC and MNC of SIM Operator");						
		SimOperator 		 = tm.getSimOperator();
		if(SimOperator != null && SimOperator.length() > 0)
		{
		  try
		  {
			// SimOperator is coded as MCC.MNC (ex: 20820 -> MCC = 208, MNC = 20)
			hmcc = Integer.parseInt(SimOperator.substring(0, 3));
			MccMnc[0] = hmcc;
			Log.d(TAG, "getMccMnc MCC: " + hmcc);
			hmnc = Integer.parseInt(SimOperator.substring(3));
			MccMnc[1] = hmnc;
			Log.d(TAG, "getMccMnc MNC: " + hmnc);
		   }
		 catch(NumberFormatException e) {}
	    }
		else
		{
			Log.d(TAG, "getHSlp: SimOperator Null");
		}

		Log.d(TAG, "getHSlp: HMCC " + MccMnc[0]);
		Log.d(TAG, "getHSlp: HMNC " + MccMnc[1]);
	 

	 return MccMnc ;
	}	

    /**
     * Returns GSM network information
     * @return array that containes LAL, MCC, MNC, CellID parameters
     */
    public static int[] getGSMInfo()
    {
        int[]  GSMinfo = { -1, -1, -1, -1,0 };

        Log.e(TAG, " getGSMInfo: in test cells!");

        // Update cell parameters used for SUPL if available from Telephony layer
        updateGSMInfo();

        GSMinfo[0] = g_CI;
        GSMinfo[1] = g_LAC;
        GSMinfo[2] = g_MCC;
        GSMinfo[3] = g_MNC;
		GSMinfo[4] = g_CiType;

        Log.d(TAG," GSMinfo.CI : " + GSMinfo[0]);
        Log.d(TAG," GSMinfo.LAC: " + GSMinfo[1]);
        Log.d(TAG," GSMinfo.MNC: " + GSMinfo[2]);
        Log.d(TAG," GSMinfo.MCC: " + GSMinfo[3]);
		Log.d(TAG," GSMinfo.CELL ID TYPE: " + GSMinfo[4]);

        return GSMinfo;
    }

    public static void updateGSMInfo()
    {
        Integer cid = -1, lac = -1, mcc = -1, mnc = -1;
		int cidType=0;
        String networkOperator;

        // Update CellId, LAC, MCC and MNC values if available from Telephony layer
        TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

		Log.d(TAG, "updateGSMInfo : PHONE TYPE: " + tm.getPhoneType());

	    if(tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
	    {
	       	networkOperator          = tm.getNetworkOperator();
	    	if(networkOperator != null && networkOperator.length() > 0)
	    	{
	        try
	        {
	            // networkOperator is coded as MCC.MNC (ex: 20820 -> MCC = 208, MNC = 20)
	            mcc = Integer.parseInt(networkOperator.substring(0, 3));
	            Log.d(TAG, "MCC: " + mcc);
	            mnc = Integer.parseInt(networkOperator.substring(3));
	            Log.d(TAG, "MNC: " + mnc);
	        }
	        catch(NumberFormatException e) {return;}
	    }

		GsmCellLocation location = (GsmCellLocation)tm.getCellLocation();
		if(location != null)
		{
		   cid = location.getCid();
		   Log.d(TAG, "Cell ID: " + cid);
		   lac = location.getLac();
		   Log.d(TAG, "LAC: " + lac);
		}

	}
	else
	{
		Log.d(TAG,"Phone type is not GSM or UMTS, Ignoring");
		return;
	}

	cidType = tm.getNetworkType();
	Log.d(TAG, "updateGSMInfo : NETWORK TYPE: " + cidType);
	if( (cidType==TelephonyManager.NETWORK_TYPE_UMTS)    || 
		(cidType==TelephonyManager.NETWORK_TYPE_HSDPA)   || 
		(cidType==TelephonyManager.NETWORK_TYPE_HSPA)    || 
		(cidType==TelephonyManager.NETWORK_TYPE_HSUPA)   || 
		(cidType==TelephonyManager.NETWORK_TYPE_IDEN) ) 
	{
		CSUPL_LP.SetCellType(CELL_TYPE_UMTS);	
		if((cid != -1) && (mcc != -1) && (mnc != -1))   
		{
			Log.d(TAG,"Updating UMTS/HSPA Cell Information");
            CSUPL_LP.SetCi(cid);
            CSUPL_LP.SetMcc(mcc);
            CSUPL_LP.SetMnc(mnc);
        }
		else
		{
			Log.d(TAG,"updateGSMInfo : Invalid UMTS/HSPA Cell Information");
		}
	}

	else if( (cidType==TelephonyManager.NETWORK_TYPE_GPRS)   || 
		     (cidType==TelephonyManager.NETWORK_TYPE_EDGE) )
	{
		CSUPL_LP.SetCellType(CELL_TYPE_GSM);
	    if((cid != -1) && (lac != -1) && (mcc != -1) && (mnc != -1))
        {
            Log.d(TAG,"Updating GSM/GPRS Cell Information");
            CSUPL_LP.SetCi(cid);
            CSUPL_LP.SetLac(lac);
            CSUPL_LP.SetMcc(mcc);
            CSUPL_LP.SetMnc(mnc);			
        }
		else
		{
			Log.d(TAG,"updateGSMInfo : Invalid GSM/GPRS Cell Information");
		}
	}
	else
	{
		Log.d(TAG,"Cell Information type is not GSM or UMTS, Ignoring");
	}
		
}

    

/*
    public static void SetContext(Context context) {
        // Set mContext value from current context as required to get values from Telephony layer
        //Log.d(TAG, "In SetContext()");
        mContext = context;
    }
*/
    public static void SetMcc(int mcc) {
        g_MCC = mcc;
    }

    public static void SetMnc(int mnc)
    {
        g_MNC = mnc;
    }

    public static void SetLac(int lac)
    {
        g_LAC = lac;
    }

    public static void SetCi(int ci)
    {
        g_CI = ci;
    }

	public static void SetCellType(int ciType) {
        g_CiType = ciType;
    }
	public static void SetMsisdn(String msisdn) {
		g_MSISDN = msisdn;
    }
		    public static void SetHMcc(int hmcc) {
        g_HMCC = hmcc;
    }

    public static void SetHMnc(int hmnc) {
        g_HMNC = hmnc;
    }
}

class IntentReceiver extends BroadcastReceiver
{
    private CSUPL_LP observer;

    public void IntentReceiver()
    {
        Log.d("CSUPL_LP","IntentReceiver Ctor");
    }

    public void notify(Object param)
    {
        CSUPL_LP.update((Intent) param);
    }

    public void onReceive(Context context, Intent intent)
    {
        Log.d("CSUPL_LP","BroadcastReceiver onReceive");
        notify(intent);
    }
}

class TimerAlertDialog extends AlertDialog.Builder
{
    public boolean cancel_flag = false;
    public AlertDialog.Builder b;
    public AlertDialog d;

    private long _time;
    private Handler _handler = new Handler();

    public TimerAlertDialog(Context context, long time)
    {
        super(context);
        Log.d("CSUPL_LP","TimerAlertDialog: Ctor");
        _time = time;
    }

    public AlertDialog show()
    {
        Log.d("CSUPL_LP","TimerAlertDialog: +Show");
        _handler.removeCallbacks(timerTask);
        _handler.postDelayed(timerTask, _time);

        d = super.create();
        d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        d.show();
        Log.d("CSUPL_LP","TimerAlertDialog: +Show");
        return d;
    }

    private Runnable timerTask = new Runnable()
    {
        public void run()
        {
            if (!cancel_flag)
            {
                d.cancel();
            }
        }
    };
}

