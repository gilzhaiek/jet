package com.reconinstruments.phone;

import java.util.Date;
import android.os.Bundle;

public class PhoneCacheBundle {

	public static final int TYPE_SMS = 1;
	public static final int TYPE_CALL = 2;

	Bundle data;

	public PhoneCacheBundle(int mType, String mSource, String mContact, String mBody, Date mDate) {
		data = new Bundle();
		
		data.putInt("type", mType);
		data.putString("source", mSource);
		data.putString("contact", mContact);
		data.putString("body", mBody);
		data.putLong("date", mDate.getTime());
	}

	public PhoneCacheBundle(int mType, String mSource, String mContact, boolean mMissed, Date mDate) {
		data = new Bundle();
		
		data.putInt("type", mType);
		data.putString("source", mSource);
		data.putString("contact", mContact);
		data.putBoolean("missed", mMissed);
		data.putLong("date", mDate.getTime());
	}
	
	public PhoneCacheBundle(Bundle b) {
		data = b;
	}
	
	public Bundle getBundle() {
		return data;
	}

	public int getmType() {
		return data.getInt("type");
	}

	public void setmType(int mType) {
		data.putInt("type", mType);
	}

	public String getmSource() {
		return data.getString("source");
	}

	public void setmSource(String mSource) {
		data.putString("source", mSource);
	}

	public String getmContact() {
		return data.getString("contact");
	}

	public void setmContact(String mContact) {
		data.putString("contact", mContact);
	}

	public String getmBody() {
		return data.getString("body");
	}

	public void setmBody(String mBody) {
		data.putString("body", mBody);
	}

	public Date getmTimeStamp() {
		return new Date(data.getLong("date"));
	}

	public void setmTimeStamp(Date mDate) {
		data.putLong("date", mDate.getTime());
	}

	public boolean ismMissed() {
		return data.getBoolean("missed");
	}

	public void setmMissed(boolean mMissed) {
		data.putBoolean("missed", mMissed);
	}

	public String getOrigin() {
		if (data.getString("contact") != null && data.getString("contact").length() > 0)
			return data.getString("contact");
		else
			return data.getString("source");
	}

}
