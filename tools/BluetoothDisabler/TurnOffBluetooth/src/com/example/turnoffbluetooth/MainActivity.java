package com.example.turnoffbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView view = (TextView) findViewById(R.id.reason);
        
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.disable()) {
        	view.setText("Disable successful");
        } else {
        	view.setText("Disable failed");
        }
        
    }
    
}
