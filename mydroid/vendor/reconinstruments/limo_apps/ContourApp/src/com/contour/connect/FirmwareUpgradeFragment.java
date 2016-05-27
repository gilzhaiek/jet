package com.contour.connect;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class FirmwareUpgradeFragment extends Fragment {

    public static final String TAG = "FIrmwareUpgradeFragment";
    View                       mActionBarTitleView;

    public static FirmwareUpgradeFragment newInstance() {
        FirmwareUpgradeFragment f = new FirmwareUpgradeFragment();

        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * The Fragment's UI is just a simple text view showing its instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.firmware_upgrade_frag, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Null pointer exception if executed before onStart(). Actionbar is not
        // set up until onPostCreate()
        mActionBarTitleView = ((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
        ((TextView) mActionBarTitleView).setText(R.string.firmwareupdatetitle);
    }
}
