package com.reconinstruments.jump.tabs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.jump.FontSingleton;
import com.reconinstruments.jump.R;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class BestDistanceTabPage extends ReconTabPage {
	
	public BestDistanceTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView, "BEST DISTANCE");
		lastJumpNumberTextView.setVisibility(View.GONE);
	}
}
