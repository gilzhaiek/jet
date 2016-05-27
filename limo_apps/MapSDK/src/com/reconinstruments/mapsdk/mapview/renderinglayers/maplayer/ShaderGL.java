package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

public class ShaderGL {

	private static String TAG = "ShaderGL";
	private Context mContext;
	private int mHandle;
	private int type;
	private String mFilename;
	private String shaderCode;
	private Resources mRes;
	
	/**
	 * Shader object which holds its parsed code, shader handle, and reference
	 * to the assets folder
	 * 
	 * @param assets
	 *            - AssetManager passed from the main activity
	 * @param filename
	 *            - Name of the shader file to compile
	 * @param type
	 *            - Shader type (e.g. GLES20.GL_VERTEX_SHADER)
	 */
	public ShaderGL(Context context, Resources res, String filename, int type) {
		this.mContext = context;
		this.mRes = res;
		this.mFilename = filename;
		this.type = type;

		loadAndCompileShader();
	}

	/**
	 * Parses and compiles shader
	 */
	private void loadAndCompileShader() {

		InputStream is = null;
		int id = mRes.getIdentifier(mFilename, "raw", mContext.getPackageName());
		if(id == 0) {
			Log.e(TAG, "Could not find raw resource " + mFilename);
			return;
		}
		else {
			is = mRes.openRawResource(id);
			Log.d(TAG, "//// Shader: " + mFilename + " retrieved from res/raw!");
		}
		shaderCode = convertIStreamToString(is).replaceAll("\r\n", "");
		mHandle = GLES20.glCreateShader(type);

		GLES20.glShaderSource(mHandle, shaderCode);
		GLES20.glCompileShader(mHandle);
		checkForErrors();
	}

	private static String convertIStreamToString(InputStream is) {
		java.util.Scanner ss = new java.util.Scanner(is).useDelimiter("\\A");
		return ss.hasNext() ? ss.next() : "";
	}

	/**
	 * Checks for shader compile errors; any errors are recorded in
	 * {@code output.txt}
	 */
	private void checkForErrors() {
		int params[] = new int[1],
				infoLogLengthParams[] = new int[1];
		String resultMessage = "";
		try {
			GLES20.glGetShaderiv(mHandle, GLES20.GL_COMPILE_STATUS, params, 0);
			
			GLES20.glGetShaderiv(mHandle, GLES20.GL_INFO_LOG_LENGTH, infoLogLengthParams, 0);
			Log.d(TAG,"InfoLogLength = " + infoLogLengthParams[0]);
			resultMessage = GLES20.glGetShaderInfoLog(mHandle);
			if (params[0] == GLES20.GL_FALSE){
				Log.e(TAG,"ShaderGL error message for " + mFilename + ": " + resultMessage);
				throw new ShaderErrorException(resultMessage);
			}
			else {
				Log.i(TAG,"ShaderGL compile message for " + mFilename + ": " + resultMessage);
			}
		}
		// error occurred; let user know
		catch (ShaderErrorException se) {
			Log.e("Shader_Error_Tag", "Error happened");
			se.printStackTrace();
		}
	}

	// get methods
	/**
	 * 
	 * @return int - Shader handle
	 */
	public int getHandle() {
		return mHandle;
	}
	
	public String getFilename(){
		return mFilename;
	}

}
