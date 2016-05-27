package com.reconinstruments.dashboard.hashmaps;

import java.util.HashMap;

import com.reconinstruments.dashboard.R;

public class ReconDashboardHashmap {
	public final static int ALT_4x1 = 1; // Altitude
	public final static int ALT_2x2 = 2;
	public final static int VRT_4x1 = 3; // Vertical
	public final static int VRT_2x2 = 4;
	public final static int TMP_4x1 = 5; // Temperature
	public final static int TMP_2x2 = 6;
	public final static int SPD_4x2 = 7; // Speed
	public final static int SPD_4x3 = 8;
	public final static int SPD_4x4 = 9;
	public final static int SPD_6x4 = 10;
	public final static int PLY_4x1 = 11; // Playlist
	public final static int DST_4x1 = 12; // Distance
	public final static int DST_2x2 = 13; 
	public final static int AIR_4x1 = 14; // Air 
	public final static int AIR_2x2 = 15; 
	public final static int MAXSPD_4x1 = 16; // Max Speed
	public final static int MAXSPD_2x2 = 17; 
	public final static int CHR_4x1 = 18;
	public final static int HR_2x2 = 19;
	public final static int HR_4x1 = 20;
	
	public HashMap<String, Integer> WidgetHashMap;
	public HashMap<String, Integer> LayoutHashMap;
	public HashMap<Integer, Integer> PlaceholderMap;
	public ReconDashboardHashmap(){
		LayoutHashMap = new HashMap<String, Integer>();
		LayoutHashMap.put("1",R.layout.dash_layout_1);
		LayoutHashMap.put("2",R.layout.dash_layout_2);
		LayoutHashMap.put("3",R.layout.dash_layout_3);
		LayoutHashMap.put("4",R.layout.dash_layout_4);
		LayoutHashMap.put("5",R.layout.dash_layout_5);
		LayoutHashMap.put("6",R.layout.dash_layout_6);
		LayoutHashMap.put("7",R.layout.dash_layout_test1);
		LayoutHashMap.put("8",R.layout.dash_layout_test2);
		LayoutHashMap.put("9",R.layout.dash_layout_test3);
		
		WidgetHashMap = new HashMap<String, Integer>();
		WidgetHashMap.put("alt_4x1", ALT_4x1);
		WidgetHashMap.put("alt_2x2", ALT_2x2);
		WidgetHashMap.put("vrt_4x1", VRT_4x1);
		WidgetHashMap.put("vrt_2x2", VRT_2x2);
		WidgetHashMap.put("tmp_4x1", TMP_4x1);
		WidgetHashMap.put("tmp_2x2", TMP_2x2);
		WidgetHashMap.put("spd_4x2", SPD_4x2);
		WidgetHashMap.put("spd_4x3", SPD_4x3);
		WidgetHashMap.put("spd_4x4", SPD_4x4);
		WidgetHashMap.put("spd_6x4", SPD_6x4);
		WidgetHashMap.put("ply_4x1", PLY_4x1);
		WidgetHashMap.put("dst_4x1", DST_4x1);
		WidgetHashMap.put("dst_2x2", DST_2x2);
		WidgetHashMap.put("air_4x1", AIR_4x1);
		WidgetHashMap.put("air_2x2", AIR_2x2);
		WidgetHashMap.put("maxspd_4x1", MAXSPD_4x1);
		WidgetHashMap.put("maxspd_2x2", MAXSPD_2x2);
		WidgetHashMap.put("chr_4x1", CHR_4x1);
		WidgetHashMap.put("hr_2x2", HR_2x2);
		WidgetHashMap.put("hr_4x1", HR_4x1);
		
		PlaceholderMap= new HashMap<Integer, Integer>();
		PlaceholderMap.put(0, R.id.recon_dash_widget_1);
		PlaceholderMap.put(1, R.id.recon_dash_widget_2);
		PlaceholderMap.put(2, R.id.recon_dash_widget_3);
		PlaceholderMap.put(3, R.id.recon_dash_widget_4);
		PlaceholderMap.put(4, R.id.recon_dash_widget_5);
		
	}
}
