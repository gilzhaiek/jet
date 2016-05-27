package com.reconinstruments.camera.ui;

import android.widget.TextView;
import com.reconinstruments.camera.R;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

/**
 * Created by jinkim on 09/03/15.
 */
public class DeleteDialogFragment extends ReconJetDialogFragment{

    private static final String TAG = DeleteDialogFragment.class.getSimpleName();

    public DeleteDialogFragment(int defaultLayout, String defaultText, int defaultImage, int item) {
        super(defaultLayout, defaultText, defaultImage, item);
    }

    public void setDefaultText(String text){
        mDefaultText = text;
        if(inflatedView != null) {
            TextView textView = ((TextView) inflatedView.findViewById(R.id.text_view));
            textView.setText(text);
            inflatedView.invalidate();
        }
    }
}
