package com.reconinstruments.aqualung;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * @author Patrick Cho
 *
 */
public class SettingAdapter extends ArrayAdapter<SettingItem>{

	Context context = null;
	int resId;
	ArrayList<SettingItem> settings = null;


	public SettingAdapter(Context context, int resId, ArrayList<SettingItem> settings) {
		super(context, resId, settings);

		this.context = context;
		this.resId = resId;
		this.settings = settings;
	}

	@Override
	public SettingItem getItem(int position) {
		return settings.get(position);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent){

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		convertView = inflater.inflate(R.layout.setting_item, null);

		SettingItem item = settings.get(position);
		
		TextView title;
		TextView subTitle;
		CheckBox checkBox;
		ImageView checkMark;
		
		if (item.title != null){
			title = (TextView) convertView.findViewById(R.id.setting_text);
			title.setText(item.title);
			title.setTextColor(title.getTextColors().withAlpha(item.titleAlpha));
			if (item.titleAlpha == 100)
				convertView.setFocusable(false);
		}
		if (item.subTitle != null){
			subTitle = (TextView) convertView.findViewById(R.id.setting_subtext);
			subTitle.setText(item.subTitle);
			subTitle.setVisibility(View.VISIBLE);
		}
		if (item.checkBox){
			checkBox = (CheckBox) convertView.findViewById(R.id.setting_checkbox);
			checkBox.setVisibility(View.VISIBLE);
			checkBox.setChecked(item.checkBoxValue);
		}
		if (item.checkMark){
			checkMark = (ImageView) convertView.findViewById(R.id.setting_checkmark);
			checkMark.setVisibility(View.VISIBLE);
		}


		return convertView;
	}

}