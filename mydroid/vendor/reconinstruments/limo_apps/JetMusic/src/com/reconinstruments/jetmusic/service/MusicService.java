package com.reconinstruments.jetmusic.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.reconinstruments.utils.BTHelper;
import com.stonestreetone.bluetopiapm.AVRCP.ElementAttributeID;
import com.stonestreetone.bluetopiapm.AVRCP.EventID;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlButtonState;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlPassThroughOperationID;

import com.reconinstruments.connect.messages.MusicMessage;
import com.reconinstruments.connect.messages.MusicMessage.PlayerInfo;
import com.reconinstruments.connect.messages.MusicMessage.PlayerState;
import com.reconinstruments.connect.messages.MusicMessage.SongInfo;
import com.reconinstruments.connect.messages.MusicMessage.Type;
import com.reconinstruments.connect.messages.SongMessage;
import com.reconinstruments.connect.messages.XMLMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.utils.BTHelper;

import java.util.EnumSet;

public class MusicService extends Service{
    private static final String TAG = MusicService.class.getSimpleName();
    public static final String ACTION_MUSIC_SERVICE = "RECON_MUSIC_SERVICE";
    public static final String EXTRA_MUSIC_COMMAND = "command";
    public static final int MUSIC_COMMAND_CONNECT = 0;
    public static final int MUSIC_COMMAND_DISCONNECT = 1;
    public static final int MUSIC_COMMAND_PLAY_OR_PAUSE = 2;
    public static final int MUSIC_COMMAND_PREVIOUS = 3;
    public static final int MUSIC_COMMAND_NEXT = 4;
    public static final int MUSIC_COMMAND_VOLUP = 5;
    public static final int MUSIC_COMMAND_VOLDOWN = 6;
    
    private AVRCPManager mAVRCPManager;
    private BTHelper mBTHelper;
    
    //handle the incoming commands
    private Handler mHandler = new Handler();
    //record the last command execution time stamp to determine if it's healthy or not
    //if the time period exceeds 2 seconds, the connection should be reset
    private long lastCommandTimeStamp = System.currentTimeMillis();
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        mAVRCPManager = AVRCPManager.getInstance(getApplicationContext());
        mBTHelper = BTHelper.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.d(TAG, "onStartCommand");
        if (intent == null || intent.getExtras() == null) { 
            return START_STICKY;
        }
        Bundle b = intent.getExtras();
        if (intent != null && b != null && mBTHelper.getBTConnectionState() == 2) {
            int command = b.getInt(EXTRA_MUSIC_COMMAND, -1);
            int delay = 0;
            
            // if processing, post command 1 second delay and ignore the previous commands
            if(mAVRCPManager.isProcessing()){
                //if the time period exceeds 2 seconds, the connection should be reset
                if((System.currentTimeMillis() - lastCommandTimeStamp) > 2000){
                    Log.w(TAG, "can't get response within 2 seconds, reset the connection");
                    mAVRCPManager.reset();
                }
                Log.i(TAG, "wait for 1 sencond to send the command and ignore previous one");
                delay = 1000;
                mHandler.removeCallbacksAndMessages(null);
            }else{
                lastCommandTimeStamp = System.currentTimeMillis();
            }
            
            switch(command){
                case MUSIC_COMMAND_CONNECT:
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            mAVRCPManager.connectRemoteControl();
                        }
                    }, delay);
                    break;
                case MUSIC_COMMAND_DISCONNECT:
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            mAVRCPManager.disconnectRemoteControl();
                        }
                    }, delay);
                    break;
                case MUSIC_COMMAND_PLAY_OR_PAUSE:
                    if(BTHelper.getInstance(this).getLastPairedDeviceType() == 0){ //android device
                        androidTogglePlayMusic();
                    }else{
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                mAVRCPManager.playOrPause(RemoteControlButtonState.PRESSED, null);
                            }
                        }, delay);
                    }
                    break;
                case MUSIC_COMMAND_PREVIOUS:
                    if(BTHelper.getInstance(this).getLastPairedDeviceType() == 0){ //android device
                        navSong(MUSIC_COMMAND_PREVIOUS);
                    }else{
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                mAVRCPManager.forwardOrBackward(RemoteControlPassThroughOperationID.BACKWARD, RemoteControlButtonState.PRESSED, null);
                            }
                        }, delay);
                    }
                    break;
                case MUSIC_COMMAND_NEXT:
                    if(BTHelper.getInstance(this).getLastPairedDeviceType() == 0){ //android device
                        navSong(MUSIC_COMMAND_NEXT);
                    }else{
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                mAVRCPManager.forwardOrBackward(RemoteControlPassThroughOperationID.FORWARD, RemoteControlButtonState.PRESSED, null);
                            }
                        }, delay);
                    }
                    break;
                case MUSIC_COMMAND_VOLUP:
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            volControl(MUSIC_COMMAND_VOLUP);
                        }
                    }, delay);
                    break;
                case MUSIC_COMMAND_VOLDOWN:
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            volControl(MUSIC_COMMAND_VOLDOWN);
                        }

                    }, delay);
                    break;
                default:
                    break;
                    
            }
        }
        return START_STICKY;
    }
    
    private void androidTogglePlayMusic() {
        MusicMessage message = new MusicMessage(new PlayerInfo(null,null,null,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.TOGGLE_PAUSE);
        sendMusicMessage(message);
    }
    
    private void volControl(int dir) {
        MusicMessage message = null;
        if(dir == MUSIC_COMMAND_VOLUP){
            message = new MusicMessage(new PlayerInfo(null,null,0.5f,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.VOLUME_UP);
        }else{
            message = new MusicMessage(new PlayerInfo(null,null,0.5f,null,null,null,null),MusicMessage.Type.CONTROL,MusicMessage.Action.VOLUME_DOWN);
        }
        sendMusicMessage(message);
    }
    
    private void navSong(int dir) {
        MusicMessage message = null;
        if(dir == MUSIC_COMMAND_NEXT){
            message = new MusicMessage(MusicMessage.Action.NEXT_SONG);
        }else if(dir == MUSIC_COMMAND_PREVIOUS){
            message = new MusicMessage(MusicMessage.Action.PREVIOUS_SONG);
        }else{
            return;
        }
        sendMusicMessage(message);
    }
    
    private void sendMusicMessage(MusicMessage message){
        Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.object");
        
        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
        cMsg.setIntentFilter(XMLMessage.MUSIC_MESSAGE);
        cMsg.setRequestKey(0);
        cMsg.setSender("com.reconinstruments.mobilesdk.mediaplayer.MusicHelper");
        byte[] data = message.toXML().getBytes();
        cMsg.setData(data);
        //Log.e(TAG,"size of music status message: "+cMsg.ToByteArray().length);
        i.putExtra(HUDConnectivityMessage.TAG,cMsg.toByteArray());
        sendBroadcast(i);
    }
}
