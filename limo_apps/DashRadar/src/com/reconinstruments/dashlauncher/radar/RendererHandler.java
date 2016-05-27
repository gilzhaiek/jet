package com.reconinstruments.dashlauncher.radar;

import android.opengl.GLSurfaceView;

public class RendererHandler {
	GLSurfaceView mGLSurfaceView = null; 
	
	public void SetGLSurfaceView(GLSurfaceView glSurfaceView) {
		mGLSurfaceView = glSurfaceView;
	}
	
	public void RedrawScene(){
		if(mGLSurfaceView != null)
			mGLSurfaceView.requestRender();
	}
}
