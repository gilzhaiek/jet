package com.reconinstruments.messagecenter.frontend;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.MessageHelper.CategoryViewData;
import com.reconinstruments.messagecenter.MessageHelper.MessageViewData;
import com.reconinstruments.messagecenter.MessageHelper.GroupViewData;
import com.reconinstruments.utils.DeviceUtils;

import com.reconinstruments.messagecenter.R;

public class MessageAlert extends Activity {
	private static final String TAG = "MessageAlert";
	public static final String TEXT_FORMAT_PREFIX = "<b><font color=#6395ff>";
	public static final String TEXT_FORMAT_SUFFIX = "</font></b> &nbsp;&nbsp;";

	final Context mContext = this;
	private Timer mTimer = new Timer();

	private static final int TimerAlert = 10000; // keep alert for 10 seconds

	private TextView mMessage = null;
	private RelativeLayout mImageContainer = null;
	private ImageView mImage = null;
	private TextView mCategory = null;
	private Intent viewerIntent;

	MessageViewData data;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setGravity(Gravity.BOTTOM);
		// removes window border
		getWindow().setBackgroundDrawableResource(android.R.color.transparent); 

		data = MessageHelper.getMessagesById(this,
				getIntent().getIntExtra("message_id", 0));
		if (data == null) {
			Log.e(TAG, "couldn't get message cursor");
			finish();
			return;
		}

		viewerIntent = (Intent) getIntent().getParcelableExtra("ViewerIntent");
		if (viewerIntent == null) { // if there is no handling intent, open message center by default to view the message
			viewerIntent = new Intent("com.reconinstruments.messagecenter.frontend");
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// paint from layout
		if(DeviceUtils.isSun()){
		    setContentView(R.layout.interactive_notification_jet);
		}else{
		    setContentView(R.layout.interactive_notification);
		}
		// have to set after setContentView otherwise it doesn't work
		getWindow().setLayout(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT); 
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.dimAmount=0.7f;
		getWindow().setAttributes(lp);
		overridePendingTransition(R.anim.dock_bottom_enter, R.anim.fadeout);

		mMessage = (TextView) findViewById(R.id.alertMessage);
		mImageContainer = (RelativeLayout) findViewById(R.id.icon_view);
		mImage = (ImageView) findViewById(R.id.alertIcon);
		mCategory = (TextView) findViewById(R.id.alertCategory);
		
        CategoryViewData category = MessageHelper.getCategoryById(this.getApplicationContext(), data.catId);
        Drawable icon = UIUtils.getDrawableFromAPK(this.getApplicationContext().getPackageManager(), category.apk,
                category.catIcon);
         mImage.setImageDrawable(icon);
        
		String grpUri = getIntent().getStringExtra("group_uri");
        if("com.reconinstruments.social.facebook".equals(grpUri)){ //facebook message
            mImageContainer.setBackgroundResource(R.drawable.fb_icon_container);
        } else if("com.reconinstruments.texts".equals(grpUri)){ //sms message
            mImageContainer.setBackgroundResource(R.drawable.text_icon_container);
        } else if("com.reconinstruments.calls".equals(grpUri)){ //phone call message
            mImageContainer.setBackgroundResource(R.drawable.text_icon_container);
        } else {
            mImageContainer.setBackgroundResource(R.drawable.fb_icon_container);
        }

        String overwrittenDesc = getIntent().getStringExtra("overwritten_desc");
		/* Paint view contents from received Notification Data */
		if (overwrittenDesc != null && overwrittenDesc.length() > 0) {
			mMessage.setText(overwrittenDesc.replace(TEXT_FORMAT_PREFIX, "")
					.replace(TEXT_FORMAT_SUFFIX, " "));
		} else {
			mMessage.setText(data.text.replace(TEXT_FORMAT_PREFIX, "").replace(
					TEXT_FORMAT_SUFFIX, ": "));
		}

		mCategory.setText(data.catDesc);

		// now start dismiss timer
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				finish();
				overridePendingTransition(0, R.anim.dock_bottom_exit);
			}

		}, MessageAlert.TimerAlert);
	}

	@Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(0, R.anim.dock_bottom_exit);
		super.onBackPressed();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                try {
                    MessageHelper.messageProcessed(MessageAlert.this, data.id);
                    viewerIntent.putExtra("grp_id", data.grpId);
                    viewerIntent.putExtra("cat_desc", data.catDesc);
                    startActivity(viewerIntent);
                    
                    overridePendingTransition(R.anim.dock_bottom_enter,
                            R.anim.fadeout);
                } catch (Exception ex) {
                    String str = getString(R.string.intentexception);
                    str += ex.getMessage();

                    Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG)
                            .show();
                }

                finish();
                overridePendingTransition(0, R.anim.dock_bottom_exit);
                return true;
            case KeyEvent.KEYCODE_BACK:
                finish();
                overridePendingTransition(0, R.anim.dock_bottom_exit);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
            }
	}

}
