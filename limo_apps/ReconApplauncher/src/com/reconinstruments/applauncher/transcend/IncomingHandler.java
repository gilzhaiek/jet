//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.reconinstruments.applauncher.R;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.hud_phone_status_exchange.HudPhoneStatusExchanger;
import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MessagePriority;
import com.reconinstruments.messagecenter.ReconMessageAPI.ReconNotification;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * Class to handle incoming requests from the Transcend service
 * clients.
 *
 */
public class IncomingHandler extends Handler {
    //Interface to remote stuff
    public static final int MSG_RESULT = 1;
    public static final int MSG_RESULT_CHRONO = 2;
    public static final int MSG_GET_ALTITUDE_BUNDLE = 3;
    public static final int MSG_GET_DISTANCE_BUNDLE = 4;
    public static final int MSG_GET_JUMP_BUNDLE = 5;
    public static final int MSG_GET_LOCATION_BUNDLE = 6; 
    public static final int MSG_GET_RUN_BUNDLE = 7;
    public static final int MSG_GET_SPEED_BUNDLE = 8;
    public static final int MSG_GET_TEMPERATURE_BUNDLE = 9;
    public static final int MSG_GET_TIME_BUNDLE = 10;
    public static final int MSG_GET_VERTICAL_BUNDLE = 11;
    public static final int MSG_GET_CHRONO_BUNDLE = 12;
    public static final int MSG_GET_FULL_INFO_BUNDLE = 13;
    public static final int MSG_CHRONO_START_STOP = 14;
    public static final int MSG_CHRONO_LAP_TRIAL = 15;
    public static final int MSG_CHRONO_START_NEW_TRIAL = 16;
    public static final int MSG_CHRONO_STOP_TRIAL = 17;
    public static final int MSG_RESET_STATS = 18;
    public static final int MSG_RESET_ALLTIME_STATS = 19;
    // Stuff pertaining to starting and starting sports activites
    public static final int MSG_START_SPORTS_ACTIVITY = 20;
    public static final int MSG_PAUSE_SPORTS_ACTIVITY = 21;
    public static final int MSG_RESTART_SPORTS_ACTIVITY = 22;
    public static final int MSG_STOP_SPORTS_ACTIVITY = 23;
    public static final int MSG_DISCARD_SPORTS_ACTIVITY = 24;
    public static final int MSG_SAVE_SPORTS_ACTIVITY = 25;
    public static final int MSG_SET_SPORTS_ACTIVITY = 26;
    private static String TAG  = "TranscendServiceIncomingHandler";
    
    ReconTranscendService mRTS;
    public IncomingHandler(ReconTranscendService rts) {
        Log.v(TAG,"constructor");
        mRTS = rts;
    }
    @Override
    public void handleMessage(Message msg) {
        if (mRTS.mIgnoreIncomingMessages) { //Used when we are shutting down 
            return;
        }
        switch (msg.what) {
        case MSG_GET_ALTITUDE_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mAltMan.getBundle());
                Log.d(TAG,"Requested Alt Bundle");
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_DISTANCE_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mDistMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_JUMP_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mJumpMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;

        case MSG_GET_LOCATION_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mRecLocMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }

        case MSG_GET_RUN_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mRunMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_SPEED_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mSpeedMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_TEMPERATURE_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mTempMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_TIME_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mTimeMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_VERTICAL_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mVertMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_CHRONO_BUNDLE:
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                lamsg.setData(mRTS.mChronoMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_GET_FULL_INFO_BUNDLE:
            try{
                Message lamsg = Message.obtain(null,MSG_RESULT,msg.what,0);
                //mFullInfo = generateFullInfoBundle();
                lamsg.setData(mRTS.mFullInfo);
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch(RemoteException e) {

            }
            break;
        case MSG_CHRONO_START_STOP:
            mRTS.mChronoMan.startStop();
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT_CHRONO,msg.what,0);
                lamsg.setData(mRTS.mChronoMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_CHRONO_LAP_TRIAL:
            mRTS.mChronoMan.lapTrial();
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT_CHRONO,msg.what,0);
                lamsg.setData(mRTS.mChronoMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_CHRONO_START_NEW_TRIAL:
            mRTS.mChronoMan.newTrial();
            mRTS.mChronoMan.start();
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT_CHRONO,msg.what,0);
                lamsg.setData(mRTS.mChronoMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_CHRONO_STOP_TRIAL:
            mRTS.mChronoMan.stop();
            try {
                Message lamsg = Message.obtain(null,MSG_RESULT_CHRONO,msg.what,0);
                lamsg.setData(mRTS.mChronoMan.getBundle());
                msg.replyTo.send(lamsg);
                if( ReconTranscendService.DEBUG_USING_TOAST )
                    Toast.makeText(mRTS.getApplicationContext(),
                                   msg.replyTo.toString(), 1000).show();
            } catch (RemoteException e) {
                // The client is dead.
            }
            break;
        case MSG_RESET_STATS:
            mRTS.resetStats();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                {Toast.makeText(mRTS.getApplicationContext(),
                                msg.replyTo.toString(), 1000).show();}
            break;
        case MSG_RESET_ALLTIME_STATS:
            mRTS.resetAllTimeStats();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                {Toast.makeText(mRTS.getApplicationContext(),
                                msg.replyTo.toString(), 1000).show();}
            break;
        case MSG_START_SPORTS_ACTIVITY:
            mRTS.getSportsActivityManager().startSportsActivity();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                Toast.makeText(mRTS.getApplicationContext(),
                               "started Activity", 1000).show();
            break;
        case MSG_PAUSE_SPORTS_ACTIVITY:
            mRTS.getSportsActivityManager().pauseSportsActivity();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                Toast.makeText(mRTS.getApplicationContext(),
                                "paused Activity", 1000).show();
            break;
        case MSG_RESTART_SPORTS_ACTIVITY:
            mRTS.getSportsActivityManager().resumeSportsActivity();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                Toast.makeText(mRTS.getApplicationContext(),
                               "resumed Activity", 1000).show();
            break;
        case MSG_STOP_SPORTS_ACTIVITY:
            mRTS.getSportsActivityManager().finishSportsActivity();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                Toast.makeText(mRTS.getApplicationContext(),
                               "finished Activity", 1000).show();
            break;
        case MSG_DISCARD_SPORTS_ACTIVITY:
            mRTS.getSportsActivityManager().discardSportsActivity();
            if( ReconTranscendService.DEBUG_USING_TOAST )
                Toast.makeText(mRTS.getApplicationContext(),
                               "discarded Activity", 1000).show();
            break;
        case MSG_SAVE_SPORTS_ACTIVITY:
            mRTS.getSportsActivityManager().saveSportsActivity();
            break;
        case MSG_SET_SPORTS_ACTIVITY:
            int type = msg.arg1;
            mRTS.getSportsActivityManager().setType(type);
            mRTS.reInitializeForNewSports(type, false);
            if( ReconTranscendService.DEBUG_USING_TOAST ) {
                Toast.makeText(mRTS.getApplicationContext(),
                               "Sports type set to "+type, 1000).show();}
            break;
        default:
            super.handleMessage(msg);
            break;
        }
    }
}