package com.reconinstruments.hudconnectivitylib;

import java.io.IOException;

import android.accounts.NetworkErrorException;
import android.content.Context;

import com.reconinstruments.hudconnectivitylib.bluetooth.HUDSPPService;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpBTConnection;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;
import com.reconinstruments.hudconnectivitylib.http.URLConnectionHUDAdaptor;

public class HUDConnectivityPhoneConnection implements IHUDConnectivityConnection {
    private HUDHttpBTConnection mHUDHttpBTConnection = null;
    private HUDSPPService mHUDBTService = null;

    private final boolean IS_HUD = false;

    public HUDConnectivityPhoneConnection(Context context, IHUDConnectivity hudConnectivity) throws Exception {
        mHUDBTService = new HUDSPPService(hudConnectivity);
        mHUDHttpBTConnection = new HUDHttpBTConnection(context, hudConnectivity, IS_HUD);
        mHUDHttpBTConnection.setBTService(mHUDBTService);
    }

    @Override
    public void start() throws IOException, InterruptedException {
        mHUDHttpBTConnection.start();
        mHUDBTService.startListening();
    }

    @Override
    public void connect(String address) throws IOException {
        mHUDBTService.connect(address);
    }

    @Override
    public boolean hasWebConnection() {
        return mHUDHttpBTConnection.hasWebConnection();
    }

    @Override
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws IOException, NetworkErrorException {
        if (mHUDHttpBTConnection.hasLocalWebConnection()) {
            return URLConnectionHUDAdaptor.sendWebRequest(request);
        }

        throw new NetworkErrorException("Phone is not connected to the internet");
    }

    @Override
    public void DEBUG_ONLY_stop() throws IOException {
        mHUDHttpBTConnection.stop();
        mHUDBTService.stopListening();
    }
}
