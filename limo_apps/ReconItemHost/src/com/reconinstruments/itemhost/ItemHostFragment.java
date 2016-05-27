
package com.reconinstruments.itemhost;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.commonwidgets.BreadcrumbView;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.R;

/**
 * <code>ItemHostFragment</code> is designed to represent the remote view item
 * layout.
 */
public class ItemHostFragment extends CarouselItemFragment {

    public ItemHostFragment() {
	// Android requires it
    }
    public ItemHostFragment(int defaultLayout, String defaultText, int defaultImage,
            int item) {
        super(defaultLayout, defaultText, defaultImage, item);
    }

    // replace the original view as remote view.
    public void setupRemoteView(Context context, RemoteViews views, Intent upLauncher,
            Intent downLauncher) {
        mUpLauncher = upLauncher;
        mDownLauncher = downLauncher;
        ViewGroup vg = (ViewGroup) getView().findViewById(R.id.remote_view);
        inflatedView = views.apply(context, vg);
        if (vg.getChildCount() > 0) {
            vg.removeAllViews();
        }
        reAlign(((CarouselItemHostActivity) getActivity()).getPager());
        vg.addView(inflatedView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        TextView textView = (TextView) v.findViewById(R.id.text_view);
        textView.setText(mDefaultText);
        ImageView imageView = (ImageView) v.findViewById(R.id.image_view);
        imageView.setImageResource(mDefaultImage);
        return v;
    }
    

}
