package com.reconinstruments.commonwidgets;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TwoOptionsAdapter extends ArrayAdapter<TwoOptionsItem>{

	Context context = null;
	ArrayList<TwoOptionsItem> items = null;


	public TwoOptionsAdapter(Context context, ArrayList<TwoOptionsItem> items) {
		super(context, R.layout.recon_two_options_item, items);
		this.context = context;
		this.items = items;
	}

	@Override
	public TwoOptionsItem getItem(int position) {
		return items.get(position);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent){

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.recon_two_options_item, null);
		TextView text = (TextView) convertView.findViewById(R.id.setting_text);
		
		text.setText(items.get(position).title);
		
		return convertView;
	}

}