package com.reconinstruments.camera.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.camera.R;

public class MenuFragment extends Fragment {
	private static final String TAG = MenuFragment.class.getSimpleName();
	
	public static final String KEY_MENU_TEXT = "menuText";
	public static final String ERROR_TEXT = "ERROR";
	
	private String mMenuText;
	private TextView mMenuTextView;

    /**
     * Factory Method. Create a new instance of CountingFragment, providing "num"
     * as an argument.
     */
    public static MenuFragment newInstance(String menuText) {
        MenuFragment menuFragment = new MenuFragment();

        // Supply text input as an argument.
        Bundle args = new Bundle();
        args.putString(KEY_MENU_TEXT, menuText);
        menuFragment.setArguments(args);

        return menuFragment;
    }
    
    public void setText(String text) {
    	mMenuText = text;
    	mMenuTextView.setText(text);
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mMenuText = getArguments() != null ? getArguments().getString(KEY_MENU_TEXT) : ERROR_TEXT ;
        
        if (mMenuText.equalsIgnoreCase(ERROR_TEXT)) {
        	Log.e(TAG, "Error has occured. Not Proper Menu");
        }
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
    	View v = inflater.inflate(R.layout.pager_menu_item, container, false);
        
        mMenuTextView = (TextView) v.findViewById(R.id.pager_menu_text);
        mMenuTextView.setText(mMenuText);
        
        return v;
    }
}
