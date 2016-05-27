/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.contour.connect;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.contour.connect.debug.CLog;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;


public class AboutFragmentActivity extends ActionBarActivity {
    public static final String TAG = "AboutFragmentActivity";
    private boolean mDebug;
    static final int NUM_ITEMS = 4;

    MyAdapter mAdapter;

    ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDebug = this.getResources().getBoolean(R.bool.debug_enabled);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_pager);
        mAdapter = new MyAdapter(getSupportFragmentManager(),mDebug);

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        TextView mActionBarTitleView = (TextView)getActionBarHelper().setTitleView(R.layout.text_title_bar);
        (mActionBarTitleView).setText(R.string.aboutcontourtitle);
    }
    
    

    public static class MyAdapter extends FragmentPagerAdapter {
        private final boolean mDebug;
        public MyAdapter(FragmentManager fm, boolean debug) {
            super(fm);
            mDebug = debug;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            return InteralPageFragment.newInstance(position,mDebug);
        }
    }

    public static class InteralPageFragment extends Fragment {
        int mNum;
        private boolean mDebug;
        static final int[]         IMAGES        = { R.drawable.about1graphic, R.drawable.about2graphic, R.drawable.about3graphic, R.drawable.about4graphic };
        static final int[]         HEADERS       = { R.string.aboutheading1, R.string.aboutheading2, R.string.aboutheading3, R.string.aboutheading4 };
        static final int[]         DESCS         = { R.string.aboutdesc1, R.string.aboutdesc2, R.string.aboutdesc3, R.string.aboutdesc4 };

        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static InteralPageFragment newInstance(int num, boolean debug) {
            InteralPageFragment f = new InteralPageFragment();
            f.mDebug = debug;
            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        @Override
        public void onResume() {
            super.onResume();
//            TextView mActionBarTitleView = (TextView)((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
//            (mActionBarTitleView).setText(R.string.about_title);
        }
        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            if(mDebug) CLog.out(TAG,"onCreateView",mNum);
            View v = inflater.inflate(R.layout.about, container, false);
            ((TextView) v.findViewById(R.id.about_header)).setText(getActivity().getString(HEADERS[mNum]));
            ((TextView) v.findViewById(R.id.about_desc)).setText(getActivity().getString(DESCS[mNum]));
            ((ImageView) v.findViewById(R.id.about_graphic)).setImageDrawable(getActivity().getResources().getDrawable(IMAGES[mNum]));
//            if (mNum == 0) {
//                TextView mActionBarTitleView = (TextView) ((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
//                (mActionBarTitleView).setText(R.string.about_title);
//            }
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }
    }
}

