package com.reconinstruments.test.buddytracking;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage;
import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage.BuddyInfo;

public class MainActivity extends Activity {
	private static final String TAG = "BUDDYTESTING";

	private static final int NUMBER_BUDDIES = 10;

	double lat = 49.2769;
	double lon = 123.1209;

	ArrayList<BuddyInfo> bdyInfo = new ArrayList<BuddyInfo>();
	private boolean doRun = false;
	private BuddyThread bdyThread;

	class BuddyThread extends Thread{

		@Override
		public void run(){
			while (doRun){
				synchronized (bdyInfo){
					for (int i = 0; i < bdyInfo.size() ; i++){
						bdyInfo.get(i).location.setLatitude(lat+(Math.random()-0.5)/100);
						bdyInfo.get(i).location.setLongitude(lon+(Math.random()-0.5)/100);
					}

					BuddyInfoMessage bim = new BuddyInfoMessage();

					String message = bim.compose(bdyInfo);

					BTCommon.broadcastMessage(getBaseContext(), message);

				}
				try {
					sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		for (int i = 0 ; i < NUMBER_BUDDIES ; i++){

			BuddyInfo info = new BuddyInfo(i, "name"+i, lat+(Math.random()-0.5)/100 , lon+(Math.random()-0.5)/100 ) ;
			bdyInfo.add(info);

		}

		final Button startBtn = (Button) findViewById(R.id.buddy_service_btn);
		startBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {

				if (doRun){
					doRun = !doRun;
					startBtn.setText("start");
				}
				else{
					BuddyInfoMessage bim = new BuddyInfoMessage();
					String message = bim.compose(bdyInfo);

					doRun = true;
					bdyThread = new BuddyThread();
					bdyThread.start();

					startBtn.setText("stop");
				}
			}

		});


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}


}
