
package com.reconinstruments.jetapplauncher.settings;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.commonwidgets.CommonUtils;

/**
 * 
 * <code>JetSettingsListActivity</code> is the base class for the settings app.
 *
 */
public abstract class JetSettingsListActivity extends FragmentActivity {

    protected ArrayList<SettingItem> mSettingList = new ArrayList<SettingItem>();
    protected SettingMainAdapter mListAdapter;
    protected ListView mListView;
    private int mPreviousLastPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.settings_list_layout);
        
        mListView = (ListView)findViewById(android.R.id.list);
        
        setupSettingsItems();

        mListAdapter = new SettingMainAdapter(this, mSettingList);

        mListView.setAdapter(mListAdapter);
        mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        mListView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Intent intent = mSettingList.get(arg2).intent;
                if (intent != null) {
                    if (intent.getAction() != null)
                        CommonUtils.launchNew(JetSettingsListActivity.this,intent);
                    else
		                CommonUtils.launchNext(JetSettingsListActivity.this,intent);
                }else{
                    settingsItemClicked(arg2);
                }
            }
        });
        
        mListView.setOnKeyListener(new OnKeyListener() {
            private int previousPos = 0;
            @Override
            public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
                if(arg1 == KeyEvent.KEYCODE_DPAD_DOWN && arg2.getAction() == KeyEvent.ACTION_DOWN){
                    if(previousPos == (mListView.getAdapter().getCount() - 1)){
                        mListView.smoothScrollToPosition(0);
                        mListView.setSelection(0);
                        previousPos = 0;
                    }else{
                        previousPos = mListView.getSelectedItemPosition() + 1;
                    }
                }else if (arg1 == KeyEvent.KEYCODE_DPAD_UP && arg2.getAction() == KeyEvent.ACTION_DOWN){
                    int currentPos = mListView.getSelectedItemPosition();
                    previousPos = currentPos - 1;
                    if(previousPos < 0) previousPos = 0;
                    if(currentPos == 0){
                        mListView.smoothScrollToPosition(mListView.getAdapter().getCount() - 1);
                        mListView.setSelection(mListView.getAdapter().getCount() - 1);
                        previousPos = mListView.getAdapter().getCount() - 1;
                    }
                }
                return false;
            }
            
        });

        mListView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settingsItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    

    private void resetLastIndex(){
        mPreviousLastPosition = 0;
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
	CommonUtils.launchPrevious(this);
    }

    protected abstract void setupSettingsItems();
    protected abstract void settingsItemClicked(int position);
    protected void settingsItemSelected(int position) {
    };


    protected class SettingMainAdapter extends ArrayAdapter<SettingItem> {

        Context context = null;
        ArrayList<SettingItem> settings = null;

        public SettingMainAdapter(Context context, ArrayList<SettingItem> settings) {
            super(context, R.layout.settings_list_item, settings);
            this.context = context;
            this.settings = settings;
        }

        @Override
        public SettingItem getItem(int position) {
            return settings.get(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.settings_list_item, null);

            ImageView iconIV = (ImageView) convertView.findViewById(R.id.setting_icon);
            TextView textTV = (TextView) convertView.findViewById(R.id.setting_text);
            TextView subTitleTV = (TextView) convertView.findViewById(R.id.sub_title);
            ImageView subIconIV = (ImageView) convertView.findViewById(R.id.sub_icon);

	    SettingItem item = getItem(position);
	    if (item.isCheckBox()) {
		item.subIconId = item.isChecked() ? R.drawable.checkbox_enabled_selectable:
		    R.drawable.checkbox_selectable;
	    }

            if (position == 0) {
                convertView.setPadding(20, 18, 30, 0);
            }

            textTV.setText(settings.get(position).title);

            if (item.iconId != null) {
                iconIV.setVisibility(View.VISIBLE);
                iconIV.setImageResource(settings.get(position).iconId);
            } else {
                iconIV.setVisibility(View.GONE);
            }

            if (item.subIconId != null) {
                subIconIV.setVisibility(View.VISIBLE);
                subIconIV.setImageResource(settings.get(position).subIconId);
            } else {
                subIconIV.setVisibility(View.GONE);
            }

            if (settings.get(position).subTitle != null) {
                subTitleTV.setVisibility(View.VISIBLE);
                subTitleTV.setText(settings.get(position).subTitle);
            } else {
                subTitleTV.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}
