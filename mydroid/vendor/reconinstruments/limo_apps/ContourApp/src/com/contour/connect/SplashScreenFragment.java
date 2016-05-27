package com.contour.connect;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class SplashScreenFragment extends Fragment {
    
    public static final String TAG = "SplashScreenFragment";
    TextView mStatusTextView; 

	private static  Handler sHandler = new Handler();
	private final Runnable mSplashRunner = new Runnable() {
        public void run() {
            MainActivity m = (MainActivity)SplashScreenFragment.this.getActivity();
            if(m != null && m.hasWindowFocus()) {
                if(m.isConnected() == false) {
                    m.loadDisconnectedFrag();
                } else {
                    mStatusTextView.setText(R.string.notif_connecting);
                }
            }
        }
    };
    
	public static SplashScreenFragment newInstance() {
        SplashScreenFragment f = new SplashScreenFragment();
        return f;
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mStatusTextView = (TextView) getActivity().findViewById(R.id.splash_status_text);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        sHandler.removeCallbacks(mSplashRunner);
        sHandler.postDelayed(mSplashRunner, 10000);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        sHandler.removeCallbacks(mSplashRunner);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.splash_screen, container, false);
        return v; 
    }
}
