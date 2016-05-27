package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.graphics.drawable.ScaleDrawable;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * This class modifies the original TextView with the property that auto-sizes
 * the text to fit within the space allocated to it by its parent view.
 * 
 * @author jinkim
 * 
 */
public class AutoTextView extends TextView {

	ScaleDrawable left, top, right, bottom;
	boolean mNeedResize = true;

	int textWidthToSet, textHeightToSet;
	float spacingMultiplier = 1;
	float spacingAdditional = 0;
	float mTargetFontSize = 1f;

	/**
     * <b>Constructor</b><br>
     * 
     * @param context
     * @param attrs
     * @param defStyle
     */
	public AutoTextView(Context context) {
		super(context);
	}

	/**
     * <b>Constructor</b><br>
     * 
     * @param context
     * @param attrs
     */
	public AutoTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
     * <b>Constructor</b><br>
     * 
     * @param context
     * @param attrs
     * @param defStyle
     */
	public AutoTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed || mNeedResize) {
			int widthToSet = right - left - getCompoundPaddingRight()
					- getCompoundPaddingLeft();
			int heightToSet = bottom - top - getCompoundPaddingBottom()
					- getCompoundPaddingTop();
			autoFitText(widthToSet, heightToSet);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		if (w != oldW || h != oldH) {
			mNeedResize = true;
		}
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start,
			final int before, final int after) {
		mNeedResize = true;
	}

	@Override
	public void setLineSpacing(float add, float mult) {
		super.setLineSpacing(add, mult);
		spacingMultiplier = mult;
		spacingAdditional = add;
	}

	protected void autoFitText(int width, int height) {
		CharSequence str = getText();
		TextPaint textPaint = getPaint();
		int textHeight = calculateTextHeight(str, textPaint, width,
				mTargetFontSize);
		float oldFontSize = getTextSize();

		while (textHeight < height) {
			if (textHeight < (height / 2)) {
				mTargetFontSize *= 1.5;
			} else {
				mTargetFontSize = Math.max(mTargetFontSize + 2, 1.0f);
			}
			textHeight = calculateTextHeight(str, textPaint, width,
					mTargetFontSize);
		}
		
		setTextSize(mTargetFontSize - 2);
		setLineSpacing(spacingAdditional, spacingMultiplier);

		mNeedResize = false;
		mTargetFontSize = 1;
	}

	private int calculateTextHeight(CharSequence str, TextPaint textPaint,
			int desiredWidth, float textFontSize) {
		TextPaint tempPaint = new TextPaint(textPaint);
		tempPaint.setTextSize(textFontSize);
		StaticLayout sLayout = new StaticLayout(str, tempPaint, desiredWidth,
				Alignment.ALIGN_NORMAL, spacingMultiplier, spacingAdditional,
				true);
		return sLayout.getHeight();
	}
}
