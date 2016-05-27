package com.contour.connect;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.contour.utils.SysUtils;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class SettingsItemSelectionFragment extends ListFragment {

    public interface OnSettingsItemSelectedListener {
        public void onSettingsItemSelected(int switchPos, int categoryPos, int itemPosition);
    }
    
    public static final String TAG = "SettingsItemSelectionFragment";
    private static final String ARG_DATA = "arg_data";
    private static final String ARG_TITLE_POS = "arg_title_pos";  
    private static final String ARG_ITEM_POS = "arg_item_pos";
    private static final String ARG_SWITCH_POS = "arg_switch_pos";


    OnSettingsItemSelectedListener mCallback;
    
    String[] mData;
    int mTitleCategoryPosition;
    int mSelectedItemPosition;
    int mSwitchPos;
    public static SettingsItemSelectionFragment newInstance(int switchPos, int titleCategoryPos, int itemPosition, String[] listData) {
    	
        SettingsItemSelectionFragment f = new SettingsItemSelectionFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_DATA, listData);
        args.putInt(ARG_TITLE_POS, titleCategoryPos);
        args.putInt(ARG_ITEM_POS, itemPosition);
        args.putInt(ARG_SWITCH_POS, switchPos);

        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if(b != null) {
            mTitleCategoryPosition = b.getInt(ARG_TITLE_POS);
            mSelectedItemPosition = b.getInt(ARG_ITEM_POS);
            String[] data = b.getStringArray(ARG_DATA);
            mSwitchPos = b.getInt(ARG_SWITCH_POS);
            mData = data;
//            SettingsItemListAdapter sla = new SettingsItemListAdapter(getActivity(),data);
//            setListAdapter(sla);
            
        } else {
            this.getFragmentManager().popBackStack();
        }
    }
    

    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_text,
                R.id.text, mData));
        
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mSwitchPos = savedInstanceState.getInt(ARG_SWITCH_POS,1);
            mSelectedItemPosition = savedInstanceState.getInt(ARG_ITEM_POS, 0);
            mTitleCategoryPosition = savedInstanceState.getInt(ARG_TITLE_POS, 0);
            mData = savedInstanceState.getStringArray(ARG_DATA);
        }
        
        ListView lv = this.getListView();

        Resources r = getActivity().getResources();

        LinearLayout footerView = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.list_footer, null);
        lv.addFooterView(footerView);
        lv.setOverscrollFooter(r.getDrawable(R.drawable.list_footer_bg));
        lv.setCacheColorHint(Color.BLACK);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setFocusableInTouchMode(true);
        lv.setItemChecked(mSelectedItemPosition, true);
        lv.setDivider(r.getDrawable(R.drawable.list_divider));
        lv.setDividerHeight(SysUtils.getDimension(getActivity(), R.dimen.listview_divider_height));
        lv.setItemsCanFocus(false);
        
        //this is required so the focus is default to the selected item in listview
        lv.requestFocus();
        lv.setSelection(mSelectedItemPosition);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(ARG_DATA, mData);
        outState.putInt(ARG_ITEM_POS, mSelectedItemPosition);
        outState.putInt(ARG_TITLE_POS, mTitleCategoryPosition);
        outState.putInt(ARG_SWITCH_POS, mSwitchPos);

    }

    @Override
    public void onStart() {
        super.onStart();
        String title = getResources().getStringArray(R.array.settings_items)[mTitleCategoryPosition];
        TextView tv = (TextView)((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
        tv.setText(title);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getListView().smoothScrollToPosition(getListView().getCheckedItemPosition());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnSettingsItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSettingsItemSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mCallback.onSettingsItemSelected(mSwitchPos, mTitleCategoryPosition,position);
        l.setItemChecked(position, true);
        Log.i("DARRELLCONTOUR","onLISTITEMCLICK!");
    }
    
//    public void setSettings(CameraSettings settings) {
//        mSwitchPos = settings.switchPosition;
//        SettingsListAdapter sla = (SettingsListAdapter)getListAdapter();
//        sla.mCameraSettings = settings;
//        sla.notifyDataSetChanged();
//        setSwitchPositionTitle();
//    }
}
