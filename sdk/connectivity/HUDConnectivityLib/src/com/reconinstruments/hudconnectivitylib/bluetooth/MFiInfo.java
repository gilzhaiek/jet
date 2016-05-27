package com.reconinstruments.hudconnectivitylib.bluetooth;

import android.bluetooth.BluetoothAdapter;

import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.MFiAccessoryInfo;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocol;
import com.stonestreetone.bluetopiapm.SPPM.MFiProtocolMatchAction;

public class MFiInfo {
    protected static final int NUMBER_OF_CHANNELS = 4;

    protected static final String BASE_CHANNEL_NAME = "com.reconinstruments.hudconnectivity";

    protected static MFiProtocol[] mMFiProtocols = null;

    static {
        mMFiProtocols = new MFiProtocol[NUMBER_OF_CHANNELS];
        for (int i = 0; i < NUMBER_OF_CHANNELS; i++) {
            mMFiProtocols[i] = new MFiProtocol(BASE_CHANNEL_NAME + "." + i, MFiProtocolMatchAction.NONE);
        }
    }
    
    protected static final MFiAccessoryInfo mMFiAccessoryInfo = new MFiAccessoryInfo(
            SPPM.MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP,
            BluetoothAdapter.getDefaultAdapter().getName(),
            0x00010000,
            0x00010000,
            "Recon Instruments",
            "Jet",
            "40984E5CCE15",
            (SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 | SPPM.MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3));
}
