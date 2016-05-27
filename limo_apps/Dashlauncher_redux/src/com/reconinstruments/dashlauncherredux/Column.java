package com.reconinstruments.dashlauncherredux;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class Column {
    public static final String TAG = "Column";
    public final List<ColumnElementInfo> theList = new ArrayList<ColumnElementInfo>();
    int currentIndex = 0;
    int mHomeElementIndex = 0;

    public void setHomeElementIndex(int i) {
	mHomeElementIndex = i;
    }

    public Column() {
	// Empty constructor
    }

    public void addColumnElementInfo(ColumnElementInfo cei) {
	theList.add(cei);
    }

    synchronized boolean incrementIndex() {
	Log.d(TAG, "incrementIndex");
	currentIndex++;
	if (currentIndex >= theList.size()) {
	    currentIndex = theList.size() - 1;
	    return false;
	}
	return true;
    }

    synchronized boolean decrementIndex() {
	Log.d(TAG, "decrementIndex");
	currentIndex--;
	if (currentIndex < 0) {
	    currentIndex = 0;
	    return false;
	}
	return true;
    }

    void launchCurrentIndexActivity(Context c) {
	Log.d(TAG, "launchCurrentIndexActivity, currentIndex=" + currentIndex);
	if (currentIndex + 1 <= theList.size()) {
	    c.startActivity(theList.get(currentIndex).mIntent);
	} else {
	    Log.w(TAG, "Bad Column Index");
	}
    }

    Intent getCurrentIndexActivityIntent() {
	Log.d(TAG, "getCurrentIndexActivityIntent, currentIndex="
	      + currentIndex);
	if (currentIndex + 1 <= theList.size()) {
	    return theList.get(currentIndex).mIntent;
	} else {
	    Log.w(TAG, "Bad Column Index");
	    return null;
	}
    }
}