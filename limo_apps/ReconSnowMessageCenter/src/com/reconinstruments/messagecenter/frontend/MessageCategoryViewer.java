package com.reconinstruments.messagecenter.frontend;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

public class MessageCategoryViewer extends FragmentActivity
{
    private static final String TAG = "MessageCategoryViewer";
    private static final String TAG_LIST = "MessageCategoryViewerFragment";
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_LIST);
	if (fragment == null) {
	    fragment = MessageCategoryViewerFragment.newInstance();
	    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    ft.add(android.R.id.content, fragment, TAG_LIST);
	    ft.commit();
	}
    }
}
