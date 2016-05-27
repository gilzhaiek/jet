package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public abstract class ReconDashboardWidget extends FrameLayout {

	//public Typeface recon_typeface;

	public ReconDashboardWidget(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
//		recon_typeface = FontSingleton.getInstance(context).getTypeface();
	}

	public ReconDashboardWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
//		recon_typeface = FontSingleton.getInstance(context).getTypeface();
	}

	public ReconDashboardWidget(Context context) {
		super(context);
//		recon_typeface = FontSingleton.getInstance(context).getTypeface();
	}

	public abstract void updateInfo(Bundle fullInfo);

	public abstract void prepareInsideViews();

	public final static ReconDashboardWidget spitReconDashboardWidget(int id,
			Context c) {
		// id argument is usually retrieve by running a widget name through
		// WidgegtHashmap("widgetName");

		switch (id) {
		case ReconDashboardHashmap.ALT_2x2:
			return new ReconAltWidget2x2(c);
			
		case ReconDashboardHashmap.ALT_4x1:
			return new ReconAltWidget4x1(c);

		case ReconDashboardHashmap.VRT_4x1:
			return new ReconVrtWidget4x1(c);

		case ReconDashboardHashmap.VRT_2x2:
			return new ReconVrtWidget2x2(c);

		case ReconDashboardHashmap.SPD_4x2:
			return new ReconSpeedWidget4x2(c);

		case ReconDashboardHashmap.SPD_4x3:
			return new ReconSpeedWidget4x3(c);

		case ReconDashboardHashmap.SPD_4x4:
			return new ReconSpeedWidget4x4(c);

		case ReconDashboardHashmap.SPD_6x4:
			return new ReconSpeedWidget6x4(c);

		case ReconDashboardHashmap.PLY_4x1:
			return new ReconPlaylistWidget4x1(c);
			
		case ReconDashboardHashmap.DST_4x1:
			return new ReconDistanceWidget4x1(c);

		case ReconDashboardHashmap.DST_2x2:
			return new ReconDistanceWidget2x2(c);
			
		case ReconDashboardHashmap.AIR_4x1:
			return new ReconAirWidget4x1(c);

		case ReconDashboardHashmap.AIR_2x2:
			return new ReconAirWidget2x2(c);
			
		case ReconDashboardHashmap.MAXSPD_4x1:
			return new ReconMaxSpeedWidget4x1(c);

		case ReconDashboardHashmap.MAXSPD_2x2:
			return new ReconMaxSpeedWidget2x2(c);
			
		case ReconDashboardHashmap.TMP_4x1:
			return new ReconTemperatureWidget4x1(c);
			
		case ReconDashboardHashmap.TMP_2x2:
			return new ReconTemperatureWidget2x2(c);
			
		case ReconDashboardHashmap.CHR_4x1:
			return new ReconChronoWidget4x1(c);
		
		case ReconDashboardHashmap.HR_2x2:
			return new ReconHRWidget2x2(c);
			
		case ReconDashboardHashmap.HR_4x1:
			return new ReconHRWidget4x1(c);
		}
		return null;
	}

}
