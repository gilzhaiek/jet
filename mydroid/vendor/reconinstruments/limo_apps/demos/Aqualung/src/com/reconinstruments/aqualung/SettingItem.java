package com.reconinstruments.aqualung;

import android.content.Intent;

public class SettingItem {

	public String title = null;
	
	public int titleAlpha = 255;
	
	public String subTitle = null;
	public Integer iconId = null;
	
	public Boolean checkBox = false;
	public Boolean checkBoxValue = false;
	
	public Boolean checkMark = false;
	
	public Intent intent = null;


	public SettingItem(Intent intent , String title){
		this.title = title;
		this.intent = intent;
	}

	// CheckBox
	public SettingItem(String title){
		this.title = title;
	}

	public SettingItem(Intent intent , int iconId , String title){
		this.intent = intent;
		this.iconId = iconId;
		this.title = title;
	}

	// Constructor For type 4
	public SettingItem(Intent intent , String title, String subTitle){
		this.intent = intent;
		this.title = title;
		this.subTitle = subTitle;
	}

}
