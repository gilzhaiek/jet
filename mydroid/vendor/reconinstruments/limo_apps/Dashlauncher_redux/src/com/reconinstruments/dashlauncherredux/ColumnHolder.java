package com.reconinstruments.dashlauncherredux;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * A container class for holding multiple columns.
 * 
 * @author <a href="mailto:ali@reconinstruments.com">Ali R. Mohazab</a>
 * @version 1.0
 n */
public class ColumnHolder {
    private static final String TAG = "ColumnHolder";
    public final List<Column> theList = new ArrayList<Column>();
    int currentIndex = 0;
    private int mHomeColumnIndex = 0;

    public void setHomeColumnIndex(int i) {
	mHomeColumnIndex = i;
    }

    /**
     * Creates a new <code>ColumnHolder</code> instance.
     * 
     */
    public ColumnHolder() {
	// Empty constructor
    }

    /**
     * Add a column to the holder
     * 
     * @param c
     *            a <code>Column</code>
     */
    public void addColumn(Column c) {
	theList.add(c);
    }

    synchronized Intent goUp() {
	Log.d(TAG, "goUp, currentIndex=" + currentIndex);
	Column col = theList.get(currentIndex);
	if (col != null) {
	    if (col.decrementIndex()) {
		return getCurrentIndexActivityIntent();
	    } else {
		return null;
	    }
	} else {
	    Log.w(TAG, "column null");
	    return null;
	}
    }

    synchronized Intent goDown() {
	Log.d(TAG, "goDown, currentIndex=" + currentIndex);
	Column col = theList.get(currentIndex);
	if (col != null) {
	    if (col.incrementIndex()) {
		return getCurrentIndexActivityIntent();
	    } else {
		return null;
	    }
	} else {
	    Log.w(TAG, "column null");
	    return null;
	}
    }

    synchronized Intent goLeft() {
	Log.d(TAG, "goLeft, currentIndex=" + currentIndex);
	if (decrementIndex()) {
	    return getCurrentIndexActivityIntent();
	} else {
	    Log.w(TAG, "sorry Can't go left");
	    return null;
	}

    }

    synchronized Intent goRight() {
	Log.d(TAG, "goRight, currentIndex=" + currentIndex);
	if (incrementIndex()) {
	    return getCurrentIndexActivityIntent();
	} else {
	    Log.w(TAG, "sorry Can't go right");
	    return null;
	}
    }

    synchronized Intent goBack() {
	Log.d(TAG, "goBack, currentIndex=" + currentIndex);
	// Find the Dash column
	currentIndex = mHomeColumnIndex;
	Column col = theList.get(currentIndex);
	if (col != null) {
	    col.currentIndex = col.mHomeElementIndex;
	    return getCurrentIndexActivityIntent();
	} else {
	    Log.w(TAG, "column null");
	    return null;
	}
    }

    synchronized private boolean incrementIndex() {
	currentIndex++;
	if (currentIndex >= theList.size()) {
	    currentIndex = theList.size() - 1;
	    return false;
	}
	return true;
    }

    synchronized private boolean decrementIndex() {
	currentIndex--;
	Log.v(TAG, "currentIndex = " + currentIndex);
	if (currentIndex < 0) {
	    currentIndex = 0;
	    return false;
	}
	return true;
    }

    void launchCurrentIndexColumnActivity(Context c) {
	Column col = theList.get(currentIndex);
	col.launchCurrentIndexActivity(c);
    }

    Intent getCurrentIndexActivityIntent() {
	Column col = theList.get(currentIndex);
	return col.getCurrentIndexActivityIntent();
    }

    int getHorizontalPosition() {
	return currentIndex;
    }
    int getVerticalPosition() {
	Column col = theList.get(currentIndex);
	return col.currentIndex;
    }
    int getTotalHorizontal() {
	return theList.size();
    }
    int getTotalVertical() {
	Column col = theList.get(currentIndex);
	return col.theList.size();
    }

    

}