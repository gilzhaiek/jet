package com.reconinstruments.dashlauncher.radar;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.radar.render.DirectionLabel;
import com.reconinstruments.dashlauncher.radar.render.LngLatLabel;
import com.reconinstruments.dashlauncher.radar.render.TextureSquare;

import android.content.Context;
import android.location.Location;
import android.opengl.GLU;
import android.util.Log;

public class CompassViewRendererHelper extends BaseRendererHelper {
	private final static String TAG = "CompassViewRendererHelper";

	private static boolean loadLngLat = true;
	
	Context mContext = null;
	
	
	ArrayList<DirectionLabel> directionList = new ArrayList<DirectionLabel>(16);
	TextureSquare maskTexture = new TextureSquare(0);
	LngLatLabel lngLatLabel = new LngLatLabel();

	
	private int directionImg[] = {
			R.drawable.compass_e, R.drawable.compass_dots_left, R.drawable.compass_ne , R.drawable.compass_dots_right,
			R.drawable.compass_n, R.drawable.compass_dots_left, R.drawable.compass_nw , R.drawable.compass_dots_right,
			R.drawable.compass_w, R.drawable.compass_dots_left, R.drawable.compass_sw , R.drawable.compass_dots_right,
			R.drawable.compass_s, R.drawable.compass_dots_left, R.drawable.compass_se , R.drawable.compass_dots_right};

	private float mYaw = 0.0f;
	
	private Location mLocation = null;

	
	public CompassViewRendererHelper(Context context) {
		super(context);
		
		mContext = context;

		for (int i = 0 ; i < 16 ; i++){
			DirectionLabel tempS = new DirectionLabel(i);
			directionList.add(tempS);
		}		
	}
	
	public void OnLocationHeadingChanged(Location location, float yaw, float pitch, boolean gpsYaw) {
		mYaw = yaw;
		mLocation = location;
	}
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		
		for ( int i = 0 ; i < directionList.size() ; i++ ){
				directionList.get(i).loadGLTexture(gl, mContext, directionImg[i]);
		}

		maskTexture.loadGLTexture(gl, mContext, R.drawable.compass_mask);
		lngLatLabel.loadGLTexture(gl, mLocation);
		
		// OpenGL parameters we need to set
		gl.glShadeModel(GL10.GL_SMOOTH); 								// Enable Smooth Shading
		gl.glClearColor(0.43f, 0.43f, 0.43f, 1.0f); 						// Background Color
		gl.glClearDepthf(1.0f); 										// Depth Buffer Setup
		gl.glEnable(GL10.GL_DEPTH_TEST); 								// Enables Depth Testing
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glLineWidth(3.0f);
		gl.glEnable(GL10.GL_BLEND);
		gl.glDepthFunc(GL10.GL_LEQUAL); 								 // The Type Of Depth Testing To Do		
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);	
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST); // Really Nice Perspective Calculations		
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		float yaw   = mYaw;

		// Reset the Modelview Matrix
		gl.glLoadIdentity();
		gl.glPushMatrix();
		gl.glTranslatef(0, 0, 5);
		gl.glRotatef(yaw, 0.0f, 1.0f, 0.0f);	
		

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		for (DirectionLabel ts : directionList )
			ts.draw(gl);
		gl.glPopMatrix();


		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		maskTexture.draw(gl);
		
		if (loadLngLat){
			lngLatLabel.loadGLTexture(gl, mLocation);
			loadLngLat = !loadLngLat;
		}
		
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		lngLatLabel.draw(gl);
				
		//Log.v(TAG,"onDrawFrame");
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		
		if(height == 0){ height = 1; }

		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix

		//Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 40.0f, (float)width / (float)height, 0.1f, 20000.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 			
	}
}
