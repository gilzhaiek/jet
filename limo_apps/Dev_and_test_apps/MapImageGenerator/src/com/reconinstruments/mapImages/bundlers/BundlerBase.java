package com.reconinstruments.mapImages.bundlers;

import java.util.ArrayList;

import com.reconinstruments.mapImages.prim.PointD;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;

public class BundlerBase {

	public static PointF GetPointF(String prefix, Bundle bundle){
		return new PointF(bundle.getFloat(prefix + ".x"), bundle.getFloat(prefix + ".y"));  
	}
	
	public static void AddPointF(String prefix, Bundle bundle, PointF pointF){
		bundle.putFloat(prefix + ".x",  pointF.x);
		bundle.putFloat(prefix + ".y",  pointF.y);
	}
	
	public static PointD GetPointD(String prefix, Bundle bundle){
		return new PointD(bundle.getDouble(prefix + ".x"), bundle.getDouble(prefix + ".y"));  
	}
	
	public static void AddPointD(String prefix, Bundle bundle, PointD pointD){
		bundle.putDouble(prefix + ".x",  pointD.x);
		bundle.putDouble(prefix + ".y",  pointD.y);
	}	
	
	public static RectF GetRectF(String prefix, Bundle bundle){
		return new RectF(bundle.getFloat(prefix + ".left"),
				bundle.getFloat(prefix + ".top"),
				bundle.getFloat(prefix + ".right"),
				bundle.getFloat(prefix + ".bottom"));
	}
	
	public static void AddRectF(String prefix, Bundle bundle, RectF rectF){
		bundle.putFloat(prefix + ".left", 	rectF.left);
		bundle.putFloat(prefix + ".top", 	rectF.top);
		bundle.putFloat(prefix + ".right", 	rectF.right);
		bundle.putFloat(prefix + ".bottom", rectF.bottom);
	}
	
	public static Rect GetRect(String prefix, Bundle bundle){
		return new Rect(bundle.getInt(prefix + ".left"),
				bundle.getInt(prefix + ".top"),
				bundle.getInt(prefix + ".right"),
				bundle.getInt(prefix + ".bottom"));
	}
	
	public static void AddRect(String prefix, Bundle bundle, Rect rect){
		bundle.putInt(prefix + ".left", 	rect.left);
		bundle.putInt(prefix + ".top", 		rect.top);
		bundle.putInt(prefix + ".right", 	rect.right);
		bundle.putInt(prefix + ".bottom", 	rect.bottom);
	}	
	
	public static ArrayList<PointD> GetArrayPointD(Bundle bundle) {
		if(bundle == null) new ArrayList<PointD>(0);
		
		int bundleSize = bundle.getInt("size");
		
		ArrayList<PointD> arrayPointD = new ArrayList<PointD>(bundleSize);
		
		for(int i = 0; i < bundleSize; i++) {
			arrayPointD.add(i, GetPointD(Integer.toString(i),bundle));
		}
		
		return arrayPointD;
	}
	
	public static Bundle GenerateArrayPointDBundle(ArrayList<PointD> arrayPointD){
		Bundle bundle = new Bundle();
		
		bundle.putInt("size", arrayPointD.size());
		
		for(int i = 0; i < arrayPointD.size(); i++) {
			AddPointD(Integer.toString(i), bundle, arrayPointD.get(i));
		}
		
		return bundle;
	}

}
