package com.reconinstruments.tabexample;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class TestTabView extends TabView {

	private Context mContext;
	private ArrayList<TabPage> tabPages = new ArrayList<TabPage>(3);
	
	public TestTabView(Context context) {
		super(context);
		
		mContext = context;
		
		Resources r = mContext.getResources();
		
		Drawable phoneGrey = r.getDrawable(R.drawable.call_icon_grey);
		Drawable phoneWhite = r.getDrawable(R.drawable.call_icon_white);
		Drawable phoneBlue = r.getDrawable(R.drawable.call_icon_blue);
		
		Drawable smsGrey = r.getDrawable(R.drawable.sms_icon_grey);
		Drawable smsWhite = r.getDrawable(R.drawable.sms_icon_white);
		Drawable smsBlue = r.getDrawable(R.drawable.sms_icon_blue);

		tabPages.add(new TestTabPage1(mContext, phoneGrey, phoneBlue, phoneWhite, this));
		tabPages.add(new TestTabPage2(mContext, smsGrey, smsBlue, smsWhite, this, "TEST LIST 1"));
		tabPages.add(new TestTabPage1(mContext, phoneGrey, phoneBlue, phoneWhite, this));
		tabPages.add(new TestTabPage2(mContext, smsGrey, smsBlue, smsWhite, this, "TEST LIST 2"));
		tabPages.add(new TestTabPage2(mContext, smsGrey, smsBlue, smsWhite, this, "TEST LIST 3"));
		
		this.setTabPages(tabPages);
	}
	
	public boolean onSelectUp(View srcView) {
		return tabPages.get(mSelectedTabIdx).onSelectUp(srcView);
	}
	
	public boolean onRightArrowUp(View srcView) {
		return tabPages.get(mSelectedTabIdx).onRightArrowUp(srcView);
	}
	
	// This TabPage isn't selectable itself, but when there is an attempt
	// to select it, a new activity is launched.
	class TestTabPage1 extends TabPage {
	
		private View layout;
		
		private TestTabPage1(Context context, Drawable iconRegular,
				Drawable iconSelected, Drawable iconFocused, TabView hostView) {
			super(context, iconRegular, iconSelected, iconFocused, hostView);
			
			LayoutInflater inflater = LayoutInflater.from(context);
			layout = inflater.inflate(R.layout.main, null);
			this.addView(layout);
		}
		
		public boolean onSelectUp(View srcView) {
			launchActivity();
			return true;
		}
		
		public boolean onRightArrowUp(View srcView) {
			launchActivity();
			return true;
		}
		
		private void launchActivity() {
			mContext.startActivity(new Intent(mContext, PopupActivity.class));
		}
    	
    }
	
	// This TabPage has a list
	class TestTabPage2 extends TabPage {
		
		private View layout;
		private ListView mListView;
		private ArrayAdapter<String> mAdapter;
		
		public TestTabPage2(Context context, Drawable iconRegular,
				Drawable iconSelected, Drawable iconFocused, TabView hostView, String listName) {
			super(context, iconRegular, iconSelected, iconFocused, hostView);
			
			LayoutInflater inflater = LayoutInflater.from(context);
			layout = inflater.inflate(R.layout.listview_layout, null);
			
			TextView titleView = (TextView) layout.findViewById(R.id.list_title);
			titleView.setText(listName);
			
			
			/* Creating the list */
			mListView = (ListView) layout.findViewById(R.id.menu_list);
			
			ArrayList<String> list = new ArrayList<String>();
			list.add("One");
			list.add("Two");
			list.add("Three");
			mAdapter = new TestArrayAdapter(context, 0, list);
			mListView.setAdapter(mAdapter);
			
			// capture the lists onKey events and pass them to the TabView
			// this is a very common issue to handle in order to get the UI
			// to behave properly
			mListView.setOnKeyListener(new OnKeyListener() {

				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
						// Call the tabPages back down method
						return TestTabPage2.this.onBackDown(v);
					}
					return false;
				}
				
			});
			
			this.addView(layout);
			this.setFocusable(true); // makes the contents of this TabPage focusable
		}
		
		@Override
		public void setFocus() {
			mListView.requestFocus();
		}
		
		public boolean onSelectUp(View srcView) {
			setFocus();
			return true;
		}
		
		public boolean onBackDown(View srcView) {
			this.mHostView.focusTabBar();
			return true;
		}

    }
	
	class TestArrayAdapter extends ArrayAdapter<String> {

		public TestArrayAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
		}
		
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View view = LayoutInflater.from(getContext()).inflate(R.layout.list_item, null);
			
			TextView tv = (TextView) view.findViewById(R.id.toptext);
			tv.setText(this.getItem(position));
			
			return view;
		}
	}
}
