package com.reconinstruments.mapImages.bundlers;

import com.reconinstruments.mapImages.objects.ResortInfo;

import android.os.Bundle;

public class ResortInfoBundler extends BundlerBase {
	public static ResortInfo GenerateResortInfo(Bundle bundle) {
		return new ResortInfo(
				bundle.getInt("ResortID"),
				bundle.getString("Name"),
				bundle.getInt("CountryRegionID"),
				GetPointF("ResortLocation", bundle),
				bundle.getInt("MapVersion"),
				GetRectF("BoundingBox", bundle),
				bundle.getString("CountryName"),
				bundle.getString("RegionName"));
	}
	
	public static Bundle GenerateBundle(ResortInfo resortInfo) {
		Bundle bundle = new Bundle();
		
		bundle.putInt("ResortID", resortInfo.ResortID);
		bundle.putString("Name", resortInfo.Name);
		bundle.putInt("CountryRegionID", resortInfo.CountryRegionID);
		AddPointF("ResortLocation", bundle, resortInfo.ResortLocation);
		bundle.putInt("MapVersion", resortInfo.MapVersion);
		AddRectF("BoundingBox", bundle, resortInfo.BoundingBox);
		bundle.putString("RegionName", resortInfo.RegionName);
		bundle.putString("CountryName", resortInfo.CountryName);
				
		return bundle;
	}
}
