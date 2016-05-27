package com.reconinstruments.stats.tabs;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.reconstats.R;
import com.reconinstruments.stats.TranscendServiceConnection;
import com.reconinstruments.stats.util.FontSingleton;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class PreferencesTabPage extends TabPage {

	private Context mContext;
	private PrefView mPrefView;

	public PreferencesTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);

		mContext = context;

		createPrefView();
		this.addView(mPrefView);
		this.setFocusable(true);
	}

	private void createPrefView() {
		mPrefView = new PrefView(mContext);

		mPrefView.setTitle("PREFERENCES");
		mPrefView.setTitleColor(0xffffffff);
		mPrefView.setHostTabPage(this);

		ArrayList options = new ArrayList(2);

		PrefView.Pref pref = mPrefView.new Pref("Reset Trip Stats",
				"Last Reset: ");
		options.add(pref);

		pref = mPrefView.new Pref("Reset All Time Stats", "Last Reset: ");
		options.add(pref);

		mPrefView.addPrefs(options);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.reconinstruments.widgets.TabPage#setFocus() When this view gains
	 * focus, set the focus to the preference list
	 */
	@Override
	public void setFocus() {
		mPrefView.setFocus();
	}
	
	@Override
	public boolean onLeftArrowUp(View srcView) {
		this.mHostView.focusTabBar();
		return true;
	}

	/*
	 * Legacy code from previous version of Stats App
	 * Handles the selectable list for the preferences tab
	 */
	class PrefView extends FrameLayout {
		public class Pref {
			String mPrefAction;
			String mPrefDesc;

			public Pref(String title, String desc) {
				mPrefAction = title;
				mPrefDesc = desc;
			}
		}

		protected ListView mPrefItemList = null;
		protected TextView mTitleLabel = null;

		protected PrefAdapter mPrefAdapter = null;

		protected ArrayList<Object> mPrefs = null;

		protected TabPage mHostTab = null;

		static final String STAT_PREF_FILE = "ReconStatsPref";
		static final String STAT_PREF_RESET_TRIP_TIME = "ResetTripTime";
		static final String STAT_PREF_RESET_ALL_TIME = "ResetAllTime";

		/*
		 * private class for defining an ArrayAdapter that has it own view of
		 * list item
		 */
		private class PrefAdapter extends ArrayAdapter<Object> {
			private ArrayList<Object> mStatItems;

			public PrefAdapter(Context context, int resource,
					int textViewResourceId, ArrayList<Object> statItems) {
				super(context, resource, textViewResourceId,
						(ArrayList<Object>) statItems);

				mStatItems = statItems;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					// create a new view from the poiCategoryitem_layout
					LayoutInflater inflater = (LayoutInflater) this
							.getContext().getSystemService(
									Context.LAYOUT_INFLATER_SERVICE);
					v = inflater.inflate(R.layout.pref_item_layout, null);

					TextView nameView = (TextView) v
							.findViewById(R.id.pref_title);
					TextView valueView = (TextView) v
							.findViewById(R.id.pref_desc);

					nameView.setTypeface(FontSingleton.getInstance(mContext)
							.getTypeface());
					valueView.setTypeface(FontSingleton.getInstance(mContext)
							.getTypeface());

				}

				TextView nameView = (TextView) v.findViewById(R.id.pref_title);
				TextView valueView = (TextView) v.findViewById(R.id.pref_desc);

				String date = null;

				if (position == 0) {
					date = getSettingString(STAT_PREF_RESET_TRIP_TIME, "Never");
				} else {
					date = getSettingString(STAT_PREF_RESET_ALL_TIME, "Never");
				}

				Pref pref = (Pref) mPrefs.get(position);
				nameView.setText(pref.mPrefAction);
				valueView.setText(pref.mPrefDesc + date);

				return v;

			}

		}

		public PrefView(Context context) {
			super(context);

			// create the view with the default view resource
			initView(context, R.layout.stat_view_2);
		}

		public PrefView(Context context, AttributeSet attr) {
			super(context, attr);

			// create the view with the default view resource
			initView(context, R.layout.stat_view_2);
		}

		public PrefView(Context context, int viewResourceID) {
			super(context);

			initView(context, viewResourceID);
		}

		private void initView(Context context, int viewResourceID) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View statView = inflater.inflate(viewResourceID, null);
			this.addView(statView);

			mPrefItemList = (ListView) statView
					.findViewById(R.id.stat_item_list);

			mTitleLabel = (TextView) statView
					.findViewById(R.id.stat_view_title);
			mTitleLabel.setTypeface(FontSingleton.getInstance(mContext)
					.getTypeface());

			mPrefItemList.setItemsCanFocus(true);
			mPrefItemList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			mPrefItemList.setDivider(null);
			mPrefItemList.setDividerHeight(0);
			mPrefItemList.setFocusable(true);

			mPrefItemList.setOnKeyListener(this.mListViewKeyListener);
			mPrefItemList.setOnItemClickListener(this.mItemClickListener);

		}

		public void setTitleColor(int clr) {
			mTitleLabel.setTextColor(clr);
		}

		public void setTitle(String title) {
			mTitleLabel.setText(title);
		}

		public void addPrefs(ArrayList<Object> stats) {
			mPrefs = stats;

			mPrefAdapter = new PrefAdapter(this.getContext(),
					R.layout.stat_item_layout, R.id.stat_item_text, stats);
			mPrefItemList.setAdapter(mPrefAdapter);
		}

		public void setFocus() {
			mPrefItemList.requestFocus();
		}

		public void setHostTabPage(TabPage tabPage) {
			mHostTab = tabPage;
		}

		private View.OnKeyListener mListViewKeyListener = new View.OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				// intercept the back key from the listView
				// and translate it to cancel the menu view
				Log.v("PrefList", "onKey()");
				if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
					Log.v("PrefList", "left");
					if (event.getAction() == KeyEvent.ACTION_UP && mHostTab != null) {
						Log.v("PrefList", "passing to host tab");
						mHostTab.onLeftArrowUp(mPrefItemList);
						return true;
					}
					return true;
				}
				return false;
			}
		};

		private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Hide the menu view, then handle the menu-item selection
				// action
				// mOverlayManager.setOverlayView(null);
				if (position == 0) {
					// TODO: handle reset trip stats
					// Util.resetStats();
					TranscendServiceConnection.getInstance(mContext).resetStats();
					
					// save the reset time to SharedPreferences
					String format = "yyyy-MM-dd hh:mm:ss";
					String timeStr = (String) android.text.format.DateFormat
							.format(format, new java.util.Date());
					setSettingString(STAT_PREF_RESET_TRIP_TIME, timeStr);

				} else {
					// TODO: handle reset all time stats
					// Util.resetAllTimeStats();
					TranscendServiceConnection.getInstance(mContext).resetAllTimeStats();

					String format = "yyyy-MM-dd hh:mm:ss";
					String timeStr = (String) android.text.format.DateFormat
							.format(format, new java.util.Date());
					setSettingString(STAT_PREF_RESET_ALL_TIME, timeStr);

				}

			}

		};

		private String getSettingString(String keyName, String defValue) {
			SharedPreferences settings = this.getContext()
					.getSharedPreferences("LauncherPrefs",
							android.content.Context.MODE_WORLD_READABLE);
			return settings.getString(keyName, defValue);
		}

		private void setSettingString(String keyName, String value) {
			SharedPreferences settings = this.getContext()
					.getSharedPreferences("LauncherPrefs",
							android.content.Context.MODE_WORLD_WRITEABLE);

			SharedPreferences.Editor editor = settings.edit();
			editor.putString(keyName, value);
			editor.commit();
		}
	}
}
