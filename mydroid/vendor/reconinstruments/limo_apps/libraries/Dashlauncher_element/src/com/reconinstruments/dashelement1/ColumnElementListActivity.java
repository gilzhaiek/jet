package com.reconinstruments.dashelement1;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.app.ListActivity;
import com.reconinstruments.dashlauncherredux.IColumnHandlerService;

public class ColumnElementListActivity extends ListActivity implements IGoDirection {
    private static final String TAG = "ColumnElementListActivity";
    // You need this because java does not have multiple inheritance
    private ColumnElementPseudoActivity mColumnElementPseudoActivity; 

    @Override
    protected void onNewIntent(Intent intent) {
	super.onNewIntent(intent);
	mColumnElementPseudoActivity.onNewIntent(intent);
    }


    @Override
    public void onCreate(Bundle b) {
	super.onCreate(b);
	mColumnElementPseudoActivity = new ColumnElementPseudoActivity(this);
	mColumnElementPseudoActivity.onCreate();
    }

    @Override
    public void onDestroy() {
	mColumnElementPseudoActivity.onDestroy();
	super.onDestroy();
    }

    @Override
    public void onResume() {
	super.onResume();
	mColumnElementPseudoActivity.onResume();
    }

    @Override
    public void onPause() {
	mColumnElementPseudoActivity.onPause();
	super.onPause();
    }

    @Override
    public void onBackPressed() {
	if (canGoDirection()) {
	    goBack();
	}
	else {
	    super.onBackPressed();
	}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	return mColumnElementPseudoActivity.onKeyDown(keyCode, event)?true:super.onKeyDown(keyCode, event);
    }

    final public void goRight() {
	mColumnElementPseudoActivity.goRight();
    }
    final public void goLeft() {
	mColumnElementPseudoActivity.goLeft();
    }
    final public void goUp() {
	mColumnElementPseudoActivity.goUp();
    }
    final public void goDown() {
	mColumnElementPseudoActivity.goDown();
    }
    final public void goBack() {
	mColumnElementPseudoActivity.goBack();
    }
    final public boolean canGoDirection() {
	return mColumnElementPseudoActivity.getBeElement();
    }

}