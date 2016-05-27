package com.reconinstruments.breadcrumb;

import android.os.Bundle;
import android.app.Activity;
import android.view.Gravity;
import android.view.Menu;
import android.widget.Toast;

public class BreadcrumbActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_breadcrumb);
		showBreadcrumb(BreadcrumbView.VERTICAL, 5, 2);
		showBreadcrumb(BreadcrumbView.HORIZONTAL, 5, 2);
	}

	
	private void showBreadcrumb(int orientation, int total, int current) {
		int[] dashFrags = new int[total];
		dashFrags[0]=BreadcrumbView.OTHER_ICON;
		for(int i=1; i<dashFrags.length; i++)
			dashFrags[i] = BreadcrumbView.OTHER_ICON;

		BreadcrumbView mBreadcrumbView = new BreadcrumbView(getApplicationContext(), orientation, current, dashFrags);

		mBreadcrumbView.invalidate();

		Toast breadcrumbToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

		if(orientation == 0){
			breadcrumbToast.setGravity(Gravity.TOP, 0, 0);
		}else{
			breadcrumbToast.setGravity(Gravity.RIGHT, 0, 0);
		}
		breadcrumbToast.setView(mBreadcrumbView);
		breadcrumbToast.show();

	}

}
