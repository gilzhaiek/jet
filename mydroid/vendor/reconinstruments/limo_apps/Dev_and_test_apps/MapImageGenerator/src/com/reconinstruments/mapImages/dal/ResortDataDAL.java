package com.reconinstruments.mapImages.dal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.mapImages.common.CommonSettings;
import com.reconinstruments.mapImages.dbf.DbfContent;
import com.reconinstruments.mapImages.dbf.DbfRecord;
import com.reconinstruments.mapImages.dbf.DbfTools;
import com.reconinstruments.mapImages.objects.Area;
import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.objects.ResortData;
import com.reconinstruments.mapImages.objects.ResortElementsList;
import com.reconinstruments.mapImages.objects.ResortInfo;
import com.reconinstruments.mapImages.objects.Trail;
import com.reconinstruments.mapImages.prim.PointD;
import com.reconinstruments.mapImages.shp.ShpContent;
import com.reconinstruments.mapImages.shp.ShpPoint;
import com.reconinstruments.mapImages.shp.ShpPolygon;
import com.reconinstruments.mapImages.shp.ShpPolyline;
import com.reconinstruments.mapImages.shp.ShpRecord;
import com.reconinstruments.mapImages.shp.ShpTools;
import com.reconinstruments.mapImages.shp.ShpType;

public class ResortDataDAL {
	protected static final String TAG = "ResortDataDAL";
	
	protected static int MAX_POINTS = 1000;
	
	protected static ZippedRecordDAL	mZippedRecordDAL = null;
	protected ResortElementsList		mResortElementsList = null;
	protected Context					mContext = null;
	
	public ResortDataDAL(Context context, ResortElementsList resortElementsList){
		mResortElementsList = resortElementsList;
		mContext = context;
		
		if(mZippedRecordDAL == null) {
			mZippedRecordDAL = new ZippedRecordDAL();
		}
	}
	
	public void PrintAvailMem(){
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		Log.i(TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
	}
	
	public void FixBoundingBox(ResortData resortData, ResortInfo resortInfo) {
		// Top < Bottom ;  Left < Right
		for(int areaIndex = 0; areaIndex < resortData.Areas.size(); areaIndex++) {
			Area tmpArea = resortData.Areas.get(areaIndex);
			for(int pointIndex = 0; pointIndex < tmpArea.AreaPoints.size(); pointIndex++) {
				PointD tmpPointD = tmpArea.AreaPoints.get(pointIndex);
				if(tmpPointD.y < resortInfo.BoundingBox.top) {
					resortInfo.BoundingBox.top = (float)tmpPointD.y; 
				}
				else if(tmpPointD.y > resortInfo.BoundingBox.bottom) {
					resortInfo.BoundingBox.bottom = (float)tmpPointD.y; 
				}
				
				if(tmpPointD.x < resortInfo.BoundingBox.left) {
					resortInfo.BoundingBox.left = (float)tmpPointD.x; 
				}
				else if(tmpPointD.x > resortInfo.BoundingBox.right) {
					resortInfo.BoundingBox.right = (float)tmpPointD.x; 
				}
			}
		}
	}
	
	public void LoadResortData(AsyncTask<?, ?, ?> asyncTask, ResortData resortData, ResortInfo resortInfo) throws IOException {
		ZipFile zippedMap = null;
		
		try{
			resortData.Release();
			resortData.mResortInfo = resortInfo;
			
			Log.v(TAG, "Loading " + resortInfo.GetParentName() +"/"+ resortInfo.Name + "..."); PrintAvailMem();
			
			File mapDataZipFile = new File(CommonSettings.GetMapDataZipFileName());
			if( mapDataZipFile.exists() == false) {
				Log.e(TAG, CommonSettings.GetMapDataZipFileName() + " Doesn't Exist");
				throw new Exception(CommonSettings.GetMapDataZipFileName() + " Doesn't Exist");
			}
			else if( mapDataZipFile.canRead() == false) {
				Log.e(TAG, "Failed to read " + CommonSettings.GetMapDataZipFileName());
				throw new Exception("Failed to read " + CommonSettings.GetMapDataZipFileName());
			}
			
			zippedMap = new ZipFile( mapDataZipFile );
			
			if(asyncTask.isCancelled()) {
				return;
			}
			LoadRecords(asyncTask, resortData, "lines", resortInfo.AssetBaseName, zippedMap);
			
			if(asyncTask.isCancelled()) {
				return;
			}
			LoadRecords(asyncTask, resortData, "areas", resortInfo.AssetBaseName, zippedMap);
			
			if(asyncTask.isCancelled()) {
				return;
			}
			LoadRecords(asyncTask, resortData, "points", resortInfo.AssetBaseName, zippedMap);
			
			Log.v(TAG, "Finished Loading " + resortInfo.Name); PrintAvailMem();
			
			// TODO - Figure out what this is for
			//mReManager.verifyTopology();
		}
		catch (Exception e) {
			resortData.Release();
		}
		finally {
			if(zippedMap != null) {
				zippedMap.close();
			}
		}
	}
	
	protected String CleanTrailName(String name) {
		int idx = name.indexOf('(');
		return (idx == 0) ? "" : ((idx < 0) ? name : name.substring(0, idx - 1));
	}
	
	protected void AddPOI(ResortData resortData, ResortElementsList.EElementArea elementArea, ShpPoint shpPoint, DbfRecord dbfRecord){
		int poiType = mResortElementsList.GetElementValue(elementArea, ShpType.SHAPE_POINT, dbfRecord.GRMNTypeFieldString);
		
		if( poiType == POI.POI_TYPE_UNDEFINED )
			return;
		
		PointD poiLocation = new PointD( shpPoint.x, shpPoint.y );
			
		String poiName = dbfRecord.GetName();
		
		//Log.v(TAG, "POI Type["+poiType+"] - "+poiName+" At " + shpPoint.x + "," + shpPoint.y);
		
		resortData.AddPOI(new POI(poiType, poiName, poiLocation));
	}
	
	protected void AddTrails(ResortData resortData, ResortElementsList.EElementArea elementArea, ShpPolyline shpPolyline, DbfRecord dbfRecord){
		int trailType = mResortElementsList.GetElementValue(elementArea, ShpType.SHAPE_POLYLINE, dbfRecord.GRMNTypeFieldString);
		
		for( ArrayList<ShpPoint> shpPointsArray : shpPolyline.rings )
    	{
    		ArrayList<PointF> trailPoints = new ArrayList<PointF>(shpPointsArray.size());
    		for( ShpPoint shpPoint : shpPointsArray )
    		{
    			trailPoints.add( new PointF( (float)shpPoint.x, (float)shpPoint.y ));
    		}
    		
    		String trailName = CleanTrailName(dbfRecord.GetName());
   		
    		//Log.v(TAG, "Trail Type["+trailType+"] - "+trailName);
    		
    		resortData.AddTrail( new Trail(trailType, trailName, trailPoints, dbfRecord.GetSpeedLimit(), dbfRecord.IsOneWay()));
    	}
	}
		
	protected void AddAreas(ResortData resortData, ResortElementsList.EElementArea elementArea, ShpPolygon shpPolygon, DbfRecord dbfRecord){
		int areaType = mResortElementsList.GetElementValue(elementArea, ShpType.SHAPE_POLYGON, dbfRecord.GRMNTypeFieldString);
		    	
    	for( ArrayList<ShpPoint> shpPointsArray : shpPolygon.rings )
    	{
    		ArrayList<PointD> areaPoints = new ArrayList<PointD>( shpPointsArray.size() );
    		//Log.v(TAG, "areaPoints " + shpPointsArray.size());
    		for( ShpPoint shpPoint : shpPointsArray )
    		{
    			areaPoints.add( new PointD( shpPoint.x, shpPoint.y ) );
    		}
    		
    		String areaName = dbfRecord.GetName();
    		resortData.AddArea(new Area( areaType, areaName,  areaPoints));
    	}
	}
	
	protected void CreateMapElements(ResortData resortData, ResortElementsList.EElementArea elementArea, ShpRecord shpRecord, DbfRecord dbfRecord)
	{	
		if(shpRecord.shapeType == ShpType.SHAPE_POINT) {
			AddPOI(resortData, elementArea, (ShpPoint)shpRecord.shape, dbfRecord);
		}
		else if(shpRecord.shapeType == ShpType.SHAPE_POLYLINE) {
			AddTrails(resortData, elementArea, (ShpPolyline)shpRecord.shape, dbfRecord);
		}
		else if(shpRecord.shapeType == ShpType.SHAPE_POLYGON) {
			AddAreas(resortData, elementArea, (ShpPolygon)shpRecord.shape, dbfRecord);
		}
	}	
	
	protected void AddRecords(AsyncTask<?, ?, ?> asyncTask, ResortData resortData, ByteBuffer shapeStream, ByteBuffer dbStream) throws Exception {
		if(shapeStream == null) throw new Exception ("shapeStream is null");
		if(dbStream == null) throw new Exception ("dbStream is null");
		
		ShpContent shpContent = null;
		DbfContent dbfContent = null;
		try {
			shpContent = ShpTools.ReadRecords( shapeStream );
			if(shpContent == null) throw new Exception ("ShpContent wasn't loaded correctly");
			
			if(asyncTask.isCancelled()) return;
			
			dbfContent = DbfTools.ReadRecords( dbStream );
			if(dbfContent == null) throw new Exception ("dbfContent wasn't loaded correctly");
						
			ResortElementsList.EElementArea elementArea = IsInAmericas(shpContent.shpRecords.get(0)) ?
					ResortElementsList.EElementArea.eAmericas : ResortElementsList.EElementArea.eNotAmericas;  
			
			int idx = 0;
			for( ShpRecord shpRecord : shpContent.shpRecords )
			{
				if(asyncTask.isCancelled()) return;
				DbfRecord dbfRecord = dbfContent.dbfRecords[idx++];
				if(dbfRecord.HasGRMNType) {
					if(mResortElementsList.HasElementType(elementArea, shpRecord.shapeType, dbfRecord.GRMNTypeFieldString)) {
						CreateMapElements(resortData, elementArea, shpRecord, dbfRecord);
					}
				}
			}	
		} catch (Exception e) { }
		finally {
			if(shpContent != null) {
				shpContent.Release();
			}
			if(dbfContent != null) {
				dbfContent.Release();
			}
		}
	}
	
	protected void LoadRecords(AsyncTask<?, ?, ?> asyncTask, ResortData resortData, String dirName, String assetName, ZipFile zippedRecords) throws Exception{
		ByteBuffer shapeByteBuffer = mZippedRecordDAL.CreateByteBuffer(zippedRecords, dirName + "/" + assetName + ".shp", dirName + "/" + assetName + ".SHP");
		ByteBuffer dbByteBuffer = mZippedRecordDAL.CreateByteBuffer(zippedRecords, dirName + "/" + assetName + ".dbf", null);
		if(asyncTask.isCancelled()) return;
		AddRecords(asyncTask, resortData, shapeByteBuffer,dbByteBuffer);
	}
	
	protected boolean IsInAmericas(ShpRecord record){
		double tmpLongitude = -90; // Americas
		  
		switch( record.shapeType ) 
		{                
			case ShpType.SHAPE_POINT: tmpLongitude = ((ShpPoint)record.shape).x; break;
			case ShpType.SHAPE_POLYLINE: tmpLongitude = ((ShpPolyline)record.shape).box.topLeft.x; break;
			case ShpType.SHAPE_POLYGON:
				tmpLongitude = ((ShpPolygon)record.shape).box.topLeft.x; break;
		}
		
		return (tmpLongitude < -34);
	}
}
