package com.reconinstruments.chrono;

import java.util.ArrayList;
import com.reconinstruments.applauncher.transcend.ReconChronoManager;
import com.reconinstruments.modservice.ReconMODServiceMessage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableRow.LayoutParams;

public class ReconChrono extends Activity {
	public static final int FORWARD_MSG_TO_CHRONO = 0;

	public static final String TAG = "ReconChrono";
	
	MODServiceConnection mMODConnection;

	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound = false;
	
	/** Latest infoBundle **/
	private Bundle mInfoBundle = null;
	
	/** View Stuff */
	private FontSingleton font;
	private TextView timeTextView, timeMsTextView;
	private TableLayout lapListTable;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mMODConnection = MODServiceConnection.getInstance(this);
		mMODConnection.doBindService();
		mMODConnection.addReceiver(new Messenger(new MODServiceHandler()));
		
		setContentView(R.layout.home);

		font = FontSingleton.getInstance(this);

		timeTextView = (TextView) findViewById(R.id.time);
		timeMsTextView = (TextView) findViewById(R.id.time_ms);
		lapListTable = (TableLayout) findViewById(R.id.lap_list);

		setTypeFaceForAll();
	}
	
	public void onResume() {
		super.onResume();
		
		mMODConnection.requestUpdate();
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View instructions = inflater.inflate(R.layout.instruct_toast, null);
		
		TextView instructStartTextView = (TextView) instructions.findViewById(R.id.instruct_start_text);
		TextView instructLapTextView = (TextView) instructions.findViewById(R.id.instruct_lap_text);
		
		instructStartTextView.setTypeface(font.getTypeface());
		instructLapTextView.setTypeface(font.getTypeface());
		
		Toast toastView = new Toast(this);
		toastView.setView(instructions);
		toastView.setDuration(Toast.LENGTH_LONG);
		toastView.setGravity(Gravity.CENTER, 0,0);
		toastView.show();
	}

	public void onDestroy() {
		super.onDestroy();
		mMODConnection.doUnBindService();
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
    		mMODConnection.issueLapTrialCommand();
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        	// Switch to history view
        	if(mInfoBundle != null) {
        		Intent myIntent = new Intent(this.getBaseContext(), HistoryView.class);
        		myIntent.putExtra("CHRONO_BUNDLE", mInfoBundle);
        		startActivityForResult(myIntent, 0);
        	}
        }
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
        	finish();
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER) {
        	mMODConnection.issueStartStopCommand();
        }
        else {
        	return false;
        }
        return true;
    }
	
	private void updateEverything() {
		ArrayList<Bundle> trials = mInfoBundle.getParcelableArrayList("Trials");

		if (trials.size() < 1)
			return;

		Bundle latestTrial = trials.get(trials.size() - 1);
		ArrayList<Bundle> laps = latestTrial.getParcelableArrayList("Laps");

		long currentLapElapsedTime = latestTrial.getLong("ElapsedTime");

		timeTextView.setText(ReconChronoManager.parseElapsedTime(currentLapElapsedTime, true));
		timeMsTextView.setText(ReconChronoManager.parseElapsedTime(currentLapElapsedTime, false));
		
		if(latestTrial.getBoolean("IsRunning")) {
			timeMsTextView.setVisibility(View.GONE);
		} else {
			timeMsTextView.setVisibility(View.VISIBLE);
		}

		// Refresh last three laps
		lapListTable.removeAllViews();
		for(int i=2; i < 5 && laps.size() >= i; i++) {
			Bundle lap = laps.get(laps.size() - i);
			String time = ReconChronoManager.parseElapsedTime(lap.getLong("ElapsedTime"), true) + ReconChronoManager.parseElapsedTime(lap.getLong("ElapsedTime"), false);
			addRowToLapList(laps.indexOf(lap) + 1, time);
		}
	}

	private void addRowToLapList(int lapNumber, String time) {
		TableRow tr = new TableRow(this);

		/* Create textviews to be the row-content. */
		TextView lapNumTextView = new TextView(this);
		lapNumTextView.setText("Lap " + Integer.toString(lapNumber));
		lapNumTextView.setTypeface(font.getTypeface());
		lapNumTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 29);
		lapNumTextView.setTextColor(getResources().getColor(R.color.white));
		tr.addView(lapNumTextView);

		TextView lapTimeTextView = new TextView(this);
		lapTimeTextView.setText(time);
		lapTimeTextView.setGravity(Gravity.RIGHT);
		lapTimeTextView.setPadding(0, 0, 15, 0);
		lapTimeTextView.setTypeface(font.getTypeface());
		lapTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 29);
		lapTimeTextView.setTextColor(getResources().getColor(R.color.white));
		tr.addView(lapTimeTextView);

		/* Add row to TableLayout. */
		lapListTable.addView(tr, new TableLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	}

	private void setTypeFaceForAll() {
		timeTextView.setTypeface(font.getTypeface());
		timeMsTextView.setTypeface(font.getTypeface());
	}
	
	class MODServiceHandler extends Handler {
		public void handleMessage(Message msg) {
    		switch (msg.what) {
			case ReconMODServiceMessage.MSG_RESULT:
				if (msg.arg1 == ReconMODServiceMessage.MSG_GET_CHRONO_BUNDLE) {
					mInfoBundle = msg.getData();
					updateEverything();
				}
				break;

			default:
				super.handleMessage(msg);
			}
		}
	}


}