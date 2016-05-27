package com.reconinstruments.messagecenter.frontend;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.R;

public class MessageAlert extends Activity {
	private static final String TAG = "MessageAlert";
	public static final String TEXT_FORMAT_PREFIX = "<b><font color=#6395ff>";
	public static final String TEXT_FORMAT_SUFFIX = "</font></b> &nbsp;&nbsp;";

	final Context mContext = this;
	private Timer mTimer = new Timer();

	private static final int TimerAlert = 10000; // keep alert for 10 seconds

	private TextView mMessage = null;
	private ImageView mImage = null;
	private TextView mCategory = null;
	private Intent viewerIntent;

	MessageHelper.MessageViewData data;

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
		if (viewerIntent == null) {
			viewerIntent = new Intent(this, MessageViewer.class);
		}

		// paint from layout
		setContentView(R.layout.alertlayout);
		// have to set after setContentView otherwise it doesn't work
		getWindow().setLayout(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT); 
		overridePendingTransition(R.anim.dock_bottom_enter, R.anim.fadeout);

		mMessage = (TextView) findViewById(R.id.alertMessage);
		mImage = (ImageView) findViewById(R.id.alertIcon);
		mCategory = (TextView) findViewById(R.id.alertCategory);
		int shouldShowIcon = getIntent().getIntExtra("should_show_icon", 0);
		String overwrittenDesc = getIntent().getStringExtra("overwritten_desc");
		if (shouldShowIcon == 0) {
			mImage.setVisibility(View.GONE);
		} else {
			mImage.setBackgroundResource(R.drawable.fb_notif_icon_big);
		}

		/* Paint view contents from received Notification Data */
		if (overwrittenDesc != null && overwrittenDesc.length() > 0) {
			mMessage.setText(overwrittenDesc.replace(TEXT_FORMAT_PREFIX, "")
					.replace(TEXT_FORMAT_SUFFIX, " "));
		} else {
			mMessage.setText(data.text.replace(TEXT_FORMAT_PREFIX, "").replace(
					TEXT_FORMAT_SUFFIX, ": "));
		}

		mCategory.setText(data.catDesc);

		Button btnHandle = (Button) findViewById(R.id.alertbtn_Handle);
		btnHandle.setText("OPEN");

		mMessage.setSelected(true); // presumably to make it scroll?

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
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			try {
				MessageHelper.messageProcessed(MessageAlert.this, data.id);
				viewerIntent.putExtra("category_id", data.catId);
				viewerIntent.putExtra("message_id", data.id);
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
		default:
			return super.onKeyUp(keyCode, event);
		}
	}

}
