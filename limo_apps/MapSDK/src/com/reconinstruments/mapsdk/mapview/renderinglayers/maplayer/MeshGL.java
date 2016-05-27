package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import com.reconinstruments.mapsdk.R;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Defines a generic mesh with vertex data. Note that shaders should be compiled before 
 * passing it into this class.
 * @author jinkim
 *
 */
public class MeshGL {
	
	private static final String TAG = "MeshGL";
	private static final int COORDS_PER_VERTEX = 3, TEX_COORDS_PER_VERTEX = 2;

	private boolean firstRun = true;
	public boolean enableAlphaTesting = false;
	public boolean enableDrawingText = false;
	public boolean enableRadialGradient = false;
	public float radialGradientStart = 0f;
	public float radialGradientEnd = 0f;

	public float[] mModelMatrix, mViewMatrix, mProjMatrix, color;
	private int mPositionAttribHandle = -1, mTextureAttribHandle = -1, mColorUniformHandle = -1, mMVPMatrixIndexHandle = -1;
	private int mTextureHandle = -1;
	private int mModelMatrixHandle, mViewMatrixHandle, mProjMatrixHandle;
	private FloatBuffer mVertBuffer, mTexBuffer, mNormBuffer;
	private boolean mVertsLoaded = false, mTexturesLoaded = false, mNormalsLoaded = false;
	private boolean isInitialized = false;
	private float[] mVertices = null, mTextures = null, mNormals = null;
	private float mBoundingRadius;

	private int mEnableAlphaTestUniform = 0;
	private int mEnableDrawingTextUniform = 0, mTextSharpnessUniform = 0;
	private int mEnableRadialGradientUniform = 0, mCenterOfRadiusUniform = 0, mRadialGradientBeginUniform = 0, mRadialGradientEndUniform = 0;
	
	private Bitmap mTextureBitmap = null;
	private Bitmap mLastBitmapLoaded = null;
	private ShaderProgram mShaderProgram;
	
	public static enum GLBufferType {
		VERTEX,
		TEXTURE,
		NORMAL
	};

	public static enum MeshType {
		MAP,
		USER_ICON,
		RETICLE,
		RETICLE_ITEM,
		POI
	};
	
	private MeshType mMeshType;

	public MeshGL(MeshType meshType, ShaderProgram shaderProgram) {
		mShaderProgram = shaderProgram;
		mMeshType = meshType;
	}

	public MeshGL(MeshType meshType, ShaderProgram shaderProgram, float[] vertices, float[] textures, float[] normals, float boundingRadius) {
		mShaderProgram = shaderProgram;
		mVertices = vertices;
		mTextures = textures;
		mNormals = normals;
		mMeshType = meshType;
		init(vertices, textures, normals, boundingRadius);
	}
	
//	//copy constructor
	public MeshGL(MeshGL other){
		this.mShaderProgram = other.mShaderProgram;
		this.mVertices = (other.mVertices != null) ? other.mVertices.clone() : null;
		this.mTextures = (other.mTextures != null) ? other.mTextures.clone() : null;
		this.mNormals = (other.mNormals != null) ? other.mNormals.clone(): null;
		this.mMeshType = other.mMeshType;
		init(mVertices, mTextures, null, other.mBoundingRadius);
		
	}

	public void init(float[] vertices, float[] textures, float[] normals, float boundingRadius) {
		if(vertices != null) initBuffer(GLBufferType.VERTEX, vertices);
		if(textures != null) initBuffer(GLBufferType.TEXTURE, textures);
//		if(normals != null) initBuffer(GLBufferType.NORMAL, normals);
		mModelMatrix = new float[16];
		color = new float[]{0f, 0f, 0f, 1f};
		mBoundingRadius = boundingRadius;
		
		//Attributes
		mPositionAttribHandle = GLES20.glGetAttribLocation(mShaderProgram.getHandle(), "vPosition");
		mTextureAttribHandle = GLES20.glGetAttribLocation(mShaderProgram.getHandle(), "vTexture");
		mMVPMatrixIndexHandle = GLES20.glGetAttribLocation(mShaderProgram.getHandle(), "a_MVPMatrixIndex");
		mModelMatrixHandle = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "modelMatrix");
		mViewMatrixHandle = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "viewMatrix");
		mProjMatrixHandle = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "projMatrix");
		
		mColorUniformHandle = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_Color");
		
		//Text rendering uniforms
        mEnableDrawingTextUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_EnableDrawingText");
        mEnableAlphaTestUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_EnableAlphaTest");
        mTextSharpnessUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_TextSharpness");
		
		//Radial gradient uniforms
        mEnableRadialGradientUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_EnableRadialGradient");
        mCenterOfRadiusUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_CenterOfRadius");
        mRadialGradientBeginUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_RadialGradientBegin");
        mRadialGradientEndUniform = GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "u_RadialGradientEnd");
        isInitialized = true;
	}
	
	private void initBuffer(GLBufferType type, float[] data) {
		
		// allocate a buffer to hold floats
		// float = 32bits = 4bytes
		ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);

		// set the byte order to the device's native byte order
		bb.order(ByteOrder.nativeOrder());

		switch(type){
		case VERTEX:
			mVertices = data;
			
			mVertBuffer = bb.asFloatBuffer();

			// actually put data in this buffer
			mVertBuffer.put(mVertices);

			// set to read from position 0
			mVertBuffer.position(0);

			mVertsLoaded = true;
			break;
		case TEXTURE:
			mTextures = data;
			
			mTexBuffer = bb.asFloatBuffer();

			// actually put data in this buffer
			mTexBuffer.put(mTextures);

			// set to read from position 0
			mTexBuffer.position(0);

			mTexturesLoaded = true;
			break;
		case NORMAL:
			mNormals = data;
			
			mNormBuffer = bb.asFloatBuffer();

			// actually put data in this buffer
			mNormBuffer.put(mNormals);

			// set to read from position 0
			mNormBuffer.position(0);

			mNormalsLoaded = true;
			break;
		}
	}
	
	/**
	 * Loads texture into OpenGL using an existing Bitmap object. This method checks if
	 * {@code image} is a duplicate before applying the texture, to avoid uploading the same bitmap twice.  
	 * @param image Bitmap to load
	 * @param forceNewTexture set to {@code true} if you want to forcefully upload
	 * 						  a new texture to OpenGL
	 * @param textureUnit optional texture unit to set
	 * @return
	 */
	public int loadMeshTexture(Bitmap image, boolean forceNewTexture, int textureUnit){
		mTextureBitmap = image;
		if(mTextureHandle == -1){
			int[] textureHandle = new int[1];
			GLES20.glGenTextures(1, textureHandle, 0);
			mTextureHandle = textureHandle[0];
			if(textureHandle[0] == -1){
				Log.d(TAG, "ERROR: Texture could not be initialized!");
			}
		}
		
		if (image == null) {
//			Log.e(TAG, "ERROR: Bitmap of type: " + mMeshType.toString() + " is null!");
			return -1;
		}
		
		
		GLES20.glUseProgram(mShaderProgram.getHandle());
		
		// Bind texture to use
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "texSampler"), 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);

		if (forceNewTexture) {
			// set texture filtering methods
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
			
			if(firstRun) {
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);
				firstRun = false;
			}
			else {
				GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, image);
			}
//			Log.d(TAG,">>>>>> Texture reloaded!!");
		}
		return mTextureHandle;
	}
	
	/**
	 * Loads texture into OpenGL. Creates a Bitmap using a provided Resource ID.
	 * @param res
	 * @param id
	 * @param bitmapOptions
	 * @param forceNewTexture
	 * @return
	 */
	public int loadMeshTexture(Resources res, int id, Options bitmapOptions, boolean forceNewTexture, int textureUnit){
		return loadMeshTexture(BitmapFactory.decodeResource(res, id, bitmapOptions), forceNewTexture, textureUnit);
	}
	
	public int loadMeshTexture(Bitmap image, boolean forceNewTexture){
		return loadMeshTexture(image, forceNewTexture, 0);
	}
	
	public void drawMesh(float[] viewMatrix, float[] projMatrix){
		mViewMatrix = viewMatrix;
		mProjMatrix = projMatrix;
		GLES20.glUseProgram(mShaderProgram.getHandle());
		
//		GLES20.glEnable(GLES20.GL_BLEND)
		
		/************Uniform Enables and Disables****************/
		//disable drawing text in the shaders
		GLES20.glUniform1f(mEnableDrawingTextUniform, (enableDrawingText) ? 1f : 0f);
		GLES20.glUniform1f(mEnableAlphaTestUniform, (enableAlphaTesting) ? 1f : 0f);
		GLES20.glUniform1f(mTextSharpnessUniform, 0.0f);
		GLES20.glUniform1f(mEnableRadialGradientUniform, (enableRadialGradient) ? 1f : 0f);
				
		/******************** Vertex Shader **************************/
		GLES20.glEnableVertexAttribArray(mPositionAttribHandle);
		GLES20.glVertexAttribPointer(mPositionAttribHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mVertBuffer);
		
		GLES20.glEnableVertexAttribArray(mTextureAttribHandle);
		GLES20.glVertexAttribPointer(mTextureAttribHandle, TEX_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mTexBuffer);
		
		//disable this attribute since we aren't using it here
		GLES20.glDisableVertexAttribArray(mMVPMatrixIndexHandle);
		GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mModelMatrix, 0);
		GLES20.glUniformMatrix4fv(mViewMatrixHandle, 1, false, viewMatrix, 0);
		GLES20.glUniformMatrix4fv(mProjMatrixHandle, 1, false, projMatrix, 0);
		
		
		/******************** Fragment Shader **************************/
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(mShaderProgram.getHandle(), "texSampler"), 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);
		GLES20.glUniform1fv(mColorUniformHandle, 1, color, 0);
		
		GLES20.glUniform3f(mCenterOfRadiusUniform, 0, 0, 0);
		GLES20.glUniform1f(mRadialGradientBeginUniform, radialGradientStart);
		GLES20.glUniform1f(mRadialGradientEndUniform, radialGradientEnd);
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, getNumVertices());
		GLES20.glDisableVertexAttribArray(mPositionAttribHandle);
		GLES20.glDisableVertexAttribArray(mTextureAttribHandle);
	}

	/**
	 * Sets the vertex data for this mesh. Set only once at initialization.
	 * 
	 * @param verts
	 */
	public void setVertexData(float[] verts) {
		if (!mVertsLoaded) {
			mVertices = verts;
			initBuffer(GLBufferType.VERTEX, mVertices);
		}
	}
	
	/**
	 * Sets the texture data for this mesh. Set only once at initialization.
	 * 
	 * @param tex
	 */
	public void setTextureData(float[] tex) {
		if (!mTexturesLoaded) {
			mTextures = tex;
			initBuffer(GLBufferType.TEXTURE, mTextures);
		}
	}
	
	/**
	 * Sets the normal data for this mesh. Set only once at initialization.
	 * 
	 * @param norms
	 */
	public void setNormalData(float[] norms) {
		if (!mNormalsLoaded) {
			mNormals = norms;
			initBuffer(GLBufferType.NORMAL, mNormals);
		}
	}
	
	/**
	 * 
	 * @return whether this mesh is initialized and ready to render
	 */
	public boolean isInitialized(){
		return isInitialized;
	}
	
	public Bitmap getTextureBitmap(){
		return mTextureBitmap;
	}
	
	public int getNumVertices(){
		return mVertices.length/COORDS_PER_VERTEX;
	}
	
	public int getCoordsPerVertex(){
		return COORDS_PER_VERTEX;
	}
	
	public int getTexCoordsPerVertex(){
		return TEX_COORDS_PER_VERTEX;
	}
	
	public float[] getVertices(){
		return mVertices;
	}
	
	public float[] getTextures(){
		return mTextures;
	} 
	
	public float[] getNormals(){
		return mNormals;
	}

	public FloatBuffer getVertBuffer(){
		return mVertBuffer;
	}
	
	public FloatBuffer getTexBuffer(){
		return mTexBuffer;
	}
	
	public FloatBuffer getNormBuffer(){
		return mNormBuffer;
	}
	
	public int getTextureHandle(){
		return mTextureAttribHandle;
	}
	
	public MeshType getMeshType(){
		return mMeshType;
	}
	
	public ShaderProgram getShaderProgram(){
		return mShaderProgram;
	}
	
	public float getMeshBoundingRadius(){
		return mBoundingRadius;
	}
}
