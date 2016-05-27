package com.reconinstruments.hudconnectivitylib;

import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener;

interface IHUDConnectivityService {
	void DEBUG_ONLY_start();
	void DEBUG_ONLY_stop();
	void register(in IHUDConnectivityListener listener);
	void unregister(in IHUDConnectivityListener listener);
	void connect(String address);
	boolean isPhoneConnected();
	boolean hasWebConnection();
	
	HUDHttpResponse sendWebRequest(in HUDHttpRequest request);
	
}