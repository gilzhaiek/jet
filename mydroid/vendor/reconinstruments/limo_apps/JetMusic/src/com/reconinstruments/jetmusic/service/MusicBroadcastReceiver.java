package com.reconinstruments.jetmusic.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.connect.messages.MusicMessage;
import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.messages.MusicMessage.PlayerState;
import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.messages.MusicMessage.Type;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;

/**
 * 
 * <code>MusicBroadcastReceiver</code> listens HUDService state changed event and takes proper action, 
 * if disconnected then disable avrcp profile, if connected then enable avrcp profile.
 *
 */
public class MusicBroadcastReceiver extends BroadcastReceiver{
    private static final String TAG = MusicBroadcastReceiver.class.getSimpleName();
    
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        final String action = arg1.getAction();
        if ("HUD_STATE_CHANGED".equals(action)) {
            int state = arg1.getIntExtra("state", 0);
            if (state == 0) { 
                Log.w(TAG, "HUD_STATE_CHANGED to 0, HUDService reports: disconnected");
                AVRCPManager.getInstance(arg0).profileDisable();
            } else if (state == 1) {
                Log.i(TAG, "HUD_STATE_CHANGED to 1, HUDService reports: connecting");
            } else if (state == 2) {
                Log.i(TAG, "HUD_STATE_CHANGED to 2, HUDService reports: connected");
                boolean profileEnabled = AVRCPManager.getInstance(arg0).profileEnable();
                Log.d(TAG, "profileEnabled = " + profileEnabled);
            }            
        }else if("RECON_MUSIC_MESSAGE".equals(action)){
            Bundle b = arg1.getExtras();
            if(b!=null){
                HUDConnectivityMessage cMsg = new HUDConnectivityMessage(b.getByteArray("message"));
                String s = new String(cMsg.getData());
                
                if(s!=null){
                    MusicMessage message = new MusicMessage(s);
                    if(message.type==MusicMessage.Type.STATUS){
                        Log.d(TAG, "got music status: "+s);
                        if(message.info != null && message.info.volume != null){
                            Log.d(TAG, "message.info.volume: "+message.info.volume);
                            //TODO send volume changed broadcast message here
                        }
                    }
                }
            }
        }
    }

}
