package com.reconinstruments.reconstats;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.reconsettings.ReconSettingsUtil;
import com.reconinstruments.reconstats.StatsActivity.StatsType;

public class StatsFragment extends Fragment {
    private static String TAG = "StatsFragment";

    private StatsType type;
    private static boolean metric;

    private static final int ROW_HEIGHT = 33;
	
    private TextView runs;

    private TextView speed;
    private TextView speedUnit;

    private TextView drop;
    private TextView dropUnit;

    private TextView dist;
    private TextView distUnit;

    private TextView alt;
    private TextView altUnit;

    private TextView air;
    private TextView airUnit;	
    private TextView distDrop;
	
    private View runsRow, speedRow, vertRow, distRow, altRow, airRow;
	
    private boolean alive = false;
	
    public StatsFragment(StatsType type){
	super();

	this.type = type;
    }

    public boolean isAlive(){
	return alive;
    }
	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			     Bundle savedInstanceState) {
	Log.d(TAG, "start to create view.");
	alive = false;
	View view = inflater.inflate(R.layout.stats_list_layout, container, false);
	final StatsScrollView scrollView = (StatsScrollView) view.findViewById(R.id.stats_scrollview);
	scrollView.requestFocus();
		
	metric = ReconSettingsUtil.getUnits(this.getActivity()) == ReconSettingsUtil.RECON_UINTS_METRIC;

	scrollView.setOnKeyListener(new OnKeyListener(){

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
		    switch(keyCode) {
		    case KeyEvent.KEYCODE_DPAD_UP:
			scrollView.post(new Runnable() {
				@Override
				public void run() {
				    scrollView.scrollBy(0, -ROW_HEIGHT);
				} 
			    });
			if (StatsActivity.currentRowPosition > 0) StatsActivity.currentRowPosition--;
			Log.v(TAG, "currentRowPosition : " + StatsActivity.currentRowPosition);
			return true;
		    case KeyEvent.KEYCODE_DPAD_DOWN:
			scrollView.post(new Runnable() {
				@Override
				public void run() {
				    scrollView.scrollBy(0, ROW_HEIGHT);
				} 
			    });
			if (StatsActivity.currentRowPosition < 6) StatsActivity.currentRowPosition++;
			Log.v(TAG, "currentRowPosition : " + StatsActivity.currentRowPosition);
			return true;
		    }
		    return false;
		}
			
	    });

	runsRow = view.findViewById(R.id.row_runs);
	TextView runLabel = (TextView) runsRow.findViewById(R.id.row_label);
	TextView runSmallLabel = (TextView) runsRow.findViewById(R.id.row_label_small);
	((TextView) runsRow.findViewById(R.id.separator)).setBackgroundResource(R.color.strip_color_run);

	speedRow = view.findViewById(R.id.row_max_speed);
	TextView speedLabel = (TextView) speedRow.findViewById(R.id.row_label);
	TextView speedSmallLabel = (TextView) speedRow.findViewById(R.id.row_label_small);
	((TextView) speedRow.findViewById(R.id.separator)).setBackgroundResource(R.color.strip_color_spd);

	vertRow = view.findViewById(R.id.row_vertical_drop);
	TextView dropLabel = (TextView) vertRow.findViewById(R.id.row_label);
	TextView dropSmallLabel = (TextView) vertRow.findViewById(R.id.row_label_small);
	((TextView) vertRow.findViewById(R.id.separator)).setBackgroundResource(R.color.strip_color_vrt);

	distRow = view.findViewById(R.id.row_distance);
	TextView distLabel = (TextView) distRow.findViewById(R.id.row_label);
	TextView distSmallLabel = (TextView) distRow.findViewById(R.id.row_label_small);
	((TextView) distRow.findViewById(R.id.separator)).setBackgroundResource(R.color.strip_color_dst);

	altRow = view.findViewById(R.id.row_max_altitude);
	TextView altLabel = (TextView) altRow.findViewById(R.id.row_label);
	TextView altSmallLabel = (TextView) altRow.findViewById(R.id.row_label_small);
	((TextView) altRow.findViewById(R.id.separator)).setBackgroundResource(R.color.strip_color_alt);

	airRow = view.findViewById(R.id.row_best_jump);
	TextView airLabel = (TextView) airRow.findViewById(R.id.row_label);
	TextView airSmallLabel = (TextView) airRow.findViewById(R.id.row_label_small);
	((TextView) airRow.findViewById(R.id.separator)).setBackgroundResource(R.color.strip_color_jmp);


	// Set all icons
	((TextView) runsRow.findViewById(R.id.row_icon)).setBackgroundResource(R.drawable.runs_icon);
	((TextView) speedRow.findViewById(R.id.row_icon)).setBackgroundResource(R.drawable.speed_icon);
	((TextView) vertRow.findViewById(R.id.row_icon)).setBackgroundResource(R.drawable.vert_icon);
	((TextView) distRow.findViewById(R.id.row_icon)).setBackgroundResource(R.drawable.distance_icon);
	((TextView) altRow.findViewById(R.id.row_icon)).setBackgroundResource(R.drawable.vert_icon);
	((TextView) airRow.findViewById(R.id.row_icon)).setBackgroundResource(R.drawable.jump_icon);


	switch (type){

	case LASTRUN:			
	    runSmallLabel.setVisibility(View.GONE);
	    runLabel.setText("RUN #");

	    speedSmallLabel.setText("MAX");
	    speedLabel.setText("SPEED");

	    dropSmallLabel.setVisibility(View.GONE);
	    dropLabel.setText("VERTICAL");

	    distSmallLabel.setVisibility(View.GONE);
	    distLabel.setText("DISTANCE");

	    altSmallLabel.setText("MAX");
	    altLabel.setText("ALTITUDE");

	    airSmallLabel.setText("LAST");
	    airLabel.setText("JUMP");
	    break;
	default:
	    runSmallLabel.setVisibility(View.GONE);
	    runLabel.setText("RUNS");

	    speedSmallLabel.setText("MAX");
	    speedLabel.setText("SPEED");

	    dropSmallLabel.setVisibility(View.GONE);
	    dropLabel.setText("VERTICAL");

	    distSmallLabel.setVisibility(View.GONE);
	    distLabel.setText("DISTANCE");

	    altSmallLabel.setText("MAX");
	    altLabel.setText("ALTITUDE");

	    airSmallLabel.setText("BEST");
	    airLabel.setText("JUMP");
	    break;

	}

	runs = (TextView) view.findViewById(R.id.row_runs).findViewById(R.id.row_value);
	TextView runsUnit = (TextView) view.findViewById(R.id.row_runs).findViewById(R.id.row_unit);
	runsUnit.setVisibility(View.GONE);


	speed = (TextView) view.findViewById(R.id.row_max_speed).findViewById(R.id.row_value);
	speedUnit = (TextView) view.findViewById(R.id.row_max_speed).findViewById(R.id.row_unit);

	drop = (TextView) view.findViewById(R.id.row_vertical_drop).findViewById(R.id.row_value);
	dropUnit = (TextView) view.findViewById(R.id.row_vertical_drop).findViewById(R.id.row_unit);

	dist = (TextView) view.findViewById(R.id.row_distance).findViewById(R.id.row_value);
	distUnit = (TextView) view.findViewById(R.id.row_distance).findViewById(R.id.row_unit);

	alt = (TextView) view.findViewById(R.id.row_max_altitude).findViewById(R.id.row_value);
	altUnit = (TextView) view.findViewById(R.id.row_max_altitude).findViewById(R.id.row_unit);

	air = (TextView) view.findViewById(R.id.row_best_jump).findViewById(R.id.row_value);
	airUnit = (TextView) view.findViewById(R.id.row_best_jump).findViewById(R.id.row_unit);
	distDrop = (TextView)view.findViewById(R.id.row_best_jump).findViewById(R.id.row_info);
		
	// FIXME hack to defeature jump
	distDrop.setVisibility(View.GONE);
	return view;
    }

    public void onResume() {
	super.onResume();
		
	final StatsScrollView scrollView = (StatsScrollView) getView().findViewById(R.id.stats_scrollview);
	scrollView.requestFocus();
		
	scrollView.post(new Runnable() {
		@Override
		public void run() {
		    switch(StatsActivity.currentRowPosition) {
		    case StatsActivity.ALT_POSITION:
			scrollView.scrollToElement(altRow);
			break;
		    case StatsActivity.DISTANCE_POSITION:
			scrollView.scrollToElement(distRow);
			break;
		    case StatsActivity.JUMP_POSITION:
			scrollView.scrollToElement(airRow);
			break;
		    case StatsActivity.MAX_SPD_POSITION:
			scrollView.scrollToElement(speedRow);
			break;
		    case StatsActivity.RUN_POSITION:
			scrollView.scrollToElement(runsRow);
			break;
		    case StatsActivity.VERT_POSITION:
			scrollView.scrollToElement(vertRow);
			break;
		    default:
			scrollView.scrollTo(0, 0);	
		    }
		} 
	    });
	Log.d(TAG, "view has been created.");
	alive = true;
    }

    /*
     * Sets info bundle and extract data from it
     */
    public void setViewData(Bundle data) {	

	//		if(runs == null){
	//			init();
	//		}

	if(alive){
	    Bundle runBundle = data.getBundle("RUN_BUNDLE");
	    Bundle speedBundle = data.getBundle("SPEED_BUNDLE");
	    Bundle distanceBundle = data.getBundle("DISTANCE_BUNDLE");
	    Bundle vertBundle = data.getBundle("VERTICAL_BUNDLE");
	    Bundle altitudeBundle = data.getBundle("ALTITUDE_BUNDLE");	
	    Bundle jumpBundle = data.getBundle("JUMP_BUNDLE");

	    if(runBundle == null && speedBundle == null && distanceBundle == null && vertBundle == null && altitudeBundle == null && jumpBundle == null) return;

	    DecimalFormat df0 = new DecimalFormat();
	    df0.setMaximumFractionDigits(0);
	    df0.setMinimumFractionDigits(0);
	    DecimalFormat df1 = new DecimalFormat();
	    df1.setMaximumFractionDigits(1);
	    df1.setMinimumFractionDigits(1);
	    DecimalFormat df2 = new DecimalFormat();
	    df2.setMaximumFractionDigits(2);
	    df2.setMinimumFractionDigits(2);

	    boolean uncertainAlt = (altitudeBundle != null) ? (altitudeBundle.getInt("HeightOffsetN") < 500) : false;
	    if(uncertainAlt) alt.setText("...");
			
	    if (type != StatsType.LASTRUN){

		if(runBundle != null) {

		    switch (type){
		    case ALLTIME:
			runs.setText(Integer.toString(runBundle.getInt("AllTimeTotalNumberOfSkiRuns")));
			break;
		    case TODAY:
			ArrayList<Bundle> runList = runBundle.getParcelableArrayList("Runs");
			runs.setText(Integer.toString(runList.size()));
			break;
		    }		

		}

		if(speedBundle != null){
		    switch (type){
		    case ALLTIME:
			speed.setText(df0.format(
						 metric? speedBundle.getFloat("AllTimeMaxSpeed"):
						 (float) ConversionUtil.kmsToMiles(speedBundle.getFloat("AllTimeMaxSpeed"))
						 ));
			speedUnit.setText(metric?"km/h":"mph");
			break;
		    case TODAY:
			speed.setText(df0.format(
						 metric? speedBundle.getFloat("MaxSpeed"):
						 (float) ConversionUtil.kmsToMiles(speedBundle.getFloat("MaxSpeed"))
						 ));
			speedUnit.setText(metric?"km/h":"mph");
			break;
		    }		
		    //averageSpeed.setText(df.format(speedBundle.getFloat("AverageSpeed")));
		    //averageSpeedUnit.setText("km/h");
		}

		if(vertBundle != null) {				
		    switch (type){
		    case TODAY:
			drop.setText(df0.format(
						metric? vertBundle.getFloat("Vert"):
						(float) ConversionUtil.metersToFeet(vertBundle.getFloat("Vert"))
						));
			dropUnit.setText(metric?"m":"ft");
			break;
		    case ALLTIME:
			drop.setText(df0.format(
						metric? vertBundle.getFloat("AllTimeVert"):
						(float) ConversionUtil.metersToFeet(vertBundle.getFloat("AllTimeVert"))
						));
			dropUnit.setText(metric?"m":"ft");
			break;
		    }
		}

		if (distanceBundle != null){
		    switch (type){
		    case TODAY:
			float distance = distanceBundle.getFloat("Distance");

			if(distance < 1000) {
			    distance = metric? distance:(float)ConversionUtil.metersToFeet(distance);
			    dist.setText(df0.format(distance));
			    distUnit.setText(metric?"m":"ft");
			} else {
			    distance = metric? distance / 1000:(float)ConversionUtil.metersToMiles(distance);
			    dist.setText(df2.format(distance));
			    distUnit.setText(metric?"km":"mi");
			}
			break;
		    case ALLTIME:
			float allTimeDist = distanceBundle.getFloat("AllTimeDistance");

			if(allTimeDist < 1000) {
			    allTimeDist = metric? allTimeDist:(float)ConversionUtil.metersToFeet(allTimeDist);

			    dist.setText(df0.format(allTimeDist));
			    distUnit.setText(metric?"m":"ft");
			} else {
			    allTimeDist = metric? allTimeDist/1000:(float)ConversionUtil.metersToMiles(allTimeDist);
			    dist.setText(df2.format(allTimeDist));
			    distUnit.setText(metric?"km":"mi");
			}
			break;
		    }

		}

		if (altitudeBundle != null){

		    if(!uncertainAlt) {
			switch (type){
			case ALLTIME:
			    alt.setText(df0.format(
						   metric? altitudeBundle.getFloat("AllTimeMaxAlt"):
						   ConversionUtil.metersToFeet(altitudeBundle.getFloat("AllTimeMaxAlt"))
						   ));
			    altUnit.setText(metric?"m":"ft");
			    break;
			case TODAY:
			    alt.setText(df0.format(
						   metric? altitudeBundle.getFloat("MaxAlt"):
						   ConversionUtil.metersToFeet(altitudeBundle.getFloat("MaxAlt"))
						   ));
			    altUnit.setText(metric?"m":"ft");
			    break;
			}
		    }
		}
		if (jumpBundle != null){
		    airUnit.setText("s");
		    String distDropUnits= metric?"m":"ft";
		    String Distance;
		    String Drop;
		    switch (type){
		    case TODAY:
			Bundle bestJump = jumpBundle.getBundle("BestJump");
			if (bestJump!=null){
			    int airTimeMs = bestJump.getInt("Air");
			    float distance = bestJump.getFloat("Distance");
			    float drop = bestJump.getFloat("Drop");
							
			    if(airTimeMs > ReconJump.INVALID_AIR) 
				air.setText(df1.format( ((double)airTimeMs)/1000) );
			    else
				air.setText(df1.format(0));

			    Distance = df1.format(metric? distance:ConversionUtil.metersToFeet(distance)) 
				+ distDropUnits + " Dist, ";
			    Drop = df1.format(metric? drop:ConversionUtil.metersToFeet(drop)) 
				+ distDropUnits + " Drop";
			    distDrop.setText(Distance+Drop);
			    distDrop.setVisibility(View.GONE);
			    //distDrop.setVisibility((distance > ReconJump.INVALID_DISTANCE || drop > ReconJump.INVALID_DROP) ? View.VISIBLE : View.GONE);
			}else {
			    air.setText(df1.format(0));
			    distDrop.setText("");
			}
			break;
		    case ALLTIME:
			Bundle allTimeBestJump = jumpBundle.getBundle("AllTimeBestJump");
			if (allTimeBestJump!=null){
			    int airTimeMs = allTimeBestJump.getInt("Air");
			    float distance = allTimeBestJump.getFloat("Distance");
			    float drop = allTimeBestJump.getFloat("Drop");
							
			    if(airTimeMs > ReconJump.INVALID_AIR) 
				air.setText(df1.format( ((double)airTimeMs)/1000) );
			    else
				air.setText(df1.format(0));
			    Distance = df1.format(metric? distance:ConversionUtil.metersToFeet(distance)) 
				+ distDropUnits + " Dist, ";
			    Drop = df1.format(metric? drop:ConversionUtil.metersToFeet(drop)) 
				+ distDropUnits + " Drop";
			    distDrop.setText(Distance+Drop);
			    distDrop.setVisibility(View.GONE);
			} else {
			    air.setText(df1.format(0));
			    distDrop.setText("");
			}
			break;
		    }
		}
	    }
	    else { 
		// -- jumps //////////////////////////////////////
		if (jumpBundle != null){
		    ArrayList<Bundle> jumpList = jumpBundle.getParcelableArrayList("Jumps");
		    if (jumpList.size()>0){
			Bundle lastJump = jumpList.get(jumpList.size()-1); 

			airUnit.setText("s");
			String distDropUnits= metric?"m":"ft";
						
			int airTimeMs = lastJump.getInt("Air");
			float distance = lastJump.getFloat("Distance");
			float drop = lastJump.getFloat("Drop");
						
			if(airTimeMs > ReconJump.INVALID_AIR) 
			    air.setText(df1.format( ((double)airTimeMs)/1000) );
			else
			    air.setText(df1.format(0));
			String Distance = df1.format(metric? distance : ConversionUtil.metersToFeet(distance)) 
			    + distDropUnits + " Dist, ";
			String Drop = df1.format(metric? drop : ConversionUtil.metersToFeet(drop)) 
			    + distDropUnits + " Drop";

			distDrop.setText(Distance+Drop);
						
			// Show this field only if is valid
			//distDrop.setVisibility((distance > ReconJump.INVALID_DISTANCE || drop > ReconJump.INVALID_DROP) ? View.VISIBLE : View.GONE);
						
			// FIXME hack to defeature drop & dist
			distDrop.setVisibility(View.GONE);
		    } else {
			air.setText(df1.format(0));
			distDrop.setText("");
		    }
		}
		//////////////////////////////////////////////////
		if (runBundle != null){
		    ArrayList<Bundle> runList = runBundle.getParcelableArrayList("Runs");

		    speedUnit.setText(metric?"km/h":"mph");
		    dropUnit.setText(metric?"m":"ft");
		    distUnit.setText(metric?"m":"ft");
		    altUnit.setText(metric?"m":"ft");
		    airUnit.setText("s");

		    if (runList.size() == 0){
			runs.setText("0");
			return;
		    }
		    Bundle lastRun = runList.get(runList.size()-1);

		    runs.setText(""+runList.size());

		    speed.setText(df0.format(
					     metric? lastRun.getFloat("MaxSpeed"):
					     ConversionUtil.kmsToMiles(lastRun.getFloat("MaxSpeed"))
					     ));

		    drop.setText(df0.format(
					    metric? lastRun.getFloat("Vertical"):
					    ConversionUtil.metersToFeet(lastRun.getFloat("Vertical"))
					    ));

		    if(!uncertainAlt) {
			alt.setText(df0.format(
					       metric? lastRun.getFloat("MaxAltitude"):
					       ConversionUtil.metersToFeet(lastRun.getFloat("MaxAltitude"))));
		    }

		    float distance = lastRun.getFloat("Distance");

		    if (!metric)
			distance = (float) ConversionUtil.metersToFeet(lastRun.getFloat("Distance"));

		    if(distance < 10000) {
			dist.setText(df0.format(distance));
			distUnit.setText(metric?"m":"ft");
		    } else {
			float distKm = distance / 1000;				
			dist.setText(df1.format(distKm));
			distUnit.setText(metric?"km":"mi");
		    }
		}
	    }
	}
    }
}
