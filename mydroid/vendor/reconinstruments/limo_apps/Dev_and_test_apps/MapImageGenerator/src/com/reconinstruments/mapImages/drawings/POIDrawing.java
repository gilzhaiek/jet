package com.reconinstruments.mapImages.drawings;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.prim.PointD;
import com.reconinstruments.mapImages.R;

public class POIDrawing {
	public POI		mPoi;
	public PointD	mLocation;
	public Bitmap	mIcon;
	public MapDrawings.State mState = MapDrawings.State.NORMAL;
	
	public POIDrawing(PointD location, POI poi) {
		mPoi = poi;
		mLocation = location;
	}
		
	public void Release(){
		mLocation = null;
		mPoi = null;
	}
	
	private void SetIcon(int type, Resources res) {
		
		int iconSize = 40;
		
		switch(type) {
		case POI.POI_TYPE_SKICENTER:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_food_icon), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_RESTAURANT:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_restaurant), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_BAR:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_bar), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_PARK:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_park), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_CARPARKING:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_parking), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_RESTROOM:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_restroom), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_CHAIRLIFTING:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_chairlift_icon), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_SKIERDROPOFF_PARKING:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_parking), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_INFORMATION:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_info), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_HOTEL:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_hotel), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_BANK:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_bank), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_SKISCHOOL:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_skischool), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_BUDDY:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.nav_buddy), iconSize, iconSize, false);
			break;
		case POI.POI_TYPE_UNDEFINED:
		default:
			mIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.no_map), iconSize, iconSize, false);
			break;
		}

	}
	
	public void Draw(Canvas canvas, Resources res, RenderSchemeManager rsm, Matrix transformMatrix, float viewScale) {
		if(mIcon == null) { 
			SetIcon(mPoi.Type, res);
		}

		float[] dl = new float[2];
		dl[0] = (float)mLocation.x;
		dl[1] = (float)mLocation.y;
		transformMatrix.mapPoints(dl);

		Bitmap poiIcon = rsm.GetPOIBitmap(mPoi.Type, mState.ordinal(), viewScale);
		double bitmapVertOffsetPercent = (double)rsm.GetPOIBitmapOffsetPercent(mPoi.Type, mState.ordinal(), viewScale)/100.0;

		if(poiIcon != null) {
			if(mState == MapDrawings.State.HAS_FOCUS || mState == MapDrawings.State.DISABLED_FOCUS) {
//				Log.e("POIDrawing","bitmap offset: " + mPoi.Type + " | " + mState.ordinal() + " | " + bitmapVertOffsetPercent);
				canvas.drawBitmap(poiIcon, dl[0] - poiIcon.getWidth()/2+1, dl[1] - (int)(poiIcon.getHeight() * (0.5 + bitmapVertOffsetPercent))+1, null);
			}
			else {
				canvas.drawBitmap(poiIcon, dl[0] - poiIcon.getWidth()/2+1, dl[1] - poiIcon.getHeight()/2+1, null);
			}
		}
	}
}
