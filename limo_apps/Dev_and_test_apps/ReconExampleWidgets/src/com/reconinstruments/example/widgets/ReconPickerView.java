package com.reconinstruments.example.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ReconPickerView extends FrameLayout {

	enum PickerType {
		TYPE_ARRAY, TYPE_INT
	};

	PickerType mType;
	String[] mContent;
	int mIndex, mCurrentVal, mMaxVal, mMinVal;
	String format = null;
	LinearLayout numberPickerDown, numberPickerUp;
	EditText pickerInput;
	ReconPickerChangeListener mListener;

	/* This constructor is required for inflation. */
	public ReconPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflateView(context);
	}

	/* This constructor takes an array of strings. */
	public ReconPickerView(Context context, String[] content) {
		super(context);
		mType = PickerType.TYPE_ARRAY;
		mContent = content;
		mIndex = 0;
		inflateView(context);
		initButtons();
	}

	/* This constructor takes an initial value. */
	public ReconPickerView(Context context, int num, int min, int max) {
		super(context);
		mType = PickerType.TYPE_INT;
		mCurrentVal = num;
		mMaxVal = max;
		mMinVal = min;
		inflateView(context);
		initButtons();
	}
	
	public ReconPickerView(Context context, int num, int min, int max, String format) {
		super(context);
		mType = PickerType.TYPE_INT;
		mCurrentVal = num;
		mMaxVal = max;
		mMinVal = min;
		this.format = format;
		inflateView(context);
		initButtons();
	}

	private void initButtons() {

		numberPickerDown = (LinearLayout) this
				.findViewById(com.reconinstruments.example.widgets.R.id.numberPickerDown);
		numberPickerUp = (LinearLayout) this
				.findViewById(com.reconinstruments.example.widgets.R.id.numberPickerUp);
		pickerInput = (EditText) this
				.findViewById(com.reconinstruments.example.widgets.R.id.pickerInput);
		
		/* Set Typeface */
		TextView downIcon = ((TextView) this.findViewById(R.id.downIconText));
		TextView upIcon = ((TextView) this.findViewById(R.id.upIconText));
		Typeface tf = Util.getMenuFont(this.getContext());
		pickerInput.setTypeface(tf);
		downIcon.setTypeface(tf);
		upIcon.setTypeface(tf);

		switch (mType) {
		case TYPE_ARRAY:
			pickerInput.setText(mContent[mIndex]);
			numberPickerUp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (++mIndex > mContent.length - 1)
						mIndex = 0;
					pickerInput.setText(mContent[mIndex]);
					if (mListener != null) {
						mListener.onChange(ReconPickerView.this,
								mContent[mIndex], true);
					}
				}
			});
			numberPickerDown.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (--mIndex < 0)
						mIndex = mContent.length - 1;
					pickerInput.setText(mContent[mIndex]);
					if (mListener != null) {
						mListener.onChange(ReconPickerView.this,
								mContent[mIndex], false);
					}
				}
			});
			return;
		case TYPE_INT:
			
			String valString = "" + mCurrentVal;
			if(format != null)
				valString = String.format(format, mCurrentVal);
			
			pickerInput.setText(valString);
			
			numberPickerUp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (++mCurrentVal > mMaxVal)
						mCurrentVal = mMinVal;

					String valString = "" + mCurrentVal;
					if(format != null) 
						valString = String.format(format, mCurrentVal);
					
					pickerInput.setText(valString);
					
					if (mListener != null) {
						mListener.onChange(ReconPickerView.this, valString, true);
					}
				}
			});
			
			numberPickerDown.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (--mCurrentVal < mMinVal)
						mCurrentVal = mMaxVal;
					
					String valString = "" + mCurrentVal;
					if(format != null)
						valString = String.format(format, mCurrentVal);
					
					pickerInput.setText(valString);
					
					if (mListener != null) {
						mListener.onChange(ReconPickerView.this, valString, false);
					}
				}
			});
			return;
		}

	}

	private void inflateView(Context context) {
		LayoutInflater factory = LayoutInflater.from(context);
		View pickerView = factory.inflate(
				com.reconinstruments.example.widgets.R.layout.picker_view, null);
		this.addView(pickerView);
	}

	public boolean setContent(String[] content) {
		if (content != null && content.length > 0) {
			mContent = content;
			mIndex = 0;
			mType = PickerType.TYPE_ARRAY;
			initButtons();
			return true;
		} else {
			return false;
		}
	}

	public void setContent(int val, int min, int max) {
		mType = PickerType.TYPE_INT;
		mCurrentVal = val;
		mMinVal = min;
		mMaxVal = max;
		initButtons();
	}
	
	public void setContent(int val, int min, int max, String format) {
		this.format = format;
		setContent(val, min, max);
	}

	public String getSelectedValue() {
		switch (mType) {
		case TYPE_INT:
			return "" + mCurrentVal;
		case TYPE_ARRAY:
			return mContent[mIndex];
		default:
			return "";
		}
	}

	public void setSelectedIndex(int index) {
		if (mType == PickerType.TYPE_ARRAY && index > 0
				&& index < mContent.length) {
			mIndex = index;
			EditText pickerInput = (EditText) this
					.findViewById(com.reconinstruments.example.widgets.R.id.pickerInput);
			pickerInput.setText(mContent[index]);
		}
	}

	public int getSelectedIndex() {
		if (mType == PickerType.TYPE_ARRAY) {
			return mIndex;
		} else {
			return -1;
		}
	}

	public void setOnChangeListener(ReconPickerChangeListener listener) {
		mListener = listener;
	}
}
