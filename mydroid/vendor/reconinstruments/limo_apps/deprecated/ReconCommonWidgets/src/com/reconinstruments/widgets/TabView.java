package com.reconinstruments.widgets;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class TabView extends FrameLayout implements IRemoteControlEventHandler {
	// tab bar stuff
	protected TabListView mTabList = null; // left tab bar
	protected TabIconAdapter mMenuItems = null;
	protected int mSelectedTabIdx = -1;
	protected ColorStateList tabTextSelected, tabTextFocused;
	
	// tab pages stuff
	protected FrameLayout mTabPageContainer = null;
	protected ArrayList<TabPage> mPages = null;

	// typeface
	protected Typeface reconFont;
	
	public TabView(Context context) {
		super(context, null);
		initView(context);
		reconFont = Typeface.createFromAsset(context.getResources().getAssets(), "fonts/Eurostib_1.TTF");
	}

	public TabView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
		reconFont = Typeface.createFromAsset(context.getResources().getAssets(), "fonts/Eurostib_1.TTF");
	}

	protected void initView(Context context) {
		// inflate a view from predefined xml file
		LayoutInflater factory = LayoutInflater.from(context);
		View navView = factory.inflate(R.layout.tab_view, null);
		this.addView(navView);

		try {
			tabTextSelected = ColorStateList.createFromXml(
					this.getResources(), getResources().getXml(R.drawable.tab_text_selected_selector));
			tabTextFocused = ColorStateList.createFromXml(
					this.getResources(), getResources().getXml(R.drawable.tab_text_focused_selector));
		} catch (NotFoundException e) {
			Log.e("TabView", e.toString());
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			Log.e("TabView", e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("TabView", e.toString());
			e.printStackTrace();
		}
		
		mTabList = (TabListView) navView.findViewById(R.id.tab_bar);
		mTabPageContainer = (FrameLayout) navView.findViewById(R.id.tab_page_container);

		mTabList.setOnItemSelectedListener(mTabSelectedListener);
		mTabList.setOnKeyListener(mTabBarKeyListener);

		mTabList.setDivider(null);
		mTabList.setDividerHeight(0);
	}

	public void focusTabBar() {
		mTabList.requestFocus();
	}

	public void setTabPages(ArrayList<TabPage> pages) {
		mMenuItems = new TabIconAdapter(this.getContext(),
				R.layout.tab_icon_view, R.id.tab_item_text, (ArrayList) pages);
		mTabList.setAdapter(mMenuItems);
		mPages = pages;
	}

	/*
	 * private class for defining an ArrayAdapter that has it own view of list
	 * item
	 */
	protected class TabIconAdapter extends ArrayAdapter<Object> {

		public TabIconAdapter(Context context, int resource,
				int textViewResourceId, ArrayList<Object> tabPages) {
			super(context, resource, textViewResourceId, tabPages);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				// create a new view from the poiCategoryitem_layout
				LayoutInflater inflater = (LayoutInflater) this.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.tab_icon_view, null);

			}

			if (mPages.get(position).mDefaultIconSelector != null) {
				ImageView icon = (ImageView) v.findViewById(R.id.tab_item_icon);

				if (mTabList.hasFocus()) {

					if (position == mSelectedTabIdx) {
						icon.setImageDrawable(mPages.get(position).mFocusedIconSelector);
					} else {
						icon.setImageDrawable(mPages.get(position).mDefaultIconSelector);
					}

				} else {
					if (position == mSelectedTabIdx) {
						icon.setImageDrawable(mPages.get(position).mSelectedIconSelector);
					} else {
						icon.setImageDrawable(mPages.get(position).mDefaultIconSelector);
					}
				}

			} else {
				TextView txt = (TextView) v.findViewById(R.id.tab_item_text);
				txt.setText(mPages.get(position).mTabText);
				txt.setTypeface(reconFont);
				
				if(mTabList.hasFocus()) {
					txt.setTextColor(tabTextFocused);
				} else {
					if(position == mSelectedTabIdx) {
						txt.setTextColor(tabTextSelected);
					} else {
						txt.setTextColor(tabTextFocused);
					}
				}
				
				if (txt != null)
					Log.d("TabView", txt.getText() + " Selected: " + txt.isSelected() + ", Focused: " + txt.isFocused());
			}

			return v;
		}
	}
	
	private AdapterView.OnItemSelectedListener mTabSelectedListener = new AdapterView.OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			if (mTabPageContainer.getChildCount() > 0) {
				mTabPageContainer.removeAllViews();
			}

			mTabPageContainer.addView(mPages.get(position));
			mSelectedTabIdx = position;
			mTabList.requestFocus();
			mTabList.invalidateViews();
			
			Log.d("TabView", "onItemSelected: " + position);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
		}
	};

	protected View.OnKeyListener mTabBarKeyListener = new View.OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {

			if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (mSelectedTabIdx >= 0) {
						// If Tab Page is focusable, then switch the focus
						// to Tab Page now, let's set the selected tab
						// background to be a bit dim then the selector
						if (mPages.get(mSelectedTabIdx).isFocusable()) {
							mPages.get(mSelectedTabIdx).setFocus();
							View v1 = mTabList.getSelectedView();
							
							if (mPages.get(mSelectedTabIdx).mDefaultIconSelector != null) {
								ImageView icon = (ImageView) v1.findViewById(R.id.tab_item_icon);
								icon.setImageDrawable(mPages.get(mSelectedTabIdx).mSelectedIconSelector);
							} else {
								TextView txt = (TextView) v1.findViewById(R.id.tab_item_text);
								txt.setTextColor(tabTextSelected);
							}

						} else {
							onRightArrowDown(TabView.this);
						}
					}

				} else if (event.getAction() == KeyEvent.ACTION_UP) {
					onRightArrowUp(TabView.this);
				}
				return true;
			}
			
			else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					if (mSelectedTabIdx >= 0) {
						if (mPages.get(mSelectedTabIdx).isFocusable()) {
							// focus set back to the TabBar, the selector will
							// take over, so set the background clear
							View v1 = mTabList.getSelectedView();
							if (mPages.get(mSelectedTabIdx).mDefaultIconSelector != null) {
								ImageView icon = (ImageView) v1.findViewById(R.id.tab_item_icon);
								icon.setImageDrawable(mPages.get(mSelectedTabIdx).mDefaultIconSelector);
							} else {
								TextView txt = (TextView) v1.findViewById(R.id.tab_item_text);
								txt.setTextColor(tabTextFocused);
							}

						} else {
							onLeftArrowUp(TabView.this);
						}

					}

				}
				return true;
			} 
			
			else if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					// let the TabView handle Back keyevent
					return TabView.this.onBackDown(TabView.this);
				} else {
					// otherwise, return false so that BACK key can be propagate up
					return false;
				}

			} 
			
			
			else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					// let the TabView handle Back keyevent
					TabView.this.onSelectDown(TabView.this);
				} else if (event.getAction() == KeyEvent.ACTION_UP) {
					TabView.this.onSelectUp(TabView.this);
				}
				return true;

			} 
			
			else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
				if (mSelectedTabIdx == mPages.size() - 1) {
					return true; // prevent the focus go to the TabPage when the  last tab is selected
				} else {
					return false;
				}
			} 
			
			else {
				return false;
			}
		}
	};

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (gainFocus == true) {
			focusTabBar();
		}
	}

	/**
	 * Down arrow is down. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onDownArrowDown(View srcView) {
		return false;
	}

	/**
	 * Down arrow is up. The derived views should implement this if they want to
	 * handle the down-arrow key event
	 */
	public boolean onDownArrowUp(View srcView) {
		return false;
	}

	/**
	 * Up arrow is down. The derived views should implement this if they want to
	 * handle the down-arrow key event
	 */
	public boolean onUpArrowDown(View srcView) {
		return false;
	}

	/**
	 * Up arrow is up. The derived views should implement this if they want to
	 * handle the up-arrow key event
	 */
	public boolean onUpArrowUp(View srcView) {
		return false;
	}

	/**
	 * Left arrow is down. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onLeftArrowDown(View srcView) {
		return false;
	}

	/**
	 * Left arrow is up. The derived views should implement this if they want to
	 * handle the down-arrow key event
	 */
	public boolean onLeftArrowUp(View srcView) {
		return false;
	}

	/**
	 * Right arrow is down. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onRightArrowDown(View srcView) {
		return false;
	}

	/**
	 * Right arrow is up. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onRightArrowUp(View srcView) {
		return false;
	}

	/**
	 * Select button is down. The derived views should implement this if they
	 * want to handle the down-arrow key event
	 */
	public boolean onSelectDown(View srcView) {
		return false;
	}

	/**
	 * Select button is up. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onSelectUp(View srcView) {
		return false;
	}

	/**
	 * back button is down. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onBackDown(View srcView) {
		return false;
	}

	/**
	 * Back button is up. The derived views should implement this if they want
	 * to handle the down-arrow key event
	 */
	public boolean onBackUp(View srcView) {
		return false;
	}

	/**
	 * Handle the key-down event from the remote control
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
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
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
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