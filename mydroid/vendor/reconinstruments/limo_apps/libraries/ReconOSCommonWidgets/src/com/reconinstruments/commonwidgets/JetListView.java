package com.reconinstruments.commonwidgets;

import android.app.Instrumentation;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ListView;

/**
 * 
 * <code>JetListView</code> supports forward/backward swipe actions by
 * simulating KeyEvent.KEYCODE_DPAD_RIGHT to KeyEvent.KEYCODE_DPAD_CENTER and
 * KEYCODE_DPAD_LEFT to KeyEvent.KEYCODE_BACK. The lower layer component on Jet
 * platform is in charge of mapping forward/backward swipe gesture to
 * KeyEvent.KEYCODE_DPAD_RIGHT and KEYCODE_DPAD_LEFT.
 * 
 */
public class JetListView extends ListView {
	
	private static final String TAG = "JetListView";
	
	public static int FORWARD_SWIPE = 1;
	public static int BACKWARD_SWIPE = 2;
	//swipe action: 1 means forward, 2 means backward
	private int action = FORWARD_SWIPE;
	
	private Instrumentation inst = new Instrumentation();
	
	public JetListView(Context context) {
		super(context);
	}

	public JetListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public JetListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_RIGHT:
                // simulate KeyEvent.KEYCODE_DPAD_CENTER
                if(getAction() == FORWARD_SWIPE){
                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                }else{
                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                }
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
                // simulate KeyEvent.KEYCODE_BACK
                if(getAction() == FORWARD_SWIPE){
                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                }else{
                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                }
				return true;
			default:
				return super.onKeyUp(keyCode, event);
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
}
