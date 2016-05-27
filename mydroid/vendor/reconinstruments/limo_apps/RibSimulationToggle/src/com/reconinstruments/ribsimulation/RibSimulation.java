package com.reconinstruments.ribsimulation;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Button;
import android.view.View;
import java.io.File;
public class RibSimulation extends Activity
{
    private Button toggle_btn;
    /** Called when the activity is first created. */
    public static final String CSV = "/data/system/gpsreplay.csv";
    public static final String CS_ = "/data/system/gpsreplay.cs_";
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	toggle_btn = (Button) findViewById(R.id.toggle_btn);
        toggle_btn.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    toggle();
		    fixLabel();
		}
	    });
	fixLabel();

    }

    public void fixLabel () {
	File file = new File(CSV);
	
	// File (or directory) with new name
	File file2 = new File(CS_);

	if (file.exists()) {
	    toggle_btn.setText("Press to disable simulation");
	}
	else if(file2.exists()) {
	    toggle_btn.setText("Press to enable simulation");
	}
	else {
	    toggle_btn.setText("Can't simulate");
	}
	
    }

    public void toggle () {
	// File (or directory) with old name
	File file = new File(CSV);
	
	// File (or directory) with new name
	File file2 = new File(CS_);

	boolean success= false;
	if (file.exists()) {
	    success = file.renameTo(file2);
	} else if (file2.exists()) {
	    success = file2.renameTo(file);
	}
	else {
	    Toast.makeText(this, "No file to toggle", Toast.LENGTH_SHORT).show();
	}
	if (!success) {
	    Toast.makeText(this, "Could not do", Toast.LENGTH_SHORT).show();
	}

    }
}
