package com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.subclass;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.GeoBuddyInfo;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Buddy;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.DrawingSet;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer.MapBuddyInfo;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;

public class SnowReticleDrawingSet extends DrawingSet {

// constants
	private final static String 	TAG = "SnowReticleDrawingSet";

// members
	protected ArrayList<ResortInfoResponse> 	mResortReticleItems0 = new ArrayList<ResortInfoResponse>();
	protected ArrayList<ResortInfoResponse> 	mResortReticleItems1 = new ArrayList<ResortInfoResponse>();

// creator	/ init / release	
	public SnowReticleDrawingSet(RenderSchemeManager rsm) {
		super(rsm);
	}
		
	public void ClearResortReticleItems() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		if(nextIndex == 0) {
			mResortReticleItems0.clear();
		}
		else {
			mResortReticleItems1.clear();
		}
	}
	
	public void AddResortReticleItem(ResortInfoResponse resortInfo) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		if(nextIndex == 0) {
			mResortReticleItems0.add(resortInfo);
		}
		else {
			mResortReticleItems1.add(resortInfo);
		}
	}
	
	public ArrayList<ResortInfoResponse> GetCurrentResortReticleItems() {
		if(mCurIndex == 0) {
			return mResortReticleItems0;
		}
		else {
			return mResortReticleItems1;
		}
	}
	
	public void CheckNewSetReady() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		if(nextIndex == 0) {
//			if(mResortReticleItems0.size() > 0) {
				mUpdateAvailable = true;
//			}
		}
		else {
//			if(mResortReticleItems1.size() > 0) {
				mUpdateAvailable = true;
//			}
		}
	}
	
	
	
	@Override
	public boolean SwitchIfUpdateReady() {
		if(mUpdateAvailable) {
			mUpdateAvailable = false;
			mCurIndex = (mCurIndex == 0) ? 1 : 0;
			return true;
		}
		return false;
	}
	
	

}
