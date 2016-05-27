package com.reconinstruments.messagecenter.frontend;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.messagecenter.R;

public class MessageGroupViewer extends ColumnElementFragmentActivity {
        private static final String TAG = "MessageGroupViewer";
        private static final String TAG_LIST = "MessageGroupViewerFragment";

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                                TAG_LIST);
                if (fragment == null) {
                        fragment = MessageGroupViewerFragment.newInstance();
                        FragmentTransaction ft = getSupportFragmentManager()
                                        .beginTransaction();
                        ft.add(android.R.id.content, fragment, TAG_LIST);
                        ft.commit();
                }
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                        return false;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                        return false;
                }
                return super.onKeyDown(keyCode, event);
        }
}
