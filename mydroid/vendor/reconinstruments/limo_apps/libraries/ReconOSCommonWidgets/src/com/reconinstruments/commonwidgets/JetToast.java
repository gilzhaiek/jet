package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * <code>JetToast</code> is a new toast class for JET. It covers the length of
 * the screen. You work with it the same way as you work with regular
 * <code>Toast</code>s.
 * 
 */
public class JetToast extends Toast {
	RelativeLayout jetToast;
	TextView toastText;
	LinearLayout jetToastMessage;
	Animation slideInBottom, fade_in;
	ImageView jetToast_image;
	public final static int NO_IMAGE = 0;

	public JetToast(Context context, int resId, String text) {
		super(context);
		// Setup the toast view to be inflated and inflate it
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.jet_toast, null);

		// Load animations to be used
		slideInBottom = AnimationUtils.loadAnimation(context,
				R.anim.slide_in_toast);
		fade_in = AnimationUtils.loadAnimation(context, R.anim.fadein);

		// Initialize the toast's layout components
		if (resId != NO_IMAGE) {
			jetToast_image = (ImageView) layout
					.findViewById(R.id.jetToast_image);
			jetToast_image.setVisibility(View.VISIBLE);
			jetToast_image.setImageResource(resId);
		}
		RelativeLayout jetToast = (RelativeLayout) layout
				.findViewById(R.id.jetToast);
		LinearLayout jetToastMessage = (LinearLayout) layout
				.findViewById(R.id.jetToast_message);
		TextView toastText = (TextView) layout.findViewById(R.id.jetToast_text);
		toastText.setText(text);
		this.setGravity(Gravity.FILL, 0, 0);
		this.setDuration(LENGTH_SHORT);

		// set the view layout
		setView(layout);
		jetToastMessage.startAnimation(slideInBottom);
	}

}