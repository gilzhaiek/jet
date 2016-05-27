package com.reconinstruments.dashlauncher.livestats;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class FactoryDashFragment extends Fragment implements IDashFragment {

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
		//FIXME Jump/Air Time will not be ready for Sonw2 3.1, HAS_AIR is removed for temporarily
		HAS_SPEED | HAS_ALTITUDE  | HAS_VERT // HAS_AIR 
	};

	private static final int RADAR_INCREMENT = 5; // Each ring on the radar represents an additional 5km/h
	private static final int MAX_NUMBER_OF_RADAR_LINES = 24;//FIXME:Ali: should be 25 but crashes it seems that some 25th asset is missign somewhere or something


	//private int style = PRECONFIG_STYLE_3; // Hack: put back to zero to see why hack

	private TextView speedTV, maxSpeedTV, speedUnit1TV,speedUnit2TV, altTV, maxAltTV, altUnit1TV, altUnit2TV, airTimeTV, verticalTV, verticalUnitTV;
	//private View radarView, radarGreenView;
	private DialView dialView;
	private boolean isDial = false;

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
			speedUnit1TV = (TextView) v.findViewById(R.id.speed_unit1);
			speedUnit2TV = (TextView) v.findViewById(R.id.speed_unit2);
			maxSpeedTV = (TextView) v.findViewById(R.id.max_speed_value);
		}

		if(HAS_ALTITUDE == (STYLE_FIELD[style] & HAS_ALTITUDE)) {
			altTV = (TextView) v.findViewById(R.id.alt_value);
			altUnit1TV = (TextView) v.findViewById(R.id.alt_unit1);
			altUnit2TV = (TextView) v.findViewById(R.id.alt_unit2);
			if(getStyle() == PRECONFIG_STYLE_2){
				maxAltTV = (TextView) v.findViewById(R.id.max_alt_value);
			}
		}

		if(HAS_SPEED_RADAR == (STYLE_FIELD[style] & HAS_SPEED_RADAR)) {

			dialView = (DialView) v.findViewById(R.id.dial);
			//			radarView = v.findViewById(R.id.radar);
			//			radarGreenView = v.findViewById(R.id.radarGreen);
		}

		if(HAS_VERT == (STYLE_FIELD[style] & HAS_VERT)) {
			verticalTV = (TextView) v.findViewById(R.id.vertical_value);
			verticalUnitTV = (TextView) v.findViewById(R.id.vertical_unit);
		}

		//FIXME Jump/Air Time will not be ready for Sonw2 3.1, HAS_AIR is removed for temporarily
//		if(HAS_AIR == (STYLE_FIELD[style] & HAS_AIR)) {
//			airTimeTV = (TextView) v.findViewById(R.id.air_value);
//		}

		return v;
	}

	public void updateFields(Bundle fullInfoBundle, boolean inActivity) {
		if (this.getActivity() == null) {
			// HACK: Ali. It means we are not ready to update fields
			// This happens at the very begining
			return;
		}
		boolean isMetric = ReconSettingsUtil.getUnits(this.getActivity()) == ReconSettingsUtil.RECON_UNITS_METRIC;
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setGroupingUsed(false);

		int style = getStyle();

		// SPEED RADAR
		if(HAS_SPEED_RADAR == (STYLE_FIELD[style] & HAS_SPEED_RADAR)) {
			
			Bundle spdBundle = (Bundle) fullInfoBundle.get("SPEED_BUNDLE");

			float spd = (spdBundle.getFloat("Speed"));
			float maxSpd = (spdBundle.getFloat("MaxSpeed"));

			dialView.setCurrentVal(
					       (float) ((spd < 0) ? -1 : isMetric ? spd : ConversionUtil.kmsToMiles(spd)),
					       isMetric ? "km/h" : "mph"
					       );

			if (maxSpd > 0) {
				dialView.setMaxVal(
						   (float) (isMetric ? maxSpd : ConversionUtil.kmsToMiles(maxSpd)),
						   isMetric ? "km/h" : "mph"
						   );
 			}

			return;
		}

		// SPEED
		if(HAS_SPEED == (STYLE_FIELD[style] & HAS_SPEED)) {
			Bundle spdBundle = (Bundle) fullInfoBundle.get("SPEED_BUNDLE");
			float spd = spdBundle.getFloat("Speed");
			float maxspd = spdBundle.getFloat("MaxSpeed");



			if(spd > -1)
				speedTV.setText(isMetric ? df.format(spd) : df.format(ConversionUtil.kmsToMiles(spd)));
			else
				speedTV.setText(getString(R.string.invalid_value));

				speedUnit1TV.setText(isMetric ? getString(R.string.kmh): getString(R.string.mph));
				speedUnit2TV.setText(isMetric ? getString(R.string.kmh): getString(R.string.mph));
			if(maxspd > -1){
				maxSpeedTV.setText(isMetric ? df.format(maxspd) : df.format(ConversionUtil.kmsToMiles(maxspd)));
			}else{
				maxSpeedTV.setText(getString(R.string.invalid_value));
			}

			
		}

		// ALTITUDE
		if(HAS_ALTITUDE == (STYLE_FIELD[style] & HAS_ALTITUDE)) {
			Bundle altBundle = (Bundle) fullInfoBundle.get("ALTITUDE_BUNDLE");
			float alt = altBundle.getFloat("Alt");
			float maxAlt = altBundle.getFloat("MaxAlt");
			boolean isUncertain = altBundle.getInt("HeightOffsetN") < 500;
			if(isUncertain) {
				altTV.setText(getString(R.string.invalid_value));
			}
			else {
				altTV.setText(isMetric ? df.format(alt) : df.format(ConversionUtil.metersToFeet((double) alt)));
			}
			if(getStyle() == PRECONFIG_STYLE_2){
				altUnit1TV.setText(isMetric ? getString(R.string.meters_abr) : getString(R.string.feet_abr));
				altUnit2TV.setText(isMetric ? getString(R.string.meters_abr) : getString(R.string.feet_abr));
				if(Float.compare(maxAlt, -10000f) != 0){ //FIXME use ReconAltitudeManager.INVALID_ALT instead
					maxAltTV.setText(isMetric ? df.format(maxAlt) : df.format(ConversionUtil.metersToFeet((double) maxAlt)));
				}else{
					maxAltTV.setText(getString(R.string.invalid_value));
				}
			}else{
				altUnit2TV.setText(isMetric ? getString(R.string.meters_abr) : getString(R.string.feet_abr));
			}
			
		}

		// VERTICAL
		if(HAS_VERT == (STYLE_FIELD[style] & HAS_VERT)) {
			Bundle vrtBundle = (Bundle) fullInfoBundle.get("VERTICAL_BUNDLE");
			float vrt = vrtBundle.getFloat("Vert");

			verticalTV.setText(isMetric ? df.format(vrt) : df.format(ConversionUtil.metersToFeet((double)vrt)));
			if(getStyle() == PRECONFIG_STYLE_2){
				verticalUnitTV.setText(isMetric ? getString(R.string.meters_abr) : getString(R.string.feet_abr));
			}else{
				verticalUnitTV.setText(isMetric ? getString(R.string.meters_abr) : getString(R.string.feet_abr));
			}
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




}
