package com.jinkim.textviewautofit;

import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.reconinstruments.commonwidgets.AutoTextView;

public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public AutoTextView myFlexibleText;
		float textSize;
		String testStrings[];
		int count = 0;
		int windowWidth = 0, windowHeight = 0;
		boolean changeLayout = false;

		public PlaceholderFragment() {
			testStrings = new String[4];
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			LinearLayout linearLayout = (LinearLayout) inflater.inflate(
					R.layout.fragment_main, container, false);
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			myFlexibleText = new AutoTextView(getActivity());
			myFlexibleText.setLayoutParams(params);
			myFlexibleText.setText(R.string.gibberish_brief);
			myFlexibleText.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					changeText(v);
				}
			});
			myFlexibleText.setClickable(true);
			myFlexibleText.setFocusable(true);
			linearLayout.addView(myFlexibleText);
			return linearLayout;
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState){
			super.onActivityCreated(savedInstanceState);
			Resources res = getActivity().getResources();
			testStrings[0] = res.getString(R.string.gibberish_brief);
			testStrings[1] = res.getString(R.string.gibberish_short);
			testStrings[2] = res.getString(R.string.gibberish_medium);
			testStrings[3] = res.getString(R.string.gibberish_full);
			
			Point windowSize = new Point();
			getActivity().getWindowManager().getDefaultDisplay().getSize(windowSize);
			windowWidth = windowSize.x;
			windowHeight = windowSize.y;
			
			myFlexibleText.requestLayout();
		}

		public void changeText(View view) {
			AutoTextView temp = (AutoTextView) view;
			temp.setText(testStrings[(++count) % 4]);
		}
	}

	

}
