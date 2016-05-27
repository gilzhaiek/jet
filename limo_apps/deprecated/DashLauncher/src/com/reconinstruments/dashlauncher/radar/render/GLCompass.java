package com.reconinstruments.dashlauncher.radar.render;

import javax.microedition.khronos.opengles.GL10;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public class GLCompass extends GLBaseTexture
{
	protected Vector3 mOrientation = null;
	
	public GLCompass(float pitch)
	{		
		super(1);
		
		mOrientation = new Vector3(0.0f, pitch, 0.0f);
	}
	
	public void setParams(float yaw)
	{
		mOrientation.x = yaw;
	}

	@Override
	public void drawGL(GL10 gl)
	{
		if(mIsHidden || mTextures[mTextureIndex] == 0 || mTextureIndex >= mTextures.length){ return; }
		
		if(mLoadGlTexture) { LoadGLTexture(gl); }

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, mAlpha);
		
		// bind the previously generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[mTextureIndex]);

		// Set the face rotation
		gl.glFrontFace(GL10.GL_CW);
		
		gl.glPushMatrix();							
			gl.glScalef(1.0f, 0.40f, 1.0f);
			gl.glTranslatef(0.0f, -0.25f, -1.15f);
			
			//gl.glRotatef(mOrientation.y, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(mOrientation.x, 0.0f, 0.0f, 1.0f);			
			
			// Point to our buffersC
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	
			// Point to our vertex buffer
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
	
			// Draw the vertices as triangle strip
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertices.length / 3);
	
			// Disable the client state before leaving
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		gl.glPopMatrix();
	}
}
