package com.reconinstruments.itemhost;

import com.reconinstruments.utils.DeviceUtils;

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

public class ItemHostHelpDialog extends DialogFragment implements DialogInterface.OnKeyListener {
	
	private boolean isDismissed = false;
	
	private ItemHostHelpDialog(){
		
	}
	
	static ItemHostHelpDialog newInstance(){
		ItemHostHelpDialog h = new ItemHostHelpDialog();
		Bundle args = new Bundle();
		h.setArguments(args);
		return h;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.ReconTheme_NoTitleBar);
	}
	
	@Override
	public void onActivityCreated(Bundle arg0) {
		Window window = getDialog().getWindow();
        LayoutParams params = window.getAttributes();
       	params.alpha = 0.9f;
        window.setAttributes((android.view.WindowManager.LayoutParams) params);
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.dialog_animation;
        super.onActivityCreated(arg0);
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState){
		//FEATURE_NO_TITLE must be set on the window before Activity.onCreate()
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		View view;
		view = inflater.inflate(R.layout.help_dialog_layout, container);
		TextView buttonTV = (TextView) view.findViewById(R.id.body);
		buttonTV.setText(getColoredText());
		buttonTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, (DeviceUtils.isSun()) ? R.drawable.select_jet : R.drawable.select_snow2, 0);
		buttonTV.setCompoundDrawablePadding(7);
		TextView bodyTV = (TextView) view.findViewById(R.id.body3);
		if(DeviceUtils.isSun()){
			bodyTV.setText(getResources().getString(R.string.jet_connect_smartphone));
		}else{
			bodyTV.setText(getResources().getString(R.string.snow_connect_smartphone));
		}
		getDialog().setOnKeyListener(this);
		return view;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		// when user has initiated quick nav, dialog is paused
		// now is the best time to dismiss it
		if(!isDismissed) getDialog().dismiss();
	};
	
	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent keyEvent) {
		// don't respond to any key except a long-pressed DPAD_CENTER and BACK; 
		// when the user launches quick nav, this DialogFragment will go
		// to onPause(), which is the only time this DialogFragment should be dismissed, 
		// except when user presses BACK
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(keyEvent.getAction() == KeyEvent.ACTION_UP) {
				isDismissed = true;
				dialog.dismiss();
				return true;
			}
			else return false;
		}
		else
			return false;
	}
	
	private CharSequence getColoredText(){
		CharSequence desc = Html.fromHtml("<font color=\"#ffb300\">Hold</font>");
        return desc;
	}
}
