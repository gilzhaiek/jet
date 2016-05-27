//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.hudservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.hud_phone_status_exchange.PhoneStateMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.SettingsUtil;

public class IncomingPhoneStatusReceiver extends BroadcastReceiver {
    private final static String TAG = IncomingPhoneStatusReceiver.class.getSimpleName();

    private final static int PHONE_LOW_BATTERY_WARNING_THRESHOLD = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action =intent.getAction();
        if (action.equals("com.reconinstruments.SPORTS_ACTIVITY")) {
            int status =
                intent.getIntExtra("status",ActivityUtil.SPORTS_ACTIVITY_STATUS_ERROR);
            int type =
                intent.getIntExtra("type",ActivityUtil.SPORTS_TYPE_DEFAULT);
            sendActivityInfoXmlViaBt(context,status,type);
            return;
        }
        Log.v(TAG, "PHONE_STATE_CHANGED");

        if (!intent.hasExtra("message")) {
            Log.w(TAG, "Received intent without HUDConnectivityMessage attached.");
            return;
        }
        String xml = new String(new HUDConnectivityMessage(intent.getByteArrayExtra("message")).getData());
        PhoneStateMessage phoneState = new PhoneStateMessage(xml);

        // Handle a connectivity state message
        if (phoneState.getConnectivityState() != null) {
            PhoneStateMessage.CONNECTIVITY_STATE cs =
                phoneState.getConnectivityState();
            switch (cs) {
            case NONE:
                BtConnectionStateChangeReceiver
                    .postConnected(context, R.drawable.sp_connectivity);
                break;
            case MOBILE:        // drop down
            case WIFI:          // drop down
            case OTHER:
                BtConnectionStateChangeReceiver
                    .postConnected(context, R.drawable.phone_data);
                break;
            }
            broadcastState(context,cs.name());
        }

        // Handle the battery level message
        if (phoneState.getBatteryPercent() != null) {
            if (phoneState.getBatteryPercent() <= PHONE_LOW_BATTERY_WARNING_THRESHOLD) {
                new ReconToast(context, "Phone battery low").show();
            }
        }
    }

    public static final String PHONE_INTERNET_STATE =
        "com.reconinstruments.hudservice.PHONE_INTERNET_STATE";
    private static Intent sPhoneISIntent =
        new Intent(PHONE_INTERNET_STATE);
    private void broadcastState(Context c, String type) {
        Log.v(TAG,"Broadcast phone internet state " + type);
        sPhoneISIntent.putExtra("Provider",type);
        sPhoneISIntent.putExtra("IsConnected",!(type.equals("none")));
        c.sendStickyBroadcast(sPhoneISIntent);
    }

    public static final void sendActivityInfoXmlViaBt(Context c, int status, int type) {
        // Make sure we are connected
        if (SettingsUtil.getCachableSystemIntOrSet(c,"BTConnectionState", 0) != 2) {
            return;
        }
    
        String xmlString = "<recon intent=\"com.reconinstruments.SPORTS_ACTIVITY\">"
            + "<sports_activity_state>"
            + status + "</sports_activity_state> <sports_activity_type>" 
            + type + "</sports_activity_type> </recon>";
        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
        cMsg.setIntentFilter("com.reconinstruments.SPORTS_ACTIVITY");
        cMsg.setRequestKey(0);
        cMsg.setData(xmlString.getBytes());
        Intent i =
            new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");
        i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());
        c.sendBroadcast(i);
    }
    public static final void sendActivityInfoXmlViaBt(Context c) {
        int status = ActivityUtil.getActivityState(c);
        int type = ActivityUtil.getCurrentSportsType(c);
        sendActivityInfoXmlViaBt(c,status,type);
    }

}
