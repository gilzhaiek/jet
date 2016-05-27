
package com.reconinstruments.itemhost;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class ItemHostActivity extends FragmentActivity {

    private static final String TAG = ItemHostActivity.class.getSimpleName();

    //the items hosting.
    public static final int ITEM_1 = 1;
    public static final int ITEM_2 = 2;
    public static final int ITEM_3 = 3;

    private PagerContainer mContainer;
    private ItemPageAdapter pageAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<Fragment> fragments = getFragments();
        pageAdapter = new ItemPageAdapter(getSupportFragmentManager(), fragments);
        mContainer = (PagerContainer) findViewById(R.id.pager_container);
        ViewPager pager = mContainer.getViewPager();
        pager.setAdapter(pageAdapter);

        // Necessary or the pager will only have one extra page to show make
        // this at least however many pages you can see
        pager.setOffscreenPageLimit(pageAdapter.getCount());
        // A little space between pages
        pager.setPageMargin(10);
        // If hardware acceleration is enabled, you should also remove clipping
        // on the pager for its children.
        pager.setClipChildren(false);

        // for demo only, start the service as long as the activity is started.
        startService(new Intent(this, ItemHostService.class));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // for demo only, stop the service as long as the activity is stopped.
        stopService(new Intent(this, ItemHostService.class));
    }

    private List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();

        fList.add(new ItemFragment(ITEM_1));
        fList.add(new ItemFragment(ITEM_2));
        fList.add(new ItemFragment(ITEM_3));

        return fList;
    }

    private class ItemPageAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments;

        public ItemPageAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return this.fragments.get(position);
        }

        @Override
        public int getCount() {
            return this.fragments.size();
        }
    }
}
