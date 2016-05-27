package com.android.server;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Handler;
import java.io.File;
import java.io.FileReader;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/*
 *
 * SUPLService has to be created with providing Binder Interface.
 *
 *
 */
public class SUPLService extends Binder {
	private static final String TAG = "SUPLService";
	private Context mContext; 
	private static SUPLService sServiceInstance;
	private static SUPLServer mSuplServer;

	/*
 	 * SUPLThread 
 	 *
 	 */
	public static class SUPLThread extends Thread {
		private Context mContext;

		private SUPLThread(Context context){
			super("SUPL Thread");
			mContext = context;
			//Log.d(TAG,"===>>> INSIDE SUPL Thread <<<===");

		}

		@Override
		public void run(){
			//Log.d(TAG,"===>>> INSIDE SUPL Thread Run 1111<<<===");
			Looper.prepare();
			synchronized(this){
				sServiceInstance = new SUPLService(mContext);
				//Log.d(TAG,"===>>> INSIDE SUPL Thread RUN 2222 <<<===");
			        mSuplServer = new SUPLServer(sServiceInstance, mContext);
				try{
					mSuplServer.start();
				}catch(IOException ignore){
					Log.e(TAG,"====>>> IO Exception  error with starting SUPL Server <<<===");
				}
				notifyAll();
			}
			Looper.loop();	
		}
		
	
	public static SUPLService getServiceInstance(Context context){
		SUPLThread thread = new SUPLThread(context);
		//Log.d(TAG,"===>>> SUPL Service Instance 1111 <<<==");
		thread.start();
		synchronized(thread){
			while(sServiceInstance ==  null){
				try{
					//Log.d(TAG,"===>>> SUPL Service Instance: Before thread.wait():  2222 <<<==");
					thread.wait();
				}catch(InterruptedException ignore){
					Log.e(TAG,"Unexpected InterruptedException while waiting for SUPLService Thread");
	
				}
			}

		}

			return sServiceInstance;
	}

	}	

	public static SUPLService getInstance(Context context){
		return SUPLThread.getServiceInstance(context);
	}

	private SUPLService(Context context){
		//Log.v(TAG,"Starting SUPL Service");
		mContext = context;
	}
}
