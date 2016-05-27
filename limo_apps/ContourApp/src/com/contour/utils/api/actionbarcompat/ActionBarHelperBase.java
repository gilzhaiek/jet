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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.contour.connect.R;


/**
 * A class that implements the action bar pattern for pre-Honeycomb devices.
 */
public class ActionBarHelperBase extends ActionBarHelper {
    private static final String MENU_RES_NAMESPACE       = "http://schemas.android.com/apk/res/android";
    private static final String MENU_ATTR_ID             = "id";
    private static final String MENU_ATTR_SHOW_AS_ACTION = "showAsAction";
    private static final String MENU_ATTR_VISIBLE = "visible";


    protected Set<Integer>      mActionItemIds           = new HashSet<Integer>();
    private boolean mRefreshing;
    
    protected ActionBarHelperBase(Activity activity) {
        super(activity);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mActivity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    }

    /** {@inheritDoc} */
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        mActivity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar_compat);
        setupActionBar();

        SimpleMenu menu = new SimpleMenu(mActivity);
        mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
        mActivity.onPrepareOptionsMenu(menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (mActionItemIds.contains(item.getItemId())) {
                addActionItemCompatFromMenuItem(item);
            }
        }
    }

    /**
     * Sets up the compatibility action bar with the given title.
     */
    private void setupActionBar() {
        final ViewGroup actionBarCompat = getActionBarCompat();
        if (actionBarCompat == null) {
            return;
        }

//        LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT);
//        springLayoutParams.weight = 1;

        // Add Home button
//        SimpleMenu tempMenu = new SimpleMenu(mActivity);
//        SimpleMenuItem homeItem = new SimpleMenuItem(tempMenu, android.R.id.home, 0, mActivity.getString(R.string.app_name));
//        homeItem.setIcon(R.drawable.ic_home);
//        addActionItemCompatFromMenuItem(homeItem);

        // Add title text
//        TextView titleText = new TextView(mActivity, null, R.attr.actionbarCompatTitleStyle);
//        titleText.setLayoutParams(springLayoutParams);
//        titleText.setText(mActivity.getTitle());
//        actionBarCompat.addView(titleText);
    }

    /** {@inheritDoc} */
    @Override
    public void setRefreshActionItemState(boolean refreshing) {
        mRefreshing = refreshing;
        View refreshButton = mActivity.findViewById(R.id.actionbar_compat_item_refresh);
        View refreshIndicator = mActivity.findViewById(R.id.actionbar_compat_item_refresh_progress);
        View disabledIndicator = mActivity.findViewById(R.id.actionbar_compat_item_disabled);
        View closeButton = mActivity.findViewById(R.id.actionbar_compat_item_close);

        if (refreshButton != null) {
            refreshButton.setVisibility(refreshing ? View.GONE : View.VISIBLE);
        }
        if (disabledIndicator != null) {
            disabledIndicator.setVisibility(View.GONE);
        }
        if (refreshIndicator != null) {
            refreshIndicator.setVisibility(refreshing ? View.VISIBLE : View.GONE);
        }
        if (closeButton != null) {
            closeButton.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void setDisabledActionItemState(boolean disabled) {
        View refreshButton = mActivity.findViewById(R.id.actionbar_compat_item_refresh);
        View refreshIndicator = mActivity.findViewById(R.id.actionbar_compat_item_refresh_progress);
        View disabledIndicator = mActivity.findViewById(R.id.actionbar_compat_item_disabled);
        View closeButton = mActivity.findViewById(R.id.actionbar_compat_item_close);



        if (disabledIndicator != null) {
            disabledIndicator.setVisibility(disabled ? View.VISIBLE : View.GONE);
        }    
        if (!disabled) {
            if (refreshButton != null) {
                refreshButton.setVisibility(mRefreshing ? View.GONE : View.VISIBLE);
            }
            if (refreshIndicator != null) {
                refreshIndicator.setVisibility(mRefreshing ? View.VISIBLE : View.GONE);
            }
        } else {
            if (refreshButton != null) {
                refreshButton.setVisibility(View.GONE);
            }
            if (refreshIndicator != null) {
                refreshIndicator.setVisibility(View.GONE);
            }
        }
        if (closeButton != null) {
            closeButton.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void setCloseActionItemState(boolean closeable) {
        View refreshButton = mActivity.findViewById(R.id.actionbar_compat_item_refresh);
        View refreshIndicator = mActivity.findViewById(R.id.actionbar_compat_item_refresh_progress);
        View disabledIndicator = mActivity.findViewById(R.id.actionbar_compat_item_disabled);
        View closeButton = mActivity.findViewById(R.id.actionbar_compat_item_close);

        if (disabledIndicator != null) {
            disabledIndicator.setVisibility(View.GONE);
        }        
        if (closeButton != null) {
            closeButton.setVisibility(closeable? View.VISIBLE : View.GONE);
        }
        if (!closeable) {
            if (refreshButton != null) {
                refreshButton.setVisibility(mRefreshing ? View.GONE : View.VISIBLE);
            }
            if (refreshIndicator != null) {
                refreshIndicator.setVisibility(mRefreshing ? View.VISIBLE : View.GONE);
            }
        } else {
            if (refreshButton != null) {
                refreshButton.setVisibility(View.GONE);
            }
            if (refreshIndicator != null) {
                refreshIndicator.setVisibility(View.GONE);
            }
        }
    }


    /**
     * Action bar helper code to be run in
     * {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
     * 
     * NOTE: This code will mark on-screen menu items as invisible.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Hides on-screen action items from the options menu.
        for (Integer id : mActionItemIds) {
            menu.findItem(id).setVisible(false);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        TextView titleView = (TextView) mActivity.findViewById(R.id.actionbar_compat_title);
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    /**
     * Returns a {@link android.view.MenuInflater} that can read action bar
     * metadata on pre-Honeycomb devices.
     */
    public MenuInflater getMenuInflater(MenuInflater superMenuInflater) {
        return new WrappedMenuInflater(mActivity, superMenuInflater);
    }

    /**
     * Returns the {@link android.view.ViewGroup} for the action bar on phones
     * (compatibility action bar). Can return null, and will return null on
     * Honeycomb.
     */
    private ViewGroup getActionBarCompat() {
        return (ViewGroup) mActivity.findViewById(R.id.actionbar_compat);
    }

    /**
     * Adds an action button to the compatibility action bar, using menu
     * information from a {@link android.view.MenuItem}. If the menu item ID is
     * <code>menu_refresh</code>, the menu item's state can be changed to show a
     * loading spinner using
     * {@link com.example.android.actionbarcompat.ActionBarHelperBase#setRefreshActionItemState(boolean)}
     * .
     */
    private View addActionItemCompatFromMenuItem(final MenuItem item) {
        final int itemId = item.getItemId();

        final ViewGroup actionBar = getActionBarCompat();
        if (actionBar == null) {
            return null;
        }

        // Create the button
        ImageButton actionButton = new ImageButton(mActivity, null, itemId == android.R.id.home ? R.attr.actionbarCompatItemHomeStyle : R.attr.actionbarCompatItemStyle);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int) mActivity.getResources().getDimension(itemId == android.R.id.home ? R.dimen.actionbar_compat_button_home_width : R.dimen.actionbar_compat_button_width), ViewGroup.LayoutParams.FILL_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

        actionButton.setLayoutParams(layoutParams);
        if (itemId == R.id.menu_settings) {
            actionButton.setId(R.id.actionbar_compat_item_refresh);
        }
        actionButton.setImageDrawable(item.getIcon());
        actionButton.setScaleType(ImageView.ScaleType.CENTER);
        actionButton.setContentDescription(item.getTitle());
        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
            }
        });

        actionBar.addView(actionButton);

        if (item.getItemId() == R.id.menu_settings) {
            // Refresh buttons should be stateful, and allow for indeterminate
            // progress indicators,
            // so add those.
            ProgressBar indicator = new ProgressBar(mActivity, null, R.attr.actionbarCompatProgressIndicatorStyle);

            final int buttonWidth = mActivity.getResources().getDimensionPixelSize(R.dimen.actionbar_compat_button_width);
            final int buttonHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.actionbar_compat_height);
            final int progressIndicatorWidth = buttonWidth / 2;

            RelativeLayout.LayoutParams indicatorLayoutParams = new RelativeLayout.LayoutParams(progressIndicatorWidth, progressIndicatorWidth);
            indicatorLayoutParams.setMargins((buttonWidth - progressIndicatorWidth) / 2, (buttonHeight - progressIndicatorWidth) / 2, (buttonWidth - progressIndicatorWidth) / 2, 0);
            indicatorLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            indicatorLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            indicator.setLayoutParams(indicatorLayoutParams);
            indicator.setVisibility(View.GONE);
            indicator.setId(R.id.actionbar_compat_item_refresh_progress);
            actionBar.addView(indicator);

            ImageButton disabledView = new ImageButton(mActivity, null, R.attr.actionbarCompatItemStyle);
            disabledView.setLayoutParams(layoutParams);
            disabledView.setScaleType(ImageView.ScaleType.CENTER);
            disabledView.setImageResource(R.drawable.ic_action_settings_disabled);
            disabledView.setVisibility(View.GONE);
            disabledView.setId(R.id.actionbar_compat_item_disabled);
            actionBar.addView(disabledView);
            
            ImageButton closeView = new ImageButton(mActivity, null, R.attr.actionbarCompatItemStyle);
            closeView.setLayoutParams(layoutParams);
            closeView.setScaleType(ImageView.ScaleType.CENTER);
            closeView.setImageResource(R.drawable.btn_close_state_list);
            closeView.setVisibility(View.GONE);
            closeView.setId(R.id.actionbar_compat_item_close);
            closeView.setOnClickListener(new View.OnClickListener() {        
                        @Override
                        public void onClick(View v) {
                           ((ActionBarActivity)mActivity).onActionCloseButtonClicked();
                        }
                    });
            actionBar.addView(closeView);
        }

        return actionButton;
    }

    /**
     * A {@link android.view.MenuInflater} that reads action bar metadata.
     */
    private class WrappedMenuInflater extends MenuInflater {
        MenuInflater mInflater;

        public WrappedMenuInflater(Context context, MenuInflater inflater) {
            super(context);
            mInflater = inflater;
        }

        @Override
        public void inflate(int menuRes, Menu menu) {
            loadActionBarMetadata(menuRes);
            mInflater.inflate(menuRes, menu);
        }

        /**
         * Loads action bar metadata from a menu resource, storing a list of
         * menu item IDs that should be shown on-screen (i.e. those with
         * showAsAction set to always or ifRoom).
         * 
         * @param menuResId
         */
        private void loadActionBarMetadata(int menuResId) {
            XmlResourceParser parser = null;
            try {
                parser = mActivity.getResources().getXml(menuResId);

                int eventType = parser.getEventType();
                int itemId;
                int showAsAction;

                boolean eof = false;
                while (!eof) {
                    switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (!parser.getName().equals("item")) {
                            break;
                        }

                        itemId = parser.getAttributeResourceValue(MENU_RES_NAMESPACE, MENU_ATTR_ID, 0);
                        if (itemId == 0) {
                            break;
                        }

                        showAsAction = parser.getAttributeIntValue(MENU_RES_NAMESPACE, MENU_ATTR_SHOW_AS_ACTION, -1);
                        if (showAsAction == MenuItem.SHOW_AS_ACTION_ALWAYS || showAsAction == MenuItem.SHOW_AS_ACTION_IF_ROOM) {
                            boolean visible = parser.getAttributeBooleanValue(MENU_RES_NAMESPACE, MENU_ATTR_VISIBLE, true);
                            if(visible)
                                mActionItemIds.add(itemId);
                        }
                        break;

                    case XmlPullParser.END_DOCUMENT:
                        eof = true;
                        break;
                    }

                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                throw new InflateException("Error inflating menu XML", e);
            } catch (IOException e) {
                throw new InflateException("Error inflating menu XML", e);
            } finally {
                if (parser != null) {
                    parser.close();
                }
            }
        }

    }

    View mLastView;
    ViewGroup mActionBarCompat;
    @Override
    public View setTitleView(int resId) {
        if(mLastView != null && mLastView.getId()==resId) return mLastView;
        
        if(mActionBarCompat == null) {
            ViewGroup actionBarCompat = getActionBarCompat();
            if (actionBarCompat == null) {
                return null;
            }
            mActionBarCompat = actionBarCompat;
        }
       
        View customView = LayoutInflater.from(this.mActivity).inflate(resId, null);
        setTitleView(customView);
        return customView;
    }
    
    @Override
    public void setTitleView(View view) {
        if(view.equals(mLastView)) return;
        
        RelativeLayout.LayoutParams springLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        springLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//        springLayoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.actionbar_compat_item_refresh);

        view.setLayoutParams(springLayoutParams);
        if(mLastView != null) {
            mActionBarCompat.removeView(mLastView);
            mLastView = null;
        }
        mActionBarCompat.addView(view,0);
        mLastView = view;
    }

    @Override
    public View getTitleView() {
        return mLastView;
    }

    
}
