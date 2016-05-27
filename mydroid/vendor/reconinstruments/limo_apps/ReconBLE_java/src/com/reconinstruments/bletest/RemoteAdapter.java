package com.reconinstruments.bletest;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.reconinstruments.nativetest.R;

/**
 * 
 * @author David Lee
 *
 */
public class RemoteAdapter extends ArrayAdapter<RemoteItem>{

	Context context = null;
	int resId;
	ArrayList<RemoteItem> remotes = null;


	public RemoteAdapter(Context context, int resId, ArrayList<RemoteItem> remotes) {
		super(context, resId, remotes);

		this.context = context;
		this.remotes = remotes;
	}

	@Override
	public RemoteItem getItem(int position) {
		return remotes.get(position);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent){

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		convertView = inflater.inflate(R.layout.remote_item, null);
		RemoteItem item = remotes.get(position);
		TextView title;
		
		if (item.title != null){
			title = (TextView) convertView.findViewById(R.id.remote_text);
			title.setText(item.title);
		}

		return convertView;
	}

}