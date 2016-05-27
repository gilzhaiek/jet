package com.reconinstruments.alphatester;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button clickButton = (Button) findViewById(R.id.button1);
		Button compassButton = (Button) findViewById(R.id.button2);
		
		clickButton.setOnClickListener( new View.OnClickListener() {

		            @Override
		            public void onClick(View v) {
		                // TODO Auto-generated method stub
		                Intent intent = new Intent(MainActivity.this, GPS.class);
		                startActivity(intent);
		            }
		        });
		compassButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(MainActivity.this, Compass.class);
                startActivity(intent);
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
