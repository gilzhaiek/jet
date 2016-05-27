package com.reconinstruments.nds;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class ReconCard {

	private int mImage;
	private int leftCard, rightCard, upCard, downCard, selectCard, backCard;
	
	public ReconCard(Context c, int image, int left, int right, int up, int down, int select, int back) {
		leftCard = left;
		rightCard = right;
		upCard = up;
		downCard = down;
		selectCard = select;
		backCard = back;
		mImage = image;
	}
	
	public int getImageResourceId() {
		return mImage;
	}
	
	public int getLeftIndex() {
		return leftCard;
	}
	
	public int getRightIndex() {
		return rightCard;
	}
	
	public int getUpIndex() {
		return upCard;
	}
	
	public int getDownIndex() {
		return downCard;
	}

	public int getBackIndex() {
		return backCard;
	}
	
	public int getSelectIndex() {
		return selectCard;
	}
	
}
