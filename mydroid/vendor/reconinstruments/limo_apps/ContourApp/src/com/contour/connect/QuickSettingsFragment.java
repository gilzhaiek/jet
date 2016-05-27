package com.contour.connect;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils.StringSplitter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.contour.api.CameraComms;
import com.contour.api.CameraSettings;
import com.contour.connect.AppInfoFragment.AppInfoListAdapter.ViewHolder;
import com.contour.connect.debug.CLog;
import com.contour.utils.SharedPrefHelper;
import com.contour.utils.SysUtils;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class QuickSettingsFragment extends ListFragment {

    public interface OnQuickSettingsSelectedListener {
        public void onQuickSettingSelected(int position, long id, String[] settingsItems);
    }

    public static final String      TAG                             = "QuickSettingsFragment";

    public static final int         SETTINGS_ITEM_SWITCH_ONE        = 1;
    public static final int         SETTINGS_ITEM_SWITCH_TWO        = 2;
    public static final int         SETTINGS_ITEM_VIDEO_FORMAT      = 4;
    public static final int         SETTINGS_ITEM_RESET_TO_DEFAULTS = 5;
    public static final int         SETTINGS_ITEM_ABOUT             = 6;
    public static final int         SETTINGS_ITEM_STATUS            = 7;

    static final String             ARG_SWITCH_POS                  = "switchpos";
    static final String             ARG_SETTINGS                    = "settingsobj";
    int                             mSwitchPos;
    private boolean                 mDebug                          = true;
    OnQuickSettingsSelectedListener mCallback;

    public static QuickSettingsFragment newInstance(int switchPos, CameraSettings settings, boolean debug) {
        QuickSettingsFragment f = new QuickSettingsFragment();
        f.mDebug = debug;
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
        if (b != null) {
            mSwitchPos = b.getInt(ARG_SWITCH_POS);
            cameraSettings = b.getParcelable(ARG_SETTINGS);
        }
        mDebug = getActivity().getResources().getBoolean(R.bool.debug_enabled);
        QuickSettingsListAdapter sla = new QuickSettingsListAdapter(getActivity(),mSwitchPos, cameraSettings);
        sla.mDebug = mDebug;
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
    }

    private void setSwitchPositionTitle() {
        String titleText = String.format(getActivity().getString(R.string.camerasettingstitle),SharedPrefHelper.getDeviceModelString(getActivity()));
        TextView mActionBarTitleView = (TextView) ((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
        (mActionBarTitleView).setText(titleText);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSwitchPositionTitle();
        ((MainActivity) getActivity()).setSettingsActionClosed(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnQuickSettingsSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSettingsSelectedListener");
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
            mCallback.onQuickSettingSelected(position, id, (String[]) l.getItemAtPosition(position));

            // Set the item as checked to be highlighted when in two-pane layout
            l.setItemChecked(position, true);
        }
    }

    public static class QuickSettingsListAdapter extends BaseAdapter {
        public static final String TAG               = "QuickSettingsListAdapter";

        private LayoutInflater     mInflater;
        private String[]           mLabels;
        private TypedArray         mLayouts;
        private CameraSettings     mCameraSettings;
        private String[]           mVideoFormats;
        private String             mVideoResSwitchOne;
        private String             mVideoResSwitchTwo;
        private String             mOnStr;
        private String             mOffStr;
        private boolean            mGpsOnOne;
        private boolean            mGpsOnTwo;
        private int                mCurrentSwitchPos = 0;
        boolean                    mDebug            = false;

        public QuickSettingsListAdapter(Activity activity, int currentSwitchPos,
                CameraSettings cameraSettings) {
            mInflater = LayoutInflater.from(activity);
            mLabels = activity.getResources().getStringArray(R.array.quick_settings_items);
            mLayouts = activity.getResources().obtainTypedArray(R.array.quick_settings_items_layout);
            mVideoFormats = activity.getResources().getStringArray(R.array.video_format_entries);
            mOnStr = activity.getResources().getString(R.string.on);
            mOffStr = activity.getResources().getString(R.string.off);

            loadSettings(activity, currentSwitchPos, cameraSettings);

            if (mDebug)
                CLog.out(TAG, mLabels.length, mLayouts.length());
        }

        public void loadSettings(Activity activity, int switchPos, CameraSettings cameraSettings) {
            mCurrentSwitchPos = switchPos;

            try {
                mCameraSettings = (CameraSettings) cameraSettings.clone();
            } catch (CloneNotSupportedException e) {
                mCameraSettings = cameraSettings;
                e.printStackTrace();
            }
            mCameraSettings.switchPosition = 1;
            if (mCameraSettings.cameraModel == CameraComms.MODEL_PLUS_2) {
                String[] allVideoResSwitchOne = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.short_video_res_entries_ntsc : R.array.short_video_res_entries_pal);
                mVideoResSwitchOne = allVideoResSwitchOne[mCameraSettings.getVideoResIndex()];
            } else if (mCameraSettings.cameraModel == CameraComms.MODEL_PLUS) {
                String[] allVideoResSwitchOne = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.short_video_res_entries_ntsc_plus : R.array.short_video_res_entries_pal_plus);
                mVideoResSwitchOne = allVideoResSwitchOne[mCameraSettings.getVideoResIndex()];
            } else {
                String[] allVideoResSwitchOne = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.short_video_res_entries_ntsc_plus : R.array.short_video_res_entries_pal_plus);
                mVideoResSwitchOne = allVideoResSwitchOne[mCameraSettings.getVideoResIndex()];
            }
            mGpsOnOne = mCameraSettings.getGps();

            mCameraSettings.switchPosition = 2;
            if (mCameraSettings.cameraModel == CameraComms.MODEL_PLUS_2) {
                String[] allVideoResSwitchOne = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.short_video_res_entries_ntsc : R.array.short_video_res_entries_pal);
                mVideoResSwitchTwo = allVideoResSwitchOne[mCameraSettings.getVideoResIndex()];
            } else if (mCameraSettings.cameraModel == CameraComms.MODEL_PLUS) {
                String[] allVideoResSwitchOne = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.short_video_res_entries_ntsc_plus : R.array.short_video_res_entries_pal_plus);
                mVideoResSwitchTwo = allVideoResSwitchOne[mCameraSettings.getVideoResIndex()];
            } else {
                String[] allVideoResSwitchOne = activity.getResources().getStringArray(mCameraSettings.getVideoFormat() == 1 ? R.array.short_video_res_entries_ntsc_plus : R.array.short_video_res_entries_pal_plus);
                mVideoResSwitchTwo = allVideoResSwitchOne[mCameraSettings.getVideoResIndex()];
            }
            mGpsOnTwo = mCameraSettings.getGps();
        }

        /**
         * The number of items in the list is determined by the number of
         * speeches in our array.
         * 
         * @see android.widget.ListAdapter#getCount()
         */
        public int getCount() {
            return 8;
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
            if (position == SETTINGS_ITEM_VIDEO_FORMAT)
                return mVideoFormats;
            return null;
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
            ViewHolder holder;

//            if (convertView == null) {
//                int layoutId = mLayouts.getResourceId(position, R.layout.list_item_text);
                convertView = mInflater.inflate(R.layout.list_item_text, null);

                // Creates a ViewHolder and store references to the two children
                // views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.headertext = (TextView) convertView.findViewById(R.id.header_text);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                holder.moreImage = (ImageView) convertView.findViewById(R.id.settings_more);
                holder.checkImage = (ImageView) convertView.findViewById(R.id.list_item_check);
                convertView.setTag(holder);
//            } else {
//                holder = (ViewHolder) convertView.getTag();
//            }

            switch (position) {
            case SETTINGS_ITEM_SWITCH_ONE:
                holder.text.setText(String.format(mLabels[position],1));
                holder.moreImage.setVisibility(View.VISIBLE);
                
                if(mCameraSettings.getVideoRes(1) == CameraSettings.VideoRes.CTRVideoResContinuousPhoto)
                    holder.desc.setText(String.format(mVideoResSwitchOne, (mGpsOnOne ? mOnStr : mOffStr),mCameraSettings.getPhotoMode(1)));
                else
                    holder.desc.setText(String.format(mVideoResSwitchOne, (mGpsOnOne ? mOnStr : mOffStr)));

                if (mCurrentSwitchPos == 1)
                    holder.checkImage.setVisibility(View.VISIBLE);
                else
                    holder.checkImage.setVisibility(View.INVISIBLE);

                break;
            case SETTINGS_ITEM_SWITCH_TWO:
                holder.text.setText(String.format(mLabels[position],2));
                holder.moreImage.setVisibility(View.VISIBLE);
                
                if(mCameraSettings.getVideoRes(2) == CameraSettings.VideoRes.CTRVideoResContinuousPhoto)
                    holder.desc.setText(String.format(mVideoResSwitchTwo, (mGpsOnTwo ? mOnStr : mOffStr),mCameraSettings.getPhotoMode(2)));
                else
                    holder.desc.setText(String.format(mVideoResSwitchTwo, (mGpsOnTwo ? mOnStr : mOffStr)));
                
                if (mCurrentSwitchPos == 2)
                    holder.checkImage.setVisibility(View.VISIBLE);
                else
                    holder.checkImage.setVisibility(View.INVISIBLE);
                break;
            case SETTINGS_ITEM_VIDEO_FORMAT:
                holder.text.setText(mLabels[position]);
                holder.moreImage.setVisibility(View.VISIBLE);
                holder.checkImage.setVisibility(View.GONE);
                holder.desc.setText(mVideoFormats[mCameraSettings.getVideoFormat()]);
                break;
            case SETTINGS_ITEM_ABOUT:
            case SETTINGS_ITEM_STATUS:
            case SETTINGS_ITEM_RESET_TO_DEFAULTS:
                holder.text.setText(mLabels[position]);
                holder.checkImage.setVisibility(View.GONE);
                holder.moreImage.setVisibility(View.GONE);
                holder.desc.setVisibility(View.GONE);
                break;
                default:
                    holder.headertext.setVisibility(View.VISIBLE);
                    holder.headertext.setText(mLabels[position]);
                    holder.text.setVisibility(View.GONE);
                    holder.checkImage.setVisibility(View.GONE);
                    holder.moreImage.setVisibility(View.GONE);
                    holder.desc.setVisibility(View.GONE);
                    
            }
            
            return convertView;
        }

        static class ViewHolder {
            TextView  text;
            TextView  headertext;
            TextView  desc;
            ImageView moreImage;
            ImageView checkImage;
        }

    }

    public void setSettings(CameraSettings settings, int currentSwitchPos) {
        QuickSettingsListAdapter sla = (QuickSettingsListAdapter) getListAdapter();
        if (mDebug)
            CLog.out(TAG, "setSettings", sla != null, settings.switchPosition, settings.getVideoRes());
        if (sla != null) {
            sla.loadSettings(getActivity(), currentSwitchPos, settings);
            sla.notifyDataSetChanged();
        }
    }
}
