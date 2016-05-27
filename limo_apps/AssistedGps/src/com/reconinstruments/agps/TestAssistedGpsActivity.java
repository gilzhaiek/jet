package com.reconinstruments.agps;
import android.view.View;
import android.location.Location;
import android.widget.Button;
import android.app.Activity;
import android.os.Bundle;
import java.net.URL;
import java.net.MalformedURLException;
import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
import com.reconinstruments.webapi.ReconWebApiClient;
import com.reconinstruments.webapi.ReconOSHttpClient;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import android.content.IntentFilter;

public class TestAssistedGpsActivity extends Activity
{
    private static String TAG = "AssistedGpsActivity";
    public static String PROVIDER_NAME = "RECON_AGPS";
    private TestReconAGpsContext mReconAGpsContext;


    // boilerplate:x
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	mReconAGpsContext = new TestReconAGpsContext(this);
	mReconAGpsContext.initialize();

	final Button requestAssist = (Button) findViewById(R.id.request_assist);
	requestAssist.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.gpsChipStateChanged(1);
		}
	    });
	final Button requestNoAssist = (Button) findViewById(R.id.request_no_assist);
	requestNoAssist.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.gpsChipStateChanged(0);
		}
	    });

	final Button almanacUpdated = (Button) findViewById(R.id.almanac_updated);
	almanacUpdated.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.almanacUpdated();
		}
	    });
	final Button gotPhoneGps = (Button) findViewById(R.id.got_phone_gps);
	gotPhoneGps.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.phoneGpsReceived(new Location("some provider"));
		}
	    });
	final Button gotHudGps = (Button) findViewById(R.id.got_hud_gps);
	gotHudGps.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.haveGoodOwnGps();
		}
	    });
	final Button gotBtDisconnect = (Button) findViewById(R.id.got_bt_disconnect);
	gotBtDisconnect.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.bluetoothStateChanged(0);
		}
	    });
	final Button gotBtConnect = (Button) findViewById(R.id.got_bt_connect);
	gotBtConnect.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    mReconAGpsContext.mStateMachine.bluetoothStateChanged(2);
		}
	    });

    }
    public void onDestroy() {
	mReconAGpsContext.cleanUp();
	super.onDestroy();
    }

}
