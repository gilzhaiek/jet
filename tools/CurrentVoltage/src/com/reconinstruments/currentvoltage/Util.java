package com.reconinstruments.currentvoltage;

import java.io.File;

import android.content.Context;
import android.graphics.Color;

import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.reconinstruments.currentvoltage.CurrentVoltage.GraphType;

public class Util {
	private static final String TAG = Util.class.getSimpleName();

	public static long getVoltage(){

		File f = new File("/sys/class/power_supply/twl6030_battery/voltage_now");
		if (f.exists()) {                            
			return OneLineReader.getValue(f, true);
		}
		else
			return -10000;

	}

	public static long getCurrentNow(){

		File f = new File("/sys/class/power_supply/twl6030_battery/current_now");
		if (f.exists()) {                            
			return OneLineReader.getValue(f, true);
		}
		else
			return -10000;

	}
	
	public static long getCurrentAvg(){

		File f = new File("/sys/class/power_supply/twl6030_battery/current_avg");
		if (f.exists()) {                            
			return OneLineReader.getValue(f, true);
		}
		else
			return -10000;

	}

	public static long getTemp(){

		File f = new File("/sys/class/power_supply/twl6030_battery/temp");
		if (f.exists()) {                            
			return OneLineReader.getValue(f, true);
		}
		else
			return -10000;

	}

	public static LineGraphView getGraphView(Context context, GraphViewSeries series, String title, GraphType type){
		
		LineGraphView gView = new LineGraphView(context , title);

		gView.setViewPort(0, 50);
		gView.setScrollable(true);
		switch (type) {
		case CURRENT:
			gView.setManualYAxisBounds(500, 100);
			series.getStyle().color = 0xFFfe9600;
			break;
		case VOLTAGE:
			gView.setManualYAxisBounds(4200, 3400);
			series.getStyle().color = 0xFFfe9600;
			break;
		}
		series.getStyle().thickness = 2;
		
		gView.addSeries(series); // data

		gView.getGraphViewStyle().setNumVerticalLabels(5);
		gView.getGraphViewStyle().setTextSize(0);
		gView.getGraphViewStyle().setGridColor(Color.LTGRAY);
		gView.getGraphViewStyle().setHorizontalLabelsColor(Color.TRANSPARENT);
		gView.getGraphViewStyle().setVerticalLabelsColor(Color.GRAY);
		
		return gView;
	}



}
