package com.reconinstruments.dashlauncher.radar.render;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

public class TextureSquare
{
	public static final int TYPE_MASK	= 0;
	public static final int TYPE_POI	= 1;
	
	// buffer holding the vertices
	private FloatBuffer vertexBuffer;
	private float vertices_mask[] = { -2.25f, -1.1f, -3.0f, // V1 - bottom left
									  -2.25f,  1.1f, -3.0f, // V2 - top left
									   2.25f, -1.1f, -3.0f, // V3 - bottom right
									   2.25f,  1.1f, -3.0f  // V4 - top right
	};
	
	private float vertices_poi[] = { -2f, -1.1f, -2.36f, // V1 - bottom left
								     -2f,  1.1f, -2.36f, // V2 - top left
								      2f, -1.1f, -2.36f, // V3 - bottom right
								      2f,  1.1f, -2.36f  // V4 - top right
	};

	// buffer holding the texture coordinates
	private FloatBuffer textureBuffer; // buffer holding the texture coordinates
	// Mapping coordinates for the vertices
	private float texture[] = { 0.0f, 1.0f, // top left (V2)
								0.0f, 0.0f, // bottom left (V1)
								1.0f, 1.0f, // top right (V4)
								1.0f, 0.0f  // bottom right (V3)
	};

	public TextureSquare(int type)
	{
		init(type);
	}
	
	public void init(int type)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices_poi.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuffer.asFloatBuffer();
		switch (type){
		case TYPE_MASK:
			vertexBuffer.put(vertices_mask);
			break;
		case TYPE_POI:
			vertexBuffer.put(vertices_poi);
			break;
		}
		vertexBuffer.position(0);

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

		// Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices_poi.length / 3);
		
		// Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

	static HashMap<Integer,Bitmap> loadedBitmaps = new HashMap<Integer,Bitmap>();
	
	/** The texture pointer */
	private int[] textures = new int[1];

	public void loadGLTexture(GL10 gl, Context context, int id)
	{
		// generate one texture pointer 
		gl.glGenTextures(1, textures, 0);
		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		
		// loading texture
		Bitmap bitmap = loadedBitmaps.get(id);
		if(bitmap==null){
			bitmap = BitmapFactory.decodeResource(context.getResources(), id);
			loadedBitmaps.put(id, bitmap);
		}

		// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		// Clean up
		//bitmap.recycle();  
	}
}
