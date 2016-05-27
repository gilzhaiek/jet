package com.reconinstruments.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TestActivtiy extends Activity
{
	Intent next_intent; 
	
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu_list);
		final ListView listView = (ListView) findViewById(R.id.sampleList);
		
		String[] values = new String[] { "Graphical View Pager", "Quick Options Menu",
				"Toast Tester", "Quickstart Guide"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, values);
		//next_intent = new Intent(this, SampleViewPagerActivity.class);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				int itemPosition = position;
				String itemValue = (String) listView
						.getItemAtPosition(position);
				Log.v("list view", "Selected: "+itemValue);
				launchNextActivity(itemValue);
			}
		});
	}
	
	private void launchNextActivity(String itemValue){
		if(itemValue.equals("Graphical View Pager")){
			next_intent = new Intent(this, ScreenSlidePagerActivity.class);
			startActivity(next_intent);
		}
	}
}
