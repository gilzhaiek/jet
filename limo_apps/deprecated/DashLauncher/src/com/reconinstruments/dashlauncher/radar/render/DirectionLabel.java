package com.reconinstruments.dashlauncher.radar.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.FloatMath;

public class DirectionLabel {

	// Everything is in Counter Clockwise due to Z inversion

	public static final int EAST		= 0;
	public static final int DOT_22_5	= 1;
	public static final int N_EAST		= 2;
	public static final int DOT_67_5	= 3;
	public static final int NORTH		= 4;
	public static final int DOT_112_5	= 5;
	public static final int N_WEST		= 6;
	public static final int DOT_157_5	= 7;
	public static final int WEST		= 8;
	public static final int DOT_202_5	= 9;
	public static final int S_WEST		= 10;
	public static final int DOT_247_5	= 11;
	public static final int SOUTH		= 12;
	public static final int DOT_292_5	= 13;
	public static final int S_EAST		= 14;
	public static final int DOT_337_5	= 15;

	public static final float RADIUS	= 13.00f;
	public static final float FACTOR	= 3.00f; // size factor of the labels

	/* notice in opengl z coordinate is inverted 
	 * didn't factor to keep consistency */
	private static final float PI = (float) Math.PI;
	private static final float PI_U = (float) Math.PI / 8;

	private static final float WIDTH_ARR[] = {
		0.22f, // EAST
		0.66f, // 22.5
		0.46f, // N_EAST
		0.66f, // 67.5
		0.28f, // NORTH
		0.66f, // 112.5
		0.54f, // N_WEST
		0.66f, // 157.5
		0.35f, // WEST
		0.66f, // 202.5
		0.50f, // S_WEST
		0.66f, // 247.5
		0.22f, // SOUTH
		0.66f, // 292.5
		0.39f, // S_EAST
		0.66f  // 337.5
	};

	public static final float HEIGHT_DOT= 0.11f;
	public static final float HEIGHT	= 0.35f;

	public static final float OFFSET	= 0.40f;

	// buffer holding the vertices
	private FloatBuffer vertexBuffer;

	private float vertices[];

	// buffer holding the texture coordinates
	private FloatBuffer textureBuffer; // buffer holding the texture coordinates
	// Mapping coordinates for the vertices

	private float texture[] = {
			0.0f, 1.0f, // top left (V2)
			0.0f, 0.0f, // bottom left (V1)
			1.0f, 1.0f, // top right (V4)
			1.0f, 0.0f  // bottom right (V3)
	};

	public DirectionLabel(int direction)
	{
		setVertices(direction);
		init();
	}
	private void setVertices(int i){

		if (i % 2 == 1){
			float temp_vert[] = {
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(PI/2 + PI_U * i)	, -HEIGHT_DOT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(PI/2 + PI_U * i),// V1 - bottom left
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(PI/2 + PI_U * i)	, HEIGHT_DOT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(PI/2 + PI_U * i),// V2 - top left
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(-PI/2 + PI_U * i)	, -HEIGHT_DOT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(-PI/2 + PI_U * i),// V3 - bottom right
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(-PI/2 + PI_U * i)	, HEIGHT_DOT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(-PI/2 + PI_U * i) // V4 - top right
			};
			vertices = temp_vert;
		}
		else{
			float temp_vert[] = {
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(PI/2 + PI_U * i)	, -HEIGHT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(PI/2 + PI_U * i),// V1 - bottom left
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(PI/2 + PI_U * i)	, HEIGHT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(PI/2 + PI_U * i),// V2 - top left
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(-PI/2 + PI_U * i)	, -HEIGHT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(-PI/2 + PI_U * i),// V3 - bottom right
					RADIUS * FloatMath.cos(i * PI_U) + WIDTH_ARR[i] * FACTOR * FloatMath.cos(-PI/2 + PI_U * i)	, HEIGHT * FACTOR + OFFSET	,-RADIUS * FloatMath.sin(i * PI_U) - WIDTH_ARR[i] * FACTOR * FloatMath.sin(-PI/2 + PI_U * i) // V4 - top right
			};
			vertices = temp_vert;
		}


	}

	public void init()
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuffer.asFloatBuffer();
		vertexBuffer.put(vertices);
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
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
		// Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

	/** The texture pointer */
	private int[] textures = new int[1];

	public void loadGLTexture(GL10 gl, Context context, int id)
	{
		// generate one texture pointer 
		gl.glGenTextures(1, textures, 0);
		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		// loading texture
		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);

		// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		// Clean up
		bitmap.recycle();  
	}
}
