package com.reconinstruments.commonwidgets;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * 
 * <code>JetListAdapter</code> is designed to support jet style common animation
 * and select/deselect common logic
 * 
 */
public abstract class JetListAdapter extends ArrayAdapter<JetListItemHolder> {

	public static final int ANIMATION_DURATOIN = 30;
	private List<JetListItemHolder> holders;

	public JetListAdapter(Context context, int resource, List objects) {
		super(context, resource, objects);
		holders = objects;
	}

	// to deal with select/disselect logic
	public void setSelected(int position) {
		for (int i = 0; i < holders.size(); i++) {
			JetListItemHolder holder = holders.get(i);
			if (i != position) {
				holder.selected = false;
			} else {
				holder.selected = true;
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View row = (TextView) super
				.getView(position, convertView, parent);
		// to implement the jet style common animation
		if (((JetListItemHolder) getItem(position)).selected) {
			row.animate().setDuration(ANIMATION_DURATOIN).x(+20);
		} else {
			row.animate().setDuration(ANIMATION_DURATOIN).x(+0);
		}
		return row;
	}

}
