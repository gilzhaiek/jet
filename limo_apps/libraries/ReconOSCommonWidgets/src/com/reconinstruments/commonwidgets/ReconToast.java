package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ReconToast extends Toast {
	public final static int NO_IMAGE = 0;
	
	public ReconToast(Context context, int resId, String text) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.recon_toast, null);
		if(resId != NO_IMAGE){
			ImageView myImage = (ImageView) layout.findViewById(R.id.image);
			myImage.setVisibility(View.VISIBLE);
			myImage.setImageResource(resId);
		}
		TextView textView = (TextView) layout.findViewById(R.id.text);
		textView.setText(text);
		setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		if(text.endsWith("...")){
			layout.setPadding(15,5,15,3);
		}else{
			layout.setPadding(15,5,20,3);
		}
		setView(layout);
		//		setDuration(30000); // 3 sec
	}
    public ReconToast(Context context, String text) {
	this(context, NO_IMAGE, text);
    }
}