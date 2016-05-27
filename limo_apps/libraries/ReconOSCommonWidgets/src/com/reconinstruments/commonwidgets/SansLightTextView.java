
package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SansLightTextView extends TextView {

    public SansLightTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTypeface();
    }

    public SansLightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTypeface();
    }

    public SansLightTextView(Context context) {
        super(context);
    }

    private void initTypeface() {
        // Typeface myTypeface =
        // Typeface.createFromAsset(getContext().getAssets(),
        // "fonts/OpenSans-Light.ttf");
        Typeface myTypeface = getFontFromRes(R.raw.opensans_light);
        setTypeface(myTypeface);
    }

    private Typeface getFontFromRes(int resource)
    {
        Typeface tf = null;
        InputStream is = null;
        try {
            is = getResources().openRawResource(resource);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        String outPath = getContext().getCacheDir() + "/tmp" + System.currentTimeMillis() + ".raw";

        try
        {
            byte[] buffer = new byte[is.available()];
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outPath));

            int l = 0;
            while ((l = is.read(buffer)) > 0)
                bos.write(buffer, 0, l);

            bos.close();

            tf = Typeface.createFromFile(outPath);

            // clean up
            new File(outPath).delete();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        return tf;
    }
}
