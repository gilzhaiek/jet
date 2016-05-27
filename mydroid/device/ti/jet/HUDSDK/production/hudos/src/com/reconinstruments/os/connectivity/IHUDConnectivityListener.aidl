package com.reconinstruments.os.connectivity;

/** {@hide}*/
oneway interface IHUDConnectivityListener {
	void onDeviceName(String deviceName);
	void onConnectionStateChanged(int connectionState);
	void onNetworkEvent(int networkEvent, boolean hasNetworkAccess);
}
