package com.reconinstruments.dashlauncher.radar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class RadarGLRenderer implements GLSurfaceView.Renderer {
	private BaseRendererHelper mBaseRendererHelper = null;

	// Settings
	protected final static float BG_GRAY_COLOR	= 0.2f;  // Background Gray Color
	

	public void SetRendererHelper(BaseRendererHelper rendererHelper) {
		mBaseRendererHelper = rendererHelper;
	}
	
	public BaseRendererHelper GetRendererHelper() {
		return mBaseRendererHelper;
	}	
	
	public void onDrawFrame(GL10 gl) {
		if(mBaseRendererHelper != null) mBaseRendererHelper.onDrawFrame(gl);
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if(height == 0){ height = 1; }

		gl.glViewport(0, 0, width, height); 	// Reset  The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	// Select The Projection Matrix
		gl.glLoadIdentity(); 					// Reset  The Projection Matrix

		// Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 50.0f, (float)width / (float)height, 0.1f, 20000.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); 	// Select The Modelview Matrix
		gl.glLoadIdentity();					// Reset  The Modelview Matrix
		
		if(mBaseRendererHelper != null) mBaseRendererHelper.onSurfaceChanged(gl, width, height);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glShadeModel(GL10.GL_SMOOTH); 								// Enable Smooth Shading
		gl.glClearColor(BG_GRAY_COLOR, BG_GRAY_COLOR, BG_GRAY_COLOR, 1.0f);
		gl.glClearDepthf(1.0f); 										// Depth Buffer Setup
		gl.glEnable(GL10.GL_DEPTH_TEST); 								// Enables Depth Testing
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glLineWidth(3.0f);
		gl.glEnable(GL10.GL_BLEND);
		gl.glDepthFunc(GL10.GL_LEQUAL); 								 // The Type Of Depth Testing To Do		
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);	
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); // Really Nice Perspective Calculations
		
		if(mBaseRendererHelper != null) mBaseRendererHelper.onSurfaceCreated(gl, config);
	}

}
