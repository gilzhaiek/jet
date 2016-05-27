package com.contour.connect;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.contour.utils.SharedPrefHelper;
import com.contour.utils.SysUtils;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class AppInfoFragment extends ListFragment {

    public static final String  TAG           = "AppInfoFragment";
    private static final String ARG_BT_DEVICE = "arg_bt_device";

    BluetoothDevice             mDevice;

    public static AppInfoFragment newInstance(BluetoothDevice device) {
        AppInfoFragment f = new AppInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_BT_DEVICE, device);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if (b != null) {

            mDevice = b.getParcelable(ARG_BT_DEVICE);

        } else {
            this.getFragmentManager().popBackStack();
        }
        
       
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mDevice = savedInstanceState.getParcelable(ARG_BT_DEVICE);
        }
        
        AppInfoListAdapter adapter = new AppInfoListAdapter(getActivity(),mDevice);
        setListAdapter(adapter);

        ListView lv = this.getListView();

        Resources r = getActivity().getResources();

        LinearLayout footerView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.list_footer, null);
        lv.addFooterView(footerView);
        lv.setOverscrollFooter(r.getDrawable(R.drawable.list_footer_bg));
        lv.setCacheColorHint(Color.BLACK);
        lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
        lv.setDivider(r.getDrawable(R.drawable.list_divider));
        lv.setDividerHeight(SysUtils.getDimension(getActivity(), R.dimen.listview_divider_height));

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_BT_DEVICE, mDevice);
    }

    @Override
    public void onStart() {
        super.onStart();
        String title = getResources().getString(R.string.app_info_title);
        TextView tv = (TextView) ((ActionBarActivity) getActivity()).getActionBarHelper().getTitleView();
        tv.setText(title);
    }

    @Override
    public void onResume() {
        super.onResume();
        // getListView().smoothScrollToPosition(getListView().getCheckedItemPosition());
    }

    public static class AppInfoListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private String[]       mLabels;
        private String[]       mData;

        public AppInfoListAdapter(Activity activity, BluetoothDevice device) {
            mInflater = LayoutInflater.from(activity);
            mLabels = activity.getResources().getStringArray(R.array.app_info_items);
            mData = this.getAppInfo(activity, device);
        }

        @Override
        public int getCount() {
            return mLabels.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_text, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.desc = (TextView) convertView.findViewById(R.id.settings_desc);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.text.setText(mLabels[position]);
            holder.desc.setText(mData[position]);
            return convertView;

        }
        
        private String[] getAppInfo(Activity activity, BluetoothDevice device) {
            ArrayList<String> stringList = new ArrayList<String>();
            String appVersionStr = SysUtils.getAppVersion(activity);
            stringList.add(appVersionStr);

//            stringList.add("ScreenSize " + SysUtils.getScreenSizeInches(activity));
//            stringList.add("Screen Density " + SysUtils.getScreenDensityString(activity));
//            stringList.add("Screen Resolution " + SysUtils.getScreenResolutionString(activity));
            if(device != null)
                stringList.add(device.getName());
            else
                stringList.add(activity.getString(R.string.statusmsgunknown));
            stringList.add(SharedPrefHelper.getFwVersionString(activity));
            stringList.add(SharedPrefHelper.getDeviceModelString(activity));
         
            String[] stringArr = new String[stringList.size()];
            stringArr = stringList.toArray(stringArr);
            return stringArr;
        }

        static class ViewHolder {
            TextView text;
            TextView desc;
        }

    }

}
