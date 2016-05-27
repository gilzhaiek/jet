package com.example.jetlistviewtester;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.reconinstruments.commonwidgets.JetListView;

/**
 * 
 * <code>MainActivity</code> is designed to demonstrate how to use jet style
 * list view to support forward/backward swipe feature.
 * 
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// in layout.activity_main, the listview has to be declared to
		// com.reconinstruments.commonwidgets.JetListView
		setContentView(R.layout.activity_main);

		final JetListView listview = (JetListView) findViewById(R.id.listview);
		String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
				"Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
				"Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
				"OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
				"Android", "iPhone", "WindowsMobile" };

		final ArrayList<ListItemHolder> list = new ArrayList<ListItemHolder>();
		for (int i = 0; i < values.length; ++i) {
			ListItemHolder holder = new ListItemHolder();
			holder.value = values[i];
			if (i == 0) {
				holder.selected = true;
			}
			list.add(holder);
		}
		final MainListAdapter adapter = new MainListAdapter(this,
				android.R.layout.simple_list_item_1, list);
		listview.setAdapter(adapter);

		// override setOnItemSelectedListener to set selected item.
		listview.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				adapter.setSelected(arg2);
				adapter.notifyDataSetChanged();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing here
			}

		});

		// override setOnItemClickListener to implement the swipe action.
		// swipping has been mapped to KeyEvent.KEYCODE_DPAD_CENTER or
		// KeyEvent.KEYCODE_BACK
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(MainActivity.this,
						"Jet ListView selected: " + position,
						Toast.LENGTH_SHORT).show();
			}

		});
	}
}
