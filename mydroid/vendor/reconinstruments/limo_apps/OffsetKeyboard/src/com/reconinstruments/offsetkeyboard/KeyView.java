package com.reconinstruments.offsetkeyboard;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class KeyView extends RelativeLayout {

	TextView textView;
	ImageView imageView;
	
	public static final int GREY = 0xFF999999;
	public static final int WHITE = Color.WHITE;
	
	public static final int LEFT_ARROW = 0;
	public static final int RIGHT_ARROW = 1;
	public static final int DELETE = 2;
	public static final int ENTER = (int) '\n';
	public static final int LOWER_CASE = 4;
	public static final int UPPER_CASE = 5;
	public static final int NUMBER = 6;
	public static final int SPACE = (int) ' ';

	public KeyView(Context context) {
		super(context);
		init();
	}

	public KeyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		this.setMinimumHeight(33);
		this.setMinimumWidth(33);
		
		textView = new TextView(getContext());
		imageView = new ImageView(getContext());

		this.addView(textView);
		this.addView(imageView);

		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
		textView.setLayoutParams(layoutParams);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
		textView.setTextColor(GREY);
		
		layoutParams = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
		imageView.setLayoutParams(layoutParams);

		textView.setTypeface(FontSingleton.getInstance(getContext())
				.getTypeface());
	}

	public void setText(String text) {
		textView.setText(text);
		textView.setVisibility(View.VISIBLE);
		imageView.setVisibility(View.GONE);
	}

	public void setTextColor(int color) {
		textView.setTextColor(color);
	}

	public void setDrawableResource(int res) {
		imageView.setImageResource(res);
		textView.setVisibility(View.GONE);
		imageView.setVisibility(View.VISIBLE);
	}

	public static int drawableLookup(int symbol, boolean white) {
		if(white) {
			switch(symbol) {
			case LEFT_ARROW:
				return R.drawable.left;
			case RIGHT_ARROW:
				return R.drawable.right;
			case DELETE:
				return R.drawable.delete;
			case ENTER:
				return R.drawable.enter;
			case LOWER_CASE:
				return R.drawable.lower_case;
			case UPPER_CASE:
				return R.drawable.upper_case;
			case NUMBER:
				return R.drawable.number;
			case SPACE:
				return R.drawable.space;
			}
		} else {
			switch(symbol) {
			case LEFT_ARROW:
				return R.drawable.left_g;
			case RIGHT_ARROW:
				return R.drawable.right_g;
			case DELETE:
				return R.drawable.delete_g;
			case ENTER:
				return R.drawable.enter_g;
			case LOWER_CASE:
				return R.drawable.lower_case_g;
			case UPPER_CASE:
				return R.drawable.upper_case_g;
			case NUMBER:
				return R.drawable.number_g;
			case SPACE:
				return R.drawable.space;
			}
		}
		
		return -1;
	}
}
