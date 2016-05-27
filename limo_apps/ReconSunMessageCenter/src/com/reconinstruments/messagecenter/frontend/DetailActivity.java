
package com.reconinstruments.messagecenter.frontend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.messagecenter.R;
import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.MessageHelper.MessageViewData;
import com.reconinstruments.messagecenter.MessageHelper.GroupViewData;
import com.reconinstruments.messagecenter.MessageHelper.CategoryViewData;
import com.reconinstruments.commonwidgets.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>DetailActivity</code> shows notification detail info when user go into it
 */
public class DetailActivity extends Activity {
    private static final String TAG = DetailActivity.class.getSimpleName();

    private MessageViewData[] mMsgs;
    private GroupViewData[] mGroups;
    private int mCurrentPos;

    private TextView mCategoryNameTV;
    private TextView mMessageNumTV;
    private TextView mDetailTV;
    private ImageView mNotifTypeIV;
    private TextView mTimestampTV;
    private ImageView numUnreadTV;
    private ImageView textUnreadTV;
    private boolean isPhoneCall;

    private boolean mBackToSummary = true;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);

        mCategoryNameTV = (TextView) findViewById(R.id.category_name);
        mMessageNumTV = (TextView) findViewById(R.id.message_num);
        mDetailTV = (TextView) findViewById(R.id.detail);
        numUnreadTV = (ImageView) findViewById(R.id.num_unread);
        textUnreadTV = (ImageView) findViewById(R.id.text_unread);

        // to be used in overlay -- later design
        // mNotifTypeIV = (ImageView) findViewById(R.id.notif_type);
        // mTimestampTV = (TextView) findViewById(R.id.timestamp);

        mGroups = MessageHelper.getGroupView(this.getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        int groupId = getIntent().getExtras().getInt("grp_id", 0);
        String description = getIntent().getExtras().getString("cat_desc");
        mBackToSummary = getIntent().getBooleanExtra("back_to_summary", true);
        mMsgs = MessageHelper
                .getMessagesByGroupIdAndCategoryDescription(this, groupId, description);
        mCurrentPos = 0;
        isPhoneCall = UIUtils.GROUP_PHONE.equals(getGroupDesc(mMsgs[mCurrentPos].grpId));
        mCategoryNameTV.setText( isPhoneCall ? "Missed Calls" : description);
        updateUI();
    }

    private int getFirstUnreadPosition(MessageViewData[] messages){
        int pos = 0;
        for(int i = 0; i<messages.length; i++){
            MessageViewData d = messages[i];
            if(!d.processed){
                pos = i;
                break;
            }
        }
        return pos;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mCurrentPos > 0) {
                    mCurrentPos--;
                    updateUI();
               }
                return false;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mCurrentPos < (mMsgs.length - 1)) {
                    mCurrentPos++;
                    updateUI();
                }
                return false;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void updateUI() {
        MessageViewData[] messages = MessageHelper.getMessagesByGroupIdAndCategoryDescription(
                this.getApplicationContext(), mMsgs[mCurrentPos].grpId, mMsgs[mCurrentPos].catDesc);

        if (isPhoneCall){
            numUnreadTV.setVisibility(View.INVISIBLE);
            mMessageNumTV.setVisibility(View.INVISIBLE);
            mDetailTV.setText("");
            MessageViewData mCurrentMessage =  mMsgs[mCurrentPos];
            String username = (mCurrentMessage.text).equals("Unknown")
                    ? mCurrentMessage.catDesc : mCurrentMessage.text;
            for (MessageViewData msg : messages) {
             mDetailTV.append(username + "    " +
                     UIUtils.formatTimestamp(msg.date) + "\n" );
             // TODO: format date to extract just the time, acc. to 12 or 24 hour clock
            }
        } else {
            // unread badge stuff
            int unreadCount = 0;
            for (MessageViewData msg : messages) {
                if (!msg.processed && (msg.id != mMsgs[mCurrentPos].id)) {
                    unreadCount++;
                }
            }
            if (unreadCount == 0) numUnreadTV.setVisibility(View.INVISIBLE);

            if (!messages[mCurrentPos].processed) textUnreadTV.setVisibility(View.VISIBLE);
            else textUnreadTV.setVisibility(View.INVISIBLE);
            // end of unread badge code

            mMessageNumTV.setText((mCurrentPos + 1) + "/" + mMsgs.length);
            mDetailTV.setText(Html.fromHtml(mMsgs[mCurrentPos].text));

            // to be used in overlay -- later design
            // mNotifTypeIV.setImageDrawable(icon);
            // mTimestampTV.setText(timestamp);

            mMsgs[mCurrentPos].processed = true;
            MessageHelper.messageProcessed(this.getApplicationContext(), mMsgs[mCurrentPos].id);
        }
    }

    // get group desc by id
    private String getGroupDesc(int id) {
        for (GroupViewData data : mGroups) {
            if (data.grpId == id) {
                return data.grpDesc;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mBackToSummary){
            mMsgs[mCurrentPos].processed = true;
            MessageHelper.messageProcessed(this.getApplicationContext(), mMsgs[mCurrentPos].id);
            CommonUtils.launchParent(this, new Intent("com.reconinstruments.messagecenter.frontend"), true);
        }else{
            finish();
        }
    }
}
