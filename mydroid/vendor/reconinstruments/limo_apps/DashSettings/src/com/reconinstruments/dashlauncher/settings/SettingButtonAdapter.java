package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.dashsettings.R;

public class SettingButtonAdapter extends ArrayAdapter<SettingItem>{

	Context context = null;
	ArrayList<SettingItem> settings = null;


	public SettingButtonAdapter(Context context, ArrayList<SettingItem> settings) {
		super(context, R.layout.settings_list_item, settings);
		this.context = context;
		this.settings = settings;
	}

	@Override
	public SettingItem getItem(int position) {
		return settings.get(position);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent){

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.settings_button_item, null);
		TextView text = (TextView) convertView.findViewById(R.id.setting_text);
		
		text.setText(settings.get(position).title);
		
		return convertView;
	}

}