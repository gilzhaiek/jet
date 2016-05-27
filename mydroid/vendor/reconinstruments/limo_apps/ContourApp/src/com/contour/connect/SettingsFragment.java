package com.contour.connect;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.contour.api.CameraComms;
import com.contour.api.CameraSettings;
import com.contour.connect.debug.CLog;
import com.contour.utils.SysUtils;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class SettingsFragment extends ListFragment implements OnCheckedChangeListener {

    public interface OnSettingsSelectedListener {
        public void onSettingSelected(CameraSettings settings, int position, long id, String[] settingsItems);
        public void onSendSettingsToCamera(CameraSettings settings);
        public void onApplySettings();
    }
    
    public static final String TAG = "SettingsFragment";
    
    static final String ARG_SWITCH_POS = "switchpos";    
    static final String ARG_SETTINGS = "settingsobj";

    public static final int    MODEL_GPS                       = 0;
    public static final int    MODEL_PLUS                      = 1;
    public static final int    MODEL_PLUS_2                    = 2;

    
    public static final int SETTINGS_ITEM_UNKNOWN            = -1;

    public static final int SETTINGS_ITEM_MODE            = 1;
    public static final int SETTINGS_ITEM_QUALITY         = 2;
    public static final int SETTINGS_ITEM_PHOTO_FREQUENCY = 3;
    public static final int SETTINGS_ITEM_METERING        = 5;
    public static final int SETTINGS_ITEM_WHITE_BALANCE   = 6;
    public static final int SETTINGS_ITEM_ADVANCED        = 7;
    public static final int SETTINGS_ITEM_CAMERA_BEEPS    = 9;
    public static final int SETTINGS_ITEM_MICROPHONE      = 10;
    public static final int SETTINGS_ITEM_GPS             = 12;
    public static final int SETTINGS_ITEM_GPS_UPDATE_RATE = 13;
//    public static final int SETTINGS_ITEM_GPS_ASSIST      = 14;
//    public static final int SETTINGS_ITEM_GLOBAL      = 14;
    public static final int SETTINGS_ITEM_VIDEO_FORMAT      = 14;

    
    public static final int SETTINGS_ITEM_CONTRAST      = 100;
    public static final int SETTINGS_ITEM_EXPOSURE      = 101;
    public static final int SETTINGS_ITEM_SHARPNESS      =102;
    
    public static final int SETTINGS_ITEM_INTERNAL_MIC      = 200;
    public static final int SETTINGS_ITEM_EXTERNAL_MIC      = 201;


    int mSwitchPos;
    
    
    OnSettingsSelectedListener mCallback;

    public static SettingsFragment newInstance(int switchPos, CameraSettings settings) {
        SettingsFragment f = new SettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SWITCH_POS, switchPos);
        args.putParcelable(ARG_SETTINGS, settings);
        f.setArguments(args);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle b = getArguments();
        CameraSettings cameraSettings = null;
        if(b != null) {
            mSwitchPos = b.getInt(ARG_SWITCH_POS);
            cameraSettings = b.getParcelable(ARG_SETTINGS);
        }
        SettingsListAdapter sla = new SettingsListAdapter(getActivity(),this,mSwitchPos,cameraSettings);
        setListAdapter(sla);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Resources r = getActivity().getResources();
        ListView lv = this.getListView();
        LinearLayout footerView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.list_footer, null);
        lv.addFooterView(footerView);   
        lv.setOverscrollFooter(r.getDrawable(R.drawable.list_footer_bg));
        lv.setCacheColorHint(Color.BLACK);
        lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
        lv.setDivider(r.getDrawable(R.drawable.list_divider));
        lv.setDividerHeight(SysUtils.getDimension(getActivity(), R.dimen.listview_divider_height));
        lv.setItemsCanFocus(false);
    }
    
    private void setSwitchPositionTitle() {
        SettingsListAdapter sla = (SettingsListAdapter)getListAdapter();
        String titleText = String.format(getActivity().getString(R.string.switchpositiontitle),mSwitchPos);
        TextView mActionBarTitleView = (TextView)((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
        (mActionBarTitleView).setText(titleText);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSwitchPositionTitle();
        ((MainActivity) getActivity()).setSettingsActionClosed(true);   

        // When in two-pane layout, set the listview to highlight the selected
        // list item
        // (We do this during onStart because at the point the listview is
        // available.)
        // if (getFragmentManager().findFragmentById(R.id.article_fragment) !=
        // null) {
        // getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // }
    }
    

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnSettingsSelectedListener) activity;

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSettingsSelectedListener");
        }
    }

	@Override
	public void onPause() {
		super.onPause();
	}
	
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Notify the parent activity of selected item
        if (v.isEnabled()) {
            mCallback.onSettingSelected(((SettingsListAdapter)getListAdapter()).mCameraSettings, position, id, (String[]) l.getItemAtPosition(position));

            // Set the item as checked to be highlighted when in two-pane layout
            l.setItemChecked(position, true);
            Log.i("DARRELLCONTOUR","V is enabled");
        }else{
        	Log.i("DARRELLCONTOUR","V is NOT enabled");
        }
    }

    public static class SettingsListAdapter extends BaseAdapter {
        public static final String TAG = "SettingsListAdapter";
        private final boolean mDebug;
        private LayoutInflater mInflater;
        private String[] mLabels;
        private TypedArray mLayouts;
        private CameraSettings mCameraSettings; 
        private String[] mVideoRes; 
        private String[] mQuality; 
        private String[] mPhotoFrequency;
        private String[] mMetering;
        private String[] mWhiteBalance;
        private String[] mGpsRate;
        private OnCheckedChangeListener mCheckedChangedListener;
        
        private int mTextDisabledColor; 
        public SettingsListAdapter(Activity activity, OnCheckedChangeListener listener, int switchPos, CameraSettings cameraSettings) {
            mDebug = activity.getResources().getBoolean(R.bool.debug_enabled);
            mInflater = LayoutInflater.from(activity);
            try {
                mCameraSettings = (CameraSettings) cameraSettings.clone();
            } catch (CloneNotSupportedException e) {
                mCameraSettings = cameraSettings;
                e.printStackTrace();
            }
            mCameraSettings.switchPosition = (byte) switchPos;
            mLabels = activity.getResources().getStringArray(R.array.settings_items);
            mLayouts = activity.getResources().obtainTypedArray(R.array.settings_items_layout);
            
            if(cameraSettings.cameraModel == CameraComms.MODEL_PLUS_2) {
                mVideoRes = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.video_res_entries_ntsc : R.array.video_res_entries_pal);
                mPhotoFrequency = activity.getResources().getStringArray(R.array.photo_frequency);
                mGpsRate = activity.getResources().getStringArray(R.array.gps_rate_entries);
            } else if(cameraSettings.cameraModel == CameraComms.MODEL_PLUS) {
                mVideoRes = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.video_res_entries_ntsc_plus : R.array.video_res_entries_pal_plus);
                mPhotoFrequency = activity.getResources().getStringArray(R.array.photo_frequency_plus);
                mGpsRate = activity.getResources().getStringArray(R.array.gps_rate_entries_plus);
            } else {
                mVideoRes = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.video_res_entries_ntsc_plus : R.array.video_res_entries_pal_plus);
                mPhotoFrequency = activity.getResources().getStringArray(R.array.photo_frequency_plus);
                mGpsRate = activity.getResources().getStringArray(R.array.gps_rate_entries_gps);
            }

            mQuality = activity.getResources().getStringArray(R.array.video_quality_entries);
            mMetering = activity.getResources().getStringArray(R.array.metering_entries);
            mWhiteBalance = activity.getResources().getStringArray(R.array.white_balance_entries);
            mCheckedChangedListener = listener;
            
            mTextDisabledColor = activity.getResources().getColor(R.color.text_disabled);
            if(mDebug) CLog.out(TAG, mLabels.length, mLayouts.length());
        }

        /**
         * The number of items in the list is determined by the number of
         * speeches in our array.
         * 
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() { 
            return mLayouts.length();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         * 
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            String[] items = null;
            switch(position) {
            case SETTINGS_ITEM_MODE: 
                items = mVideoRes;
                break;
            case SETTINGS_ITEM_QUALITY:
                items = mQuality;
                break;
            case SETTINGS_ITEM_PHOTO_FREQUENCY:
                items = mPhotoFrequency;
                break;
            case SETTINGS_ITEM_METERING:
                items = mMetering;
                break;
            case SETTINGS_ITEM_WHITE_BALANCE:
                items = mWhiteBalance;
                break;
           case SETTINGS_ITEM_GPS_UPDATE_RATE:
                items = mGpsRate;
                break;      
            }
            return items;
        }

        /**
         * Use the array index as a unique id.
         * 
         * @see android.widget.ListAdapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a view to hold each row.
         * 
         * @see android.widget.ListAdapter#getView(int, android.view.View,
         *      android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid
            // unneccessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is
            // no need
            // to reinflate it. We only inflate a new View when the convertView
            // supplied
            // by ListView is null.
            // if (convertView == null) {
                int layoutId = mLayouts.getResourceId(position, R.layout.list_item_text);
                convertView = mInflater.inflate(layoutId, null);

                // Creates a ViewHolder and store references to the two children
                // views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text);
                switch (position) {
                case SETTINGS_ITEM_MODE:
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setText(mVideoRes[mCameraSettings.getVideoResIndex()]);
                    break;
                case SETTINGS_ITEM_QUALITY:
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setText(mQuality[mCameraSettings.getQuality()]);
                    break;
                case SETTINGS_ITEM_PHOTO_FREQUENCY:
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setText(mPhotoFrequency[mCameraSettings.getPhotoModeIndex()]);
                    if (mCameraSettings.getVideoRes() != 4) {
                        convertView.setEnabled(false);
                        holder.text.setEnabled(false);
                        holder.desc.setEnabled(false);
                    } else {
                        convertView.setEnabled(true);
                        holder.text.setEnabled(true);
                        holder.desc.setEnabled(true);
                    }
                    break;
                case SETTINGS_ITEM_METERING:
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setText(mMetering[mCameraSettings.getMeteringMode()]);
                    break;
                case SETTINGS_ITEM_WHITE_BALANCE:
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setText(mWhiteBalance[mCameraSettings.getWhiteBalance()]);
                    
                    if(mCameraSettings.cameraModel == CameraComms.MODEL_GPS) {
                        convertView.setEnabled(false);
                        holder.text.setEnabled(false);
                        holder.desc.setEnabled(false);
                    }
                    break;
                case SETTINGS_ITEM_CAMERA_BEEPS:
                    holder.checkbox = (CheckBox) convertView.findViewById(R.id.settings_toggle);
                    holder.checkbox.setTag(Integer.valueOf(SETTINGS_ITEM_CAMERA_BEEPS));
                    holder.checkbox.setChecked(mCameraSettings.getBeep());
                    holder.checkbox.setOnCheckedChangeListener(mCheckedChangedListener);
                    break;
                case SETTINGS_ITEM_GPS:
                    holder.checkbox = (CheckBox) convertView.findViewById(R.id.settings_toggle);
                    holder.checkbox.setTag(Integer.valueOf(SETTINGS_ITEM_GPS));
                    holder.checkbox.setChecked(mCameraSettings.getGps());
                    holder.checkbox.setOnCheckedChangeListener(mCheckedChangedListener);
                    break;
                case SETTINGS_ITEM_GPS_UPDATE_RATE:
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setText(mGpsRate[mCameraSettings.getGpsRateIndex()]);
                    break;
                case SETTINGS_ITEM_MICROPHONE:
                case SETTINGS_ITEM_ADVANCED:
                    holder.moreImage = (ImageView) convertView.findViewById(R.id.settings_more);
                    holder.moreImage.setVisibility(View.VISIBLE);
                    holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                    holder.desc.setVisibility(View.GONE);
                    break;
                }
                convertView.setTag(holder);
                
            // } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
            // holder = (ViewHolder) convertView.getTag();
            // }

            // Bind the data efficiently with the holder.
            holder.text.setText(mLabels[position]);


            return convertView;
        }

        static class ViewHolder {
            TextView text;
            TextView desc;
            CheckBox checkbox;
            ImageView moreImage;
        }

    }
    
    public void setSettings(CameraSettings settings) {
        mSwitchPos = settings.switchPosition;
    	SettingsListAdapter sla = (SettingsListAdapter)getListAdapter();
    	sla.mCameraSettings = settings;
    	sla.notifyDataSetChanged();
    	if(this.isResumed())
    	    setSwitchPositionTitle();
    }
    

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int position = (Integer)buttonView.getTag();
        mCallback.onSettingSelected(((SettingsListAdapter)getListAdapter()).mCameraSettings,position,0,null); 
    }
}
