package com.reconinstruments.dashlauncher.radar.render;

import java.util.ArrayList;

import android.util.Log;

public class GLZoomControl extends GLFrontTexture
{
	private static final String TAG = "GLZoomControl";

	public static final float HIDE_X_OFFSET = -60.0f;

	protected static ArrayList<Integer> mTexturesIDs = null;

	public GLZoomControl(int maxTextures)
	{
		super(maxTextures);

		SetHidden(true);

		SetFixedXOffset(0.24f);

		HideNow();
	}

	public void HideNow()
	{
		SetOffsets(HIDE_X_OFFSET, 0.0f, 0.0f);
	}

	public void ShowNow()
	{
		SetOffsets(0.0f, 0.0f, 0.0f);
	}

}
