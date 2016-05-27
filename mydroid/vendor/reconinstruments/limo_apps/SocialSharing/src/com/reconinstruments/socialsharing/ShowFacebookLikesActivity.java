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
import android.os.Build;
import android.os.Bundle;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;
import com.reconinstruments.utils.MessageCenterUtils;
import com.reconinstruments.socialsharing.ReplyMessageActivity;

public class ShowFacebookLikesActivity extends Activity {

	private static final String TAG = "ShowFacebookLikesActivity";

	final Context mContext = this;

	// controls
	private TextView  txtCategory   = null;
	private TextView  txtDate = null;
	private TextView  txtCount = null;
	
	private int category_id;
	private int msg_id;

	MessageViewData[] messages;
	
	ListView likesListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.facebook_likes);

		txtCategory = (TextView) findViewById(R.id.msgCategory);
		txtDate     = (TextView) findViewById(R.id.msgDate);
		txtCount    = (TextView) findViewById(R.id.msgnav_count);
		
		// Message Group and Category must be passed to us as part of Intent
		Intent intent = getIntent();
		int category_id = intent.getIntExtra("category_id",0);
		int msg_id = intent.getIntExtra("message_id",0);
		
		ContentResolver contentResolver = getContentResolver();
		String msgSelect = MsgSchema.COL_CATEGORY_ID+" = "+category_id;
		Cursor cursor = contentResolver.query(ReconMessageAPI.MESSAGES_VIEW_URI, ALL_FIELDS, msgSelect, null, null);
				
		messages = new MessageViewData[cursor.getCount()];
		
		String[] likeItems = new String[cursor.getCount()];
		
		for(int i=0, j=messages.length - 1; i<messages.length; i++, j--){	
			cursor.moveToNext();
			messages[i] = new MessageViewData(cursor);
			Log.d(TAG, "msg: "+messages[i].id+" processed: "+messages[i].processed);
			
			likeItems[j] = messages[i].text.replace(SocialMessageReceiver.FACEBOOK_LIKE_SUFFIX, "");
		}
		// close the cursor.
		cursor.close();
		if (messages.length > 0){
			txtCategory.setText(messages[messages.length - 1].catDesc);
			txtCount.setText(messages.length + " people like this post");
			txtDate.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messages[messages.length - 1].date));
		}
		
		likesListView = (ListView) findViewById(android.R.id.list);
        ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.facebook_likes_item, likeItems);
        likesListView.setAdapter(adapter);
        likesListView.setSelection(0);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_UP){
			int firstPos = likesListView.getSelectedItemPosition();
			Log.d(TAG, "firstPos1=" + firstPos);
			firstPos -= 3;
			if(firstPos < 0)
				firstPos = 0;
			Log.d(TAG, "firstPos2=" + firstPos);
			likesListView.setSelection(firstPos);
		}else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
			int firstPos = likesListView.getSelectedItemPosition();
			Log.d(TAG, "firstPos1=" + firstPos);
			firstPos += 3;
			if(firstPos > likesListView.getAdapter().getCount())
				firstPos = likesListView.getAdapter().getCount() - 3;
			Log.d(TAG, "firstPos2=" + firstPos);
			likesListView.setSelection(firstPos);
		}
		return super.onKeyUp(keyCode, event);
	}

	protected void onResume() {
	super.onResume();
	int categoryId = getIntent().getIntExtra("category_id",-1);
	if (categoryId != -1) {
	    ReconMessageAPI.markAllMessagesInCategoryAsRead(this, categoryId);
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
