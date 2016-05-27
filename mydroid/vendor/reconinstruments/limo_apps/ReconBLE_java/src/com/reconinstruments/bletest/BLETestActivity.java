package com.reconinstruments.bletest;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import com.reconinstruments.nativetest.R;
import com.reconinstruments.reconble.*;

public class BLETestActivity extends Activity {
    private static final String TAG = "BLE_TEST_ACTIVITY";
    private TextView txtBLEStatus;    // status of Pairing Monitor
    private TextView txtBLETx;        // status of Outgoing (Tx) Monitor
    private TextView txtBLERx;        // status of Incoming (Rx) Monitor
	 
    private EditText txtBLEInput;     // Input Box for Outgoing Tx: Type anything you want
    private TextView txtBLEData;      // Incoming (received) buffer
	 
    private Intent getGoodIntent(int command){
	Intent theIntent = new Intent().setAction("private_ble_command");
	theIntent.putExtra("command",command);
	return theIntent;
    }

    private Intent getGoodIntent(int command, int value){
	Intent theIntent = new Intent().setAction("private_ble_command");
	theIntent.putExtra("command",command);
	theIntent.putExtra("value", value);
	return theIntent;
    }

    private Intent getGoodIntent(int command, String xmlMessage, int prior){
	Intent theIntent = new Intent().setAction("private_ble_command");
	theIntent.putExtra("command",command);
	theIntent.putExtra("data",xmlMessage);
	theIntent.putExtra("prioriry", prior);
	return theIntent;
    }


    private void slaveMode() {

	sendBroadcast(getGoodIntent(1));
    }

    private void masterMode() {
	sendBroadcast(getGoodIntent(2));
    }


    private void sendFile() {
	sendBroadcast(getGoodIntent(3));
    }

    private void receiveFile() {
	sendBroadcast(getGoodIntent(4));
    }

    private void unpairDevice() {
	sendBroadcast(getGoodIntent(5));
    }

    private void pairDevice() {
	sendBroadcast(getGoodIntent(6));
    }

    private void resetDevice() {
	sendBroadcast(getGoodIntent(7));
    }

    private void setPriority(int val) {
	BLELog.d(TAG,"setPriority");
	sendBroadcast(getGoodIntent(8, val));
    }

    private void getPriority(int val) {
	BLELog.d(TAG,"getPriority");
	sendBroadcast(getGoodIntent(9));
    }


    private void cancelsend() {
	sendBroadcast(getGoodIntent(10));
    }

    private void getrcvPriority() {
	BLELog.d(TAG,"getrcvPriority");
	sendBroadcast(getGoodIntent(11));
    }

    private void sendXml() {
	BLELog.d(TAG,"sendXml");
	sendBroadcast(getGoodIntent(12,"<recon>junkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunkjunk</recon>",1));
    }
    /** Called when the activity is first created. */ 
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.main);

        // instantiate View controls
        txtBLEInput  = (EditText) findViewById(R.id.bleinput);
	final Button btnsendxml = (Button) findViewById(R.id.btnsendxml);
        btnsendxml.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    sendXml();
		}
	    });

	final Button btngetrcvprior = (Button) findViewById(R.id.btngetrcvprior);
        btngetrcvprior.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    getrcvPriority();
		}
	    });


	final Button btngetprior = (Button) findViewById(R.id.btngetprior);
        btngetprior.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    getPriority(0);
		}
	    });


	final Button btnprior0 = (Button) findViewById(R.id.btnprior0);
        btnprior0.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    setPriority(0);
		}
	    });

	final Button btnprior1 = (Button) findViewById(R.id.btnprior1);
        btnprior1.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    BLELog.d(TAG,"Set priority 1");
		    setPriority(1);
		}
	    });




	final Button btnslavemode = (Button) findViewById(R.id.btnslavemode);
        btnslavemode.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    slaveMode();
		}
	    });


	final Button btnmastermode = (Button) findViewById(R.id.btnmastermode);
        btnmastermode.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    masterMode();
		}
	    });


	
        final Button btnSend = (Button) findViewById(R.id.btnsend);
        btnSend.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    sendFile();
		}
	    });

        
        final Button btncancelsend = (Button) findViewById(R.id.btncancelsend);
        btncancelsend.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    cancelsend();    
		}
	    });
        
        
        final Button btnPair = (Button) findViewById(R.id.btnpair);
        btnPair.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    pairDevice();
		}
	    });
        
    
        final Button btnReset = (Button) findViewById(R.id.btnreset);
        btnReset.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    resetDevice();
		}
	    });
        
        final Button btnUnpair = (Button) findViewById(R.id.btnunpair);
        btnUnpair.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    unpairDevice();
		}
	    });


	startService(new Intent("RECON_BLE_TEST_SERVICE"));
    }
}