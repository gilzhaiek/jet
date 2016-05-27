package com.contour.utils.api;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

public abstract class ApiHelper {
    protected Activity mActivity;

    protected ApiHelper(Activity activity) {
        mActivity = activity;
    }

    public static ApiHelper createInstance(Activity activity) {
        if (AndroidUtils.isJellybeanOrHigher()) {
            return new ApiHelperICS(activity);
        } else if (AndroidUtils.isIceCreamSandwichOrHigher()) {
            return new ApiHelperICS(activity);
        } else if (AndroidUtils.isHoneycombOrHigher()) {
            return new ApiHelperHoneycomb(activity);
        } else if (AndroidUtils.isGingerbreadOrHigher()) {
            return new ApiHelperGingerbread(activity);
        } else if (AndroidUtils.isFroyoOrHigher()) {
            return new ApiHelperFroyo(activity);
        } else {
            return new ApiHelperBase(activity);
        }
    }

    public void onCreate(Bundle savedInstanceState) {

    }


    /**
     * Action bar helper code to be run in {@link Activity#onPostCreate(android.os.Bundle)}.
     */
    public void onPostCreate(Bundle savedInstanceState) {
    }
    
    /**
     * Action bar helper code to be run in {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
     *
     * NOTE: Setting the visibility of menu items in <em>menu</em> is not currently supported.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * Action bar helper code to be run in {@link Activity#onTitleChanged(CharSequence, int)}.
     */
    protected void onTitleChanged(CharSequence title, int color) {
    }

    /**
     * Sets the indeterminate loading state of the item with ID {@link R.id.menu_refresh}.
     * (where the item ID was menu_refresh).
     */
    public abstract void setRefreshActionItemState(boolean refreshing);

    /**
     * Returns a {@link MenuInflater} for use when inflating menus. The implementation of this
     * method in {@link ActionBarHelperBase} returns a wrapped menu inflater that can read
     * action bar metadata from a menu resource pre-Honeycomb.
     */
    public MenuInflater getMenuInflater(MenuInflater superMenuInflater) {
        return superMenuInflater;
    }

    protected abstract void enableStrictMode();

    protected abstract HttpClient getHttpClient(String userAgentString);
}
