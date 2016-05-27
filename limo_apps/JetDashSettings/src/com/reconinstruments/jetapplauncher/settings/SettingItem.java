package com.reconinstruments.jetapplauncher.settings;

import android.content.Intent;
import android.content.Context;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

public class SettingItem {

    public String title = null;
    public String subTitle = null;
    public Integer iconId = null;
    public Integer subIconId = null;
    public Intent intent = null;

    public String mSystemField = null; // Associated Field in Settings.System;
    
    

    // We don't use inheritance for optimizaiton purposes.
    // If this grows anymore then we do. 
    protected Boolean mIsCheckBox = false;
    protected Boolean mIsChecked = false;
	
    //these properties will be deprecated
	public int titleAlpha = 255;
	public Boolean checkBox = false;
	public Boolean checkBoxValue = false;
	public Boolean checkMark = false;
	public Boolean phoneIcon = false;

    // FIXME CheckBox ???

    public void setIsChecked(Boolean value) {
	mIsCheckBox = true;
	mIsChecked = value;
    }
    public Boolean isCheckBox() {
	return mIsCheckBox;
    }
    public Boolean isChecked(){
	return mIsChecked;
    }
    
    public SettingItem(String title){
        this.title = title;
    }

    public SettingItem(String title, Boolean isChecked) {
	this(title);
	setIsChecked(isChecked);
    }

    public SettingItem(Context c, String title, String systemField,Boolean dflt) {
	this(title,decideCheckValue(c,systemField,dflt));
	mSystemField = systemField;
    }

    private static Boolean decideCheckValue(Context c, String field, Boolean defvalue) {
	if (field == null) {
	    return false;
	}
	int defint = defvalue?1:0;
	return SettingsUtil.getSystemIntOrSet(c,field,defint)==1;
    }

	// title only
	public SettingItem(Intent intent, String title){
		this.title = title;
		this.intent = intent;
	}

	// title with icon
	public SettingItem(Intent intent, int iconId, String title){
		this.intent = intent;
		this.iconId = iconId;
		this.title = title;
	}

    // title, icon subTitle
    public SettingItem(Intent intent, int iconId, String title, String subTitle){
        this.intent = intent;
        this.iconId = iconId;
        this.title = title;
        this.subTitle = subTitle;
    }

	// title with subTitle
	public SettingItem(Intent intent, String title, String subTitle){
		this.intent = intent;
		this.title = title;
		this.subTitle = subTitle;
	}

    // title with icon, subIcon
    public SettingItem(Intent intent, int iconId, String title, int subIconId){
        this.intent = intent;
        this.title = title;
        this.iconId = iconId;
        this.subIconId = subIconId;
    }

    // title with subIcon
    public SettingItem(Intent intent, String title, int subIconId){
        this.intent = intent;
        this.title = title;
        this.subIconId = subIconId;
    }

    /**
     * <code>toggle</code> Toggles the check box value of the settings
     * item and if there is a corresponding system field
     * (Settings.System) updates that too.
     *
     * @param context a <code>Context</code> value
     */
    public void toggle(Context context) {
	mIsChecked = !mIsChecked;
	setIsChecked(mIsChecked);
	if (mSystemField != null) {
	    SettingsUtil.setSystemInt(context, mSystemField, mIsChecked?1:0);
	}
    }
    
    /**
     * <code>setItemChecked</code> set the check box value of the settings
     * item and if there is a corresponding system field (Settings.System)
     * updates that too.
     * 
     * @param context a <code>Context</code> value
     * @param enabled item checked or not
     */
    public void setItemChecked(Context context, boolean checked) {
        mIsChecked = checked;
        setIsChecked(checked);
        if (mSystemField != null) {
            SettingsUtil.setSystemInt(context, mSystemField, checked ? 1 : 0);
        }
    }
}
