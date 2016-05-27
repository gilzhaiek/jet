package com.example.jetlistviewtester;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.JetListAdapter;

/**
 * 
 * <code>MainListAdapter</code> is designed to demonstrate how to use the base
 * class <code>JetListAdapter</code> to implement the specify animation when the
 * list item has been selected and/or deselected.
 * 
 */
public class MainListAdapter extends JetListAdapter {

	public MainListAdapter(Context context, int resource, List objects) {
		super(context, resource, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final TextView row = (TextView) super.getView(position, convertView,
				parent);
		row.setText(((ListItemHolder) getItem(position)).value);
		//to implement the specify UI_related animation
		if (((ListItemHolder) getItem(position)).selected) {
			new Handler().postDelayed(new Runnable() {
				public void run() {
					row.setTypeface(null, Typeface.BOLD);
				}
			}, JetListAdapter.ANIMATION_DURATOIN);
		} else {
			new Handler().postDelayed(new Runnable() {
				public void run() {
					row.setTypeface(null, Typeface.NORMAL);
				}
			}, JetListAdapter.ANIMATION_DURATOIN);
		}
		return row;
	}
}
