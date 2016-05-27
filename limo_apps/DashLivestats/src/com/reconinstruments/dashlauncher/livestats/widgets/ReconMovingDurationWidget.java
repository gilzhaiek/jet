package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public abstract class ReconMovingDurationWidget extends ReconDashboardWidget {

	private static final String TAG = ReconMovingDurationWidget.class.getSimpleName();

	public ReconMovingDurationWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconMovingDurationWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconMovingDurationWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("SPORTS_ACTIVITY_BUNDLE");
		long value = tempBundle.getLong("Durations");
		float fontSize = fieldTextView.getTextSize();
		if(value > 0l) {
	        SimpleDateFormat df = null;
	        if(value < 60*60*1000){ // <1h
	            df = new SimpleDateFormat("mm:ss");
	            adjustDurationLayoutAndFontSize(value);
	        }else if(value < 10*60*60*1000){ // < 10h
	            df = new SimpleDateFormat("H:mm:ss");
	            adjustDurationLayoutAndFontSize(value);
	        }else{ // >=10h
	            df = new SimpleDateFormat("HH:mm:ss");
	            adjustDurationLayoutAndFontSize(value);
	        }
	        df.setTimeZone(TimeZone.getTimeZone("GMT"));
			fieldTextView.setText(df.format(value));
		} else {
			fieldTextView.setText(Html.fromHtml(((inActivity) ? "00:00" : "<font color=\"#808181\">00:00</font>")));
			adjustDurationLayoutAndFontSize(0);
		}
		fieldTextView.invalidate();
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
 	}
	
	private void adjustDurationLayoutAndFontSize(long value){
	    android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
	            LayoutParams.MATCH_PARENT,      
	            LayoutParams.MATCH_PARENT
        );
        if(value < 60*60*1000){ // <1h
            if(this instanceof ReconMovingDurationWidget6x4) {
                fieldTextView.setTextSize(116);
            }else if(this instanceof ReconMovingDurationWidget3x4) {
                params.setMargins(-10, 0, -10, 0);
                fieldTextView.setLayoutParams(params);
                fieldTextView.setTextSize(70);
            }else{
                params.setMargins(0, 20, 0, 0);
                fieldTextView.setLayoutParams(params);
                fieldTextView.setTextSize(58);
            }
        }else if(value < 10*60*60*1000){ // < 10h
            if(this instanceof ReconMovingDurationWidget6x4) {
                fieldTextView.setTextSize(100);
            }else if(this instanceof ReconMovingDurationWidget3x4) {
                params.setMargins(-10, 0, -10, 0);
                fieldTextView.setLayoutParams(params);
                fieldTextView.setTextSize(52);
            }else{
                params.setMargins(0, 24, 0, 0);
                fieldTextView.setLayoutParams(params);
                fieldTextView.setTextSize(50);
            }
        }else{ // >=10h
            if(this instanceof ReconMovingDurationWidget6x4) {
                fieldTextView.setTextSize(90);
            } else if(this instanceof ReconMovingDurationWidget3x4) {
                params.setMargins(-10, 0, -10, 0);
                fieldTextView.setLayoutParams(params);
                fieldTextView.setTextSize(44);
            } else{
                params.setMargins(0, 20, 0, 0);
                fieldTextView.setLayoutParams(params);
                fieldTextView.setTextSize(44);
            }
        }
	}

}
