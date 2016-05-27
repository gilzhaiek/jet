package com.reconinstruments.phone;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.TextView;


public class PhoneActivity extends FragmentActivity {
	/** Called when the activity is first created. */
	private static final String TAG = "PhoneActivity";
	
	private Gallery tabGallery;
	private Fragment[] listFragments;
	private FrameLayout fragmentFrame;
	private String[] tabNames = {"Calls", "Messages"};
	private int selection = 0;
	
	// TODO: jar this shit
	public static final int TYPE_PHONE = 1;
	public static final int TYPE_SMS = 2;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list_gallery);
		
		tabGallery = (Gallery) findViewById(R.id.tab_gallery);
		tabGallery.setFocusable(false);
		tabGallery.setFocusableInTouchMode(false);
		/*tabGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.v(TAG, "arg2: " + arg2);
				if(selection != arg2)
					setSelection(arg2, selection);
				
				selection = arg2;
			}
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});*/
		tabGallery.setAdapter(new StringAdapter(this, tabNames));
		
		tabGallery.setEnabled(false);

		Log.d("PhoneActivity", "onCreate");
		
		listFragments = new Fragment[] {new CallsListFragment(),new MessagesListFragment()};
		

		if(getIntent().hasExtra("Notification")){
			int notificationType = getIntent().getIntExtra("Notification",0);
			switch(notificationType){
			case TYPE_PHONE:
				selection = 0;
				break;
			case TYPE_SMS:
				selection = 1;
				break;
			}
		}
		
		tabGallery.setSelection(selection);
		setSelection(selection, -1);
		
		
		//mPhoneLogContentObserver = new PhoneLogContentObserver(new Handler());
		//ContentResolver cr = getContentResolver();
		//cr.registerContentObserver(PhoneLogProvider.CONTENT_URI, true, mPhoneLogContentObserver);
	}
	
	public void setSelection(int position, int prevPosition) {
		Log.v(TAG, "position: " + position + ", prev: " + prevPosition);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_NONE);
		
		if(prevPosition != -1) {
			//ft.setCustomAnimations(R.anim.fadein, R.anim.fadeout);
			ft.remove(listFragments[prevPosition]);
		}

		ft.add(R.id.details, listFragments[position]);		
		ft.commit();	
		
		selection = position;
	}
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyUp: " + keyCode);
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			setSelection(0,selection);
			tabGallery.setSelection(0,true);
			return true;
			
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			setSelection(1,selection);
			tabGallery.setSelection(1,true);
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}
	class StringAdapter extends BaseAdapter {
	    /** The context your gallery is running in (usually the activity) */
	    private Context mContext;
	    private final String[] strings;
	    public StringAdapter(Context c, String[] strings) {
	        this.mContext = c;
	        this.strings = strings;
	    }
	    public int getCount() {
	        return 2;
	    }
	    public Object getItem(int position) {
	        return strings[position];
	    }
	    public long getItemId(int position) {
	        return position;
	    }
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			TextView tabTV = (TextView) li.inflate(R.layout.gallery_tab, null);
			tabTV.setText(strings[position]);
			tabTV.setFocusable(false);
			tabTV.setFocusableInTouchMode(false);

			tabTV.setEnabled(false);
			//tabTV.setVisibility(View.GONE);
			
			return tabTV;
		}
	}
}