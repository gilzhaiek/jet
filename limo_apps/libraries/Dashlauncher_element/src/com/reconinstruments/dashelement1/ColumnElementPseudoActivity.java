package com.reconinstruments.dashelement1;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.reconinstruments.dashlauncherredux.IColumnHandlerService;

// Java does not support multiple inheritence so I can't enfore all
// these functions here on both FragmentActivity and Activity. So this
// pseudo activity should sit inside the parent activity as a member
class ColumnElementPseudoActivity {
    ColumnElementPseudoActivity(Activity parentActivity) {
	mParent = parentActivity;
    }
    private boolean mBeElement = false; // If should behave as a column element
    private Activity mParent;
    private static final String TAG = "ColumnElementPseudoActivity";
    protected IColumnHandlerService columnHandlerService;
    protected enum Dir {RIGHT, LEFT, UP, DOWN, BACK};
    boolean shouldReact = true;
    // Service connection stuff;
    ServiceConnection mConnection = new ServiceConnection() {
	    final public void onServiceConnected(ComponentName className,
						 IBinder boundService) {
		Log.d(TAG, "onServiceConnected");
		columnHandlerService = IColumnHandlerService.Stub
		    .asInterface((IBinder) boundService);
	    }

	    final public void onServiceDisconnected(ComponentName className) {
		columnHandlerService = null;
		Log.d(TAG, "onServiceDisconnected");
	    }
	};

    void initService() {
	Intent i = new Intent("com.reconinstruments.COLUMN_HANDLER_SERVICE");
	mParent.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    void releaseService() {
	mParent.unbindService(mConnection);
	columnHandlerService = null;
    }
    // End of service connection
    boolean getBeElement() {
	return mBeElement;
    }

    void setBeElement() {
	Intent i = mParent.getIntent();
	if (i == null) {
	    mBeElement = false;
	    return;
	}
	mBeElement =
	    i.getBooleanExtra("com.reconinstruments.columnelement.BeElement",false);
	Log.v(TAG,"mBeElement "+mBeElement);
    }

    void onCreate() {
	Log.v(TAG,"onCreate");
	initService();
    }

    void onResume() {
	Log.v(TAG,"onResume_2");
	setBeElement();
	shouldReact = true;
    }

    void onPause() {
	Log.v(TAG,"onPause");
	shouldReact = false;
    }

    void onDestroy() {
	releaseService();
    }
    // Direction handling
    Intent goDir(Dir dir) {
	Intent i = null;
	try {
	    if (columnHandlerService != null) {
		switch (dir) {
		case RIGHT:
		    i = columnHandlerService.goRight();
		    break;
		case LEFT:
		    i = columnHandlerService.goLeft();
		    break;
		case UP:
		    i = columnHandlerService.goUp();
		    break;
		case DOWN:
		    i = columnHandlerService.goDown();
		    break;
		case BACK:
		    i = columnHandlerService.goBack();
		    break;
		}
	    }
	} catch (RemoteException e) {
	    e.printStackTrace();
	}
	return i;
    }

    final void goRight() {
	Log.v(TAG, "goRight");
	if (!shouldReact || !mBeElement)
	    return;
	Intent i = goDir(Dir.RIGHT);
	if (i != null) {
	    Log.v(TAG, "intent is not null");

	    shouldReact = false;
	    mParent.startActivity(i);
	    mParent.overridePendingTransition(R.anim.slide_in_right,
					      R.anim.slide_out_left);
	} else {
	    Log.v(TAG, "intent is null");
	    shakeDir(Dir.RIGHT);
	}
    }

    final void goLeft() {
	Log.v(TAG, "goLeft");
	if (!shouldReact || !mBeElement)
	    return;
	Intent i = goDir(Dir.LEFT);
	if (i != null) {
	    Log.v(TAG, "intent is not null");
	    shouldReact = false;
	    mParent.startActivity(i);
	    mParent.overridePendingTransition(R.anim.slide_in_left,
					      R.anim.slide_out_right);
	} else {
	    shakeDir(Dir.LEFT);
	    Log.v(TAG, "intent is null");
	}
    }

    final void goUp() {
	if (!shouldReact || !mBeElement)
	    return;
	Intent i = goDir(Dir.UP);
	if (i != null) {
	    shouldReact = false;
	    mParent.startActivity(i);
	    mParent.overridePendingTransition(R.anim.slide_in_top,
					      R.anim.slide_out_bottom);
	} else {
	    shakeDir(Dir.UP);
	    Log.v(TAG, "intent is null");
	}
    }

    final void goDown() {
	if (!shouldReact || !mBeElement)
	    return;
	Intent i = goDir(Dir.DOWN);
	if (i != null) {
	    shouldReact = false;
	    mParent.startActivity(i);
	    mParent.overridePendingTransition(R.anim.slide_in_bottom,
					      R.anim.slide_out_top);
	} else {
	    shakeDir(Dir.DOWN);
	    Log.v(TAG, "intent is null");
	}
    }

    final void goBack() {
	if (!shouldReact || !mBeElement)
	    return;
	Intent i = goDir(Dir.BACK);
	if (i != null) {
	    shouldReact = false;
	    mParent.startActivity(i);
	    mParent.overridePendingTransition(android.R.anim.fade_in,
					      android.R.anim.fade_out);
	} else {
	    Log.v(TAG, "intent is null");
	    // TODO: Failed animation
	}
    }

    final void shakeDir(Dir dir) {
	View rootView = mParent.findViewById(android.R.id.content);
	Animation shake = null;
	switch (dir) {
	case RIGHT:
	    shake = AnimationUtils.loadAnimation(mParent, R.anim.shake_right);
	    break;
	case LEFT:
	    shake = AnimationUtils.loadAnimation(mParent, R.anim.shake_left);
	    break;
	case UP:
	    shake = AnimationUtils.loadAnimation(mParent, R.anim.shake_up);
	    break;
	case DOWN:
	    shake = AnimationUtils.loadAnimation(mParent, R.anim.shake_down);
	    break;
	}
	if (shake != null)
	    rootView.startAnimation(shake);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
	switch(keyCode) {
	case KeyEvent.KEYCODE_DPAD_LEFT:
	    goLeft();
	    return true;
	case KeyEvent.KEYCODE_DPAD_RIGHT:
	    goRight();
	    return true;
	case KeyEvent.KEYCODE_DPAD_UP:
	    goUp();
	    return true;
	case KeyEvent.KEYCODE_DPAD_DOWN:
	    goDown();
	    return true;
	default:
	    return false;
	}
    }

    public void onNewIntent(Intent intent) {
	mParent.setIntent(intent);
    }

}