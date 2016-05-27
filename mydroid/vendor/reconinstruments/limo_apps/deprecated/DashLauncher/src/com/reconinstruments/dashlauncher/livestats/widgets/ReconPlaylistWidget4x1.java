package com.reconinstruments.dashlauncher.livestats.widgets;

import java.util.Timer;
import java.util.TimerTask;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.livestats.widgets.MarqueeViewSingle;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class ReconPlaylistWidget4x1 extends ReconDashboardWidget {

	private MarqueeViewSingle fieldTextView;
	private TextView nothingTextView;
	private long lastUpdate;
	//private Timer playlistTimeout;
	
	//private TimerTask timeout = new TimerTask() {
	//	public void run() {
	//		fieldTextView.setText("--");
	//	}
	//};
	
	public ReconPlaylistWidget4x1(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_playlist_4x1, null);
        //playlistTimeout = new Timer();
        //playlistTimeout.schedule(timeout, 10000);
        this.addView(myView);
        
        prepareInsideViews();
	}


	public ReconPlaylistWidget4x1(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_playlist_4x1, null);
       // playlistTimeout = new Timer();
       //playlistTimeout.schedule(timeout, 10000);
        this.addView(myView);
        
        prepareInsideViews();
	}

	public ReconPlaylistWidget4x1(Context context) {
		super(context);
		LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_playlist_4x1, null);
      //  playlistTimeout = new Timer();
      //  playlistTimeout.schedule(timeout, 10000);
        this.addView(myView);
        
        prepareInsideViews();
    }

	
	@Override
	/* Shows new song info, and resets if it hasn't received info in 10000ms */
	public void updateInfo(Bundle fullinfo) {
	    if (fullinfo == null ) {
		return;
	    }
		String music = fullinfo.getString("CURRENT_SONG");
		
		if (music != null) lastUpdate = SystemClock.elapsedRealtime();
		
		// Only update text if there is new text (to prevent flickering)
		if (music != null && music != fieldTextView.getText()) {
			Log.v("PlaylistWidget", music);
			fieldTextView.reset();
			fieldTextView.setText1(music);
			fieldTextView.startMarquee();
			fieldTextView.setVisibility(View.VISIBLE);
			nothingTextView.setVisibility(View.GONE);
		}
		
		if (music == null) {
			fieldTextView.reset();
			fieldTextView.setText1("--");
			fieldTextView.setVisibility(View.INVISIBLE);
			nothingTextView.setVisibility(View.VISIBLE);
		}
	}


	@Override
	public void prepareInsideViews() {
		fieldTextView = (MarqueeViewSingle) findViewById(R.id.song_field);
		nothingTextView = (TextView) findViewById(R.id.nothing_text);
//		nothingTextView.setTypeface(FontSingleton.getInstance(getContext()).getTypeface());
		fieldTextView.setText1("--");
//		fieldTextView.setTypeface(livestats_widget_typeface);
		fieldTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 29);
		
		fieldTextView.setVisibility(View.INVISIBLE);
		nothingTextView.setVisibility(View.VISIBLE);
	}

}
