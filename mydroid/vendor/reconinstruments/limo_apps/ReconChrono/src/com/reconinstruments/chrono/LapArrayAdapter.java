package com.reconinstruments.chrono;

import java.util.List;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class LapArrayAdapter extends ArrayAdapter<Lap> {

	private final List<Lap> list;
	private final Context context;

	public LapArrayAdapter(Context context, List<Lap> list) {
		super(context, R.layout.row_lap_layout, list);
		this.context = context;
		this.list = list;
	}
	
	static class ViewHolder {
		protected TextView lapNum;
		protected TextView lapTime;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		if (convertView == null) {
			LayoutInflater inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflator.inflate(R.layout.row_lap_layout, null);
			final ViewHolder viewHolder = new ViewHolder();

			viewHolder.lapNum = (TextView) view.findViewById(R.id.lap_num);
			viewHolder.lapTime = (TextView) view.findViewById(R.id.lap_time);
			
			// Set font
			FontSingleton font = FontSingleton.getInstance(context);
			viewHolder.lapNum.setTypeface(font.getTypeface());
			viewHolder.lapTime.setTypeface(font.getTypeface());
			
			view.setTag(viewHolder);
		} else {
			view = convertView;
		}
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.lapNum.setText("Lap " + Integer.toString(list.get(position).getLapNumber()));
		holder.lapTime.setText(list.get(position).getLapTime());
		return view;
	}

}