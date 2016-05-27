package com.rxnetworks.device;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class NetworkStatus 
{
	public static final String TAG = "RXNetworksService.NetworkStatus";
		
	public static boolean IsDownloadAllowed(Context context, NetworkInfo networkStatus)
	{
		
		if(networkStatus == null)
		{
			Log.i(TAG, "unable to detect data connectivity.");
			return false;
		}
		
		if(networkStatus.isConnected() == true)
		{
			if(networkStatus.getType() == ConnectivityManager.TYPE_MOBILE)
			{
				if(networkStatus.isRoaming() == true)
				{
//					TODO: When upgrading to Jellybean MR1 (Android API 17) use this code block
//					if(Settings.Secure.getInt(context.getContentResolver(), Settings.Global.DATA_ROAMING,3)==1)					
					if(Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.DATA_ROAMING,3)==1)
					{
						Log.i(TAG, "device has ROAMING network data connectivity.");
						return true;
					}
					else
					{
						Log.i(TAG, "device has ROAMING network data connectivity, but data roaming is disabled.");
						return false;
					}
				}
				else
				{
					Log.i(TAG, "device has HOME network data connectivity.");
					return true;
				}
			}
			else
			{
				/* NetworkStatus.getType() == ConnectivityManager.TYPE_WIFI */
				Log.i(TAG, "device has WIFI connectivity.");
				return true;
			}
		}
		else
		{
			/* No data connection */
			Log.i(TAG, "device has not data connectivity.");
			return false;
		}
	}
}
