package com.reconinstruments.mapImages.bo;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.mapImages.common.MapDrawingsHandler;
import com.reconinstruments.mapImages.drawings.AreaDrawing;
import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.drawings.TrailDrawing;
import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.objects.ResortData;
import com.reconinstruments.mapImages.objects.Trail;

public class MapDrawingsBO {
	protected static final String TAG = "MapDrawingsBO";

	private static MapDrawingsHandler mMapDrawingsHandler = null;
	private static TransformResortDataTask mTransformResortDataTask = null;
	private static MapDrawings mMapDrawings = null;
	private static LocationTransformer mLocationTransformer = null;
	
	private static AreaDrawingBO mAreaDrawingBO = null;
	private static TrailDrawingBO mTrailDrawingBO = null;
	private static POIDrawingBO mPOIDrawingBO = null;

	public MapDrawingsBO(MapDrawingsHandler mapDrawingsHandler) {
		mMapDrawingsHandler = mapDrawingsHandler;
	}
	
	public static Bitmap GetMapSegmentBitmap(MapDrawings mapDrawings, Rect segment, boolean drawTrails)
	{		
		Bitmap sectionBitmap = Bitmap.createBitmap(segment.width(), segment.height(), Config.ARGB_4444);
		Canvas sectionCanvas = new Canvas(sectionBitmap);
		
		RectF boundingBox = mapDrawings.mTransformedBoundingBox;

//		Log.v(TAG,"b.l(-"+boundingBox.left+") - s.l("+segment.left+")="+(-boundingBox.left - segment.left)+
//				" b.t(-"+boundingBox.top+") - s.t("+segment.top+")="+(-boundingBox.top - segment.top));
		
		Matrix matrix = new Matrix();
		matrix.reset();
		matrix.preTranslate(-boundingBox.left - segment.left, -boundingBox.top - segment.top);
		sectionCanvas.concat(matrix);
		
		sectionCanvas.drawColor(Color.WHITE);		
		
	  	for(int i = 0; i < mapDrawings.AreaDrawings.size(); i++) {
	  		AreaDrawing areaDrawing = mapDrawings.AreaDrawings.get(i);
	  		mAreaDrawingBO.Draw(sectionCanvas, areaDrawing);
	  	}	
	  	
	  	// Draw all the trails that are not lifts
	  	for(int i = 0; i < mapDrawings.TrailDrawings.size(); i++) {
	  		TrailDrawing trailDrawing = mapDrawings.TrailDrawings.get(i);
	  		if((trailDrawing.mTrail.Type != Trail.SKI_LIFT) && (drawTrails)) {
		  		mTrailDrawingBO.Draw(sectionCanvas, trailDrawing);
		  	}
  		}
	  	
	  	// Draw the lifts last
	  	for(int i = 0; i < mapDrawings.TrailDrawings.size(); i++) {
	  		TrailDrawing trailDrawing = mapDrawings.TrailDrawings.get(i);
	  		if(trailDrawing.mTrail.Type == Trail.SKI_LIFT) {
		  		mTrailDrawingBO.Draw(sectionCanvas, trailDrawing);
		  	}
  		}	  	
	  	
		/*Paint north = new Paint( );
		north.setAlpha(255);
		north.setStyle(Paint.Style.STROKE);
		north.setStrokeCap(Paint.Cap.ROUND);
		north.setStrokeJoin(Paint.Join.ROUND);
		north.setAntiAlias(true);
		north.setColor(0xff000000);
		north.setStrokeWidth(4.0f);
		
		Paint east = new Paint( );
		east.setAlpha(255);
		east.setStyle(Paint.Style.STROKE);
		east.setStrokeCap(Paint.Cap.ROUND);
		east.setStrokeJoin(Paint.Join.ROUND);
		east.setAntiAlias(true);
		east.setColor(0xffff0000);
		east.setStrokeWidth(4.0f);
		
	  	Path path = new Path();
		
		path.incReserve(2);
					
	  	float x = Math.abs(-boundingBox.left - segment.left) + segment.width()/6.0f;
	  	float y = Math.abs(-boundingBox.top - segment.top) + segment.height()/6.0f;
	  	//float endX = x + segment.width() - segment.width()/6.0f;
	  	float endY = y + segment.height() - segment.height()/6.0f;
	  	
	  	//Log.v(TAG,x+","+y+" - "+endX+","+endY);
	  	
	  	//for(;x < endX; x += segment.width()/3.0f){
	  		for(;y < endY; y += segment.height()/3.0f){
	  			path.reset();
	  			path.moveTo(x,y);
	  			path.lineTo(x,y+segment.height()/8.0f);
	  			sectionCanvas.drawPath(path, north);
	  			
	  			path.reset();
	  			path.moveTo(x,y);
	  			path.lineTo(x+segment.width()/8.0f,y);
	  			sectionCanvas.drawPath(path, east);
	  		}
	  	//}*/
		
		return sectionBitmap;
	}
	

	private static final int MAX_WIDTH = 2856;
	private static final int MAX_HEIGHT = 2058;
	
	private int[] scaleBound(int width, int height) {
		int result[] = new int[2];
		result[0] = width;
		result[1] = height;
		
		int diff1 =  width - MAX_WIDTH;
		int diff2 = height - MAX_HEIGHT ;
		
		float rate = 1.0f;
		
		if (diff1 >= diff2 && diff1 > 0) {
			rate = ((float)MAX_WIDTH)/width;
			result[0] = MAX_WIDTH;
			result[1] = (int) ( height * rate);
		}
		
		if (diff2 >= diff1 && diff2 > 0) {
			rate = ((float)MAX_HEIGHT)/height;
			result[0] = (int) (width * rate);
			result[1] = MAX_HEIGHT;
		}
		Log.v(TAG,"scaleBound, oldWidth=" + width + ", oldHight=" + height  +" ,newWidth=" + result[0] + ", newHight=" + result[1]);
		return result;
	}
	
	public Bitmap CreateBitmap(MapDrawings mapDrawings) {
		if(mapDrawings == null) {
			Log.e(TAG,"CreateBitmap mmapDrawings=null");
			return null;
		}
		Bitmap bitmap;
		Log.v(TAG,"bbx w=" + mapDrawings.mTransformedBoundingBox.width() + " h=" + mapDrawings.mTransformedBoundingBox.height());
		int newBound[] = scaleBound((int)mapDrawings.mTransformedBoundingBox.width(), (int)mapDrawings.mTransformedBoundingBox.height());
		try {
			bitmap = Bitmap.createBitmap((int)(mapDrawings.mTransformedBoundingBox.width()/3.0), (int)(mapDrawings.mTransformedBoundingBox.height()/3.0), Config.ARGB_4444);
			//bitmap = Bitmap.createBitmap((int)(1000), (int)(1000), Config.ARGB_4444);
			
		}
		catch(Exception e) {
			Log.v(TAG,"bbx OUT OF MEMORY EXCEPTION");
			return null;
		}
  		Canvas canvas = new Canvas(bitmap);
  		canvas.drawColor(Color.WHITE);
		int m =0;
		int n=0;
	  	for(int i = 0; i < mapDrawings.AreaDrawings.size(); i++) {
	  		AreaDrawing areaDrawing = mapDrawings.AreaDrawings.get(i);
	  		if (areaDrawing== null)
	  			continue;
	  		mAreaDrawingBO.Draw(canvas, areaDrawing);
	  		++m;
	  	}		

	  	for(int i = 0; i < mapDrawings.TrailDrawings.size(); i++) {
	  		TrailDrawing trailDrawing = mapDrawings.TrailDrawings.get(i);
	  		if (trailDrawing == null)
	  			continue;
	  		mTrailDrawingBO.Draw(canvas, trailDrawing);
	  		++n;
	  	}
	  	Log.v(TAG,"CreateBitmap, area=" + m + ", n=" + n);
	  	
	  	canvas = null;
	  	return bitmap;
	}	
	
	public void Draw(MapDrawings mapDrawings, Canvas canvas) {
		if(mapDrawings == null)
			return;
		Log.e(TAG,"$$$$$$$$$$$$ MApViewBO.Draw()");
	  	for(int i = 0; i < mapDrawings.AreaDrawings.size(); i++) {
	  		mAreaDrawingBO.Draw(canvas, mapDrawings.AreaDrawings.get(i));
	  	}		

	  	for(int i = 0; i < mapDrawings.TrailDrawings.size(); i++) {
	  		mTrailDrawingBO.Draw(canvas, mapDrawings.TrailDrawings.get(i));
	  	}
	}
	
	public void TransformResortData(ResortData resortData, MapDrawings mapDrawings) throws Exception {
		if(mTransformResortDataTask != null) { return; }
		
		try {
			mTransformResortDataTask = new TransformResortDataTask();
			
			if(mLocationTransformer == null) {
				mLocationTransformer = new LocationTransformer();
				mAreaDrawingBO = new AreaDrawingBO(mLocationTransformer);
				mTrailDrawingBO = new TrailDrawingBO(mLocationTransformer);
				mPOIDrawingBO = new POIDrawingBO(mLocationTransformer);
			}
			
			mLocationTransformer.SetBoundingBox(resortData.mResortInfo.BoundingBox);
			
			mMapDrawings = mapDrawings;
			mTransformResortDataTask.execute(resortData);
		} catch (Exception e) {
			mTransformResortDataTask = null;
		}
	}
    
    protected static class TransformResortDataTask extends AsyncTask<ResortData, Integer, MapDrawings> {

		@Override
		protected MapDrawings doInBackground(ResortData... params) {
			try{
				mMapDrawings.Release();
	    	  	ResortData resortData = params[0];
	    	  	
	    	  	mMapDrawings.SetResortInfo(resortData.mResortInfo);
	    	  	
	    	  	mMapDrawings.SetTransformedBoundingBox((float)mLocationTransformer.TransformLatitude(resortData.mResortInfo.BoundingBox.top, resortData.mResortInfo.BoundingBox.right),
	    	  			(float)mLocationTransformer.TransformLongitude(resortData.mResortInfo.BoundingBox.bottom, resortData.mResortInfo.BoundingBox.right),
	    	  			(float)mLocationTransformer.TransformLatitude(resortData.mResortInfo.BoundingBox.bottom, resortData.mResortInfo.BoundingBox.right),
	    	  			(float)mLocationTransformer.TransformLongitude(resortData.mResortInfo.BoundingBox.bottom, resortData.mResortInfo.BoundingBox.left));
	    	  	
	    	  	for(int i = 0; i < resortData.Areas.size(); i++) {
	    	  		mMapDrawings.AddAreaDrawing(mAreaDrawingBO.CreateTransformedAreaDrawing(resortData.Areas.get(i), resortData.mResortInfo));
	    	  	}
	    	  	
	    	  	for(int i = 0; i < resortData.Trails.size(); i++) {
	    	  		mMapDrawings.AddTrailDrawing(mTrailDrawingBO.CreateTransformedTrailDrawing(resortData.Trails.get(i), resortData.mResortInfo));
	    	  	}
	
	    	  	for(int i = 0; i < resortData.POIs.size(); i++) {
	    	  		mMapDrawings.AddPOIDrawing(mPOIDrawingBO.CreateTransformedPOIDrawing(resortData.POIs.get(i), resortData.mResortInfo));
	    	  	}
			}
			catch (Exception e) {
				return null;
			}
    	  	
    	  	return mMapDrawings;
		}

		protected void onProgressUpdate(Integer... progress) {
			mMapDrawingsHandler.onProgressUpdate(progress[0]);
		}
  
		@Override
		protected void onPostExecute(MapDrawings mapDrawings) {
			Log.v(TAG,"onPostExecute(MapDrawings mapDrawings)");
			if(mapDrawings != null) {
				Log.v(TAG,"mMapDrawingsHandler.onPostExecute(MapDrawings mapDrawings)");
				mMapDrawingsHandler.onPostExecute(mapDrawings);
			}
			
			mTransformResortDataTask = null;
		}		
    }	
}
