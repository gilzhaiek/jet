package com.reconinstruments.dashlauncher.livestats.widgets;

import com.reconinstruments.dashlivestats.R;

import java.util.HashMap;

public class ReconDashboardHashmap {
	public final static int ALT_4x1 = 1; // Altitude
	public final static int ALT_2x2 = 2;
    
    public final static int ALT_6x4 = 29;
    public final static int ALT_3x4 = 30;
    public final static int ALT_3x2 = 31;
    
	public final static int VRT_4x1 = 3; // Vertical
	public final static int VRT_2x2 = 4;
	
	public final static int VRT_6x4 = 23;
	public final static int VRT_3x4 = 24;
	public final static int VRT_3x2 = 25;
	
	public final static int TMP_4x1 = 5; // Temperature
	public final static int TMP_2x2 = 6;
	public final static int SPD_4x2 = 7; // Speed
	public final static int SPD_4x3 = 8;
	public final static int SPD_4x4 = 9;
	public final static int SPD_6x4 = 10;
	
    public final static int SPD_3x4 = 21;
    public final static int SPD_3x2 = 22;
    
	public final static int PLY_4x1 = 11; // Playlist
	public final static int DST_4x1 = 12; // Distance
	public final static int DST_2x2 = 13; 
    
    public final static int DST_6x4 = 26;
    public final static int DST_3x4 = 27;
    public final static int DST_3x2 = 28;
    
	public final static int AIR_4x1 = 14; // Air 
	public final static int AIR_2x2 = 15; 
    
    public final static int AIR_6x4 = 32;
    public final static int AIR_3x4 = 33;
    public final static int AIR_3x2 = 34;
    
	public final static int MAXSPD_4x1 = 16; // Max Speed
	public final static int MAXSPD_2x2 = 17; 
	public final static int CHR_4x1 = 18;
	public final static int HR_2x2 = 19;
	public final static int HR_4x1 = 20;
	
	public final static int CAD_6x4 = 35; // Cadence
    public final static int CAD_3x4 = 36;
    public final static int CAD_3x2 = 37;
    
    public final static int CAL_6x4 = 38; // Calorie
    public final static int CAL_3x4 = 39;
    public final static int CAL_3x2 = 40;
    
    public final static int EV_6x4 = 41; // Elevation Gain
    public final static int EV_3x4 = 42;
    public final static int EV_3x2 = 43;
    
    public final static int HRT_6x4 = 44; // HeartRate
    public final static int HRT_3x4 = 45;
    public final static int HRT_3x2 = 46;
    
    public final static int DUR_6x4 = 47; // Moving Duration
    public final static int DUR_3x4 = 48;
    public final static int DUR_3x2 = 49;
    
    public final static int PACE_6x4 = 50; // Pace
    public final static int PACE_3x4 = 51;
    public final static int PACE_3x2 = 52;
    
    public final static int TRG_6x4 = 53; // Terrain Grade
    public final static int TRG_3x4 = 54;
    public final static int TRG_3x2 = 55;
    
    public final static int PWR_6x4 = 56; // Power
    public final static int PWR_3x4 = 57;
    public final static int PWR_3x2 = 58;
    
    public final static int PACE_AVG_6x4 = 59; // Avg Pace
    public final static int PACE_AVG_3x4 = 60;
    public final static int PACE_AVG_3x2 = 61;
    
    public final static int CAD_AVG_6x4 = 62; // Avg Cadence
    public final static int CAD_AVG_3x4 = 63;
    public final static int CAD_AVG_3x2 = 64;
    
    public final static int HRT_AVG_6x4 = 65; // Avg HeartRate
    public final static int HRT_AVG_3x4 = 66;
    public final static int HRT_AVG_3x2 = 67;
    
    public final static int SPD_AVG_6x4 = 68; // Avg Speed
    public final static int SPD_AVG_3x4 = 69;
    public final static int SPD_AVG_3x2 = 70;
    
    public final static int PWR_AVG_6x4 = 71; // Power avg
    public final static int PWR_AVG_3x4 = 72;
    public final static int PWR_AVG_3x2 = 73;
    
    public final static int PWR_3S_6x4 = 74; // Power avg 3s
    public final static int PWR_3S_3x4 = 75;
    public final static int PWR_3S_3x2 = 76;
    
    public final static int PWR_10S_6x4 = 77; // Power avg 10s
    public final static int PWR_10S_3x4 = 78;
    public final static int PWR_10S_3x2 = 79;
    
    public final static int PWR_30S_6x4 = 80; // Power avg 30s
    public final static int PWR_30S_3x4 = 81;
    public final static int PWR_30S_3x2 = 82;
    
    public final static int PWR_MAX_6x4 = 83; // Power max
    public final static int PWR_MAX_3x4 = 84;
    public final static int PWR_MAX_3x2 = 85;

	public HashMap<String, Integer> WidgetHashMap;
	public HashMap<String, Integer> LayoutHashMap;
	public HashMap<Integer, Integer> PlaceholderMap;
	public ReconDashboardHashmap(){
		LayoutHashMap = new HashMap<String, Integer>();
		LayoutHashMap.put("1",R.layout.livestats_dash_layout_1_old);
		LayoutHashMap.put("2",R.layout.livestats_dash_layout_2_old);
		LayoutHashMap.put("3",R.layout.livestats_dash_layout_3_old);
		LayoutHashMap.put("4",R.layout.livestats_dash_layout_4_old);
		LayoutHashMap.put("5",R.layout.livestats_dash_layout_5_old);
		LayoutHashMap.put("6",R.layout.livestats_dash_layout_6_old);
//		LayoutHashMap.put("7",R.layout.dash_layout_test1);
//		LayoutHashMap.put("8",R.layout.dash_layout_test2);
//		LayoutHashMap.put("9",R.layout.dash_layout_test3);
		
		LayoutHashMap.put("7",R.layout.livestats_dash_layout_7);
		LayoutHashMap.put("8",R.layout.livestats_dash_layout_8);
		LayoutHashMap.put("9",R.layout.livestats_dash_layout_9);
		LayoutHashMap.put("10",R.layout.livestats_dash_layout_10);
		
		WidgetHashMap = new HashMap<String, Integer>();
		WidgetHashMap.put("alt_4x1", ALT_4x1);
		WidgetHashMap.put("alt_2x2", ALT_2x2);
        
        WidgetHashMap.put("alt_6x4", ALT_6x4);
        WidgetHashMap.put("alt_3x4", ALT_3x4);
        WidgetHashMap.put("alt_3x2", ALT_3x2);
        
		WidgetHashMap.put("vrt_4x1", VRT_4x1);
		WidgetHashMap.put("vrt_2x2", VRT_2x2);
		
		WidgetHashMap.put("vrt_6x4", VRT_6x4);
		WidgetHashMap.put("vrt_3x4", VRT_3x4);
		WidgetHashMap.put("vrt_3x2", VRT_3x2);
		
		WidgetHashMap.put("tmp_4x1", TMP_4x1);
		WidgetHashMap.put("tmp_2x2", TMP_2x2);
		WidgetHashMap.put("spd_4x2", SPD_4x2);
		WidgetHashMap.put("spd_4x3", SPD_4x3);
		WidgetHashMap.put("spd_4x4", SPD_4x4);
		WidgetHashMap.put("spd_6x4", SPD_6x4);
		
		WidgetHashMap.put("spd_3x4", SPD_3x4);
		WidgetHashMap.put("spd_3x2", SPD_3x2);
		
		WidgetHashMap.put("ply_4x1", PLY_4x1);
		WidgetHashMap.put("dst_4x1", DST_4x1);
		WidgetHashMap.put("dst_2x2", DST_2x2);
		
        WidgetHashMap.put("dst_6x4", DST_6x4);
        WidgetHashMap.put("dst_3x4", DST_3x4);
        WidgetHashMap.put("dst_3x2", DST_3x2);
		
		WidgetHashMap.put("air_4x1", AIR_4x1);
		WidgetHashMap.put("air_2x2", AIR_2x2);
        
        WidgetHashMap.put("air_6x4", AIR_6x4);
        WidgetHashMap.put("air_3x4", AIR_3x4);
        WidgetHashMap.put("air_3x2", AIR_3x2);
        
		WidgetHashMap.put("maxspd_4x1", MAXSPD_4x1);
		WidgetHashMap.put("maxspd_2x2", MAXSPD_2x2);
		WidgetHashMap.put("chr_4x1", CHR_4x1);
		WidgetHashMap.put("hr_2x2", HR_2x2);
		WidgetHashMap.put("hr_4x1", HR_4x1);

		WidgetHashMap.put("cad_6x4", CAD_6x4);
		WidgetHashMap.put("cad_3x4", CAD_3x4);
		WidgetHashMap.put("cad_3x2", CAD_3x2);
	    
		WidgetHashMap.put("cal_6x4", CAL_6x4);
		WidgetHashMap.put("cal_3x4", CAL_3x4);
		WidgetHashMap.put("cal_3x2", CAL_3x2);
	    
		WidgetHashMap.put("ev_6x4", EV_6x4);
		WidgetHashMap.put("ev_3x4", EV_3x4);
		WidgetHashMap.put("ev_3x2", EV_3x2);
	    
		WidgetHashMap.put("hrt_6x4", HRT_6x4);
		WidgetHashMap.put("hrt_3x4", HRT_3x4);
		WidgetHashMap.put("hrt_3x2", HRT_3x2);
	    
		WidgetHashMap.put("dur_6x4", DUR_6x4);
		WidgetHashMap.put("dur_3x4", DUR_3x4);
		WidgetHashMap.put("dur_3x2", DUR_3x2);
	    
		WidgetHashMap.put("pace_6x4", PACE_6x4);
		WidgetHashMap.put("pace_3x4", PACE_3x4);
		WidgetHashMap.put("pace_3x2", PACE_3x2);
	    
		WidgetHashMap.put("trg_6x4", TRG_6x4);
		WidgetHashMap.put("trg_3x4", TRG_3x4);
		WidgetHashMap.put("trg_3x2", TRG_3x2);
	    
		WidgetHashMap.put("pwr_6x4", PWR_6x4);
		WidgetHashMap.put("pwr_3x4", PWR_3x4);
		WidgetHashMap.put("pwr_3x2", PWR_3x2);
        
        WidgetHashMap.put("pwr_avg_6x4", PWR_AVG_6x4);
        WidgetHashMap.put("pwr_avg_3x4", PWR_AVG_3x4);
        WidgetHashMap.put("pwr_avg_3x2", PWR_AVG_3x2);
        
        WidgetHashMap.put("pwr_3s_6x4", PWR_3S_6x4);
        WidgetHashMap.put("pwr_3s_3x4", PWR_3S_3x4);
        WidgetHashMap.put("pwr_3s_3x2", PWR_3S_3x2);
        
        WidgetHashMap.put("pwr_10s_6x4", PWR_10S_6x4);
        WidgetHashMap.put("pwr_10s_3x4", PWR_10S_3x4);
        WidgetHashMap.put("pwr_10s_3x2", PWR_10S_3x2);
        
        WidgetHashMap.put("pwr_30s_6x4", PWR_30S_6x4);
        WidgetHashMap.put("pwr_30s_3x4", PWR_30S_3x4);
        WidgetHashMap.put("pwr_30s_3x2", PWR_30S_3x2);
        
        WidgetHashMap.put("pwr_max_6x4", PWR_MAX_6x4);
        WidgetHashMap.put("pwr_max_3x4", PWR_MAX_3x4);
        WidgetHashMap.put("pwr_max_3x2", PWR_MAX_3x2);

		WidgetHashMap.put("pace_avg_6x4", PACE_AVG_6x4);
		WidgetHashMap.put("pace_avg_3x4", PACE_AVG_3x4);
		WidgetHashMap.put("pace_avg_3x2", PACE_AVG_3x2);
	    
		WidgetHashMap.put("cad_avg_6x4", CAD_AVG_6x4);
		WidgetHashMap.put("cad_avg_3x4", CAD_AVG_3x4);
		WidgetHashMap.put("cad_avg_3x2", CAD_AVG_3x2);
	    
		WidgetHashMap.put("hrt_avg_6x4", HRT_AVG_6x4);
		WidgetHashMap.put("hrt_avg_3x4", HRT_AVG_3x4);
		WidgetHashMap.put("hrt_avg_3x2", HRT_AVG_3x2);
	    
		WidgetHashMap.put("spd_avg_6x4", SPD_AVG_6x4);
		WidgetHashMap.put("spd_avg_3x4", SPD_AVG_3x4);
		WidgetHashMap.put("spd_avg_3x2", SPD_AVG_3x2);
		
		PlaceholderMap= new HashMap<Integer, Integer>();
		PlaceholderMap.put(0, R.id.recon_dash_widget_1);
		PlaceholderMap.put(1, R.id.recon_dash_widget_2);
		PlaceholderMap.put(2, R.id.recon_dash_widget_3);
		PlaceholderMap.put(3, R.id.recon_dash_widget_4);
		PlaceholderMap.put(4, R.id.recon_dash_widget_5);
		
	}
}
