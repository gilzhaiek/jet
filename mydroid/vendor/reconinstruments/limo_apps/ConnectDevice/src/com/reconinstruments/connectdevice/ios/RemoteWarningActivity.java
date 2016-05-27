package com.reconinstruments.connectdevice.ios;

import com.reconinstruments.connectdevice.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import com.reconinstruments.connectdevice.ConnectionActivity;

public class RemoteWarningActivity extends ConnectionActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_ios_remote_warning);

		RelativeLayout next = (RelativeLayout) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(RemoteWarningActivity.this,
						FirstConnectActivity.class));
			}
		});
	}
}
