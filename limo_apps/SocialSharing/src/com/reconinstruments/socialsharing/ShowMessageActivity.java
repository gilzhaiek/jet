package com.reconinstruments.socialsharing;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;
import com.reconinstruments.utils.MessageCenterUtils;
//import com.reconinstruments.messagecenter.MessagesProvider;
import com.reconinstruments.socialsharing.ReplyMessageActivity;

public class ShowMessageActivity extends Activity {

	private static final String TAG = "ShowMessageActivity";

	final Context mContext = this;

	// controls
	private TextView  txtMessage = null;
	private TextView  txtCategory   = null;
	private TextView  txtDate = null;
	private TextView  txtCount = null;
	
	private ImageView  leftArrow = null;
	private ImageView  rightArrow   = null;
	private ImageView  upArrow = null;
	private ImageView  downArrow = null;

	private int mIndex = 0;           // current nav index
	
	private int category_id;
	private int msg_id;

	MessageViewData[] messages;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// get ui controls
		setContentView(R.layout.social_message);

		txtMessage  = (TextView) findViewById(R.id.notification);
		txtCategory = (TextView) findViewById(R.id.msgCategory);
		txtDate     = (TextView) findViewById(R.id.msgDate);
		txtCount    = (TextView) findViewById(R.id.msgnav_count);
		
		leftArrow   = (ImageView) findViewById(R.id.msgbtn_navprev);
		rightArrow  = (ImageView) findViewById(R.id.msgbtn_navnext);
		
		txtMessage.setMovementMethod(new ArrowKeyMovementMethod());

		// Message Group and Category must be passed to us as part of Intent
		Intent intent = getIntent();
		int category_id = intent.getIntExtra("category_id",0);
		int msg_id = intent.getIntExtra("message_id",0);
		
		ContentResolver contentResolver = getContentResolver();
		String msgSelect = MsgSchema.COL_CATEGORY_ID+" = "+category_id;
		Cursor cursor = contentResolver.query(ReconMessageAPI.MESSAGES_VIEW_URI, ALL_FIELDS, msgSelect, null, null);
				
		messages = new MessageViewData[cursor.getCount()];
		
		for(int i=0;i<messages.length;i++){	
			cursor.moveToNext();
			messages[i] = new MessageViewData(cursor);
			Log.d(TAG, "msg: "+messages[i].id+" processed: "+messages[i].processed);
			
			if(msg_id==messages[i].id)//if there is a msg_id and it matches this one, view this one
				mIndex = i;
		}
		// close the cursor.
		cursor.close();
		if(mIndex==0)
			mIndex = messages.length-1; //default view the last message

		/* attach actions for dismiss and handle buttons */
		TextView txtHandle  = (TextView) findViewById(R.id.msgbtn_Handle);

		// navigate to current index, which is start
		if (messages.length > 0)
		{
			// category is immutable
			txtCategory.setText(messages[0].catDesc);
			this.navigate ();
			
		}
		else
			txtMessage.setText(R.string.nomsg);

		
		Typeface source_sans_bold = Typeface.createFromAsset(getAssets(), "fonts/SourceSansPro-Bold.ttf");
		Typeface source_sans_semi_bold = Typeface.createFromAsset(getAssets(), "fonts/SourceSansPro-Semibold.ttf");
		Typeface source_sans_regular = Typeface.createFromAsset(getAssets(), "fonts/SourceSansPro-Regular.ttf");
		txtCategory.setTypeface(source_sans_bold);
		txtDate.setTypeface(source_sans_semi_bold);
		txtCount.setTypeface(source_sans_semi_bold);
		txtMessage.setTypeface(source_sans_regular);
	}

    @Override
    public void onResume() {
	super.onResume();
	int categoryId = getIntent().getIntExtra("category_id",-1);
	if (categoryId != -1) {
	    ReconMessageAPI.markAllMessagesInCategoryAsRead(this, categoryId);
	}
    }

	// internal helper -- paints UI from current index
	// date, message and nav count
	private void navigate ()
	{
		// flag notification as handled per design -- as soon as it is viewed here
		if (!messages[mIndex].processed)
		{
			messages[mIndex].processed = true;
			
			ReconMessageAPI.markMessageRead(this, messages[mIndex].id);
		}

		txtMessage.setMovementMethod(ScrollingMovementMethod.getInstance() );
		txtMessage.setText(Html.fromHtml(messages[mIndex].text));

		txtDate.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[mIndex].date));
		
		txtCount.setText(String.format("%d/%d", mIndex + 1, messages.length) );
		if(mIndex==0) leftArrow.setImageResource(R.drawable.left_arrow_inactive);
		else leftArrow.setImageResource(R.drawable.left_arrow_active);
		if(mIndex==messages.length-1) rightArrow.setImageResource(R.drawable.right_arrow_inactive);
		else rightArrow.setImageResource(R.drawable.right_arrow_active);
		

		txtMessage.setSelected(true);
		txtMessage.scrollTo(0, 0);
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		switch(keyCode){
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (mIndex > 0) {
				mIndex--; navigate();
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if ( (mIndex + 1) < messages.length) {
				mIndex++; navigate();
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			return true;
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			return true;
		default:
			return super.onKeyUp(keyCode, event);	
		}
	}

	public static final String[] ALL_FIELDS = new String[] {
		MsgSchema._ID,
		MsgSchema.COL_TIMESTAMP,
		MsgSchema.COL_TEXT,
		MsgSchema.COL_PROCESSED,
		CatSchema.COL_DESCRIPTION,
		CatSchema.COL_PRESS_INTENT,
		CatSchema.COL_PRESS_CAPTION,
		MsgSchema.COL_CATEGORY_ID
	};
	public static class MessageViewData{
		MessageViewData(Cursor cursor){
			id = cursor.getInt(0);
			date = new Date(cursor.getLong(1));
			text = cursor.getString(2);
			processed = cursor.getInt(3)==1;
			catDesc = cursor.getString(4);
			pressIntent = MessageCenterUtils.BytesToIntent(cursor.getBlob(5));
			pressCaption = cursor.getString(6);
			catId = cursor.getInt(7);
		}
		int id;
		Date date;
		String text;
		boolean processed;
		String catDesc;
		Intent pressIntent;
		String pressCaption;
		int catId;
	}
	
}
