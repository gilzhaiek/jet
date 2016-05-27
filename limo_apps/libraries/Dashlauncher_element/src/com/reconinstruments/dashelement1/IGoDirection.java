package com.reconinstruments.dashelement1;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import android.app.Activity;
import com.reconinstruments.dashlauncherredux.IColumnHandlerService;

// Java does not support multiple inheritence so I can't enfore all
// these functions here on both FragmentActivity and Activity. So this
// pseudo activity should sit inside the parent activity as a member
interface IGoDirection {
    public void goRight();
    public void goLeft();
    public void goUp();
    public void goDown();
    public void goBack();
    public boolean canGoDirection();
}