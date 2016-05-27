package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderProgram {

	private ShaderGL mVertShader = null, mFragShader = null;
	private int mProgramHandle = -1;
	private boolean mProgramLinked = false;
	
	public ShaderProgram(ShaderGL vert, ShaderGL frag){
		mVertShader = vert;
		mFragShader = frag;
		
		linkProgram();
	}
	
	/**
	 * Attaches and links shaders
	 */
	private void linkProgram() {
		try {
			if (mVertShader != null && mFragShader != null) {
				mProgramHandle = GLES20.glCreateProgram();
				GLES20.glAttachShader(mProgramHandle, mVertShader.getHandle());
				GLES20.glAttachShader(mProgramHandle, mFragShader.getHandle());
				GLES20.glLinkProgram(mProgramHandle);
				checkForLinkErrors();
			} else {
				throw new ShaderErrorException(
						"Vertex and/or Fragment shader is null!");
			}
		} catch (ShaderErrorException e) {
			Log.e("Shader_Error_Tag", e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks for shader linking errors
	 */
	private void checkForLinkErrors(){
		int[] params = new int[1];
		String errorMessage = "";
		GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, params, 0);
		try{
		if(params[0] == GLES20.GL_FALSE){
			throw new ShaderErrorException(
					errorMessage = GLES20.glGetProgramInfoLog(mProgramHandle));
		}
		} catch (ShaderErrorException e) {
			Log.e("Shader_Error_Tag", errorMessage);
			e.printStackTrace();
		}
	}
	
//	public void setVertShader(ShaderGL vert){
//		mVertShader = vert;
//		mProgramLinked = false;
//	}
//	
//	public void setFragShader(ShaderGL frag){
//		mFragShader = frag;
//		mProgramLinked = false;
//	}
	
	public int getHandle(){
		return mProgramHandle;
	}
	
	public ShaderGL getVertShader(){
		return mVertShader;
	}
	
	public ShaderGL getFragShader(){
		return mFragShader;
	}
	
	public boolean isProgramLinked(){
		return mProgramLinked;
	}

}
