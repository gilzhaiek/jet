package com.reconinstruments.symptomchecker;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	private Button mStartButton;
	private Button mConfigButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);
		
		//mStartButton = (Button) findViewById(R.id.START_BUTTON);
		//mStartButton.setOnClickListener(this);

		//mConfigButton = (Button) findViewById(R.id.CONFIG_BUTTON);
		//mConfigButton.setOnClickListener(this);
		
		File file = new File(Config.Directory);
		if(file.exists() == false) {
			file.mkdir();
		}
	}
	
	@Override
	public void onStart(){

		//AutoLaunch Test
		super.onStart();
		LaunchTest();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.START_BUTTON:
			LaunchTest();
			break;
		case R.id.CONFIG_BUTTON:
			break;
		}
	}
	
	private void LaunchTest(){
		Intent AutomatedTestIntent = new Intent(getApplicationContext(), AutomatedTestActivity.class);
		startActivity(AutomatedTestIntent);
		finish();
	}
}
