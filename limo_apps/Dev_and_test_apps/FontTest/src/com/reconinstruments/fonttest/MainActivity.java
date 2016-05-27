package com.reconinstruments.fonttest;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;
import java.util.ArrayList;
import android.graphics.Color;
import android.widget.CheckBox;
import android.graphics.Paint;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.util.Log;

public class MainActivity extends Activity implements OnCheckedChangeListener {
    private ArrayList<TextView> mTextViews =
        new ArrayList<TextView>();
    private ArrayList<TextView> mTextViews2 =
        new ArrayList<TextView>();

    private float mSize = 42;
    private CheckBox mAntialiasBox;
    private CheckBox mDitherBox;
    private CheckBox mSubpixelBox;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	TextView light1    = (TextView) findViewById(R.id.light1);
	TextView reg1    = (TextView) findViewById(R.id.reg1);
	TextView semi1    = (TextView) findViewById(R.id.semi1);
	TextView bold1    = (TextView) findViewById(R.id.bold1);
	TextView light2    = (TextView) findViewById(R.id.light2);
	TextView reg2    = (TextView) findViewById(R.id.reg2);
	TextView semi2    = (TextView) findViewById(R.id.semi2);
	TextView bold2    = (TextView) findViewById(R.id.bold2);

	mTextViews.add(light1);
	mTextViews.add(reg1);
	mTextViews.add(semi1);
	mTextViews.add(bold1);

	mTextViews2.add(light2);
	mTextViews2.add(reg2);
	mTextViews2.add(semi2);
	mTextViews2.add(bold2);

	Typeface open_sans_light =
	    Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Light.ttf");
	Typeface open_sans_regular =
	    Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");
	Typeface open_sans_semi_bold =
	    Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Semibold.ttf");
	Typeface open_sans_bold =
	    Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");

	light1.setTypeface(open_sans_light);
	reg1.setTypeface(open_sans_regular);
	semi1.setTypeface(open_sans_semi_bold);
	bold1.setTypeface(open_sans_bold);
	light2.setTypeface(open_sans_light);
	reg2.setTypeface(open_sans_regular);
	semi2.setTypeface(open_sans_semi_bold);
	bold2.setTypeface(open_sans_bold);


	mAntialiasBox = (CheckBox) findViewById(R.id.antialiasbox);
	mDitherBox = (CheckBox) findViewById(R.id.ditherbox);
	mSubpixelBox = (CheckBox) findViewById(R.id.subpixelbox);
	mAntialiasBox.setOnCheckedChangeListener(this);
	mDitherBox.setOnCheckedChangeListener(this);
	mSubpixelBox.setOnCheckedChangeListener(this);

	updateSizes(mSize);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
	    mSize -= 1;
	    if (mSize < 0) mSize = 0;
	    updateSizes(mSize);
	    return true;
	}
	else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
	    mSize += 1;
	    updateSizes(mSize);
	    return true;
	} else {
	    return super.onKeyDown(keyCode,event);
	}
    }
    private void update() {
	updateSizes(mSize);
	
    }
    private void updateSizes(float size) {
	for (TextView tv : mTextViews) {
	    String tvText = tv.getText().toString();
	    tv.setText(tvText.replaceAll("[0-9 ]","")+" "+(int)size);
	    tv.setTextSize(size);
	    tv.setTextColor(Color.WHITE);
	    Log.v("FontTest","start--------");
	    setFlag(tv,Paint.ANTI_ALIAS_FLAG,mAntialiasBox.isChecked());
	    setFlag(tv,Paint.DITHER_FLAG,mDitherBox.isChecked());
	    setFlag(tv,Paint.SUBPIXEL_TEXT_FLAG,mSubpixelBox.isChecked());
	}
	for (TextView tv : mTextViews2) {
	    String tvText = tv.getText().toString();
	    tv.setText(tvText.replaceAll("[0-9 ]","")+" "+(int)size);
	    tv.setTextSize(size);
	    tv.setTextColor(Color.WHITE);
	    Log.v("FontTest","start--------");
	}


    }

    private void setFlag (TextView tv, int theFlag, boolean on) {
	int pf = tv.getPaintFlags();
	if (on) pf = pf | theFlag;
	else pf = pf & ~theFlag;
	tv.setPaintFlags(pf);
	Log.v("FontTest","pf is "+pf);
    }
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	update();
    }

}
