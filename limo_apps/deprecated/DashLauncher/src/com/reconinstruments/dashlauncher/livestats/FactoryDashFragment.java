package com.reconinstruments.dashlauncher.livestats;

import android.util.Log;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.reconinstruments.dashlauncher.ConversionUtil;
import com.reconinstruments.dashlauncher.MODServiceConnection;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.ReconSettingsUtil;
import com.reconinstruments.modservice.ReconMODServiceMessage;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FactoryDashFragment extends Fragment {

    // Debug: Ali
    private static final String TAG = "FactoryDashFragment";
    //End debug
	public static final int PRECONFIG_STYLE_1 = 0;
	public static final int PRECONFIG_STYLE_2 = 1;
	public static final int PRECONFIG_STYLE_3 = 2;
	
	// Bit mask
	private static final int HAS_SPEED 			= 1;
	private static final int HAS_ALTITUDE		= 4;
	private static final int HAS_DISTANCE		= 8;
	private static final int HAS_AIR 			= 16;
	private static final int HAS_TEMP 			= 32;
	private static final int HAS_SPEED_RADAR 	= 64;
	private static final int HAS_VERT			= 128;
	
	// Describes the widgets each style has so that we know what to update
	private static final int[] STYLE_FIELD = new int[] {
		HAS_SPEED | HAS_SPEED_RADAR, 
		HAS_SPEED | HAS_ALTITUDE, // (e.g. PRECONFIG_STYLE_2 has speed & altitude widgets
		HAS_SPEED | HAS_AIR | HAS_VERT
	};
	
	private static final int RADAR_INCREMENT = 5; // Each ring on the radar represents an additional 5km/h
    private static final int MAX_NUMBER_OF_RADAR_LINES = 24;//FIXME:Ali: should be 25 but crashes it seems that some 25th asset is missign somewhere or something
	
	
	//private int style = PRECONFIG_STYLE_3; // Hack: put back to zero to see why hack
	
	private TextView speedTV, maxSpeedTV, speedUnitTV, altTV, altUnitTV, airTimeTV, verticalTV, verticalUnitTV;
	private View radarView, radarGreenView;
	
	private MODServiceConnection mMODConnection;
	private Messenger mMODConnectionMessenger;
	
	// Current drawable ints of radar, useful for knowing what transitions to use.
	int radarGreyState = 0;
	int radarGreenState = 0;
    
    public FactoryDashFragment() { // Empty constructor
	// Ali: Apparently all extensions of fragment need this
	// otherwise the system may ocasioanlly crash. e.g. when
	// running for the first time as the home screen.
    	super();
    }

    public static FactoryDashFragment newInstance(int dashStyle) {
    	FactoryDashFragment c = new FactoryDashFragment();
		
		// Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("dashStyle", dashStyle);
        c.setArguments(args);

        return c;
	}
	
	public void onResume() {
		super.onResume();
		
		// Set up connection to MOD Service
        mMODConnection = new MODServiceConnection(this.getActivity().getApplicationContext());
        mMODConnectionMessenger = new Messenger(new MODServiceHandler());
        mMODConnection.addReceiver(mMODConnectionMessenger);
        mMODConnection.doBindService();
	}
	
	public void onPause() {
		super.onPause();
		
    	mMODConnection.doUnBindService();
	}
	
	public int getStyle() {
		int style = getArguments() != null ? getArguments().getInt("dashStyle") : PRECONFIG_STYLE_3;
		//Log.v(TAG, "getStyle(), style: " + style);
		return style;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;
		int style = getStyle();
		
		// inflate correct view
		switch(style) {
		case PRECONFIG_STYLE_1:
			v = inflater.inflate(R.layout.livestats_dash_layout_1up, null);
			break;
		
		case PRECONFIG_STYLE_2:
			v = inflater.inflate(R.layout.livestats_dash_layout_2up, null);
			break;
		
		case PRECONFIG_STYLE_3:
			v = inflater.inflate(R.layout.livestats_dash_layout_3up, null);
			break;
			
		default:
			v = inflater.inflate(R.layout.placeholder, null);
		}
		
		// init fields
		if(HAS_SPEED == (STYLE_FIELD[style] & HAS_SPEED)) {
			speedTV = (TextView) v.findViewById(R.id.speed_value);
			speedUnitTV = (TextView) v.findViewById(R.id.speed_unit);
			maxSpeedTV = (TextView) v.findViewById(R.id.max_speed_value);
		}
		
		if(HAS_ALTITUDE == (STYLE_FIELD[style] & HAS_ALTITUDE)) {
			altTV = (TextView) v.findViewById(R.id.alt_value);
			altUnitTV = (TextView) v.findViewById(R.id.alt_unit);
		}
		
		if(HAS_SPEED_RADAR == (STYLE_FIELD[style] & HAS_SPEED_RADAR)) {
			radarView = v.findViewById(R.id.radar);
			radarGreenView = v.findViewById(R.id.radarGreen);
		}
		
		if(HAS_VERT == (STYLE_FIELD[style] & HAS_VERT)) {
			verticalTV = (TextView) v.findViewById(R.id.vertical_value);
			verticalUnitTV = (TextView) v.findViewById(R.id.vertical_unit);
		}
		
		if(HAS_AIR == (STYLE_FIELD[style] & HAS_AIR)) {
			airTimeTV = (TextView) v.findViewById(R.id.air_value);
		}
		
		return v;
	}
	
	public void updateFields(Bundle fullInfoBundle) {
	    if (this.getActivity() == null) {
		// HACK: Ali. It means we are not ready to update fields
		// This happens at the very begining
		return;
	    }
		boolean isMetric = ReconSettingsUtil.getUnits(this.getActivity()) == ReconSettingsUtil.RECON_UINTS_METRIC;
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		
		int style = getStyle();
		
		// SPEED
		if(HAS_SPEED == (STYLE_FIELD[style] & HAS_SPEED)) {
			Bundle spdBundle = (Bundle) fullInfoBundle.get("SPEED_BUNDLE");
			float spd = spdBundle.getFloat("Speed");
			float maxspd = spdBundle.getFloat("MaxSpeed");
			

			
			if(spd > -1)
				speedTV.setText(isMetric ? df.format(spd) : df.format(ConversionUtil.kmsToMiles(spd)));
			else
				speedTV.setText(getString(R.string.invalid_value));
			
			if(maxspd > -1)
				speedUnitTV.setText(isMetric ? getString(R.string.kmh): getString(R.string.mph));
			else
				speedUnitTV.setText(getString(R.string.invalid_value));
			
			maxSpeedTV.setText(isMetric ? df.format(maxspd) : df.format(ConversionUtil.kmsToMiles(maxspd)));
		}
		
		// ALTITUDE
		if(HAS_ALTITUDE == (STYLE_FIELD[style] & HAS_ALTITUDE)) {
			Bundle altBundle = (Bundle) fullInfoBundle.get("ALTITUDE_BUNDLE");
			float alt = altBundle.getFloat("Alt");
			boolean isUncertain = altBundle.getInt("HeightOffsetN") < 500;
			if(isUncertain) {
			    altTV.setText("...");
			}
			else {
			    altTV.setText(isMetric ? df.format(alt) : df.format(ConversionUtil.metersToFeet((double) alt)));
			}
			altUnitTV.setText(isMetric ? getString(R.string.meters) : getString(R.string.feet));
		}
		
		// SPEED RADAR
		if(HAS_SPEED_RADAR == (STYLE_FIELD[style] & HAS_SPEED_RADAR)) {
			Bundle spdBundle = (Bundle) fullInfoBundle.get("SPEED_BUNDLE");
			
			int spd = (int) (spdBundle.getFloat("Speed") / RADAR_INCREMENT);
			int maxspd = (int) (spdBundle.getFloat("MaxSpeed") / RADAR_INCREMENT);
			spd = (int) (spdBundle.getFloat("Speed") / RADAR_INCREMENT);
			spd = (int) (spdBundle.getFloat("Speed") / RADAR_INCREMENT);
			spd = (spd > MAX_NUMBER_OF_RADAR_LINES)?
			    MAX_NUMBER_OF_RADAR_LINES:spd;
			maxspd = (maxspd > MAX_NUMBER_OF_RADAR_LINES)?
			    MAX_NUMBER_OF_RADAR_LINES:maxspd;
			//////////////////////////////////
			// Do grey radar (speed)
			//////////////////////////////////
			Drawable greyRadar = getResources().getDrawable(radarDrawable[spd]);
			if(radarGreyState > 0 && spd < maxspd && radarGreyState != radarDrawable[spd]) {
				// Only draw grey rings up beneath the current maxspeed
				greyRadar = new TransitionDrawable(new Drawable[] {
						getResources().getDrawable(radarGreyState),
						getResources().getDrawable(radarDrawable[spd])
				});
				((TransitionDrawable) greyRadar).setCrossFadeEnabled(true);
			}
			radarView.setBackgroundDrawable(greyRadar);

			//////////////////////////////////
			// Do green radar (max speed)
			//////////////////////////////////
			if (maxspd > 0) { // Hack? Ali: MaxSpeed may
					  // be zero. Don't know how
					  // it might be handled to
					  // bound it by iff statement
			    // Log.d(TAG,"maxspd is "+maxspd);
			    //David Lee: check by manually putting maxspd to your 

			    int greenRadarDrawable = radarGreenDrawable[maxspd];			
			
			    if(spd == maxspd) { 
				// If we are at our max speed, use heavy green glow
				greenRadarDrawable = radarLargeGreenDrawable[maxspd];
			    } else if(spd == maxspd - 1) {
				// If we are close to our max speed, show mild green glow
				greenRadarDrawable = radarSmallGreenDrawable[maxspd];
			    }
			
			    Drawable greenRadar = getResources().getDrawable(greenRadarDrawable);
			    if(radarGreenState > 0 && greenRadarDrawable != radarGreenState) {
				greenRadar = new TransitionDrawable(new Drawable[] {
					getResources().getDrawable(radarGreenState),
					getResources().getDrawable(greenRadarDrawable)
				    });
				((TransitionDrawable) greenRadar).setCrossFadeEnabled(true);
			    }
			
			    // Do backgrounds
			    radarView.setBackgroundDrawable(greyRadar);
			    radarGreenView.setBackgroundDrawable(greenRadar);
			    if(greyRadar instanceof TransitionDrawable)
				((TransitionDrawable) greyRadar).startTransition(600);
			    if(greenRadar instanceof TransitionDrawable)
				((TransitionDrawable) greenRadar).startTransition(600);
			
			    // Save states
			    radarGreyState = radarDrawable[spd];
			    radarGreenState = greenRadarDrawable;
			}
			
		}
		
		// VERTICAL
		if(HAS_VERT == (STYLE_FIELD[style] & HAS_VERT)) {
			Bundle vrtBundle = (Bundle) fullInfoBundle.get("VERTICAL_BUNDLE");
			float vrt = vrtBundle.getFloat("Vert");
			
			verticalTV.setText(df.format(isMetric ? vrt : ConversionUtil.metersToFeet(vrt)));
			verticalUnitTV.setText(getString(isMetric ? R.string.meters : R.string.feet));
		}
		
		// AIR
		if(HAS_AIR == (STYLE_FIELD[style] & HAS_AIR)) {
			Bundle jumpBundle = (Bundle) fullInfoBundle.get("JUMP_BUNDLE");
			ArrayList<Bundle> jumpBundles = jumpBundle.getParcelableArrayList("Jumps");
			
			if(jumpBundles.isEmpty()) {
				airTimeTV.setText(getString(R.string.invalid_value));
			} else {
				Bundle jump = jumpBundles.get(jumpBundles.size() - 1);
				Double time = ((double) jump.getInt("Air")) / 1000;
				
				DecimalFormat dfAir = new DecimalFormat();
				dfAir.setMaximumFractionDigits(1);
				dfAir.setMinimumFractionDigits(1);
				
				airTimeTV.setText(dfAir.format(time));
			}
		}
	}
	
	class MODServiceHandler extends Handler {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case ReconMODServiceMessage.MSG_RESULT:
				if (msg.arg1 == ReconMODServiceMessage.MSG_GET_FULL_INFO_BUNDLE) {
					Bundle data = msg.getData();
					
					updateFields(data);
				}
				break;

			default:
				super.handleMessage(msg);
			}
		}
    }
	
	int[] radarDrawable = new int[] {
			R.drawable.radar_1,
			R.drawable.radar_2,
			R.drawable.radar_3,
			R.drawable.radar_4,
			R.drawable.radar_5,
			R.drawable.radar_6,
			R.drawable.radar_7,
			R.drawable.radar_8,
			R.drawable.radar_9,
			R.drawable.radar_10,
			R.drawable.radar_11,
			R.drawable.radar_12,
			R.drawable.radar_13,
			R.drawable.radar_14,
			R.drawable.radar_15,
			R.drawable.radar_16,
			R.drawable.radar_17,
			R.drawable.radar_18,
			R.drawable.radar_19,
			R.drawable.radar_20,
			R.drawable.radar_21,
			R.drawable.radar_22,
			R.drawable.radar_23,
			R.drawable.radar_24,
			R.drawable.radar_25
	};
	
	int[] radarGreenDrawable = new int[] {
			R.drawable.radar_1_green,
			R.drawable.radar_2_green,
			R.drawable.radar_3_green,
			R.drawable.radar_4_green,
			R.drawable.radar_5_green,
			R.drawable.radar_6_green,
			R.drawable.radar_7_green,
			R.drawable.radar_8_green,
			R.drawable.radar_9_green,
			R.drawable.radar_10_green,
			R.drawable.radar_11_green,
			R.drawable.radar_12_green,
			R.drawable.radar_13_green,
			R.drawable.radar_14_green,
			R.drawable.radar_15_green,
			R.drawable.radar_16_green,
			R.drawable.radar_17_green,
			R.drawable.radar_18_green,
			R.drawable.radar_19_green,
			R.drawable.radar_20_green,
			R.drawable.radar_21_green,
			R.drawable.radar_22_green,
			R.drawable.radar_23_green,
			R.drawable.radar_24_green,
			R.drawable.radar_25_green
	};
	
	int[] radarSmallGreenDrawable = new int[] {
			R.drawable.radar_1_smallgreen,
			R.drawable.radar_2_smallgreen,
			R.drawable.radar_3_smallgreen,
			R.drawable.radar_4_smallgreen,
			R.drawable.radar_5_smallgreen,
			R.drawable.radar_6_smallgreen,
			R.drawable.radar_7_smallgreen,
			R.drawable.radar_8_smallgreen,
			R.drawable.radar_9_smallgreen,
			R.drawable.radar_10_smallgreen,
			R.drawable.radar_11_smallgreen,
			R.drawable.radar_12_smallgreen,
			R.drawable.radar_13_smallgreen,
			R.drawable.radar_14_smallgreen,
			R.drawable.radar_15_smallgreen,
			R.drawable.radar_16_smallgreen,
			R.drawable.radar_17_smallgreen,
			R.drawable.radar_18_smallgreen,
			R.drawable.radar_19_smallgreen,
			R.drawable.radar_20_smallgreen,
			R.drawable.radar_21_smallgreen,
			R.drawable.radar_22_smallgreen,
			R.drawable.radar_23_smallgreen,
			R.drawable.radar_24_smallgreen,
			R.drawable.radar_25_smallgreen
	};
	
	int[] radarLargeGreenDrawable = new int[] {
			R.drawable.radar_1_largegreen,
			R.drawable.radar_2_largegreen,
			R.drawable.radar_3_largegreen,
			R.drawable.radar_4_largegreen,
			R.drawable.radar_5_largegreen,
			R.drawable.radar_6_largegreen,
			R.drawable.radar_7_largegreen,
			R.drawable.radar_8_largegreen,
			R.drawable.radar_9_largegreen,
			R.drawable.radar_10_largegreen,
			R.drawable.radar_11_largegreen,
			R.drawable.radar_12_largegreen,
			R.drawable.radar_13_largegreen,
			R.drawable.radar_14_largegreen,
			R.drawable.radar_15_largegreen,
			R.drawable.radar_16_largegreen,
			R.drawable.radar_17_largegreen,
			R.drawable.radar_18_largegreen,
			R.drawable.radar_19_largegreen,
			R.drawable.radar_20_largegreen,
			R.drawable.radar_21_largegreen,
			R.drawable.radar_22_largegreen,
			R.drawable.radar_23_largegreen,
			R.drawable.radar_24_largegreen,
			R.drawable.radar_25_largegreen
	};
}
