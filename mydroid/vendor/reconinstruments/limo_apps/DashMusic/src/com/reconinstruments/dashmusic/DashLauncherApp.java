package com.reconinstruments.dashmusic;
import android.app.Application;
import android.content.Intent;

public class DashLauncherApp extends Application
{
	private final static String TAG = "DashLauncherApp";
	
	public static DashLauncherApp instance;
	@Override
	public void onCreate()
	{

		instance = this;

		//registerReceiver(phoneConnectionReceiver, new IntentFilter(XMLMessage.MUSIC_MESSAGE));
		//registerReceiver(phoneConnectionReceiver, new IntentFilter(XMLMessage.SONG_MESSAGE));
		
	}



	@Override
	public void onTerminate()
	{
		super.onTerminate();
		//unregisterReceiver(phoneConnectionReceiver);
	}



	/** Static access to a singleton application context */
	public static DashLauncherApp getInstance() {
		return instance;
	}
	
	/** broadcasts to bleService if the current activity belongs to music module
	 * @param inMusic
	 */
	public void setInMusicApp(boolean inMusic){
		int command;
		if(inMusic)
			command = 15;
		else
			command = 16;
		Intent theIntent = new Intent().setAction("private_ble_command");
        theIntent.putExtra("command",command);
        sendBroadcast(theIntent);
	}
	
	

}
