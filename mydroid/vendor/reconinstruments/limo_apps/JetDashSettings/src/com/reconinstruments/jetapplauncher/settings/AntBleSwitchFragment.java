package com.reconinstruments.jetapplauncher.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

/**
 * Created by jinkim on 05/03/15.
 */
public class AntBleSwitchFragment extends CarouselItemFragment {

    private static final String TAG = AntBleSwitchFragment.class.getSimpleName();

    private static final int CHECKMARK_ICON = com.reconinstruments.commonwidgets.R.drawable.checkmark_icon;

    public AntBleSwitchFragment() {

    }

    public AntBleSwitchFragment(int defaultLayout, String defaultText, int item) {
        super(defaultLayout, defaultText, CHECKMARK_ICON, item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(mDefaultLayout, container, false);
        TextView text = (TextView) view.findViewById(R.id.text_view);
        int chosenSetting = (SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) ? 0 : 1;
        text.setText(mDefaultText);
        text.setCompoundDrawablesWithIntrinsicBounds((chosenSetting == mItem) ? mDefaultImage : 0, 0, 0, 0);
        inflatedView = view;
        reAlign(((CarouselItemHostActivity) getActivity()).getPager());
        return inflatedView;
    }

    public void showCheckmark() {
        if (inflatedView != null) {
            TextView itemText = (TextView) inflatedView.findViewById(R.id.text_view);
            itemText.setCompoundDrawablesWithIntrinsicBounds(mDefaultImage, 0, 0, 0);
        }
    }

    public void hideCheckmark() {
        if (inflatedView != null) {
            TextView itemText = (TextView) inflatedView.findViewById(R.id.text_view);
            itemText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }
}
