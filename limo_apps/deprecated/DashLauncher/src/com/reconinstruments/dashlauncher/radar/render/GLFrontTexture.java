package com.reconinstruments.dashlauncher.radar.render;


public class GLFrontTexture extends GLBaseTexture {
	protected static final int SCREEN_PIX_WIDTH			= 428;
	protected static final int SCREEN_PIX_HEIGHT		= 240;
	protected static final float ACTUAL_SCREEN_WIDTH	= 3.2f;
	protected static final float ACTUAL_SCREEN_HEIGHT	= 1.72f;
	
	protected int mFrontTexturePixWidth			= 428;
	protected int mFrontTexturePixHeight		= 240;
	protected float mActualLocalWidth			= ACTUAL_SCREEN_WIDTH;
	protected float mActualLocalHeight			= ACTUAL_SCREEN_HEIGHT;
	protected float mLocalPerPixWidth			= mActualLocalWidth/(float)mFrontTexturePixWidth;
	protected float mLocalPerPixHeight 			= mActualLocalHeight/(float)mFrontTexturePixHeight;
	
	protected float mFixedXOffset			= 0.0f;
	protected float mFixedYOffset			= -0.1f;
	
	protected void Init() {
		SetOffsets(0.0f, 0.0f, 0.0f);
		
		// V1 - bottom left
		mVertexBuffer.put(0, -1.0f*(mActualLocalWidth/2.0f));
		mVertexBuffer.put(1, -1.0f*(mActualLocalHeight/2.0f));
		mVertexBuffer.put(2, -1.64f);
		
		// V2 - top left
		mVertexBuffer.put(3, -1.0f*(mActualLocalWidth/2.0f));
		mVertexBuffer.put(4, (mActualLocalHeight/2.0f));
		mVertexBuffer.put(5, -1.64f);
		
		// V3 - bottom right
		mVertexBuffer.put(6, (mActualLocalWidth/2.0f));
		mVertexBuffer.put(7, -1.0f*(mActualLocalHeight/2.0f));
		mVertexBuffer.put(8, -1.64f);
		
		// V4 - top right
		mVertexBuffer.put(9, (mActualLocalWidth/2.0f));
		mVertexBuffer.put(10, (mActualLocalHeight/2.0f));
		mVertexBuffer.put(11, -1.64f);
		
		mTextureBuffer.put(0, 0.0f);
		mTextureBuffer.put(1, 1.0f);
		mTextureBuffer.put(2, 0.0f);
		mTextureBuffer.put(3, 0.0f);
		mTextureBuffer.put(4, 1.0f);
		mTextureBuffer.put(5, 1.0f);
		mTextureBuffer.put(6, 1.0f);
		mTextureBuffer.put(7, 0.0f);
	}
	
	public GLFrontTexture(int maxTextures, int textureWidth, int textureHeight)
	{
		super(1, maxTextures);
		
		mFrontTexturePixWidth	= textureWidth;
		mFrontTexturePixHeight	= textureHeight;
		
		mActualLocalWidth	= ACTUAL_SCREEN_WIDTH*((float)textureWidth/(float)SCREEN_PIX_WIDTH);
		mActualLocalHeight	= ACTUAL_SCREEN_HEIGHT*((float)textureHeight/(float)SCREEN_PIX_HEIGHT);
		
		mLocalPerPixWidth	= mActualLocalWidth/(float)mFrontTexturePixWidth;
		mLocalPerPixHeight 	= mActualLocalHeight/(float)mFrontTexturePixHeight;		
				
		Init();
	}
	
	public GLFrontTexture(int maxTextures)
	{
		super(1, maxTextures);
		
		Init();
	}
	
	protected void SetFixedXOffset(float fixedXOffset){
		mFixedXOffset = fixedXOffset;
	}

	protected void SetFixedYOffset(float fixedYOffset){
		mFixedYOffset = fixedYOffset;
	}

	@Override
	public float GetXOffset() {return (mXOffset-mFixedXOffset)/mLocalPerPixWidth; }
	
	@Override
	public float GetYOffset() {return (mYOffset-mFixedYOffset)/mLocalPerPixHeight; }
	
	@Override
	public void SetOffsets(float pixX, float pixY, float pixZ) {
		super.SetOffsets(pixX*mLocalPerPixWidth+mFixedXOffset, pixY*mLocalPerPixHeight+mFixedYOffset, pixZ);
	}
}
