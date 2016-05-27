/*
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.contour.utils.api.actionbarcompat;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.contour.connect.R;


/**
 * An extension of {@link ActionBarHelper} that provides Android 3.0-specific functionality for
 * Honeycomb tablets. It thus requires API level 11.
 */
@TargetApi(11)
public class ActionBarHelperHoneycomb extends ActionBarHelper {
    private Menu mOptionsMenu;
    private View mRefreshIndeterminateProgressView = null;

    protected ActionBarHelperHoneycomb(Activity activity) {
        super(activity);       
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        mActivity.getActionBar().setDisplayShowHomeEnabled(false);
        mActivity.getActionBar().setDisplayShowTitleEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void setRefreshActionItemState(boolean refreshing) {
        // On Honeycomb, we can set the state of the refresh button by giving it a custom
        // action view.
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_settings);
        if (refreshItem != null) {
            if (refreshing) {
                if (mRefreshIndeterminateProgressView == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            getActionBarThemedContext().getSystemService(
                                    Context.LAYOUT_INFLATER_SERVICE);
                    mRefreshIndeterminateProgressView = inflater.inflate(
                            R.layout.actionbar_indeterminate_progress, null);
                }

                refreshItem.setActionView(mRefreshIndeterminateProgressView);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }
    
    @Override
    public void setDisabledActionItemState(boolean disabled) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem disabledItem = mOptionsMenu.findItem(R.id.menu_settings);
        if (disabledItem != null) {
            if (disabled) {
                disabledItem.setEnabled(false);
            } else {
                disabledItem.setEnabled(true);
            }
        }
    }
    
    @Override
    public void setCloseActionItemState(boolean closeable) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem settingsItem = mOptionsMenu.findItem(R.id.menu_settings);
        final MenuItem closeItem = mOptionsMenu.findItem(R.id.menu_close);

        if (settingsItem != null && closeItem != null) {
            if (closeable) {
                settingsItem.setVisible(false);
                closeItem.setVisible(true);
            } else {
                settingsItem.setVisible(true);
                closeItem.setVisible(false);
            }
        }
    }

    /**
     * Returns a {@link Context} suitable for inflating layouts for the action bar. The
     * implementation for this method in {@link ActionBarHelperICS} asks the action bar for a
     * themed context.
     */
    protected Context getActionBarThemedContext() {
        return mActivity;
    }

    @Override
    public View setTitleView(int resId) {   
        View lastCustomView = mActivity.getActionBar().getCustomView();
        if(lastCustomView != null && lastCustomView.getId() == resId) return lastCustomView;

        View customView = LayoutInflater.from(this.mActivity).inflate(resId, null);
        setTitleView(customView);
        return customView;
    }

    @Override
    public View getTitleView() {
       return mActivity.getActionBar().getCustomView();
    }

    @Override
    public void setTitleView(View view) {
        if(view.equals(mActivity.getActionBar().getCustomView())) return;
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        mActivity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM); 
        mActivity.getActionBar().setCustomView(view, params);        
    }    
}
