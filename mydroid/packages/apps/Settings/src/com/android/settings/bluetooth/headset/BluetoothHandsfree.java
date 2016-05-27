/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth.headset;

import android.bluetooth.AtCommandHandler;
import android.bluetooth.AtCommandResult;
import android.bluetooth.AtParser;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.HeadsetBase;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.util.Log;

/**
 * Bluetooth headset manager for the Phone app.
 * @hide
 */
public class BluetoothHandsfree {
    private static final String TAG = "Bluetooth HS/HF";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    public static final int TYPE_UNKNOWN           = 0;
    public static final int TYPE_HEADSET           = 1;
    public static final int TYPE_HANDSFREE         = 2;

    /** The singleton instance. */
    private static BluetoothHandsfree sInstance;

    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private BluetoothA2dp mA2dp;

    private BluetoothDevice mA2dpDevice;
    private int mA2dpState;
    private boolean mPendingAudioState;
    private int mAudioState;

    private ServiceState mServiceState;
    private HeadsetBase mHeadset;
    private BluetoothHeadset mBluetoothHeadset;
    private int mHeadsetType;   // TYPE_UNKNOWN when not connected
    private boolean mAudioPossible;

    private AudioManager mAudioManager;
    private PowerManager mPowerManager;

    private boolean mA2dpSuspended;
    private boolean mUserWantsAudio;
//    private WakeLock mStartVoiceRecognitionWakeLock;  // held while waiting for voice recognition

    private long mBgndEarliestConnectionTime = 0;
    private boolean mIndicatorsEnabled = false;
    private boolean mCmee = false;  // Extended Error reporting
    
    // do not connect audio until service connection is established
    // for 3-way supported devices, this is after AT+CHLD
    // for non-3-way supported devices, this is after AT+CMER (see spec)
    private boolean mServiceConnectionEstablished;
    
    private DebugThread mDebugThread;

    // Audio parameters
    private static final String HEADSET_NREC = "bt_headset_nrec";
    private static final String HEADSET_NAME = "bt_headset_name";

    private int mRemoteBrsf = 0;
    private int mLocalBrsf = 0;

    /* Constants from Bluetooth Specification Hands-Free profile version 1.5 */
    private static final int BRSF_AG_THREE_WAY_CALLING = 1 << 0;
    private static final int BRSF_AG_EC_NR = 1 << 1;
    private static final int BRSF_AG_VOICE_RECOG = 1 << 2;
    private static final int BRSF_AG_IN_BAND_RING = 1 << 3;
    private static final int BRSF_AG_VOICE_TAG_NUMBE = 1 << 4;
    private static final int BRSF_AG_REJECT_CALL = 1 << 5;
    private static final int BRSF_AG_ENHANCED_CALL_STATUS = 1 <<  6;
    private static final int BRSF_AG_ENHANCED_CALL_CONTROL = 1 << 7;
    private static final int BRSF_AG_ENHANCED_ERR_RESULT_CODES = 1 << 8;

    private static final int BRSF_HF_EC_NR = 1 << 0;
    private static final int BRSF_HF_CW_THREE_WAY_CALLING = 1 << 1;
    private static final int BRSF_HF_CLIP = 1 << 2;
    private static final int BRSF_HF_VOICE_REG_ACT = 1 << 3;
    private static final int BRSF_HF_REMOTE_VOL_CONTROL = 1 << 4;
    private static final int BRSF_HF_ENHANCED_CALL_STATUS = 1 <<  5;
    private static final int BRSF_HF_ENHANCED_CALL_CONTROL = 1 << 6;

    public static String typeToString(int type) {
        switch (type) {
        case TYPE_UNKNOWN:
            return "unknown";
        case TYPE_HEADSET:
            return "headset";
        case TYPE_HANDSFREE:
            return "handsfree";
        }
        return null;
    }

    /**
     * Initialize the singleton BluetoothHandsfree instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     */
    /* package */ static BluetoothHandsfree init(Context context) {
        synchronized (BluetoothHandsfree.class) {
            if (sInstance == null) {
                sInstance = new BluetoothHandsfree(context);
            } else {
                Log.wtf(TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private BluetoothHandsfree(Context context) {
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean bluetoothCapable = (mAdapter != null);
        mHeadset = null;
        mHeadsetType = TYPE_UNKNOWN; // nothing connected yet
        if (bluetoothCapable) {
            mAdapter.getProfileProxy(mContext, mProfileListener,
                                     BluetoothProfile.A2DP);
        }
        mA2dpState = BluetoothA2dp.STATE_DISCONNECTED;
        mA2dpDevice = null;
        mA2dpSuspended = false;

        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        
//        mStartVoiceRecognitionWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                                                       TAG + ":VoiceRecognition");
//        mStartVoiceRecognitionWakeLock.setReferenceCounted(false);

        mLocalBrsf = BRSF_AG_THREE_WAY_CALLING |
                     BRSF_AG_EC_NR |
                     BRSF_AG_REJECT_CALL |
                     BRSF_AG_ENHANCED_CALL_STATUS;

        HandlerThread thread = new HandlerThread("BluetoothHandsfreeHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mUserWantsAudio = true;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (bluetoothCapable) {
            resetAtState();
        }

    }  

    /* package */ synchronized void onBluetoothDisabled() {
        // Used to Close off the SCO sockets
        audioOff();
    }

    private boolean isHeadsetConnected() {
        if (mHeadset == null || mHeadsetType == TYPE_UNKNOWN) {
            return false;
        }
        return mHeadset.isConnected();
    }

    /* package */ synchronized void connectHeadset(HeadsetBase headset, int headsetType) {
        mHeadset = headset;
        mHeadsetType = headsetType;
        if (mHeadsetType == TYPE_HEADSET) {
            initializeHeadsetAtParser();
        } else {
            //initializeHandsfreeAtParser();
        }

        // Headset vendor-specific commands
        registerAllVendorSpecificCommands();

        headset.startEventThread();
        configAudioParameters();

        if (inDebug()) {
            startDebug();
        }
    }

    /* package */ synchronized void disconnectHeadset() {
        audioOff();

        mHeadsetType = TYPE_UNKNOWN;
        stopDebug();
        resetAtState();
    }

    /* package */ synchronized void resetAtState() {
        mIndicatorsEnabled = false;
        mServiceConnectionEstablished = false;
        mCmee = false;
        mRemoteBrsf = 0;
    }

    /* package */ HeadsetBase getHeadset() {
        return mHeadset;
    }

    private void configAudioParameters() {
        String name = mHeadset.getRemoteDevice().getName();
        if (name == null) {
            name = "<unknown>";
        }
        mAudioManager.setParameters(HEADSET_NAME+"="+name+";"+HEADSET_NREC+"=on");
    }

    private synchronized void setAudioState(int state, BluetoothDevice device) {
        if (VDBG) log("setAudioState(" + state + ")");
        if (mBluetoothHeadset == null) {
            mAdapter.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET);
            mPendingAudioState = true;
            mAudioState = state;
            return;
        }
        mBluetoothHeadset.setAudioState(device, state);
    }

    private synchronized int getAudioState(BluetoothDevice device) {
        if (mBluetoothHeadset == null) return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
        return mBluetoothHeadset.getAudioState(device);
    }

    private BluetoothProfile.ServiceListener mProfileListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
                synchronized(BluetoothHandsfree.this) {
                    if (mPendingAudioState) {
                        mBluetoothHeadset.setAudioState(mHeadset.getRemoteDevice(), mAudioState);
                        mPendingAudioState = false;
                    }
                }
            } else if (profile == BluetoothProfile.A2DP) {
                mA2dp = (BluetoothA2dp) proxy;
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            } else if (profile == BluetoothProfile.A2DP) {
                mA2dp = null;
            }
        }
    };

    /*
     * Put the AT command, company ID, arguments, and device in an Intent and broadcast it.
     */
    private void broadcastVendorSpecificEventIntent(String command,
                                                    int companyId,
                                                    int commandType,
                                                    Object[] arguments,
                                                    BluetoothDevice device) {
        if (VDBG) log("broadcastVendorSpecificEventIntent(" + command + ")");
        Intent intent =
                new Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, command);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,
                        commandType);
        // assert: all elements of args are Serializable
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arguments);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);

        intent.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
            + "." + Integer.toString(companyId));

        mContext.sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
    }

    /** Request to establish SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     * Returns false if the user has requested audio off, or if there
     * is some other immediate problem that will prevent BT audio.
     */
    /* package */ synchronized boolean audioOn() {
        if (VDBG) log("audioOn()");
        if (!isHeadsetConnected()) {
            if (DBG) log("audioOn(): headset is not connected!");
            return false;
        }
        if (mHeadsetType == TYPE_HANDSFREE && !mServiceConnectionEstablished) {
            if (DBG) log("audioOn(): service connection not yet established!");
            return false;
        }

        if (!mUserWantsAudio) {
            if (DBG) log("audioOn(): user requested no audio, ignoring");
            return false;
        }

        mA2dpSuspended = false;
        if (isA2dpMultiProfile() && mA2dpState == BluetoothA2dp.STATE_PLAYING) {
            if (DBG) log("suspending A2DP stream for SCO");
            mA2dpSuspended = mA2dp.suspendSink(mA2dpDevice);
            if (!mA2dpSuspended){
                Log.w(TAG, "Could not suspend A2DP stream for SCO, going ahead with SCO");
            }
        }

        return true;
    }

    /** Used to indicate the user requested BT audio on.
     *  This will establish SCO (BT audio), even if the user requested it off
     *  previously on this call.
     */
    /* package */ synchronized void userWantsAudioOn() {
        mUserWantsAudio = true;
        audioOn();
    }
    /** Used to indicate the user requested BT audio off.
     *  This will prevent us from establishing BT audio again during this call
     *  if audioOn() is called.
     */
    /* package */ synchronized void userWantsAudioOff() {
        mUserWantsAudio = false;
        audioOff();
    }

    /** Request to disconnect SCO (audio) connection to bluetooth
     * headset/handsfree, if one is connected. Does not block.
     */
    /* package */ synchronized void audioOff() {
        if (VDBG) log("mA2dpState: " + mA2dpState +
                ", mA2dpSuspended: " + mA2dpSuspended);

        if (mA2dpSuspended) {
            if (isA2dpMultiProfile()) {
                if (DBG) log("resuming A2DP stream after disconnecting SCO");
                mA2dp.resumeSink(mA2dpDevice);
            }
            mA2dpSuspended = false;
        }
    }

    private boolean isA2dpMultiProfile() {
        return mA2dp != null && mHeadset != null && mA2dpDevice != null &&
                mA2dpDevice.equals(mHeadset.getRemoteDevice());
    }

    private void sendURC(String urc) {
        if (isHeadsetConnected()) {
            mHeadset.sendURC(urc);
        }
    }

    /*
     * Register a vendor-specific command.
     * @param commandName the name of the command.  For example, if the expected
     * incoming command is <code>AT+FOO=bar,baz</code>, the value of this should be
     * <code>"+FOO"</code>.
     * @param companyId the Bluetooth SIG Company Identifier
     * @param parser the AtParser on which to register the command
     */
    private void registerVendorSpecificCommand(String commandName,
                                               int companyId,
                                               AtParser parser) {
        parser.register(commandName,
                        new VendorSpecificCommandHandler(commandName, companyId));
    }

    /*
     * Register all vendor-specific commands here.
     */
    private void registerAllVendorSpecificCommands() {
        AtParser parser = mHeadset.getAtParser();

        // Plantronics-specific headset events go here
        registerVendorSpecificCommand("+XEVENT",
                                      BluetoothAssignedNumbers.PLANTRONICS,
                                      parser);
    }

    /**
     * Register AT Command handlers to implement the Headset profile
     */
    private void initializeHeadsetAtParser() {
        if (VDBG) log("Registering Headset AT commands");
        AtParser parser = mHeadset.getAtParser();
        // Headsets usually only have one button, which is meant to cause the
        // HS to send us AT+CKPD=200 or AT+CKPD.
        parser.register("+CKPD", new AtCommandHandler() {
            private AtCommandResult headsetButtonPress() {
				return null;
            }
            @Override
            public AtCommandResult handleActionCommand() {
                return headsetButtonPress();
            }
            @Override
            public AtCommandResult handleSetCommand(Object[] args) {
                return headsetButtonPress();
            }
        });
    }

    /**
     * Register AT Command handlers to implement the Handsfree profile
     */
//    private void initializeHandsfreeAtParser() {
//        if (VDBG) log("Registering Handsfree AT commands");
//        AtParser parser = mHeadset.getAtParser();
//
//        // Answer
//        parser.register('A', new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleBasicCommand(String args) {
//                sendURC("OK");
//                return new AtCommandResult(AtCommandResult.UNSOLICITED);
//            }
//        });
//        parser.register('D', new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleBasicCommand(String args) {
//                if (args.length() > 0) {
//                    if (args.charAt(0) == '>') {
//                        // Yuck - memory dialling requested.
//                        // Just dial last number for now
//                        if (args.startsWith(">9999")) {   // for PTS test
//                            return new AtCommandResult(AtCommandResult.ERROR);
//                        }
//                        return; //redial();
//                    } else {
//                        // Remove trailing ';'
//                        if (args.charAt(args.length() - 1) == ';') {
//                            args = args.substring(0, args.length() - 1);
//                        }
//
//                        args = PhoneNumberUtils.convertPreDial(args);
//
//                        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
//                                Uri.fromParts(Constants.SCHEME_TEL, args, null));
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        mContext.startActivity(intent);
//
//                        expectCallStart();
//                        return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing
//                    }
//                }
//                return new AtCommandResult(AtCommandResult.ERROR);
//            }
//        });
//
//        // Hang-up command
//        parser.register("+CHUP", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                sendURC("OK");
//                if (isVirtualCallInProgress()) {
//                    terminateScoUsingVirtualVoiceCall();
//                } else {
//                    if (mCM.hasActiveFgCall()) {
//                        PhoneUtils.hangupActiveCall(mCM.getActiveFgCall());
//                    } else if (mCM.hasActiveRingingCall()) {
//                        PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
//                    } else if (mCM.hasActiveBgCall()) {
//                        PhoneUtils.hangupHoldingCall(mCM.getFirstActiveBgCall());
//                    }
//                }
//                return new AtCommandResult(AtCommandResult.UNSOLICITED);
//            }
//        });
//
//        // Bluetooth Retrieve Supported Features command
//        parser.register("+BRSF", new AtCommandHandler() {
//            private AtCommandResult sendBRSF() {
//                return new AtCommandResult("+BRSF: " + mLocalBrsf);
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // AT+BRSF=<handsfree supported features bitmap>
//                // Handsfree is telling us which features it supports. We
//                // send the features we support
//                if (args.length == 1 && (args[0] instanceof Integer)) {
//                    mRemoteBrsf = (Integer) args[0];
//                } else {
//                    Log.w(TAG, "HF didn't sent BRSF assuming 0");
//                }
//                return sendBRSF();
//            }
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // This seems to be out of spec, but lets do the nice thing
//                return sendBRSF();
//            }
//            @Override
//            public AtCommandResult handleReadCommand() {
//                // This seems to be out of spec, but lets do the nice thing
//                return sendBRSF();
//            }
//        });
//
//        // Call waiting notification on/off
//        parser.register("+CCWA", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // Seems to be out of spec, but lets return nicely
//                return new AtCommandResult(AtCommandResult.OK);
//            }
//            @Override
//            public AtCommandResult handleReadCommand() {
//                // Call waiting is always on
//                return new AtCommandResult("+CCWA: 1");
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // AT+CCWA=<n>
//                // Handsfree is trying to enable/disable call waiting. We
//                // cannot disable in the current implementation.
//                return new AtCommandResult(AtCommandResult.OK);
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                // Request for range of supported CCWA paramters
//                return new AtCommandResult("+CCWA: (\"n\",(1))");
//            }
//        });
//
//        // Mobile Equipment Event Reporting enable/disable command
//        // Of the full 3GPP syntax paramters (mode, keyp, disp, ind, bfr) we
//        // only support paramter ind (disable/enable evert reporting using
//        // +CDEV)
//        parser.register("+CMER", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                return new AtCommandResult(
//                        "+CMER: 3,0,0," + (mIndicatorsEnabled ? "1" : "0"));
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                if (args.length < 4) {
//                    // This is a syntax error
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                } else if (args[0].equals(3) && args[1].equals(0) &&
//                           args[2].equals(0)) {
//                    boolean valid = false;
//                    if (args[3].equals(0)) {
//                        mIndicatorsEnabled = false;
//                        valid = true;
//                    } else if (args[3].equals(1)) {
//                        mIndicatorsEnabled = true;
//                        valid = true;
//                    }
//                    if (valid) {
//                        if ((mRemoteBrsf & BRSF_HF_CW_THREE_WAY_CALLING) == 0x0) {
//                            mServiceConnectionEstablished = true;
//                            sendURC("OK");  // send immediately, then initiate audio
//                            if (isIncallAudio()) {
//                                audioOn();
//                            } else if (mCM.getFirstActiveRingingCall().isRinging()) {
//                                // need to update HS with RING cmd when single
//                                // ringing call exist
//                                mBluetoothPhoneState.ring();
//                            }
//                            // only send OK once
//                            return new AtCommandResult(AtCommandResult.UNSOLICITED);
//                        } else {
//                            return new AtCommandResult(AtCommandResult.OK);
//                        }
//                    }
//                }
//                return reportCmeError(BluetoothCmeError.OPERATION_NOT_SUPPORTED);
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                return new AtCommandResult("+CMER: (3),(0),(0),(0-1)");
//            }
//        });
//
//        // Mobile Equipment Error Reporting enable/disable
//        parser.register("+CMEE", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // out of spec, assume they want to enable
//                mCmee = true;
//                return new AtCommandResult(AtCommandResult.OK);
//            }
//            @Override
//            public AtCommandResult handleReadCommand() {
//                return new AtCommandResult("+CMEE: " + (mCmee ? "1" : "0"));
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // AT+CMEE=<n>
//                if (args.length == 0) {
//                    // <n> ommitted - default to 0
//                    mCmee = false;
//                    return new AtCommandResult(AtCommandResult.OK);
//                } else if (!(args[0] instanceof Integer)) {
//                    // Syntax error
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                } else {
//                    mCmee = ((Integer)args[0] == 1);
//                    return new AtCommandResult(AtCommandResult.OK);
//                }
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                // Probably not required but spec, but no harm done
//                return new AtCommandResult("+CMEE: (0-1)");
//            }
//        });
//
//        // Bluetooth Last Dialled Number
//        parser.register("+BLDN", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                return redial();
//            }
//        });
//
//        // Indicator Update command
//        parser.register("+CIND", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                return mBluetoothPhoneState.toCindResult();
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                return mBluetoothPhoneState.getCindTestResult();
//            }
//        });
//
//        // Query Signal Quality (legacy)
//        parser.register("+CSQ", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                return mBluetoothPhoneState.toCsqResult();
//            }
//        });
//
//        // Query network registration state
//        parser.register("+CREG", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                return new AtCommandResult(mBluetoothPhoneState.toCregString());
//            }
//        });
//
//        // Send DTMF. I don't know if we are also expected to play the DTMF tone
//        // locally, right now we don't
//        parser.register("+VTS", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                if (args.length >= 1) {
//                    char c;
//                    if (args[0] instanceof Integer) {
//                        c = ((Integer) args[0]).toString().charAt(0);
//                    } else {
//                        c = ((String) args[0]).charAt(0);
//                    }
//                    if (isValidDtmf(c)) {
//                        phone.sendDtmf(c);
//                        return new AtCommandResult(AtCommandResult.OK);
//                    }
//                }
//                return new AtCommandResult(AtCommandResult.ERROR);
//            }
//            private boolean isValidDtmf(char c) {
//                switch (c) {
//                case '#':
//                case '*':
//                    return true;
//                default:
//                    if (Character.digit(c, 14) != -1) {
//                        return true;  // 0-9 and A-D
//                    }
//                    return false;
//                }
//            }
//        });
//
//        // List calls
//        parser.register("+CLCC", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                int phoneType = phone.getPhoneType();
//                // Handsfree carkits expect that +CLCC is properly responded to.
//                // Hence we ensure that a proper response is sent for the virtual call too.
//                if (isVirtualCallInProgress()) {
//                    String number = phone.getLine1Number();
//                    AtCommandResult result = new AtCommandResult(AtCommandResult.OK);
//                    String args;
//                    if (number == null) {
//                        args = "+CLCC: 1,0,0,0,0,\"\",0";
//                    }
//                    else
//                    {
//                        args = "+CLCC: 1,0,0,0,0,\"" + number + "\"," +
//                                  PhoneNumberUtils.toaFromString(number);
//                    }
//                    result.addResponse(args);
//                    return result;
//                }
//                if (phoneType == Phone.PHONE_TYPE_CDMA) {
//                    return cdmaGetClccResult();
//                } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//                    return gsmGetClccResult();
//                } else {
//                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
//                }
//            }
//        });
//
//        // Call Hold and Multiparty Handling command
//        parser.register("+CHLD", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                int phoneType = phone.getPhoneType();
//                Call ringingCall = mCM.getFirstActiveRingingCall();
//                Call backgroundCall = mCM.getFirstActiveBgCall();
//
//                if (args.length >= 1) {
//                    if (args[0].equals(0)) {
//                        boolean result;
//                        if (ringingCall.isRinging()) {
//                            result = PhoneUtils.hangupRingingCall(ringingCall);
//                        } else {
//                            result = PhoneUtils.hangupHoldingCall(backgroundCall);
//                        }
//                        if (result) {
//                            return new AtCommandResult(AtCommandResult.OK);
//                        } else {
//                            return new AtCommandResult(AtCommandResult.ERROR);
//                        }
//                    } else if (args[0].equals(1)) {
//                        if (phoneType == Phone.PHONE_TYPE_CDMA) {
//                            if (ringingCall.isRinging()) {
//                                // Hangup the active call and then answer call waiting call.
//                                if (VDBG) log("CHLD:1 Callwaiting Answer call");
//                                PhoneUtils.hangupRingingAndActive(phone);
//                            } else {
//                                // If there is no Call waiting then just hangup
//                                // the active call. In CDMA this mean that the complete
//                                // call session would be ended
//                                if (VDBG) log("CHLD:1 Hangup Call");
//                                PhoneUtils.hangup(PhoneApp.getInstance().mCM);
//                            }
//                            return new AtCommandResult(AtCommandResult.OK);
//                        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//                            // Hangup active call, answer held call
//                            if (PhoneUtils.answerAndEndActive(
//                                    PhoneApp.getInstance().mCM, ringingCall)) {
//                                return new AtCommandResult(AtCommandResult.OK);
//                            } else {
//                                return new AtCommandResult(AtCommandResult.ERROR);
//                            }
//                        } else {
//                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
//                        }
//                    } else if (args[0].equals(2)) {
//                        sendURC("OK");
//                        if (phoneType == Phone.PHONE_TYPE_CDMA) {
//                            // For CDMA, the way we switch to a new incoming call is by
//                            // calling PhoneUtils.answerCall(). switchAndHoldActive() won't
//                            // properly update the call state within telephony.
//                            // If the Phone state is already in CONF_CALL then we simply send
//                            // a flash cmd by calling switchHoldingAndActive()
//                            if (ringingCall.isRinging()) {
//                                if (VDBG) log("CHLD:2 Callwaiting Answer call");
//                                PhoneUtils.answerCall(ringingCall);
//                                PhoneUtils.setMute(false);
//                                // Setting the second callers state flag to TRUE (i.e. active)
//                                cdmaSetSecondCallState(true);
//                            } else if (PhoneApp.getInstance().cdmaPhoneCallState
//                                    .getCurrentCallState()
//                                    == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
//                                if (VDBG) log("CHLD:2 Swap Calls");
//                                PhoneUtils.switchHoldingAndActive(backgroundCall);
//                                // Toggle the second callers active state flag
//                                cdmaSwapSecondCallState();
//                            }
//                        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//                            PhoneUtils.switchHoldingAndActive(backgroundCall);
//                        } else {
//                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
//                        }
//                        return new AtCommandResult(AtCommandResult.UNSOLICITED);
//                    } else if (args[0].equals(3)) {
//                        sendURC("OK");
//                        if (phoneType == Phone.PHONE_TYPE_CDMA) {
//                            CdmaPhoneCallState.PhoneCallState state =
//                                PhoneApp.getInstance().cdmaPhoneCallState.getCurrentCallState();
//                            // For CDMA, we need to check if the call is in THRWAY_ACTIVE state
//                            if (state == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
//                                if (VDBG) log("CHLD:3 Merge Calls");
//                                PhoneUtils.mergeCalls();
//                            } else if (state == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
//                                // State is CONF_CALL already and we are getting a merge call
//                                // This can happen when CONF_CALL was entered from a Call Waiting
//                                mBluetoothPhoneState.updateCallHeld();
//                            }
//                        } else if (phoneType == Phone.PHONE_TYPE_GSM) {
//                            if (mCM.hasActiveFgCall() && mCM.hasActiveBgCall()) {
//                                PhoneUtils.mergeCalls();
//                            }
//                        } else {
//                            throw new IllegalStateException("Unexpected phone type: " + phoneType);
//                        }
//                        return new AtCommandResult(AtCommandResult.UNSOLICITED);
//                    }
//                }
//                return new AtCommandResult(AtCommandResult.ERROR);
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                mServiceConnectionEstablished = true;
//                sendURC("+CHLD: (0,1,2,3)");
//                sendURC("OK");  // send reply first, then connect audio
//                if (isIncallAudio()) {
//                    audioOn();
//                } else if (mCM.getFirstActiveRingingCall().isRinging()) {
//                    // need to update HS with RING when single ringing call exist
//                    mBluetoothPhoneState.ring();
//                }
//                // already replied
//                return new AtCommandResult(AtCommandResult.UNSOLICITED);
//            }
//        });
//
//        // Get Network operator name
//        parser.register("+COPS", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                String operatorName = phone.getServiceState().getOperatorAlphaLong();
//                if (operatorName != null) {
//                    if (operatorName.length() > 16) {
//                        operatorName = operatorName.substring(0, 16);
//                    }
//                    return new AtCommandResult(
//                            "+COPS: 0,0,\"" + operatorName + "\"");
//                } else {
//                    return new AtCommandResult(
//                            "+COPS: 0");
//                }
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // Handsfree only supports AT+COPS=3,0
//                if (args.length != 2 || !(args[0] instanceof Integer)
//                    || !(args[1] instanceof Integer)) {
//                    // syntax error
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                } else if ((Integer)args[0] != 3 || (Integer)args[1] != 0) {
//                    return reportCmeError(BluetoothCmeError.OPERATION_NOT_SUPPORTED);
//                } else {
//                    return new AtCommandResult(AtCommandResult.OK);
//                }
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                // Out of spec, but lets be friendly
//                return new AtCommandResult("+COPS: (3),(0)");
//            }
//        });
//
//        // Mobile PIN
//        // AT+CPIN is not in the handsfree spec (although it is in 3GPP)
//        parser.register("+CPIN", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                return new AtCommandResult("+CPIN: READY");
//            }
//        });
//
//        // Bluetooth Response and Hold
//        // Only supported on PDC (Japan) and CDMA networks.
//        parser.register("+BTRH", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                // Replying with just OK indicates no response and hold
//                // features in use now
//                return new AtCommandResult(AtCommandResult.OK);
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // Neeed PDC or CDMA
//                return new AtCommandResult(AtCommandResult.ERROR);
//            }
//        });
//
//        // Request International Mobile Subscriber Identity (IMSI)
//        // Not in bluetooth handset spec
//        parser.register("+CIMI", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // AT+CIMI
//                String imsi = phone.getSubscriberId();
//                if (imsi == null || imsi.length() == 0) {
//                    return reportCmeError(BluetoothCmeError.SIM_FAILURE);
//                } else {
//                    return new AtCommandResult(imsi);
//                }
//            }
//        });
//
//        // Calling Line Identification Presentation
//        parser.register("+CLIP", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleReadCommand() {
//                // Currently assumes the network is provisioned for CLIP
//                return new AtCommandResult("+CLIP: " + (mClip ? "1" : "0") + ",1");
//            }
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // AT+CLIP=<n>
//                if (args.length >= 1 && (args[0].equals(0) || args[0].equals(1))) {
//                    mClip = args[0].equals(1);
//                    return new AtCommandResult(AtCommandResult.OK);
//                } else {
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                }
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                return new AtCommandResult("+CLIP: (0-1)");
//            }
//        });
//
//        // AT+CGSN - Returns the device IMEI number.
//        parser.register("+CGSN", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // Get the IMEI of the device.
//                // phone will not be NULL at this point.
//                return new AtCommandResult("+CGSN: " + phone.getDeviceId());
//            }
//        });
//
//        // AT+CGMM - Query Model Information
//        parser.register("+CGMM", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // Return the Model Information.
//                String model = SystemProperties.get("ro.product.model");
//                if (model != null) {
//                    return new AtCommandResult("+CGMM: " + model);
//                } else {
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                }
//            }
//        });
//
//        // AT+CGMI - Query Manufacturer Information
//        parser.register("+CGMI", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                // Return the Model Information.
//                String manuf = SystemProperties.get("ro.product.manufacturer");
//                if (manuf != null) {
//                    return new AtCommandResult("+CGMI: " + manuf);
//                } else {
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                }
//            }
//        });
//
//        // Noise Reduction and Echo Cancellation control
//        parser.register("+NREC", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                if (args[0].equals(0)) {
//                    mAudioManager.setParameters(HEADSET_NREC+"=off");
//                    return new AtCommandResult(AtCommandResult.OK);
//                } else if (args[0].equals(1)) {
//                    mAudioManager.setParameters(HEADSET_NREC+"=on");
//                    return new AtCommandResult(AtCommandResult.OK);
//                }
//                return new AtCommandResult(AtCommandResult.ERROR);
//            }
//        });
//
//        // Voice recognition (dialing)
//        parser.register("+BVRA", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                if (!BluetoothHeadset.isBluetoothVoiceDialingEnabled(mContext)) {
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                }
//                if (args.length >= 1 && args[0].equals(1)) {
//                    synchronized (BluetoothHandsfree.this) {
//                        if (!isVoiceRecognitionInProgress() &&
//                            !isCellularCallInProgress() &&
//                            !isVirtualCallInProgress()) {
//                            try {
//                                mContext.startActivity(sVoiceCommandIntent);
//                            } catch (ActivityNotFoundException e) {
//                                return new AtCommandResult(AtCommandResult.ERROR);
//                            }
//                            expectVoiceRecognition();
//                        }
//                    }
//                    return new AtCommandResult(AtCommandResult.UNSOLICITED);  // send nothing yet
//                } else if (args.length >= 1 && args[0].equals(0)) {
//                    return new AtCommandResult(AtCommandResult.OK);
//                }
//                return new AtCommandResult(AtCommandResult.ERROR);
//            }
//            @Override
//            public AtCommandResult handleTestCommand() {
//                return new AtCommandResult("+BVRA: (0-1)");
//            }
//        });
//
//        // Retrieve Subscriber Number
//        parser.register("+CNUM", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleActionCommand() {
//                String number = phone.getLine1Number();
//                if (number == null) {
//                    return new AtCommandResult(AtCommandResult.OK);
//                }
//                return new AtCommandResult("+CNUM: ,\"" + number + "\"," +
//                        PhoneNumberUtils.toaFromString(number) + ",,4");
//            }
//        });
//
//        // Microphone Gain
//        parser.register("+VGM", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // AT+VGM=<gain>    in range [0,15]
//                // Headset/Handsfree is reporting its current gain setting
//                return new AtCommandResult(AtCommandResult.OK);
//            }
//        });
//
//        // Speaker Gain
//        parser.register("+VGS", new AtCommandHandler() {
//            @Override
//            public AtCommandResult handleSetCommand(Object[] args) {
//                // AT+VGS=<gain>    in range [0,15]
//                if (args.length != 1 || !(args[0] instanceof Integer)) {
//                    return new AtCommandResult(AtCommandResult.ERROR);
//                }
////                int flag =  mAudioManager.isBluetoothScoOn() ? AudioManager.FLAG_SHOW_UI:0;
////                mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, mScoGain, flag);
//                return new AtCommandResult(AtCommandResult.OK);
//            }
//        });
//    }

    /* package */ synchronized boolean startVoiceRecognition() {
        return audioOn();
    }

    /* package */ synchronized boolean stopVoiceRecognition() {

        sendURC("+BVRA: 0");
        audioOff();
        return true;
    }

    /*
     * This class broadcasts vendor-specific commands + arguments to interested receivers.
     */
    private class VendorSpecificCommandHandler extends AtCommandHandler {

        private String mCommandName;

        private int mCompanyId;

        private VendorSpecificCommandHandler(String commandName, int companyId) {
            mCommandName = commandName;
            mCompanyId = companyId;
        }

        @Override
        public AtCommandResult handleReadCommand() {
            return new AtCommandResult(AtCommandResult.ERROR);
        }

        @Override
        public AtCommandResult handleTestCommand() {
            return new AtCommandResult(AtCommandResult.ERROR);
        }
        
        @Override
        public AtCommandResult handleActionCommand() {
            return new AtCommandResult(AtCommandResult.ERROR);
        }

        @Override
        public AtCommandResult handleSetCommand(Object[] arguments) {
            broadcastVendorSpecificEventIntent(mCommandName,
                                               mCompanyId,
                                               BluetoothHeadset.AT_CMD_TYPE_SET,
                                               arguments,
                                               mHeadset.getRemoteDevice());
            return new AtCommandResult(AtCommandResult.OK);
        }
    }

    private boolean inDebug() {
        return DBG && SystemProperties.getBoolean(DebugThread.DEBUG_HANDSFREE, false);
    }

    private boolean allowAudioAnytime() {
        return inDebug() && SystemProperties.getBoolean(DebugThread.DEBUG_HANDSFREE_AUDIO_ANYTIME,
                false);
    }

    private void startDebug() {
        if (DBG && mDebugThread == null) {
            mDebugThread = new DebugThread();
            mDebugThread.start();
        }
    }

    private void stopDebug() {
        if (mDebugThread != null) {
            mDebugThread.interrupt();
            mDebugThread = null;
        }
    }


    /** Debug thread to read debug properties - runs when debug.bt.hfp is true
     *  at the time a bluetooth handsfree device is connected. Debug properties
     *  are polled and mock updates sent every 1 second */
    private class DebugThread extends Thread {
        /** Turns on/off handsfree profile debugging mode */
        static final String DEBUG_HANDSFREE = "debug.bt.hfp";

        /** Mock battery level change - use 0 to 5 */
        static final String DEBUG_HANDSFREE_BATTERY = "debug.bt.hfp.battery";

        /** Mock no cellular service when false */
        static final String DEBUG_HANDSFREE_SERVICE = "debug.bt.hfp.service";

        /** Mock cellular roaming when true */
        static final String DEBUG_HANDSFREE_ROAM = "debug.bt.hfp.roam";

        /** false to true transition will force an audio (SCO) connection to
         *  be established. true to false will force audio to be disconnected
         */
        static final String DEBUG_HANDSFREE_AUDIO = "debug.bt.hfp.audio";

        /** true allows incoming SCO connection out of call.
         */
        static final String DEBUG_HANDSFREE_AUDIO_ANYTIME = "debug.bt.hfp.audio_anytime";

        /** Mock signal strength change in ASU - use 0 to 31 */
        static final String DEBUG_HANDSFREE_SIGNAL = "debug.bt.hfp.signal";

        /** Debug AT+CLCC: print +CLCC result */
        static final String DEBUG_HANDSFREE_CLCC = "debug.bt.hfp.clcc";

        /** Debug AT+BSIR - Send In Band Ringtones Unsolicited AT command.
         * debug.bt.unsol.inband = 0 => AT+BSIR = 0 sent by the AG
         * debug.bt.unsol.inband = 1 => AT+BSIR = 0 sent by the AG
         * Other values are ignored.
         */

        static final String DEBUG_UNSOL_INBAND_RINGTONE = "debug.bt.unsol.inband";

        @Override
        public void run() {
            boolean oldService = true;
            boolean oldRoam = false;
            boolean oldAudio = false;

            while (!isInterrupted() && inDebug()) {

                boolean serviceStateChanged = false;
                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_SERVICE, true) != oldService) {
                    oldService = !oldService;
                    serviceStateChanged = true;
                }
                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_ROAM, false) != oldRoam) {
                    oldRoam = !oldRoam;
                    serviceStateChanged = true;
                }

                if (SystemProperties.getBoolean(DEBUG_HANDSFREE_AUDIO, false) != oldAudio) {
                    oldAudio = !oldAudio;
                    if (oldAudio) {
                        audioOn();
                    } else {
                        audioOff();
                    }
                }

                int signalLevel = SystemProperties.getInt(DEBUG_HANDSFREE_SIGNAL, -1);
                if (signalLevel >= 0 && signalLevel <= 31) {
                    SignalStrength signalStrength = new SignalStrength(signalLevel, -1, -1, -1,
                            -1, -1, -1, true);
                    Intent intent = new Intent();
                    Bundle data = new Bundle();
                    signalStrength.fillInNotifierBundle(data);
                    intent.putExtras(data);
                }
                
                try {
                    sleep(1000);  // 1 second
                } catch (InterruptedException e) {
                    break;
                }

                int inBandRing =
                    SystemProperties.getInt(DEBUG_UNSOL_INBAND_RINGTONE, -1);
                if (inBandRing == 0 || inBandRing == 1) {
                    AtCommandResult result =
                        new AtCommandResult(AtCommandResult.UNSOLICITED);
                    result.addResponse("+BSIR: " + inBandRing);
                    sendURC(result.toString());
                }
            }
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
