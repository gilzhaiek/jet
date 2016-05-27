/*
 * Copyright (C) 2011 Recon Instruments.
 * Most of code is take from Android SDK's SoftKeyboard sample
 * with modification to support text inputting with 4 ARROW keys + selection keys 
 */


package com.reconinstruments.softkeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;

public class LimoKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;

    public LimoKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LimoKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }
}
