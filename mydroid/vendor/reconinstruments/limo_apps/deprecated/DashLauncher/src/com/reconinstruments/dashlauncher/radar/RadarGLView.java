package com.reconinstruments.dashlauncher.radar;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class RadarGLView extends GLSurfaceView {
	private Context mContext;
	
	public RadarGLView(Context context) {
		super(context);
		mContext = context;
		
		setEGLConfigChooser(8, 8, 8, 8, 0, 0); // RGBA8888
		/*setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
			public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
				int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
				EGLConfig[] configs = new EGLConfig[1];
				int[] result = new int[1];
				egl.eglChooseConfig(display, attributes, configs, 1, result);
				return configs[0];
			}});*/
	}
}
