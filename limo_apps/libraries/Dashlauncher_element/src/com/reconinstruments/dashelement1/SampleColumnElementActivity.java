package com.reconinstruments.dashelement1;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
public class SampleColumnElementActivity extends ColumnElementActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	final Button buttonUp = (Button) findViewById(R.id.up_button);
	buttonUp.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    goUp();
		}
	    });
	final Button buttonLeft = (Button) findViewById(R.id.left_button);
	buttonLeft.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    goLeft();
		}
	    });
	final Button buttonRight = (Button) findViewById(R.id.right_button);
	buttonRight.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    goRight();
		}
	    });
	final Button buttonDown = (Button) findViewById(R.id.down_button);
	buttonDown.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    goDown();
		}
	    });
	final Button buttonBack = (Button) findViewById(R.id.back_button);
	buttonBack.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    goBack();
		}
	    });

    }

}
