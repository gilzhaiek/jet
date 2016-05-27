package com.reconinstruments.hudremotetester;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.reconinstruments.os.hudremote.HUDRemoteListener;
import com.reconinstruments.os.hudremote.HUDRemoteManager;

public class MainActivity extends Activity implements HUDRemoteListener {
    private HUDRemoteManager mManager = null;
    private static final String TAG = "HUDRemoteTester";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mManager = new HUDRemoteManager(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mManager != null) {
            if (mManager.unregisterHUDRemoteListener(this) != HUDRemoteManager.SUCCESS) {
                Log.e(TAG, "Failed to unregister HUD remote listener");
            }
            mManager.cleanup();
            mManager = null;
        }
    }

    public void registerEvents(View view) {
        if (mManager != null) {
            if (mManager.registerHUDRemoteListener(this) != HUDRemoteManager.SUCCESS) {
                Log.e(TAG, "Failed to register HUD remote listener");
            } else {
                Log.d(TAG, "Registered HUD remote listener");
            }
        }
    }

    public void startRemoteScan(View view) {
        if (mManager != null) {
            if (mManager.startRemoteScan() != HUDRemoteManager.SUCCESS) {
                Log.e(TAG, "Failed to start remote scan");
            }
        }
    }

    public void stopRemoteScan(View view) {
        if (mManager != null) {
            if (mManager.stopRemoteScan() != HUDRemoteManager.SUCCESS) {
                Log.e(TAG, "Failed to stop remote scan");
            }
        }
    }

    @Override
    public void onHUDRemoteScan(String address) {
        Log.d(TAG, "onHUDRemoteScan: " + address);
    }
}
