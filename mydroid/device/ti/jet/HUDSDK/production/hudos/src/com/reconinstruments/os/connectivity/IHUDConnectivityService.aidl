package com.reconinstruments.os.connectivity;

import com.reconinstruments.os.connectivity.http.HUDHttpResponse;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.IHUDConnectivityListener;

/** {@hide}*/
interface IHUDConnectivityService {
	void register(in IHUDConnectivityListener listener);
	void unregister(in IHUDConnectivityListener listener);
	boolean isPhoneConnected();
	boolean hasWebConnection();
	
	HUDHttpResponse sendWebRequest(in HUDHttpRequest request);
	
}
