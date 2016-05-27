package com.reconinstruments.dashlauncher.radar.render;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.PointF;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.maps.objects.MapImagesInfo;

public class GLMapRenderer extends GLMapObject
{
	private static String TAG = "GLMapRenderer";
	
	protected int mMapSurfacesRenderesSize = 0;
    protected ArrayList<GLSurfaceTexture> mMapNTSurfacesRenderers = null;
    protected ArrayList<GLSurfaceTexture> mMapWTSurfacesRenderers = null;
    
    protected boolean mIsRadarMode 		= true;
    protected float mMaxDistanceToDraw 	= 0;
    protected PointF mUserLocation 		= new PointF();
    
    public GLMapRenderer()
    {
    	Log.i(TAG, "GLMapRenderer()");
    	mMapNTSurfacesRenderers = new ArrayList<GLSurfaceTexture>();
		mMapWTSurfacesRenderers = new ArrayList<GLSurfaceTexture>();
    } 
    
    public void setParams(boolean isRadarMode, float maxDistanceToDraw, PointF userLocation)
    {
    	//Log.i(TAG, "setParams(" + isRadarMode + ", " + maxDistanceToDraw + ", " + userLocation + ")");
    	mIsRadarMode 		= isRadarMode;
    	mMaxDistanceToDraw 	= maxDistanceToDraw;
    	
    	mUserLocation.set(userLocation); 
    }
    
    public void loadGLObjects()
    {
    	Log.i(TAG, "loadGLObjects()");
    	for(int i = 0; i < mMapNTSurfacesRenderers.size(); i++)
    	{
			mMapNTSurfacesRenderers.get(i).SetLoadOnDraw();
    	}
		for(int i = 0; i < mMapWTSurfacesRenderers.size(); i++)
		{
			mMapWTSurfacesRenderers.get(i).SetLoadOnDraw();
		}
    }
    
    public void setMapImages(MapImagesInfo mapImagesInfo)
	{
    	Log.i(TAG, "setMapImages()");
		mMapSurfacesRenderesSize = 0;
		for (int i = 0; i < mapImagesInfo.mMapImageFileBoundingBox.size(); i++)
		{
			if (i < mMapNTSurfacesRenderers.size())
			{
				mMapNTSurfacesRenderers.get(i).SetTextureFile(mapImagesInfo.mMapImageFileNameNoTrails.get(i), mapImagesInfo.mMapImageFileBoundingBox.get(i));
			}
			else
			{
				mMapNTSurfacesRenderers.add(new GLSurfaceTexture(mapImagesInfo.mMapImageFileNameNoTrails.get(i), mapImagesInfo.mMapImageFileBoundingBox.get(i)));
			}

			if (i < mMapWTSurfacesRenderers.size())
			{
				mMapWTSurfacesRenderers.get(i).SetTextureFile(mapImagesInfo.mMapImageFileNameWithTrails.get(i), mapImagesInfo.mMapImageFileBoundingBox.get(i));
			}
			else
			{
				mMapWTSurfacesRenderers.add(new GLSurfaceTexture(mapImagesInfo.mMapImageFileNameWithTrails.get(i), mapImagesInfo.mMapImageFileBoundingBox.get(i)));
			}
		}
		mMapSurfacesRenderesSize = mapImagesInfo.mMapImageFileBoundingBox.size();
	}
    
    @Override
	public void drawGL(GL10 gl)
    {
    	if(mAlpha == 0.0f) return;
    	
    	ArrayList<GLSurfaceTexture> mapSurfacesRendererArray = (mIsRadarMode) ? mMapNTSurfacesRenderers : mMapWTSurfacesRenderers; 
    	
    	if(mapSurfacesRendererArray.size() == 0){ return; }
    	
    	// -- Draw map texture grid
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		
		gl.glPushMatrix();
		gl.glTranslatef(mCamOffset.x, mCamOffset.y, mCamOffset.z);
		gl.glRotatef(mCamOrientation.y, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(mCamOrientation.x, 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(mUserOffset.x, mUserOffset.y, mUserOffset.z);
			 
		GLSurfaceTexture loadThisMapSurfaceRenderer = null;
		int shortestDistance = -1;

		for (int i = 0; i < mapSurfacesRendererArray.size(); i++)
		{
			GLSurfaceTexture mapSurfaceRenderer = mapSurfacesRendererArray.get(i);
			float xDistToUser = mapSurfaceRenderer.mBoundingBox.centerX() - (int) mUserLocation.x;
			float yDistToUser = mapSurfaceRenderer.mBoundingBox.centerY() - (int) mUserLocation.y;
			int distance = (int) Math.sqrt((xDistToUser * xDistToUser) + (yDistToUser * yDistToUser));
			if (distance < (mMaxDistanceToDraw + mapSurfaceRenderer.mBoundingBox.width() / 2 + mapSurfaceRenderer.mBoundingBox.height() / 2))
			{
				if (mapSurfaceRenderer.NeedsLoading())
				{
					if (shortestDistance < 0)
					{
						loadThisMapSurfaceRenderer = mapSurfaceRenderer;
						shortestDistance = distance;
					}
					else if (distance < shortestDistance)
					{
						loadThisMapSurfaceRenderer = mapSurfaceRenderer;
						shortestDistance = distance;
					}
				}
			}
			else
			{
				if (!mapSurfaceRenderer.NeedsLoading())
				{
					mapSurfaceRenderer.UnloadGLTexture(gl);
				}
			}
		}

		if (loadThisMapSurfaceRenderer != null)
			loadThisMapSurfaceRenderer.LoadGLTexture(gl);

		for (int i = 0; i < mapSurfacesRendererArray.size(); i++)
		{
			GLSurfaceTexture mapSurfaceRenderer = mapSurfacesRendererArray.get(i);
			if (!mapSurfaceRenderer.NeedsLoading())
			{
				mapSurfaceRenderer.draw(gl, mAlpha);
			}
		}

		gl.glPopMatrix();
	}
    
    public boolean hasRenderObjects()
    {
    	return (mMapSurfacesRenderesSize > 0);
    }
}
