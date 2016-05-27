package com.reconinstruments.offsetkeyboard;

import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

@SuppressWarnings("unused")
public class OffsetKeyboard extends InputMethodService {

	private static final String TAG = "OffsetKeyboard";
	
	private OffsetKeyboardView mInputView = null;
	private boolean visible = false;
	
	@Override public void onCreate() {
		super.onCreate();
	}
	
	@Override public View onCreateInputView() {
		mInputView = (OffsetKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
		
		mInputView.setClickable(true);
		mInputView.setFocusableInTouchMode(true);
		mInputView.setFocusable(true);
		mInputView.setEnabled(true);

		ExtractEditText extractEditTextView = new ExtractEditText(this);
		extractEditTextView.setId(android.R.id.inputExtractEditText);
		
		return mInputView;
	}
	
	@Override public View onCreateCandidatesView() {
		super.onCreateCandidatesView();
		return null;
		
	}
	
	@Override public void onStartInputView(EditorInfo info, boolean restarting) {
		super.onStartCandidatesView(info, restarting);
	}
	
	@Override public void onFinishInput() {
		super.onFinishInput();
	}
	
	@Override public void onDestroy() {
		super.onDestroy();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Log.v(TAG, "onKeyDown keyCode: " + keyCode);
		if(mInputView != null && visible) {

			
			if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction() == KeyEvent.ACTION_DOWN) {
				mInputView.shiftLeft();
				return true;
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction() == KeyEvent.ACTION_DOWN) {
				mInputView.shiftRight();
				return true;
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_UP && event.getAction() == KeyEvent.ACTION_DOWN) {
				mInputView.shiftUp();
				return true;
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
				mInputView.shiftDown();
				return true;
			} else if(mInputView != null && visible && keyCode == KeyEvent.KEYCODE_DPAD_CENTER|| keyCode == KeyEvent.KEYCODE_ENTER) {
				char c = mInputView.getSelectedChar();
				
				if(c == (char) 0x21D0) { // delete
					getCurrentInputConnection().deleteSurroundingText(1, 0);
				} else if(c == (char) 0x2190) { // left arrow
					this.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
				} else if(c == (char) 0x2192) { // right arrow
					this.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
				} else if(c == 0x1 || c == 0x2 || c == 0x3 ) {
					mInputView.toggleCharList();
				} else {
					getCurrentInputConnection().commitText(String.valueOf(c), 1);
				}
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onWindowShown() {
		visible = true;
	}
	
	@Override
	public void onWindowHidden() {
		visible = false;
	}
}
