package com.reconinstruments.mfi;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.MFiAccessoryInfo;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocol;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocolMatchAction;


public class MFiInfo {

    private static final String TAG = "MFiInfo";
    private static final boolean DEBUG = true;

    public static final int NUMBER_OF_HUD_CONNECTIVITY_CHANNELS = 4;
    protected static final String BASE_CHANNEL_NAME = "com.reconinstruments.hudconnectivity";

    protected static final int NUMBER_OF_HUD_SERVICE_CHANNELS = 3;
    public static final int COMMAND_CHANNEL_PROTOCOL_IDX = 5;
    public static final int OBJECT_CHANNEL_PROTOCOL_IDX = 6;
    public static final int FILE_CHANNEL_PROTOCOL_IDX = 7;

    protected static final String COMMAND_CHANNEL_NAME = "com.reconinstruments.command";
    protected static final String OBJECT_CHANNEL_NAME = "com.reconinstruments.object";
    protected static final String FILE_CHANNEL_NAME = "com.reconinstruments.file";

    protected static int mSessionIDs[] = new int[NUMBER_OF_HUD_CONNECTIVITY_CHANNELS + NUMBER_OF_HUD_SERVICE_CHANNELS];

    public static MFiProtocol[] mMFiProtocols = null;
    protected static MFiProtocol[] mHUDConnectivityMFiProtocols = null;
    protected static MFiProtocol[] mHUDServiceMFiProtocols = null;

    public static void clearSessionIDs() {
        if (DEBUG) Log.d(TAG, "clearSessionIDs()");
        synchronized (mSessionIDs) {
            for (int i = 0; i < mSessionIDs.length; i++) {
                mSessionIDs[i] = -1;
            }
        }
    }

    public static void setSessionID(int protocolIndex, int sessionID) {
        if (DEBUG) Log.d(TAG, "setSessionID() protocolIndex:" + protocolIndex + " sessionID:" + sessionID);
        if (protocolIndex < 1 || protocolIndex > mSessionIDs.length) {
            Log.e(TAG, "Illegal Protocol Index " + protocolIndex);
            return;
        }

        synchronized (mSessionIDs) {
            mSessionIDs[protocolIndex - 1] = sessionID;
        }
    }

    public static int getSessionID(int protocolIndex) {
        synchronized (mSessionIDs) {
            int sessionID = mSessionIDs[protocolIndex - 1];
            if (DEBUG) Log.d(TAG, "getSessionID() protocolIndex:" + protocolIndex + " sessionID:" + sessionID);
            return sessionID;
        }
    }

    /**
     * @param sessionID
     * @return -1 if not found
     */
    public static final int getProtocolIndex(int sessionID) {
        synchronized (mSessionIDs) {
            for (int i = 0; i < mSessionIDs.length; i++) {
                if (mSessionIDs[i] == sessionID) {
                    return i + 1;
                }
            }
        }

        return -1;
    }

    public static final boolean isHUDConnectivityProtocol(int protocolIndex) {
        if (protocolIndex <= NUMBER_OF_HUD_CONNECTIVITY_CHANNELS) {// Starts from 1
            return true;
        }
        return false;
    }

    static {
        if (mMFiProtocols == null) {
            int pos = 0;

            mMFiProtocols = new MFiProtocol[NUMBER_OF_HUD_CONNECTIVITY_CHANNELS + NUMBER_OF_HUD_SERVICE_CHANNELS];
            mHUDConnectivityMFiProtocols = new MFiProtocol[NUMBER_OF_HUD_CONNECTIVITY_CHANNELS];
            mHUDServiceMFiProtocols = new MFiProtocol[NUMBER_OF_HUD_SERVICE_CHANNELS];

            for (int i = 0; i < NUMBER_OF_HUD_CONNECTIVITY_CHANNELS; i++) {
                mHUDConnectivityMFiProtocols[i] = new MFiProtocol(BASE_CHANNEL_NAME + "." + i, MFiProtocolMatchAction.NONE);
                mMFiProtocols[pos++] = mHUDConnectivityMFiProtocols[i];
            }

            mHUDServiceMFiProtocols[0] = new MFiProtocol(COMMAND_CHANNEL_NAME, MFiProtocolMatchAction.NONE);
            mMFiProtocols[COMMAND_CHANNEL_PROTOCOL_IDX - 1] = mHUDServiceMFiProtocols[0];

            mHUDServiceMFiProtocols[1] = new MFiProtocol(OBJECT_CHANNEL_NAME, MFiProtocolMatchAction.NONE);
            mMFiProtocols[OBJECT_CHANNEL_PROTOCOL_IDX - 1] = mHUDServiceMFiProtocols[1];

            mHUDServiceMFiProtocols[2] = new MFiProtocol(FILE_CHANNEL_NAME, MFiProtocolMatchAction.NONE);
            mMFiProtocols[FILE_CHANNEL_PROTOCOL_IDX - 1] = mHUDServiceMFiProtocols[2];
        }
    }

    public static final MFiAccessoryInfo mMFiAccessoryInfo = new MFiAccessoryInfo(
            SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
            BluetoothAdapter.getDefaultAdapter().getName(),
            0x00010000,
            0x00010000,
            "Recon Instruments",
            "Jet",
            "40984E5CCE15",
            (SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3));

}
