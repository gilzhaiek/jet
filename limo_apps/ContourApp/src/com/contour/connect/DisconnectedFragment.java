package com.contour.connect;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class DisconnectedFragment extends Fragment {

    public static final String TAG = "DisconnectedFragment";
    static Handler sHandler = new Handler(); 

    View mActionBarTitleView;
    TextView mHowToPairTextView;
    TextView mFirmwareUpgradeTextView;
    /**
     * Create a new instance of AboutFragment, providing "num" as an argument.
     */
    public static DisconnectedFragment newInstance() {
        DisconnectedFragment f = new DisconnectedFragment();

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
        View v = inflater.inflate(R.layout.disconnected_frag, container, false);
        mHowToPairTextView = (TextView) v.findViewById(R.id.disconnected_desc);
        mFirmwareUpgradeTextView = (TextView) v.findViewById(R.id.firmware_upgrade_desc);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        //Null pointer exception if executed before onStart(). Actionbar is not set up until onPostCreate()
        mActionBarTitleView = ((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
        ((TextView)mActionBarTitleView).setText(R.string.connectionhelptitle);
        postDelayedTextSwap();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        sHandler.removeCallbacks(mTextSwapRunner);
    }
    
    public void setTitleTextConnecting(boolean connecting) {
        if(this.isResumed() == false) return;
        if(connecting)
            ((TextView)mActionBarTitleView).setText(R.string.notif_connecting);
        else
            ((TextView)mActionBarTitleView).setText(R.string.connectionhelptitle);

    }
    
    private void postDelayedTextSwap() { 
        sHandler.removeCallbacks(mTextSwapRunner);
        sHandler.postDelayed(mTextSwapRunner, 10000);
    }
    
    //this shows UI for cameras that are having difficulty connecting due to out of date firmware 
    private void toggleFirmwareUpgradeReminder() {
        if(mHowToPairTextView.getVisibility() == View.VISIBLE) {
            mHowToPairTextView.setVisibility(View.GONE);
            mFirmwareUpgradeTextView.setVisibility(View.VISIBLE);
        } else {
            mHowToPairTextView.setVisibility(View.VISIBLE);
            mFirmwareUpgradeTextView.setVisibility(View.GONE);
        }
        postDelayedTextSwap();
    }
    
    Runnable  mTextSwapRunner = new Runnable() {
        @Override
        public void run() {
            toggleFirmwareUpgradeReminder();
        }
    };
    
}
