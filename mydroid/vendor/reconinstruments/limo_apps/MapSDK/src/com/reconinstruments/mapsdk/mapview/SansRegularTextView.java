package com.reconinstruments.mapsdk.mapview;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import com.reconinstruments.mapsdk.R;

import java.io.*;

public class SansRegularTextView extends TextView {

    public SansRegularTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTypeface();
    }

    public SansRegularTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTypeface();
    }

    public SansRegularTextView(Context context) {
        super(context);
    }

    private void initTypeface() {
        Typeface myTypeface = getFontFromRes(R.raw.opensans_regular);
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