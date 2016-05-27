package com.reconinstruments.dashlauncher.radar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.radar.render.CommonRender;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

abstract  public class BaseRendererHelper implements GLSurfaceView.Renderer  {
	private final static String BASE_TAG = "BaseRendererHelper";
	
	protected Context mContext = null;
		
	// Fog
	protected final static float	FOG_START			= 0.90f;//0.85f;
	protected final static float	FOG_END				= 1.20f;
	protected final static float	FOG_DENSITY			= 0.08f;

	
	protected static float			mAspect			= 1.0f;
	
	public BaseRendererHelper(Context context){
		mContext = context;
	}
	
	public void onResume() {}
	
	public void onPause() {}
	
	public void PrintAvailMem(){
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		Log.i(BASE_TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
	}	
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	}

	public void onDrawFrame(GL10 gl) {
	} 

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mAspect = (float)width/(float)height;
	}
	
	// Set up the GL fog
	protected void SetFog(GL10 gl, float maxFogDistance)
	{
		gl.glFogf(GL10.GL_FOG_MODE, GL10.GL_LINEAR);
		gl.glFogf(GL10.GL_FOG_START, maxFogDistance*FOG_START);
		gl.glFogf(GL10.GL_FOG_END, maxFogDistance*FOG_END);
		gl.glFogf(GL10.GL_FOG_DENSITY, FOG_DENSITY);
		
		float[] color = {CommonRender.FOG_GRAY_COLOR_R, CommonRender.FOG_GRAY_COLOR_G, CommonRender.FOG_GRAY_COLOR_B, 1.0f}; 
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(color.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		gl.glFogfv(GL10.GL_FOG_COLOR, (FloatBuffer) byteBuffer.asFloatBuffer().put(color).position(0));
		gl.glEnable(GL10.GL_FOG);
		gl.glHint(GL10.GL_FOG_HINT, GL10.GL_DONT_CARE);
	}		
}
