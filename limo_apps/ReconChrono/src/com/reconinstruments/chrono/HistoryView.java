package com.reconinstruments.chrono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.reconinstruments.applauncher.transcend.ReconChronoManager;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryView extends Activity {
	
	private static final String TAG = "HistoryView";
	
	private HistoryTabView mTabView;
	
	/** Latest infoBundle **/
	private Bundle mInfoBundle = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mInfoBundle = getIntent().getExtras().getBundle("CHRONO_BUNDLE");
		
		if(mInfoBundle == null) {
			finish();
			return;
		}
		
		mTabView = new HistoryTabView(this);
		mTabView.createTabPages(mInfoBundle);
		setContentView(mTabView);
	}
	
	class HistoryTabView extends TabView {

		ArrayList<TabPage> tabPages = new ArrayList<TabPage>();
		
		public HistoryTabView(Context context) {
			super(context);
			//createTabPages();
		}
		
		public void createTabPages(Bundle b) {
			Context context = this.getContext();
			Resources r = context.getResources();
			
			Drawable chronoIconReg = r.getDrawable(R.drawable.tab_chrono_grey);
			Drawable chronoIconSelected = r.getDrawable(R.drawable.tab_chrono_white);
			
			ArrayList<Bundle> trials = mInfoBundle.getParcelableArrayList("Trials");
			for(int i=trials.size() - 1; i >= 0; i--) {
				TabPage t = new TrialTabPage(context, Integer.toString(i+1), this, trials.get(i));
				t.setFocusable(true);
				tabPages.add(t);
			}
			
			// Other init stuff
			this.setTabPages(tabPages);
			this.focusTabBar();
		}
		
		public boolean onSelectUp(View srcView) {
			return tabPages.get(this.mSelectedTabIdx).onSelectUp(srcView);
		}
		
	}
	
	class TrialTabPage extends TabPage {
		private View previewView, lapsView;
		private TextView pTrialTimeTextView, pTrialTimeMsTextView, pNumTrialsTextView, pStartEndTextView; // previewView child views
		private TextView lTrialTimeTextView, lTrialTimeMsTextView; // lapsView child views
		private ListView lLapsListView;
		
		public TrialTabPage(Context context, String tabTxt, TabView hostView, Bundle trialInfo) {
			super(context, tabTxt, hostView);
			
			createPreviewView(trialInfo);
			createLapsView(trialInfo);

			lapsView.setVisibility(View.GONE);
			this.addView(previewView);
			this.addView(lapsView);
			this.setFocusable(true);
		}

		@Override
		public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
			super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
			
			Log.d("TrialTabPage", "gainFocus: " + gainFocus);
			
			if(gainFocus) {
				lapsView.setVisibility(View.VISIBLE);
				previewView.setVisibility(View.GONE);
			
				lLapsListView.requestFocus();
			}
		}
		
		private void createPreviewView(Bundle b) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			previewView = inflater.inflate(R.layout.view_trial, null);
			
			pTrialTimeTextView = (TextView) previewView.findViewById(R.id.trial_time);
			pTrialTimeMsTextView = (TextView) previewView.findViewById(R.id.trial_time_ms);
			pNumTrialsTextView = (TextView) previewView.findViewById(R.id.number_of_trials);
			pStartEndTextView = (TextView) previewView.findViewById(R.id.start_end_time);
			
			// Set typeface
			FontSingleton font = FontSingleton.getInstance(this.getContext());
			pTrialTimeTextView.setTypeface(font.getTypeface());
			pTrialTimeMsTextView.setTypeface(font.getTypeface());
			pNumTrialsTextView.setTypeface(font.getTypeface());
			pStartEndTextView.setTypeface(font.getTypeface());
			
			// Set elapsed time
			pTrialTimeTextView.setText(ReconChronoManager.parseElapsedTime(b.getLong("ElapsedTime"), true));
			pTrialTimeMsTextView.setText(ReconChronoManager.parseElapsedTime(b.getLong("ElapsedTime"), false));
			
			// Count and set lap number
			String lapsText = Integer.toString(b.getParcelableArrayList("Laps").size()) + " Lap";
			if (b.getParcelableArrayList("Laps").size() > 1)
				lapsText += "s";
			pNumTrialsTextView.setText(lapsText);
			
			// Count and set start and end time
			Date startTime = new Date(b.getLong("StartTime"));
			Date endTime = new Date(b.getLong("EndTime"));
			if(DateFormat.is24HourFormat(this.getContext())) {
				pStartEndTextView.setText(DateFormat.format("k:mm", startTime) + " - " + DateFormat.format("k:mm", endTime));
			} else {
				pStartEndTextView.setText(DateFormat.format("h:mmaa", startTime) + " - " + DateFormat.format("h:mmaa", endTime));
			}
			if(!b.getBoolean("HasRun")) {
				pStartEndTextView.setVisibility(View.GONE);
			}
		}
		
		private void createLapsView(Bundle b) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			lapsView = inflater.inflate(R.layout.view_trial_laps, null);
			
			lTrialTimeTextView = (TextView) lapsView.findViewById(R.id.trial_time);
			lTrialTimeMsTextView = (TextView) lapsView.findViewById(R.id.trial_time_ms);
			lLapsListView = (ListView) lapsView.findViewById(R.id.laps_list);
			
			// Set typeface
			FontSingleton font = FontSingleton.getInstance(this.getContext());
			lTrialTimeTextView.setTypeface(font.getTypeface());
			lTrialTimeMsTextView.setTypeface(font.getTypeface());
			
			// Set elapsed time
			lTrialTimeTextView.setText(ReconChronoManager.parseElapsedTime(b.getLong("ElapsedTime"), true));
			lTrialTimeMsTextView.setText(ReconChronoManager.parseElapsedTime(b.getLong("ElapsedTime"), false));
			
			// Parse laps and display them
			ArrayList<Bundle> lapsBundleList = b.getParcelableArrayList("Laps");
			List<Lap> lapsList = new ArrayList<Lap>();
			int i = 1;
			for(Bundle lapBundle : lapsBundleList) {
				String elapsedTime = ReconChronoManager.parseElapsedTime(lapBundle.getLong("ElapsedTime"), true);
				elapsedTime += ReconChronoManager.parseElapsedTime(lapBundle.getLong("ElapsedTime"), false);
				lapsList.add(new Lap(i, elapsedTime));
				i++;
			}
			lapsList.get(0).setSelected(true);
			
			LapArrayAdapter mLapArrayAdapter = new LapArrayAdapter(this.getContext(), lapsList);
			lLapsListView.setAdapter(mLapArrayAdapter);
			lLapsListView.setFocusable(true);
			lLapsListView.setOnFocusChangeListener(new OnFocusChangeListener() {

				@Override
				public void onFocusChange(View arg0, boolean hasFocus) {
					if(!hasFocus) {
						lapsView.setVisibility(View.GONE);
						previewView.setVisibility(View.VISIBLE);
					}
				}
				
			});
			
			lLapsListView.setOnKeyListener(new OnKeyListener() {

				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
						return TrialTabPage.this.onBackDown(v);
					}
					return false;
				}
				
			});
			
			lapsView.setFocusable(true);
		}
		
		public boolean onSelectUp(View srcView) {
			this.requestFocus();
			return true;
		}
		
		public boolean onBackDown(View srcView) {
			this.mHostView.focusTabBar();
			return true;
		}
	}
}
