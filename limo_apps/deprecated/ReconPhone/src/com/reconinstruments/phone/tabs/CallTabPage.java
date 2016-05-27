package com.reconinstruments.phone.tabs;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.reconinstruments.applauncher.phone.PhoneLogProvider;
import com.reconinstruments.phone.DisplaySMSActivity;
import com.reconinstruments.phone.PhoneEventListAdapter;
import com.reconinstruments.phone.SMSReplyActivity;
import com.reconinstruments.widgets.*;

public class CallTabPage extends TabPage {
	
	private static final String CALL_INIT_BEGIN = "<recon intent=\"RECON_PHONE_CONTROL\"><action type=\"initiate_call\">";
    private static final String CALL_INIT_END = "</action></recon>";
	
	private Context mContext;
	ListView callLv;
	TabView hostView;
	private boolean phoneConnected = false;

	public CallTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);

		this.hostView = hostView;
		mContext = context;

		/* Set up view */
		LayoutInflater inflater = LayoutInflater.from(context);
		View layout = inflater.inflate(R.layout.default_lv, null);
		TextView titleView = (TextView) layout.findViewById(R.id.list_title);
		titleView.setText("RECENT CALLS");
		this.addView(layout);
		this.setFocusable(true);

		/* Set up list */
		PhoneEventListAdapter callAdapter = generateListAdapter();
		callLv = (ListView) layout.findViewById(R.id.menu_list);
		//callLv.setOnKeyListener(mKeyListener);
		callLv.setOnItemClickListener(mOnItemClickListener);
		callLv.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
				if(keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
					return onBackDown(v);
				}
				return false;
			}
			
		});
		callLv.setAdapter(callAdapter);
		callLv.requestFocus();

		Typeface tf = Typeface.createFromAsset(this.getContext().getAssets(),
				"fonts/Eurostib.ttf");
		titleView.setTypeface(tf);
	}

	@Override
	public void setFocus() {
		callLv.requestFocus();
	}

	public boolean onSelectUp(View srcView) {
		setFocus();
		return true;
	}
	
	public boolean onBackDown(View srcView) {
		this.mHostView.focusTabBar();
		return true;
	}
	
	private PhoneEventListAdapter generateListAdapter() {
		ContentResolver cr = mContext.getContentResolver();

		Uri allCalls = Uri.parse(PhoneLogProvider.CONTENT_URI + "/calls");
		Cursor c = cr.query(allCalls, null, null, null, null);
		ArrayList<Bundle> calls = PhoneLogProvider.cursorToBundleList(c);

		return new PhoneEventListAdapter(mContext, calls);
	}
	
	public void setPhoneConnectionState(boolean connected) {
		phoneConnected = connected;
	}

	public void setData(ArrayList<Bundle> calls) {
		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) callLv.getAdapter();
		adapter.clear();
		for (Bundle b : calls) {
			adapter.add(b);
		}
		adapter.notifyDataSetChanged();
	}

	/*
	 * private void sendMessage(String msg) { Intent myi = new Intent();
	 * myi.setAction("RECON_SMARTPHONE_CONNECTION_MESSAGE");
	 * myi.putExtra("message", msg);
	 * 
	 * mContext.sendBroadcast(myi); Log.v("ReconPhone", "Sent: " + msg); }
	 */
/*
	OnKeyListener mKeyListener = new OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
					&& event.getAction() == KeyEvent.ACTION_UP) {
				hostView.focusTabBar();
				return true;
			}
			return false;
		}
	};
	*/
	OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if(phoneConnected) {
				final Bundle pcb = (Bundle) arg0.getItemAtPosition(arg2);
				
				String msg = CALL_INIT_BEGIN + "<num>" + pcb.getString("source") + "</num>";
				msg += CALL_INIT_END;
				
				Intent myi = new Intent();
				myi.setAction("RECON_SMARTPHONE_CONNECTION_MESSAGE");
				myi.putExtra("message", msg);
				mContext.sendBroadcast(myi);
				
				/* Create pop up view */
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View instructions = inflater.inflate(R.layout.dialog_call, null);
				
				//Hide buttons
				(instructions.findViewById(R.id.buttonBar)).setVisibility(View.GONE);
				
				//Set text
				Typeface tf = Typeface.createFromAsset(mContext.getAssets(), "fonts/Eurostib.ttf");
				TextView title = (TextView) instructions.findViewById(R.id.dialogTitle);
				title.setText("OUTGOING CALL");
				title.setTypeface(tf);
				TextView dialog = (TextView) instructions.findViewById(R.id.callFromText);
				if(pcb.getString("contact").equals("Unknown")) {
					dialog.setText(pcb.getString("source"));
				} else {
					dialog.setText(pcb.getString("contact"));
				}
				dialog.setTypeface(tf);
				
				// toast the popup
				Toast toastView = new Toast(mContext);
				toastView.setView(instructions);
				toastView.setDuration(Toast.LENGTH_LONG);
				toastView.setGravity(Gravity.FILL, 0,0);
				toastView.show();
			}
		}

	};
}
