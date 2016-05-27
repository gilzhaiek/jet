// This is a OpenGL ES 1.0 dynamic font rendering system. It loads actual font
// files, generates a font map (texture) from them, and allows rendering of
// text strings.
//
// NOTE: the rendering portions of this class uses a sprite batcher in order
// provide decent speed rendering. Also, rendering assumes a BOTTOM-LEFT
// origin, and the (x,y) positions are relative to that, as well as the
// bottom-left of the string to render.

package com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.reconinstruments.mapsdk.mapview.LoadFromResourceUtil;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.GLHelper;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;

public class GLText {

	//--Constants--//
	public final static int CHAR_START = 32;           // First Character (ASCII Code)
	public final static int CHAR_END = 126;            // Last Character (ASCII Code)
	public final static int CHAR_CNT = ( ( ( CHAR_END - CHAR_START ) + 1 ) + 1 );  // Character Count (Including Character to use for Unknown)

	public final static int CHAR_NONE = 32;            // Character to Use for Unknown (ASCII Code)
	public final static int CHAR_UNKNOWN = ( CHAR_CNT - 1 );  // Index of the Unknown Character

	public final static int FONT_SIZE_MIN = 6;         // Minumum Font Size (Pixels)
	public final static int FONT_SIZE_MAX = 180;       // Maximum Font Size (Pixels)

	public final static int CHAR_BATCH_SIZE = 24;     // Number of Characters to Render Per Batch
													  // must be the same as the size of u_MVPMatrix 
													  // in BatchTextProgram
	
	/**
	 * Sharpness of text (i.e. fragments with alpha values
	 * lower than this value will be rejected, making the 
	 * resulting texture look less smooth) 
	 */
	public final static float TEXT_SHARPNESS = 0.45f;
	
	private static final String TAG = "GLTEXT";
	
	private static final float PI = 3.141592f;
	private static final float DEG2RAD = PI / 180f;
	private static final float RAD2DEG = 180f / PI;

	//--Members--//
	Context mContext;
	Resources res;
	SpriteBatch batch;                                 // Batch Renderer

	int fontPadX, fontPadY;                            // Font Padding (Pixels; On Each Side, ie. Doubled on Both X+Y Axis)

	float fontHeight;                                  // Font Height (Actual; Pixels)
	float fontAscent;                                  // Font Ascent (Above Baseline; Pixels)
	float fontDescent;                                 // Font Descent (Below Baseline; Pixels)

	int textureId;                                     // Font Texture ID [NOTE: Public for Testing Purposes Only!]
	int textureSize;                                   // Texture Size for Font (Square) [NOTE: Public for Testing Purposes Only!]
	TextureRegion textureRgn;                          // Full Texture Region

	float charLengthConverted = 0f, charHeightConverted = 0;
	float charWidthMax;                                // Character Width (Maximum; Pixels)
	float charHeight;                                  // Character Height (Maximum; Pixels)
	float[] charWidths;                          // Width of Each Character (Actual; Pixels)
	TextureRegion[] charRgn;                           // Region of Each Character (Texture Coordinates)
	int cellWidth, cellHeight;                         // Character Cell Width/Height
	int rowCnt, colCnt;                                // Number of Rows/Columns

	float scaleX, scaleY;                              // Font Scale (X,Y Axis)
	float spaceX;                                      // Additional (X,Y Axis) Spacing (Unscaled)
	
	float textPosX, textPosY, textPosZ;
	
	private ShaderProgram mProgram; 						   // OpenGL Program object
	
	//Uniforms
	private int mColorHandle;						   // Shader color handle	
	private int mTextureUniformHandle;                 // Shader texture handle
	private int mEnableAlphaTestUniform = 0;
	private int mTextSharpnessUniform = 0;
	private int mEnableDrawingTextUniform = 0;
	private int mEnableRadialGradientUniform = 0;
	private int mCenterOfRadiusUniform = 0;
	private int mRadialGradientBeginUniform = 0;
	private int mRadialGradientEndUniform = 0;

	private int mModelMatrixHandle, mViewMatrixHandle, mProjMatrixHandle;
	
	private int SCREEN_WIDTH = 0, SCREEN_HEIGHT = 0;
	private float[] mInvertedMVP, mVPMatrix;
	private float zNearFactor = 0f;
	
	private float[] mModelMatrix, mViewMatrix, mProjMatrix, mIdentityMatrix;
	private float[] mCameraPos, mTextToCamVec, mCamLookAtVec;
	float mCameraHeading, mCameraAltitudeScale;
	float mAngleToFaceCamera = 0;
	
	/**
	 * This vector represents where the text face is looking.
	 * By default, it's looking along the +Z axis (out of the screen)
	 */
	private float[] mTextLookAtVec;
	
	private float[] DEFAULT_LOOK_AT_VEC;
	
	/**
	 * temporary variable to be reused
	 */
	private float[] ndcCoordsVec, worldCoordsVec;
	
	private float[] vectorA, vectorB, axisOfRotation;
	
	NumberFormat mNumFormat;


	//--Constructor--//
	// D: save program + asset manager, create arrays, and initialize the members
	public GLText(Context context, String vertShader, String fragShader, ShaderProgram program) {
		mContext = context;
		this.res = context.getResources();		
		if (program == null) {
			ShaderGL vert = new ShaderGL(context, res, vertShader, GLES20.GL_VERTEX_SHADER),
					 frag = new ShaderGL(context, res, fragShader, GLES20.GL_FRAGMENT_SHADER);
			program = new ShaderProgram(vert, frag);
		}
		
		batch = new SpriteBatch(CHAR_BATCH_SIZE, program );  // Create Sprite Batch (with Defined Size)

		charWidths = new float[CHAR_CNT];               // Create the Array of Character Widths
		charRgn = new TextureRegion[CHAR_CNT];          // Create the Array of Character Regions
		
		mInvertedMVP = new float[16];
		ndcCoordsVec = new float[4];
		worldCoordsVec = new float[4];
		mTextToCamVec = new float[3];
		mCamLookAtVec = new float[3];
		vectorA = new float[3];
		vectorB = new float[3];
		axisOfRotation = new float[3];
		
		mTextLookAtVec = new float[]{0, 0, 1f};
		DEFAULT_LOOK_AT_VEC = new float[]{0, 0, 1};

		// initialize remaining members
		fontPadX = 0;
		fontPadY = 0;

		fontHeight = 0.0f;
		fontAscent = 0.0f;
		fontDescent = 0.0f;

		textureId = -1;
		textureSize = 0;

		charWidthMax = 0;
		charHeight = 0;

		cellWidth = 0;
		cellHeight = 0;
		rowCnt = 0;
		colCnt = 0;

		scaleX = 1.0f;                                  // Default Scale = 1 (Unscaled)
		scaleY = 1.0f;                                  // Default Scale = 1 (Unscaled)
		spaceX = 0.0f;
		
		mModelMatrix = new float[16];
		mViewMatrix = new float[16];
		mProjMatrix = new float[16];
		mIdentityMatrix = new float[16];
		mVPMatrix = new float[16];
		
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.setIdentityM(mViewMatrix, 0);
		Matrix.setIdentityM(mProjMatrix, 0);
		Matrix.setIdentityM(mIdentityMatrix, 0);

		// Initialize the color and texture handles
		mProgram = program; 
		mColorHandle = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_Color");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram.getHandle(), "texSampler");

        //Text rendering uniforms
        mEnableDrawingTextUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_EnableDrawingText");
        mEnableAlphaTestUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_EnableAlphaTest");
        mTextSharpnessUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_TextSharpness");

        //Radial gradient uniforms
        mEnableRadialGradientUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_EnableRadialGradient");
        mCenterOfRadiusUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_CenterOfRadius");
        mRadialGradientBeginUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_RadialGradientBegin");
        mRadialGradientEndUniform = GLES20.glGetUniformLocation(mProgram.getHandle(), "u_RadialGradientEnd");

        //Transformation matrices
        mModelMatrixHandle = GLES20.glGetUniformLocation(mProgram.getHandle(), "modelMatrix");
		mViewMatrixHandle = GLES20.glGetUniformLocation(mProgram.getHandle(), "viewMatrix");
		mProjMatrixHandle = GLES20.glGetUniformLocation(mProgram.getHandle(), "projMatrix");
		
		mNumFormat = new DecimalFormat("#0.00");  
	}
	
	// Constructor using the default program (BatchTextProgram)
	public GLText(Context context, String vertShader, String fragShader) {
		this(context, vertShader, fragShader, null);
	}

	//--Load Font--//
	// description
	//    this will load the specified font file, create a texture for the defined
	//    character range, and setup all required values used to render with it.
	// arguments:
	//    file - Filename of the font (.ttf, .otf) to use. In 'Assets' folder.
	//    size - Requested pixel size of font (height)
	//    padX, padY - Extra padding per character (X+Y Axis); to prevent overlapping characters.
	public boolean load(String filename, int size, int padX, int padY) {

		// setup requested values
		fontPadX = padX;                                // Set Requested X Axis Padding
		fontPadY = padY;                                // Set Requested Y Axis Padding

		// load the font and setup paint instance for drawing
//		Typeface tf = Typeface.createFromAsset( assets, file );  // Create the Typeface from Font File
		Typeface tf = LoadFromResourceUtil.getFontFromRes(mContext, filename);
		Paint paint = new Paint();                      // Create Android Paint Instance
		paint.setAntiAlias( true );                     // Enable Anti Alias
		paint.setTextSize( size );                      // Set Text Size
		paint.setColor( 0xffffffff );                   // Set ARGB (White, Opaque)
		paint.setTypeface( tf );                        // Set Typeface

		// get font metrics
		Paint.FontMetrics fm = paint.getFontMetrics();  // Get Font Metrics
		fontHeight = (float)Math.ceil( Math.abs( fm.bottom ) + Math.abs( fm.top ) );  // Calculate Font Height
		fontAscent = (float)Math.ceil( Math.abs( fm.ascent ) );  // Save Font Ascent
		fontDescent = (float)Math.ceil( Math.abs( fm.descent ) );  // Save Font Descent

		// determine the width of each character (including unknown character)
		// also determine the maximum character width
		char[] s = new char[2];                         // Create Character Array
		charWidthMax = charHeight = 0;                  // Reset Character Width/Height Maximums
		float[] w = new float[2];                       // Working Width Value
		int cnt = 0;                                    // Array Counter
		for ( char c = CHAR_START; c <= CHAR_END; c++ )  {  // FOR Each Character
			s[0] = c;                                    // Set Character
			paint.getTextWidths( s, 0, 1, w );           // Get Character Bounds
			charWidths[cnt] = w[0];                      // Get Width
			if ( charWidths[cnt] > charWidthMax )        // IF Width Larger Than Max Width
				charWidthMax = charWidths[cnt];           // Save New Max Width
			cnt++;                                       // Advance Array Counter
		}
		s[0] = CHAR_NONE;                               // Set Unknown Character
		paint.getTextWidths( s, 0, 1, w );              // Get Character Bounds
		charWidths[cnt] = w[0];                         // Get Width
		if ( charWidths[cnt] > charWidthMax )           // IF Width Larger Than Max Width
			charWidthMax = charWidths[cnt];              // Save New Max Width
		cnt++;                                          // Advance Array Counter

		// set character height to font height
		charHeight = fontHeight;                        // Set Character Height

		// find the maximum size, validate, and setup cell sizes
		cellWidth = (int)charWidthMax + ( 2 * fontPadX );  // Set Cell Width
		cellHeight = (int)charHeight + ( 2 * fontPadY );  // Set Cell Height
		int maxSize = cellWidth > cellHeight ? cellWidth : cellHeight;  // Save Max Size (Width/Height)
		if ( maxSize < FONT_SIZE_MIN || maxSize > FONT_SIZE_MAX )  // IF Maximum Size Outside Valid Bounds
			return false;                                // Return Error

		// set texture size based on max font size (width or height)
		// NOTE: these values are fixed, based on the defined characters. when
		// changing start/end characters (CHAR_START/CHAR_END) this will need adjustment too!
		if ( maxSize <= 24 )                            // IF Max Size is 18 or Less
			textureSize = 256;                           // Set 256 Texture Size
		else if ( maxSize <= 40 )                       // ELSE IF Max Size is 40 or Less
			textureSize = 512;                           // Set 512 Texture Size
		else if ( maxSize <= 80 )                       // ELSE IF Max Size is 80 or Less
			textureSize = 1024;                          // Set 1024 Texture Size
		else                                            // ELSE IF Max Size is Larger Than 80 (and Less than FONT_SIZE_MAX)
			textureSize = 2048;                          // Set 2048 Texture Size

		// create an empty bitmap (alpha only)
		Bitmap bitmap = Bitmap.createBitmap( textureSize, textureSize, Bitmap.Config.ALPHA_8 );  // Create Bitmap
		Canvas canvas = new Canvas( bitmap );           // Create Canvas for Rendering to Bitmap
		bitmap.eraseColor( 0x00000000 );                // Set Transparent Background (ARGB)

		// calculate rows/columns
		// NOTE: while not required for anything, these may be useful to have :)
		colCnt = textureSize / cellWidth;               // Calculate Number of Columns
		rowCnt = (int)Math.ceil( (float)CHAR_CNT / (float)colCnt );  // Calculate Number of Rows

		// render each of the characters to the canvas (ie. build the font map)
		float x = fontPadX;                             // Set Start Position (X)
		float y = ( cellHeight - 1 ) - fontDescent - fontPadY;  // Set Start Position (Y)
		for ( char c = CHAR_START; c <= CHAR_END; c++ )  {  // FOR Each Character
			s[0] = c;                                    // Set Character to Draw
			canvas.drawText( s, 0, 1, x, y, paint );     // Draw Character
			x += cellWidth;                              // Move to Next Character
			if ( ( x + cellWidth - fontPadX ) > textureSize )  {  // IF End of Line Reached
				x = fontPadX;                             // Set X for New Row
				y += cellHeight;                          // Move Down a Row
			}
		}
		s[0] = CHAR_NONE;                               // Set Character to Use for NONE
		canvas.drawText( s, 0, 1, x, y, paint );        // Draw Character

		// save the bitmap in a texture
		textureId = TextureHelper.loadTexture(bitmap);

		// setup the array of character texture regions
		x = 0;                                          // Initialize X
		y = 0;                                          // Initialize Y
		for ( int c = 0; c < CHAR_CNT; c++ )  {         // FOR Each Character (On Texture)
			charRgn[c] = new TextureRegion( textureSize, textureSize, x, y, cellWidth-1, cellHeight-1 );  // Create Region for Character
			x += cellWidth;                              // Move to Next Char (Cell)
			if ( x + cellWidth > textureSize )  {
				x = 0;                                    // Reset X Position to Start
				y += cellHeight;                          // Move to Next Row (Cell)
			}
		}

		// create full texture region
		textureRgn = new TextureRegion( textureSize, textureSize, 0, 0, textureSize, textureSize );  // Create Full Texture Region

		// return success
		return true;                                    // Return Success
	}
	
	/**
	 * Provides GLText with camera position and lookAt vector, which are needed
	 * to orient it towards the camera
	 * @param cameraPos
	 * @param cameraLookAtVec
	 * @param cameraHeading
	 * @param cameraAltitudeScale
	 */
	public void setCameraProps(float[] cameraPos, float[] cameraLookAtVec, float cameraHeading, float cameraAltitudeScale){
		mCameraPos = cameraPos;
		mCamLookAtVec = cameraLookAtVec;
		mCameraHeading = cameraHeading;
		mCameraAltitudeScale = cameraAltitudeScale;
	}

	//--Begin/End Text Drawing--//
	// D: call these methods before/after (respectively all draw() calls using a text instance
	//    NOTE: color is set on a per-batch basis, and fonts should be 8-bit alpha only!!!
	// A: red, green, blue - RGB values for font (default = 1.0)
	//    alpha - optional alpha value for font (default = 1.0)
	// 	  vpMatrix - View and projection matrix to use
	// R: [none]
	public void begin(float[] viewMatrix, float[] projMatrix, float zNearFactor)  {
		begin( 1.0f, 1.0f, 1.0f, 1.0f, viewMatrix, projMatrix, zNearFactor);                // Begin with White Opaque
	}
	public void begin(float alpha, float[] viewMatrix, float[] projMatrix, float zNearFactor)  {
		begin( 1.0f, 1.0f, 1.0f, alpha, viewMatrix, projMatrix, zNearFactor);               // Begin with White (Explicit Alpha)
	}
	public void begin(float red, float green, float blue, float alpha, float[] viewMatrix, float[] projMatrix, float zNearFactor)  {
		this.zNearFactor = zNearFactor;
		setMatrices(viewMatrix, projMatrix);
		Matrix.multiplyMM(mVPMatrix, 0, projMatrix, 0, viewMatrix, 0);
		initDraw(red, green, blue, alpha);
		batch.beginBatch(mVPMatrix);                             // Begin Batch
	}
	
	void initDraw(float red, float green, float blue, float alpha) {
		GLES20.glUseProgram(mProgram.getHandle()); // specify the program to use
		
		// set color TODO: only alpha component works, text is always black #BUG
		float[] color = {red, green, blue, alpha}; 
		GLES20.glUniform4fv(mColorHandle, 1, color , 0); 
		GLES20.glEnableVertexAttribArray(mColorHandle);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);  // Set the active texture unit to texture unit 0
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId); // Bind the texture to this unit
		
		//set these to identity since we wont be using them
		GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, mIdentityMatrix, 0);
		GLES20.glUniformMatrix4fv(mViewMatrixHandle, 1, false, mIdentityMatrix, 0);
		GLES20.glUniformMatrix4fv(mProjMatrixHandle, 1, false, mIdentityMatrix, 0);
		
		//Enable/Disable things we want to do/avoid in shader
		GLES20.glUniform1f(mEnableDrawingTextUniform, 1.0f);
		GLES20.glUniform1f(mEnableAlphaTestUniform, 1.0f);
		GLES20.glUniform1f(mEnableRadialGradientUniform, 0.0f);
		
		// Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0
		GLES20.glUniform1i(mTextureUniformHandle, 0); 
		
		//define how sharp the text should be
		GLES20.glUniform1f(mTextSharpnessUniform, TEXT_SHARPNESS);
		
		//setting Radial gradient stuff to zero
		GLES20.glUniform3f(mCenterOfRadiusUniform, 0, 0, 0);
		GLES20.glUniform1f(mRadialGradientBeginUniform, 0.0f);
		GLES20.glUniform1f(mRadialGradientEndUniform, 0.0f);
	}
	
	public void end()  {
		batch.endBatch();                               // End Batch
		GLES20.glDisableVertexAttribArray(mColorHandle);
	}
	
	public void draw(String text, float x, float y, float z, float angleDegX, float angleDegY, float angleDegZ){
		draw(text, x, y, z, angleDegX, angleDegY, angleDegZ, false);
	}

	//--Draw Text--//
	// D: draw text at the specified x,y position
	// A: text - the string to draw
	//    x, y, z - the x, y, z position to draw text at (bottom left of text; including descent)
	//    angleDeg - angle to rotate the text
	// R: [none]
	private void draw(String text, float x, float y, float z, float angleDegX, float angleDegY, float angleDegZ, boolean isCentered)  {
		float chrHeight = cellHeight * scaleY;          // Calculate Scaled Character Height
		float chrWidth = cellWidth * scaleX;            // Calculate Scaled Character Width
		int len = text.length();                        // Get String Length
		if(!isCentered){
			textPosX = x;
			textPosY = y;
			textPosZ = z;
		}
//		x += ( chrWidth / 2.0f ) - ( fontPadX * scaleX );  // Adjust Start X
//		y += ( chrHeight / 2.0f ) - ( fontPadY * scaleY );  // Adjust Start Y
//		x = x - mScreenWidth/2;
//		y = y - mScreenHeight/2;
		
		//this matrix is for correctly translating or orienting the text in the map
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.rotateM(mModelMatrix, 0, mCameraHeading, 0f, 0f, 1f);
		/*if(!isCentered) */Matrix.translateM(mModelMatrix, 0, textPosX, textPosY, textPosZ);
//		else Matrix.translateM(mModelMatrix, 0, textPosX + x, textPosY + y, textPosZ);
		Matrix.rotateM(mModelMatrix, 0, -mCameraHeading, 0f, 0f, 1f);
		
		// create a model matrix based on x, y and angleDeg
		float[] modelMatrix = new float[16];
		Matrix.setIdentityM(modelMatrix, 0);
		
//		mTextLookAtVec = GLHelper.normalize(mDefaultLookAtVec);
		
		mTextToCamVec[0] = mCameraPos[0] - textPosX;
		mTextToCamVec[1] = mCameraPos[1] - textPosY;
		mTextToCamVec[2] = mCameraPos[2] - textPosZ;
		mTextToCamVec = GLHelper.normalize(mTextToCamVec);
		
		mAngleToFaceCamera = RAD2DEG*(float)Math.acos(GLHelper.clamp(GLHelper.dot(DEFAULT_LOOK_AT_VEC, mTextToCamVec), -1.0f, 1.0f));
		GLHelper.cross(axisOfRotation, DEFAULT_LOOK_AT_VEC, mTextToCamVec);
		GLHelper.normalize(axisOfRotation);
		
//		Log.d(TAG, "TextToCam = {" + mNumFormat.format(mTextToCamVec[0]) + ", " + mNumFormat.format(mTextToCamVec[1]) + ", " + mNumFormat.format(mTextToCamVec[2]) + "}"
//					+ "TextLookAt = {" + mNumFormat.format(mTextLookAtVec[0]) + ", " + mNumFormat.format(mTextLookAtVec[1]) + ", " + mNumFormat.format(mTextLookAtVec[2]) + "}");
//		Log.d(TAG, "mCameraAltitudeScale = " + mCameraAltitudeScale);
//		if(isCentered && angleDegZ != 0){
//			Matrix.translateM(modelMatrix, 0, x*(float)Math.cos((angleDegZ%360)*DEG2RAD), x*(float)Math.sin((angleDegZ%360)*DEG2RAD), 0f);
//		}
//		else {
//			Matrix.translateM(modelMatrix, 0, x, y, z);
//		}
//		Log.d(TAG,"mTextToCamVec magnitude = " + GLHelper.magnitude(mTextToCamVec));
		float mag = GLHelper.magnitude(mTextToCamVec);
		float scaleFactor;
		if(mag < 1.0){
			scaleFactor = 0.01f;
		}
		else if(1.0 <= mag && mag < 1.02){
			scaleFactor = 0.01f * (mag);
		}
		else {
			scaleFactor = 0.01f * 1.02f;
		}
//		scaleFactor = 0.01f * (mCameraAltitudeScale/2);
		
		if(Math.abs(mAngleToFaceCamera) <= 0.01) mAngleToFaceCamera = 0;
		Matrix.rotateM(mModelMatrix, 0, mAngleToFaceCamera, axisOfRotation[0], axisOfRotation[1], axisOfRotation[2]);
		Matrix.rotateM(mModelMatrix, 0, angleDegZ, 0, 0, 1);
		Matrix.rotateM(mModelMatrix, 0, angleDegX, 1, 0, 0);
		Matrix.rotateM(mModelMatrix, 0, angleDegY, 0, 1, 0);
		Matrix.scaleM(mModelMatrix, 0, scaleFactor, scaleFactor, scaleFactor);
//		if(isCentered) Matrix.translateM(mModelMatrix, 0, x, y, z);
		
//		Matrix.multiplyMM(modelMatrix, 0, mModelMatrix, 0, modelMatrix, 0);
		
		mTextLookAtVec = mTextToCamVec.clone();
		
		float letterX, letterY; 
		letterX = letterY = 0;
		
		for (int i = 0; i < len; i++)  {              // FOR Each Character in String
			int c = (int)text.charAt(i) - CHAR_START;  // Calculate Character Index (Offset by First Char in Font)
			if (c < 0 || c >= CHAR_CNT)                // IF Character Not In Font
				c = CHAR_UNKNOWN;                         // Set to Unknown Character Index			

			//TODO: optimize - applying the same model matrix to all the characters in the string
			batch.drawSprite(letterX, letterY, chrWidth, chrHeight, charRgn[c], mModelMatrix);  // Draw the Character
			letterX += (charWidths[c] + spaceX ) * scaleX;    // Advance X Position by Scaled Character Width
		}
	}
	public void draw(String text, float x, float y, float z, float angleDegZ) {
		draw(text, x, y, z, 0, 0, angleDegZ, false);
	}
	public void draw(String text, float x, float y, float angleDeg) {
		draw(text, x, y, 0, angleDeg);
	}
	
	public void draw(String text, float x, float y) {
		draw(text, x, y, 0, 0);
	}

	//--Draw Text Centered--//
	// D: draw text CENTERED at the specified x,y position
	// A: text - the string to draw
	//    x, y, z - the x, y, z position to draw text at (bottom left of text)
	//    angleDeg - angle to rotate the text
	// R: the total width of the text that was drawn
	public float drawC(String text, float x, float y, float z, float angleDegX, float angleDegY, float angleDegZ)  {
		textPosX = x;
		textPosY = y;
		textPosZ = z;
		float len = getLength( text );                  // Get Text Length
		Matrix.invertM(mInvertedMVP, 0, mVPMatrix, 0);
		convertScreenXYToWorld(worldCoordsVec, ndcCoordsVec, mInvertedMVP, len, getCharHeight());
		draw( text, -( worldCoordsVec[0] / 2.0f ), ( worldCoordsVec[1] / 2.0f ), z, angleDegX, angleDegY, angleDegZ, true);  // Draw Text Centered
		return len;                                     // Return Length
	}
	
	public float drawC(String text, float x, float y, float z, float angleDegZ) {
		return drawC(text, x, y, z, 0, 0, angleDegZ);
	}
	public float drawC(String text, float x, float y, float angleDeg) {
		return drawC(text, x, y, 0, angleDeg);
	}
	public float drawC(String text, float x, float y) {
		float len = getLength( text );                  // Get Text Length
		return drawC(text, x - (len / 2.0f), y - ( getCharHeight() / 2.0f ), 0);
		
	}
	public float drawCX(String text, float x, float y)  {
		float len = getLength( text );                  // Get Text Length
		draw( text, x - ( len / 2.0f ), y);            // Draw Text Centered (X-Axis Only)
		return len;                                     // Return Length
	}
	public void drawCY(String text, float x, float y)  {
		draw( text, x, y - ( getCharHeight() / 2.0f ));  // Draw Text Centered (Y-Axis Only)
	}
	
	public void drawInWorld(String text, float x, float y, float z, float angleDegX, float angleDegY, float angleDegZ){
		
	}

	//--Set Scale--//
	// D: set the scaling to use for the font
	// A: scale - uniform scale for both x and y axis scaling
	//    sx, sy - separate x and y axis scaling factors
	// R: [none]
	public void setScale(float scale)  {
		scaleX = scaleY = scale;                        // Set Uniform Scale
	}
	public void setScale(float sx, float sy)  {
		scaleX = sx;                                    // Set X Scale
		scaleY = sy;                                    // Set Y Scale
	}

	//--Get Scale--//
	// D: get the current scaling used for the font
	// A: [none]
	// R: the x/y scale currently used for scale
	public float getScaleX()  {
		return scaleX;                                  // Return X Scale
	}
	public float getScaleY()  {
		return scaleY;                                  // Return Y Scale
	}

	//--Set Space--//
	// D: set the spacing (unscaled; ie. pixel size) to use for the font
	// A: space - space for x axis spacing
	// R: [none]
	public void setSpace(float space)  {
		spaceX = space;                                 // Set Space
	}

	//--Get Space--//
	// D: get the current spacing used for the font
	// A: [none]
	// R: the x/y space currently used for scale
	public float getSpace()  {
		return spaceX;                                  // Return X Space
	}

	//--Get Length of a String--//
	// D: return the length of the specified string if rendered using current settings
	// A: text - the string to get length for
	// R: the length of the specified string (pixels)
	public float getLength(String text) {
		charLengthConverted = charHeightConverted = 0f;
		float len = 0f;
		int strLen = text.length();                     // Get String Length (Characters)
		
		for ( int i = 0; i < strLen; i++ )  {           // For Each Character in String (Except Last
			int c = (int)text.charAt( i ) - CHAR_START;  // Calculate Character Index (Offset by First Char in Font)
			len += (charWidths[c] * scaleX);
		}
		len += ( strLen > 1 ? ( ( strLen - 1 ) * spaceX ) * scaleX : 0 );  // Add Space Length
		return len;                                     // Return Total Length
	}

	//--Get Width/Height of Character--//
	// D: return the scaled width/height of a character, or max character width
	//    NOTE: since all characters are the same height, no character index is required!
	//    NOTE: excludes spacing!!
	// A: chr - the character to get width for
	// R: the requested character size (scaled)
	public float getCharWidth(char chr)  {
		int c = chr - CHAR_START;                       // Calculate Character Index (Offset by First Char in Font)
		return ( charWidths[c] * scaleX );              // Return Scaled Character Width
	}
	public float getCharWidthMax()  {
		return ( charWidthMax * scaleX );               // Return Scaled Max Character Width
	}
	public float getCharHeight() {
		return ( charHeight * scaleY );                 // Return Scaled Character Height
	}

	//--Get Font Metrics--//
	// D: return the specified (scaled) font metric
	// A: [none]
	// R: the requested font metric (scaled)
	public float getAscent()  {
		return ( fontAscent * scaleY );                 // Return Font Ascent
	}
	public float getDescent()  {
		return ( fontDescent * scaleY );                // Return Font Descent
	}
	public float getHeight()  {
		return ( fontHeight * scaleY );                 // Return Font Height (Actual)
	}

	//--Draw Font Texture--//
	// D: draw the entire font texture (NOTE: for testing purposes only)
	// A: width, height - the width and height of the area to draw to. this is used
	//    to draw the texture to the top-left corner.
	//    vpMatrix - View and projection matrix to use
	public void drawTexture(int width, int height, float[] vpMatrix)  {
		initDraw(1.0f, 1.0f, 1.0f, 1.0f);

		batch.beginBatch(vpMatrix);                  // Begin Batch (Bind Texture)
		float[] idMatrix = new float[16];
		Matrix.setIdentityM(idMatrix, 0);
		batch.drawSprite(width - (textureSize / 2), height - ( textureSize / 2 ), 
				textureSize, textureSize, textureRgn, idMatrix);  // Draw
		batch.endBatch();                               // End Batch
	}
	
	public void setScreenWidthHeight(int width, int height){
		SCREEN_WIDTH = width;
		SCREEN_HEIGHT = height;
	}
	
	/**
	 * Converts window coordinates to world coordinates
	 * @param output A 4-element vector that will receive world coordinates
	 * @param inputNDCVec A 4-element vector that will receive NDC coordinates
	 * @param invMVPMatrix Inverted MVP matrix
	 * @param x Screen x-coordinate that you want to convert. Set to a negative value if you want to ignore this.
	 * @param y Screen y-coordinate that you want to convert. Set to a negative value if you want to ignore this.
	 */
	private void convertScreenXYToWorld(float[] output, float[] inputNDCVec, float[] invMVPMatrix, float x, float y){
		//converting the width of each character
		// from screen pixels to OpenGL world coordinates 
		inputNDCVec[0] = (x >= 0) ? ((SCREEN_WIDTH/2 + x) * 2 / SCREEN_WIDTH) - 1 : 0;
		inputNDCVec[1] = (y >= 0) ? ((SCREEN_HEIGHT/2 + y) * 2 / SCREEN_HEIGHT) - 1 : 0;
		inputNDCVec[2] = 0f;
		inputNDCVec[3] = 1.0f;
		Matrix.multiplyMV(output, 0, invMVPMatrix, 0, inputNDCVec, 0);
		
		if(output[3] != 0){
			output[0] /= (output[3] * zNearFactor);
			output[1] /= (output[3] * zNearFactor);
			output[2] /= (output[3] * zNearFactor);
		}
	}
	
	public void setMatrices(float[] viewMat, float[] projMat){
		mViewMatrix = viewMat;
		mProjMatrix = projMat;
	}
	
//	/**
//	 * Returns a string list of {@code array}, using {@code divider} to
//	 * insert in between each item
//	 * @param array - Array to print
//	 * @param divider - If specified, a string to be inserted in between each item in the 
//	 * 					resulting string. If not used, set to null.
//	 * @return
//	 */
//	public String arrayToString(float[] array, String divider){
//		String result = "";
//		divider = (divider == null) ? "" : divider;
//		for(float item : array){
//			result.concat(String.valueOf(item) + divider);
//		}
//		return result;
//	}
}
