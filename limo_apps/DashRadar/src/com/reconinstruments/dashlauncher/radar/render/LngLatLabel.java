package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.location.Location;
import android.opengl.GLUtils;
import android.util.Log;

public class LngLatLabel
{
	private static final String TAG = "RenderLabel";

	// buffer holding the vertices
	private FloatBuffer vertexBuffer;

	private static final float LONGITUDE_POINT	= -0.37f;
	private static final float LATITUDE_POINT	= 0.40f;
	private static final float VERTICAL_POINT	= -0.62f;
	
	private static final float FONT_SIZE		= 11f;
	
	private float vertices_long[] = {
			LONGITUDE_POINT,	VERTICAL_POINT,	-2.75f,	// 0 , 1 , 2 Bottom Left
			LONGITUDE_POINT,	VERTICAL_POINT,	-2.75f,	// 3 , 4 , 5 Top Left
			LONGITUDE_POINT,	VERTICAL_POINT,	-2.75f,	// 6 , 7 , 8 Bottom Right
			LONGITUDE_POINT,	VERTICAL_POINT,	-2.75f	// 9 ,10 ,11 Top Right
			};                                     
                                                   
	private float vertices_lat[] = {	           
			LATITUDE_POINT,		VERTICAL_POINT,	-2.75f,	// 0 , 1 , 2 Bottom Left
			LATITUDE_POINT,		VERTICAL_POINT,	-2.75f,	// 3 , 4 , 5 Top Left
			LATITUDE_POINT,		VERTICAL_POINT,	-2.75f,	// 6 , 7 , 8 Bottom Right
			LATITUDE_POINT,		VERTICAL_POINT,	-2.75f	// 9 ,10 ,11 Top Right 
			};

	// buffer holding the texture coordinates
	private FloatBuffer textureBuffer; // buffer holding the texture coordinates
	// Mapping coordinates for the vertices
	private float texture[] = { 0.0f, 1.0f, // top left (V2)
			0.0f, 0.0f, // bottom left (V1)
			1.0f, 1.0f, // top right (V4)
			1.0f, 0.0f  // bottom right (V3)
	};

	private DecimalFormat df = new DecimalFormat();

	public LngLatLabel()
	{
		df.setMaximumFractionDigits(4);
		df.setMinimumFractionDigits(4);
		init();
	}

	public void init()
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices_long.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuffer.asFloatBuffer();

		byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuffer.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
	}

	/** The draw method for the square with the GL context */
	public void draw(GL10 gl)
	{	
		// bind the previously generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// Point to our buffers
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		// Point to our vertex buffer
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);

		vertexBuffer.put(vertices_long);
		vertexBuffer.position(0);

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices_long.length / 3);

		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[1]);
		vertexBuffer.put(vertices_lat);
		vertexBuffer.position(0);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices_lat.length / 3);

		// Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

	/** The texture pointer */
	private int[] textures = new int[2];

	public void loadGLTexture(GL10 gl, Location location)
	{
		// generate one texture pointer 
		gl.glGenTextures(2, textures, 0);

		// loading texture
		Bitmap bitmap;
		float width;
		float height;


		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		// loading texture
		if (location != null){
			double longitude = location.getLongitude();
			if (longitude > 0)
				bitmap = GraphicUtil.getLngLatBitmap("LON" + " E" + df.format(Math.abs(longitude)) );
			else if (longitude < 0)
				bitmap = GraphicUtil.getLngLatBitmap("LON" + " W" + df.format(Math.abs(longitude)) );
			else
				bitmap = GraphicUtil.getLngLatBitmap("LON" + "  " + "000.0000" );
		}
		else
			bitmap = GraphicUtil.getLngLatBitmap("LON" + "  " + "000.0000");

		// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		// calculate the bitmap's size respectively to the size of the screen 
		width = bitmap.getWidth() / 1000f * FONT_SIZE;
		height = bitmap.getHeight() / 1000f * FONT_SIZE;

		//Log.v(TAG , "width : " + width + " , height : " + height );

		// resize the vertices according to the size of the bitmap
		vertices_long[0] = LONGITUDE_POINT - width; // left
		vertices_long[3] = LONGITUDE_POINT - width; // left
		vertices_long[1] = VERTICAL_POINT - height;// bottom
		vertices_long[7] = VERTICAL_POINT - height;// bottom

		// Clean up
		bitmap.recycle();

		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[1]);

		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		// loading texture
		if (location != null){
			double latitude = location.getLatitude();
			if (latitude > 0)
				bitmap = GraphicUtil.getLngLatBitmap("LAT" + " N" + df.format(Math.abs(latitude)) );
			else if (latitude < 0)
				bitmap = GraphicUtil.getLngLatBitmap("LAT" + " S" + df.format(Math.abs(latitude)) );
			else
				bitmap = GraphicUtil.getLngLatBitmap("LAT" + "  " + "000.0000" );
		}
		else
			bitmap = GraphicUtil.getLngLatBitmap("LAT" + "  " + "000.0000");

		// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		// calculate the bitmap's size respectively to the size of the screen 
		width = bitmap.getWidth() / 1000f * FONT_SIZE;
		height = bitmap.getHeight() / 1000f * FONT_SIZE;

		//Log.v(TAG , "width : " + width + " , height : " + height );

		// resize the vertices according to the size of the bitmap
		vertices_lat[6] = LATITUDE_POINT + width; // right
		vertices_lat[9] = LATITUDE_POINT + width; // right
		vertices_lat[1] = VERTICAL_POINT - height;// bottom
		vertices_lat[7] = VERTICAL_POINT - height;// bottom

		// Clean up
		bitmap.recycle();  
	}

}
