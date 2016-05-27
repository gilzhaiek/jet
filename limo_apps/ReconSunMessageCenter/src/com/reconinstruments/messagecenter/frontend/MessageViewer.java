package com.reconinstruments.messagecenter.frontend;

import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.R;
import com.reconinstruments.messagecenter.ReconMessageAPI;

/* In this class we don't do automatic notifications of db updates
 * This is fairly consistent with similar types of viewers */
public class MessageViewer extends Activity {
	private static final String TAG = MessageViewer.class.getSimpleName();
	final Context mContext = this;

	// controls
	private TextView txtMessage = null;
	private TextView txtCategory = null;
	private TextView txtDate = null;
	private TextView txtCount = null;

	private ImageView leftArrow = null;
	private ImageView rightArrow = null;
	private ImageView upArrow = null;
	private ImageView downArrow = null;

	private int mIndex = 0; // current nav index

	MessageHelper.MessageViewData[] messages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get ui controls
		setContentView(R.layout.messagelayout);

		txtMessage = (TextView) findViewById(R.id.notification);
		txtCategory = (TextView) findViewById(R.id.msgCategory);
		txtDate = (TextView) findViewById(R.id.msgDate);
		txtCount = (TextView) findViewById(R.id.msgnav_count);

		leftArrow = (ImageView) findViewById(R.id.msgbtn_navprev);
		rightArrow = (ImageView) findViewById(R.id.msgbtn_navnext);
		// upArrow = (ImageView) findViewById(R.id.msgbtn_scrollup);
		// downArrow = (ImageView) findViewById(R.id.msgbtn_scrolldown);

		txtMessage.setMovementMethod(new ArrowKeyMovementMethod());

		// Message Group and Category must be passed to us as part of Intent
		Intent intent = getIntent();
		int category_id = intent.getIntExtra("category_id", 0);
		int msg_id = intent.getIntExtra("message_id", 0);

		messages = MessageHelper.getMessagesByCategoryId(this, category_id);
		for (int i = 0; i < messages.length; i++) {
			Log.d(TAG, "msg: " + messages[i].id + " processed: "
					+ messages[i].processed);

			if (msg_id == messages[i].id)// if there is a msg_id and it matches
											// this one, view this one
				mIndex = i;
		}

		if (mIndex == 0)
			mIndex = messages.length - 1; // default view the last message

		/* attach actions for dismiss and handle buttons */
		TextView txtHandle = (TextView) findViewById(R.id.msgbtn_Handle);

		// navigate to current index, which is start
		if (messages.length > 0) {
			// category is immutable
			txtCategory.setText(messages[0].catDesc);
			this.navigate();

			if (messages[0].pressIntent != null)
				txtHandle.setText(messages[0].pressCaption);
			else {
				LinearLayout btmRow = (LinearLayout) findViewById(R.id.bottomRow);
				btmRow.setVisibility(View.GONE);
			}
		} else
			txtMessage.setText(R.string.nomsg);

//		Typeface source_sans_bold = Typeface.createFromAsset(getAssets(),
//				"fonts/SourceSansPro-Bold.ttf");
//		Typeface source_sans_semi_bold = Typeface.createFromAsset(getAssets(),
//				"fonts/SourceSansPro-Semibold.ttf");
//		Typeface source_sans_regular = Typeface.createFromAsset(getAssets(),
//				"fonts/SourceSansPro-Regular.ttf");
//		txtCategory.setTypeface(source_sans_bold);
//		txtDate.setTypeface(source_sans_semi_bold);
//		txtCount.setTypeface(source_sans_semi_bold);
//		txtMessage.setTypeface(source_sans_regular);
	}

	public void onResume() {
		// Design change: Now we mark all messages in the category as
		// read as soon as we show one message inside the category.
		super.onResume();
		int categoryId = getIntent().getIntExtra("category_id", -1);
		if (categoryId != -1) {
			ReconMessageAPI.markAllMessagesInCategoryAsRead(this, categoryId);
		}

	}

	// internal helper -- paints UI from current index
	// date, message and nav count
	private void navigate() {
		// flag notification as handled per design -- as soon as it is viewed
		// here
		if (!messages[mIndex].processed) {
			messages[mIndex].processed = true;

			/*
			 * ContentValues values = new ContentValues();
			 * values.put(MsgSchema.COL_PROCESSED, 1); String msgSelect =
			 * "_id="+messages[mIndex].id;
			 * getContentResolver().update(MessagesProvider.MESSAGES_URI,
			 * values, msgSelect, null);
			 */
			ReconMessageAPI.markMessageRead(this, messages[mIndex].id);
		}

		txtMessage.setMovementMethod(ScrollingMovementMethod.getInstance());
		txtMessage.setText(messages[mIndex].text);

		txtDate.setText(UIUtils.dateFormat(messages[mIndex].date));

		txtCount.setText(String.format("%d/%d", mIndex + 1, messages.length));
		if (mIndex == 0)
			leftArrow.setImageResource(R.drawable.left_arrow_inactive);
		else
			leftArrow.setImageResource(R.drawable.left_arrow_active);
		if (mIndex == messages.length - 1)
			rightArrow.setImageResource(R.drawable.right_arrow_inactive);
		else
			rightArrow.setImageResource(R.drawable.right_arrow_active);

		txtMessage.setSelected(true);
		txtMessage.scrollTo(0, 0);
		// scroll();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (mIndex > 0) {
				mIndex--;
				navigate();
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if ((mIndex + 1) < messages.length) {
				mIndex++;
				navigate();
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			scroll();
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			scroll();
			return true;
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			try {
				if (messages[mIndex].pressIntent != null)
					startActivity(messages[mIndex].pressIntent);
			} catch (Exception ex) {
				String str = getString(R.string.intentexception);
				str += ex.getMessage();

				Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG)
						.show();

			}
			return true;
		default:
			return super.onKeyUp(keyCode, event);
		}
	}

	private void scroll() {

		/*
		 * if(txtMessage.getScrollY()<txtMessage.getTop())
		 * upArrow.setVisibility(View.GONE); else
		 * upArrow.setVisibility(View.VISIBLE);
		 * 
		 * if(txtMessage.getScrollY()+txtMessage.getTop()+txtMessage.getHeight()>=
		 * txtMessage.getBottom()) downArrow.setVisibility(View.GONE); else
		 * downArrow.setVisibility(View.VISIBLE);
		 * 
		 * 
		 * Log.d("MessageViewer",
		 * "scroll y: "+txtMessage.getScrollY()+" bottom: "
		 * +txtMessage.getBottom()+
		 * " top: "+txtMessage.getTop()+" height: "+txtMessage
		 * .getHeight()+" measured height: "+txtMessage.getMeasuredHeight());
		 */
	}
}
