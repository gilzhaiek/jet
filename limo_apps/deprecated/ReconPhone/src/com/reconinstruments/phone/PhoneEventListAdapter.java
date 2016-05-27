package com.reconinstruments.phone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.reconinstruments.applauncher.phone.PhoneLogProvider;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This adapter generates the views used by the SideTabWidget AdapterView.
 */
public class PhoneEventListAdapter extends ArrayAdapter<Bundle> {

	/**
	 * Constructor for the adapter.
	 * 
	 * @param context the application context
	 * @param items an arraylist of SideTabItems for which views will be created.
	 */
	public PhoneEventListAdapter(final Context context, final ArrayList<Bundle> items) {
		super(context, 0, items);
	}

	/**
	 * Generates a view for a given position.
	 * 
	 * @param position the position in the data set.
	 * @param convertView an old view to re-use.
	 * @param parent parent view to be attached to.
	 */
	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {

		/** Get the data for the current item. */
		Bundle thisItem = getItem(position);

		/** Validate the existing view. */
		View view = null;
		
		view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_small, null);

		/** Get the TextView child and set its text. */
		final TextView topText = (TextView) view.findViewById(R.id.toptext);
		final TextView timeText = (TextView) view.findViewById(R.id.datetext);

		String mContact = thisItem.getString("contact");
		String mSource = thisItem.getString("source");
		String mBody = thisItem.getString("body");
		Date mDate = new Date(thisItem.getLong("date"));
		
		Log.v("PhoneEventListAdapter", mContact 
				+ "," + mSource + "," + mBody + "," 
				+ (thisItem.getInt("type") == PhoneLogProvider.TYPE_CALL ? "call" : "sms") + "," 
				+ thisItem.getBoolean("replied"));
		
		try {

			if (thisItem.getBoolean("missed") && thisItem.getInt("type") == PhoneLogProvider.TYPE_CALL)
				topText.setTextColor(0xFFFF0000);
			else if(thisItem.getBoolean("replied") && thisItem.getInt("type") == PhoneLogProvider.TYPE_SMS)
				topText.setTextColor(0xFF64B446);
			
			/* Set typeface */
			Typeface tf = Typeface.createFromAsset(this.getContext().getAssets(), "fonts/Eurostib.ttf");
			topText.setTypeface(tf);
			timeText.setTypeface(tf);
			
			/* Show contact or their number if unknown */
			if(mContact == null || mContact.equals("Unknown")) 
				topText.setText(mSource);
			else 
				topText.setText(mContact);

			/* Set the date */
			SimpleDateFormat d = new SimpleDateFormat("h:mm aa", Locale.US);
			SimpleDateFormat d2 = new SimpleDateFormat("MM.dd.yy", Locale.US);
			SimpleDateFormat d3 = new SimpleDateFormat("EE", Locale.US);

			Calendar cal = Calendar.getInstance();
			Date currentDate = cal.getTime();
			long currentDateInMillis = currentDate.getTime();
			long eventDateInMillis = mDate.getTime();
			
			int diffInDays = (int) Math.floor((currentDateInMillis - eventDateInMillis) / (24 * 60 * 60 * 1000));

			if (diffInDays == 0) {
				timeText.setText(d.format(mDate));
			} else if (diffInDays == 1) {
				timeText.setText("Yesterday");
			} else if (diffInDays <= 7) {
				timeText.setText(d3.format(mDate));
			} else {
				timeText.setText(d2.format(mDate));
			}

		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		return view;
	}
}
