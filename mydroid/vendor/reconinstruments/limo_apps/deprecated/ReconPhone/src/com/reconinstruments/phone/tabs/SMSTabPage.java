package com.reconinstruments.phone.tabs;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.applauncher.phone.PhoneLogProvider;
import com.reconinstruments.phone.DisplaySMSActivity;
import com.reconinstruments.phone.PhoneEventListAdapter;
import com.reconinstruments.widgets.*;

public class SMSTabPage extends TabPage {

	private Context mContext;
	
	ListView smsLv;
	TabView hostView;
	View layout;
	
	public SMSTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		this.hostView = hostView;
		mContext = context;
		
		/* Set up view */
		LayoutInflater inflater = LayoutInflater.from(context);
		layout = inflater.inflate(R.layout.default_lv, null);
		TextView titleView = (TextView) layout.findViewById(R.id.list_title);
		titleView.setText("TEXT MESSAGES");
		this.addView(layout);
		this.setFocusable(true);
		
		/* Set up list */
		PhoneEventListAdapter smsAdapter = generateListAdapter();
		smsLv = (ListView) layout.findViewById(R.id.menu_list);
		smsLv.setOnItemClickListener(mOnItemClickListener);
		smsLv.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
				if(keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
					return onBackDown(v);
				}
				return false;
			}
			
		});
		smsLv.setAdapter(smsAdapter);
		
		Typeface tf = Typeface.createFromAsset(this.getContext().getAssets(), "fonts/Eurostib.ttf");
		titleView.setTypeface(tf);
	}
	
	@Override
	public void setFocus() {
		smsLv.requestFocus();
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
		
		Uri allSMS = Uri.parse(PhoneLogProvider.CONTENT_URI+"/sms");
		Cursor c = cr.query(allSMS, null, null, null, null);
		
		ArrayList<Bundle> sms = PhoneLogProvider.cursorToBundleList(c);
		
		return new PhoneEventListAdapter(mContext, sms);
	}
	
	public void setData(ArrayList<Bundle> sms) {
		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) smsLv.getAdapter();
		adapter.clear();
		for (Bundle b : sms) {
			adapter.add(b);
		}
		adapter.notifyDataSetChanged();
	}
	

	OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			final Bundle pcb = (Bundle) arg0.getItemAtPosition(arg2);
			
			if (pcb.getInt("type") == PhoneLogProvider.TYPE_SMS) {
				Intent i = new Intent(mContext, DisplaySMSActivity.class);
				//SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa");
				i.putExtra("source", pcb.getString("source"));
				i.putExtra("body", pcb.getString("body"));
				i.putExtra("contact", pcb.getString("contact"));
				i.putExtra("time", pcb.getLong("date"));
				i.putExtra("uri", pcb.getString("uri"));
				mContext.startActivity(i);
			}
		}

	};
	
}
