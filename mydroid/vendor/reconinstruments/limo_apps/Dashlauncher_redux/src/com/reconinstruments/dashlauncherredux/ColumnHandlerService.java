package com.reconinstruments.dashlauncherredux;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.content.Context;
import com.reconinstruments.profile.ProfileParser;
import com.reconinstruments.profile.ProfileManager;
import com.reconinstruments.commonwidgets.BreadcrumbView;

import android.widget.Toast;
import android.os.Message;
import android.os.Handler;
import android.view.Gravity;

/**
 * A class for managing multiple columns. This is the mother class of the new
 * dashlauncher paradigm. It can "host" activities from different packags and if
 * necessary launches them in accordance of the user navigation going left and
 * right, and up and down.
 * 
 * In the current paradigm activities bind to this service and then request the
 * intent to launch the activities to their up down right and left. So in most
 * cases the service just passes in the correct intent and itself does not
 * launch the activity.
 * 
 * @author <a href="mailto:ali@reconinstruments.com">Ali R. Mohazab</a>
 */
public class ColumnHandlerService extends Service {
    private static final String TAG = "ColumnHandlerService";
    private ColumnHolder mColumnHolder;
    private Handler handler;

    private Toast breadcrumbToast;
    private BreadcrumbView mBreadcrumbView;
    private boolean breadcrumbViewCreated = false; // double check this

    @Override
    public IBinder onBind(Intent intent) {
	if (intent.getAction() != null
	    && intent.getAction()
	    .equals("com.reconinstruments.COLUMN_HANDLER_SERVICE")) {
	    // This is through normal invocation of the service by
	    // column element activities
	    return binder;
	} else {
	    // This is just for the controller activity that calls the
	    // service by component name.
	    return controllerBinder;
	}
    }

    @Override
    public void onCreate() {
	super.onCreate();
	Log.d(TAG, "onCreate");

	handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		    super.handleMessage(msg);
		    showHorizontalBreadcrumb(ColumnHandlerService.this, mColumnHolder.getTotalHorizontal(), mColumnHolder.getHorizontalPosition());
		}	
	    };
    }

    @Override
    public int onStartCommand(Intent i, int flag, int startId) {
	Log.d(TAG, "onStartCommand");
	if (i.hasExtra("ChosenProfile")) {
	    ProfileManager
		.updateChosenProfile(i.getStringExtra("ChosenProfile"));
	}
	updateProfileInfo();
	launchHomeElement();
	return START_STICKY;
    }

    private void updateProfileInfo() {
	Log.d(TAG, "updateProfileInfo");
	// Use parser to parse the user profile.
	ProfileParser pp = new ProfileParser(ProfileManager
					     .fetchTheChosenProfile());
	mColumnHolder = pp.generateColumnHolder((Context) this);
    }

    private void launchHomeElement() {
	Log.d(TAG, "launchHomeElement");
	// Run the home element.
	goDir(4); // goes to the home index
	mColumnHolder.launchCurrentIndexColumnActivity(this);
    }

    private Intent goDir(int dir) {
	Intent i = null;
	switch (dir) {
	case 0:
	    i = mColumnHolder.goRight();
	    break;
	case 1:
	    i = mColumnHolder.goLeft();
	    break;
	case 2:
	    i = mColumnHolder.goUp();
	    break;
	case 3:
	    i = mColumnHolder.goDown();
	    break;
	case 4:
	    i = mColumnHolder.goBack();
	    break;
	}
	Message showBreadcrumb = Message.obtain();
	handler.sendMessage(showBreadcrumb);
	return i;
    }

    private final IColumnHandlerService.Stub binder =
	new IColumnHandlerService.Stub() {
	    public Intent goRight() {
		return goDir(0);
	    }
	    public Intent goLeft() {
		return goDir(1);
	    }
	    public Intent goUp() {
		return goDir(2);
	    }
	    public Intent goDown() {
		return goDir(3);
	    }
	    public Intent goBack() {
		return goDir(4);
	    }
	};

    // This binder is only for the controller activity that binds to
    // this service.
    private final IColumnHandlerServiceController.Stub controllerBinder =
	new IColumnHandlerServiceController.Stub() {
	    public void updateProfileInfo() {
		ColumnHandlerService.this.updateProfileInfo();
	    }

	    public void launchHomeElement() {
		ColumnHandlerService.this.launchHomeElement();
	    }
	};

    public void showHorizontalBreadcrumb(Context context, int size, int currentPosition){

        if(breadcrumbViewCreated){
            mBreadcrumbView.redrawBreadcrumbs(context, currentPosition);
            mBreadcrumbView.invalidate();
        } else {
            int[] dashFrags = new int[size];
            for(int i=0; i<dashFrags.length; i++)
                dashFrags[i] = BreadcrumbView.DYNAMIC_ICON;

            mBreadcrumbView = new BreadcrumbView(context, true, currentPosition, dashFrags);
            mBreadcrumbView.invalidate();

            if(breadcrumbToast == null)
                breadcrumbToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);

            breadcrumbToast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
            breadcrumbViewCreated = true;
        }

        breadcrumbToast.setView(mBreadcrumbView);
        breadcrumbToast.show();
    }

}