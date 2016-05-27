package com.reconinstruments.example.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

public class TabPage extends FrameLayout implements IRemoteControlEventHandler
{
	protected final static int TAB_PADDING = 2;

	protected Drawable mIconNormal = null;
	protected Drawable mIconSelected = null;
	protected Drawable mIconFocused = null;
	public StateListDrawable mDefaultIconSelector = null;
	public StateListDrawable mFocusedIconSelector = null;
	public StateListDrawable mSelectedIconSelector = null;
	public String mTabText = null;

	protected TabView mHostView = null;

	public TabPage(Context context, Drawable iconRegular, Drawable iconSelected, Drawable iconFocused, TabView hostView)
	{
		super(context);
		mIconNormal = iconRegular;
		mIconSelected = iconSelected;
		mIconFocused = iconFocused;
		mHostView = hostView;

		StateListDrawable states = new StateListDrawable();
		states.addState(new int[] { android.R.attr.state_selected }, mIconFocused);
		states.addState(new int[] {}, mIconNormal);
		mDefaultIconSelector = states;

		states = new StateListDrawable();
		states.addState(new int[] { android.R.attr.state_selected }, mIconFocused);
		states.addState(new int[] {}, mIconFocused);
		mFocusedIconSelector = states;

		states = new StateListDrawable();
		states.addState(new int[] { android.R.attr.state_selected }, mIconFocused);
		states.addState(new int[] {}, mIconSelected);
		mSelectedIconSelector = states;

		this.setPadding(TAB_PADDING, TAB_PADDING, TAB_PADDING, TAB_PADDING);
	}

	public TabPage(Context context, String tabTxt, TabView hostView)
	{
		super(context);
		mHostView = hostView;
		mTabText = tabTxt;
		this.setPadding(TAB_PADDING, TAB_PADDING, TAB_PADDING, TAB_PADDING);
	}

	public Drawable getIconNormal()
	{
		return mIconNormal;
	}

	public Drawable getIconSelected()
	{
		return mIconSelected;
	}

	public Drawable getIconFocused()
	{
		return mIconFocused;
	}

	// set the focus to this tab page
	public void setFocus()
	{
		this.requestFocus();
	}

	@Override
	public boolean onDownArrowDown(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDownArrowUp(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUpArrowDown(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUpArrowUp(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onLeftArrowDown(View srcView)
	{
		if (mHostView != null)
		{
			mHostView.focusTabBar();
		}

		return true;
	}

	@Override
	public boolean onLeftArrowUp(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onRightArrowDown(View srcView)
	{

		return false;
	}

	@Override
	public boolean onRightArrowUp(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onSelectDown(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onSelectUp(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onBackDown(View srcView)
	{
		Log.d("TabPage", "onBackDown()");
		// call the TabVew onBackDown
		if (mHostView != null)
		{
			return mHostView.onBackDown(srcView);
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean onBackUp(View srcView)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Handle the key-down event from the remote control
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return onDownArrowDown(this);

			case KeyEvent.KEYCODE_DPAD_UP:
				return onUpArrowDown(this);

			case KeyEvent.KEYCODE_DPAD_LEFT:
				return onLeftArrowDown(this);

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return onRightArrowDown(this);

			case KeyEvent.KEYCODE_DPAD_CENTER:
				return onSelectDown(this);

			case KeyEvent.KEYCODE_BACK:
				return onBackDown(this);

				// all the other buttons, just ignore it
			default:
				return false;
		}
	}

	/**
	 * Handle the key-up event from the remote control
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return onDownArrowUp(this);

			case KeyEvent.KEYCODE_DPAD_UP:
				return onUpArrowUp(this);

			case KeyEvent.KEYCODE_DPAD_LEFT:
				return onLeftArrowUp(this);

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return onRightArrowUp(this);

			case KeyEvent.KEYCODE_DPAD_CENTER:
				return onSelectUp(this);

			case KeyEvent.KEYCODE_BACK:
				return onBackUp(this);

				// all the other buttons, just ignore it
			default:
				return false;
		}
	}

}