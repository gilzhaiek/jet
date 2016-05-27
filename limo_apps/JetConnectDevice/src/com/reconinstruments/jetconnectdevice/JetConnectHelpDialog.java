package com.reconinstruments.jetconnectdevice;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;

import com.reconinstruments.utils.DeviceUtils;

public class JetConnectHelpDialog extends DialogFragment implements DialogInterface.OnKeyListener {
	
	private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;
	
	private JetConnectHelpDialog(){
		
	}
	
	static JetConnectHelpDialog newInstance(){
		JetConnectHelpDialog h = new JetConnectHelpDialog();
		Bundle args = new Bundle();
		h.setArguments(args);
		return h;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
	}
	
	@Override
	public void onActivityCreated(Bundle arg0) {
		Window window = getDialog().getWindow();
        LayoutParams params = window.getAttributes();
		window.setAttributes((android.view.WindowManager.LayoutParams) params);
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.dialog_animation;
        super.onActivityCreated(arg0);
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState){
//		FEATURE_NO_TITLE must be set on the window before Activity.onCreate()
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().setOnKeyListener(this);
		View view;
		if(DeviceUtils.isSun())	{
			view = inflater.inflate(R.layout.help_dialog_layout_jet, container);
			TextView bodyTV = (TextView) view.findViewById(R.id.body);
			bodyTV.setText(getColoredText());
		}
		else {
			view = inflater.inflate(R.layout.help_dialog_layout_snow, container);
		}
		view.setBackgroundColor(0xB3000000);
		return view;
	}

	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent keyEvent) {
	    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) return false;
	    switch (keyCode) {
	    case KeyEvent.KEYCODE_DPAD_CENTER:
	    	dialog.dismiss();
	    	return true;
	    case KeyEvent.KEYCODE_BACK:
	    	dialog.dismiss();
	    	return true;
	    default:
		break;
	    }
	    return false;
	}
	
	private CharSequence getColoredText(){
		CharSequence desc = "";
        desc = Html.fromHtml("Swipe <font color=\"" + RECON_YELLOW_TEXT + "\">forward</font> or "
        		+ "<font color=\"" + RECON_YELLOW_TEXT +"\">backward</font> <br>to navigate JET menus.");
        return desc;
	}
}
