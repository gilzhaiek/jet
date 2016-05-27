package com.reconinstruments.dashlauncher.connect;


import android.view.View;
import android.widget.TextView;

public interface SmartphoneInterface
{
	public abstract View getNoConnectOverlay();
	public abstract View getNoConnectSetupButton(View overlay);
	public abstract TextView getNoConnectSetupTitle(View overlay);
	public abstract TextView getNoConnectSetupInfo(View overlay);
	public abstract TextView getNoConnectSetupConnectText(View overlay);
	public abstract void onConnect();
	public abstract void onDisconnect();
	public abstract SmartphoneConnector getConnector();
}
