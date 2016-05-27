package com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D;


public enum AttribVariable {
	A_Position(1, "vPosition"), 
	A_TexCoordinate(2, "vTexture"), 
	A_MVPMatrixIndex(3, "a_MVPMatrixIndex");
	
	private int mHandle;
	private String mName;

	private AttribVariable(int handle, String name) {
		mHandle = handle;
		mName = name;
	}

	public int getHandle() {
		return mHandle;
	}
	
	public String getName() {
		return mName;
	}
}
