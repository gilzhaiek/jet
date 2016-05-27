package com.reconinstruments.maps;

import java.util.List;

import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.commonwidgets.ReconJetDialog;

public class FindBuddyOverlay extends ReconJetDialog {
	private static final String TAG = FindBuddyOverlay.class.getSimpleName();

	private MapActivity mActivity;
	private int lastIndex = 0;

    public FindBuddyOverlay(String title, List<Fragment> list, int layout, MapActivity activity) {
        super(title, list, layout, 230, 0, 200, 130, 1.0f);
        mActivity = activity;
        enableDynamicPager = true;
    }

	@Override
	protected void setupKeyListener() {
		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
					if (event.getAction() != KeyEvent.ACTION_UP) {
						return false;
					}
					int index = mPager.getCurrentItem();
					Log.v(TAG, "setOnKeyListeneronKey_enter, index=" + index);

					// (new JetToast(mActivity.getApplicationContext(),
					// JetToast.NO_IMAGE, "No Sensor Found")).show();

					//getDialog().dismiss();
					//mActivity.ShowStatusBar(true);
					//mActivity.mMapFragment.showMapBtns(true);

					//return true;
				} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

					if (event.getAction() != KeyEvent.ACTION_UP) {
						return false;
					}

					int index = mPager.getCurrentItem();
					Log.v(TAG, "setOnKeyListener. onKey_enter, index=" + index);
					
					if (lastIndex == index)
						return false;
					
					lastIndex = index;
					mActivity.showBuddyInMap(index);

					// getDialog().dismiss();
					return false;

				} else if ( keyCode == KeyEvent.KEYCODE_BACK) {

					if (event.getAction() != KeyEvent.ACTION_UP) {
						return false;
					}

					int index = mPager.getCurrentItem();
					Log.v(TAG, "setOnKeyListener. onKey_UP, index=" + index);

					getDialog().dismiss();
					mActivity.mMapFragment.gotoBaseMapModeFromFindBuddy();
					
					return false;

				}
				return false;
			}
		});
	}

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mPager.setPadding(0,0,0,0); // left aligned
        ((CarouselItemPageAdapter)mPager.getAdapter()).setBreadcrumbView(false);
        view.setBackgroundColor(0x00000000);
        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

}
