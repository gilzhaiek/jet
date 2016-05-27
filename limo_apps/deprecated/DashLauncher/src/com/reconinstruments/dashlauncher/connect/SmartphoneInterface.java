package com.reconinstruments.dashlauncher.connect;

import android.view.View;

public interface SmartphoneInterface
{
	public abstract boolean requiresAndroid();
	public abstract View getNoConnectOverlay();
	public abstract View getNoConnectSetupButton(View overlay);
	public abstract View getNoConnectNoShowButton(View overlay);
	public abstract View getAndroidOverlay();
	public abstract View getIOSOverlay();
	public abstract View getIOSConnectButton(View overlay);
	public abstract void onConnect();
	public abstract void onDisconnect();
}
