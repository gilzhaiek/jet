
package com.reconinstruments.messagecenter.frontend;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.messagecenter.R;
import com.reconinstruments.messagecenter.MessageHelper;
import com.reconinstruments.messagecenter.MessageHelper.CategoryViewData;
import com.reconinstruments.messagecenter.MessageHelper.MessageViewData;
import com.reconinstruments.messagecenter.MessageHelper.GroupViewData;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.ProgressDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <code>SummaryActivity</code> main screen, pulls the most recent grouped and
 * categoried messages from DB and represent them on the UI. It supports the
 * filter function as well.
 */
public class SummaryActivity extends CarouselItemHostActivity {
    private static final String TAG = SummaryActivity.class.getSimpleName();

    private MessageViewData[] mMessages;
    private static GroupViewData[] mGroups;
    private MessageViewData mCurrentMessage;
    private int mCurrentGrpId = 0;
    private static int mCurrentPos = -1;
    private boolean initializedPager = false;

    private static TextView mSummaryTV;
    private static ImageView mNotifTypeIV;
    private static TextView mTimestampTV;
    private static TextView mNewCountTV;

    private enum filterMode {UNREAD, ALL};
    private filterMode mode = filterMode.UNREAD;
    private List<MessageViewData> mUnreadMessages;
    private boolean noUnreadMessages = false;
    private Handler mHandler = new Handler();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.summary);
        mSummaryTV = (TextView) findViewById(R.id.summary);
        mNotifTypeIV = (ImageView) findViewById(R.id.notif_type);
        mTimestampTV = (TextView) findViewById(R.id.timestamp);
        mNewCountTV = (TextView) findViewById(R.id.new_count);
        mGroups = MessageHelper.getGroupView(this.getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        filtering(getIntent().getIntExtra("grp_id", 0));
    }

    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();

        MessageViewData[] iterMessages;
        if(mode == filterMode.UNREAD) {
           iterMessages = new MessageViewData[mUnreadMessages.size()];
           iterMessages = mUnreadMessages.toArray(iterMessages);
        } else {
            iterMessages = mMessages.clone();
        }

        for (int i = 0; i < iterMessages.length; i++) {
            MessageViewData data = ((MessageViewData) (iterMessages[i]));
            String input = data.catDesc;

            // if phone call and name not unknown, show name instead
            if ( UIUtils.GROUP_PHONE.equals(getGroupDesc(data.grpId))
                    && !((data.text).equals("Unknown")) ) input = data.text;

            fList.add(new SummaryFragment(R.layout.summary_fragment, UIUtils.lastInitial(input), 0, i));
        }

        enableDynamicPager = true; // enabling dynamic view pager
        // enableInfinitePager = true; // if enabled, will crash the app when updated programmatically with new data
        Log.d(TAG, mode+" Fragments: "+fList);
        return fList;
    }

    private void setNoMessageLayout(){ setContentView(R.layout.no_message); }

    /**
     * filter the notifications
     * 
     * @param grpId if equals 0, means don't filter, otherwise filter by group
     */
    public void filtering(int grpId) {
        mCurrentGrpId = grpId;
        if (mCurrentGrpId != 0) {
            mMessages = MessageHelper.getMessagesByGroupId(this.getApplicationContext(), grpId);
        } else {
            mMessages = MessageHelper.getRecentMessages(this.getApplicationContext());
        }
        
        if(mMessages.length == 0){
            setNoMessageLayout();
            return;
        }

        if(mode == filterMode.UNREAD) {
            mUnreadMessages = new ArrayList<MessageViewData>();
            for (int i = 0; i < mMessages.length; i++) {
                MessageViewData data = ((MessageViewData) (mMessages[i]));
                MessageViewData[] messages = MessageHelper.getMessagesByGroupIdAndCategoryDescription(
                        this.getApplicationContext(), data.grpId, data.catDesc);
                int unread = 0;
                for (MessageViewData msg : messages) {
                    if (!msg.processed) unread++;
                    Log.d(TAG, "Processed?" + msg.processed);
                }
                if (unread > 0) {
                    mUnreadMessages.add(mMessages[i]);
                }
            }
            if(mUnreadMessages.isEmpty()) {
                mode = filterMode.ALL;
                noUnreadMessages = true;
                showNoUnreadActivity();
            } else noUnreadMessages = false;
        }

        initPager();
        initializedPager = true;
        mPager.setPadding(0, 0, 0, 0); // to make it left aligned dynamic pager
        mPager.setPageMargin(10); // more spacing to account for less padding
        mPager.setCurrentItem(getFirstUnreadPosition(mMessages));
        updateUI();
    }

    /**
     * get first unread position so can nav to this message, if mCurrentPos is valid, use mCurrentPos instead
     * @param messages
     * @return
     */
    private int getFirstUnreadPosition(MessageViewData[] messages){
        if(mCurrentPos != -1){
            return mCurrentPos;
        }
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
    
    private void updateUI() {
        if (mode == filterMode.UNREAD && mMessages.length == 0) {
            mNotifTypeIV.setVisibility(View.INVISIBLE);
            mNewCountTV.setBackgroundResource(R.drawable.no_new_badge);
            return;
        }

        if(mode == filterMode.UNREAD) {
            mCurrentMessage = ((MessageViewData) (mUnreadMessages.get(mPager.getCurrentItem() % mMessages.length)));
        } else mCurrentMessage = ((MessageViewData) (mMessages[mPager.getCurrentItem() % mMessages.length]));

        mCurrentPos = mPager.getCurrentItem(); // remember the current position

        // calc unread count
        int unreadCount = 0;
        if (mCurrentGrpId != 0) {
            MessageViewData[] msgs = MessageHelper.getRecentMessages(this.getApplicationContext());
            for (MessageViewData d : msgs) {
                if (d.grpId == mCurrentGrpId && !d.processed) {
                    unreadCount++;
                }
            }
        } else {
            MessageViewData[] msgs = MessageHelper.getRecentMessages(this.getApplicationContext());
            for (MessageViewData d : msgs) {
                if (!d.processed) {
                    unreadCount++;
                }
            }
        }

        String summary = mCurrentMessage.text;
        String groupDesc = getGroupDesc(mCurrentMessage.grpId);
        final MessageViewData[] messages = MessageHelper.getMessagesByGroupIdAndCategoryDescription(
                this.getApplicationContext(), mCurrentMessage.grpId, mCurrentMessage.catDesc);

        // if phone call, show missed phone calls instead
        if (UIUtils.GROUP_PHONE.equals(groupDesc)){
            if(messages.length == 1) summary = messages.length + " Missed Call";
            else summary = messages.length + " Missed Calls";
        }

        // truncation and unread functionality
        String nOnemore = "";
        final String[] newLine = {"<br>"};
        final String[] seeAll = {""};
        String timestamp = UIUtils.formatTimestamp(mCurrentMessage.date);

        int unread = 0;
        for (MessageViewData msg : messages) {
            if (!msg.processed && (msg.id != mCurrentMessage.id)) {
                unread++;
            }
        }

        TextUtils.EllipsizeCallback ellipsizeCallback = new TextUtils.EllipsizeCallback(){
            public void ellipsized(int start, int end) {
                seeAll[0] = "";
            }
        };
        TextUtils.EllipsizeCallback seeAllCallback = new TextUtils.EllipsizeCallback(){
            public void ellipsized(int start, int end) {
                if (end !=0) seeAll[0] = "<font color=#ffb300>(see all)</font>";
                Log.d("SeeAllCallBack:", seeAll[0]);
            }
        };
        TextUtils.EllipsizeCallback newLineCallback = new TextUtils.EllipsizeCallback(){
            public void ellipsized(int start, int end) {
                if (end !=0) newLine[0] = " ";
            }
        };

        int summaryViewWidth = mSummaryTV.getWidth();
        if (summaryViewWidth == 0) {
            // figure out width through the parameters set
            // in this case screen width, right and left margin
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) mSummaryTV.getLayoutParams();
            DisplayMetrics displaymetrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            // screenWidth - margins [428-(30+30) @Nov19,2014]
            summaryViewWidth = displaymetrics.widthPixels-(mlp.leftMargin + mlp.rightMargin);
        }

        if (unread > 0) {
            nOnemore = "<font color=#ffb300>(+"+unread+" unread)</font>";
            summary = TextUtils.ellipsize(summary, mSummaryTV.getPaint(),
                    (float) (1.5*summaryViewWidth), TextUtils.TruncateAt.END, false, ellipsizeCallback).toString();
        }
        else{
            summary = TextUtils.ellipsize(summary, mSummaryTV.getPaint(),
                    (float) (1.6*summaryViewWidth), TextUtils.TruncateAt.END, false, seeAllCallback).toString();
        }

        TextUtils.ellipsize(summary, mSummaryTV.getPaint(),
                (float) (summaryViewWidth), TextUtils.TruncateAt.END, false, newLineCallback);

        //--end of truncation and unread functionality---
        CharSequence processedSummary = Html.fromHtml(summary+newLine[0]+nOnemore+seeAll[0]);
        if (UIUtils.GROUP_PHONE.equals(groupDesc)) processedSummary = summary; // no "unread" for phone calls
        mSummaryTV.setText(processedSummary);
        Log.d("SummaryText:", processedSummary+"");

        CategoryViewData category = MessageHelper.getCategoryById(this.getApplicationContext(), mCurrentMessage.catId);
        Drawable icon = UIUtils.getDrawableFromAPK(this.getApplicationContext().getPackageManager(), category.apk,
                category.catIcon);

        if (UIUtils.GROUP_PHONE.equals(groupDesc)) {
            mSummaryTV.setGravity(Gravity.CENTER);
        }
        else if (UIUtils.GROUP_TEXTS.equals(groupDesc)) {
            mSummaryTV.setGravity(Gravity.LEFT);
        }
        else {
            mSummaryTV.setGravity(Gravity.LEFT);
        }


        mNotifTypeIV.setImageDrawable(icon);
        
        mTimestampTV.setText(timestamp);

        if (unreadCount > 0) {
            mNewCountTV.setText(String.valueOf(unreadCount));
        } else {
            mNewCountTV.setText("0");
            mNewCountTV.setBackgroundResource(R.drawable.no_new_badge);
        }

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if(mMessages.length == 0) return true; // don't do if there is no message
                if (mCurrentMessage != null && mCurrentMessage.id > 0 && !mCurrentMessage.processed) {
                    mCurrentMessage.processed = true;
                    MessageHelper
                            .messageProcessed(this.getApplicationContext(), mCurrentMessage.id);
                }
                updateUI();
                return false;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(mMessages.length == 0) return true; // don't do if there is no message
                if (mCurrentMessage != null && mCurrentMessage.id > 0) {
                    Intent intent = new Intent(this, DetailActivity.class);
                    intent.putExtra("grp_id", mCurrentMessage.grpId);
                    intent.putExtra("cat_desc", mCurrentMessage.catDesc);
                    intent.putExtra("back_to_summary", true);
                    CommonUtils.launchNew(this, intent, false);
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
                if(mMessages.length == 0) return true; // don't do if there is no message
                if(mode == filterMode.UNREAD){
                    showFeedback("All Notifications");
                    mHandler.postDelayed(dismissAllNotifFeedback, 1000);
                    mode = filterMode.ALL;
                    filtering(getIntent().getIntExtra("grp_id", 0));
                }else{
                    showFeedback("New Notifications");
                    mHandler.postDelayed(dismissNewNotifFeedback, 1000);
                }
                return false;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // get group id by desc
    public int getGroupId(String desc) {
        for (GroupViewData data : mGroups) {
            if (desc.equals(data.grpDesc)) {
                return data.grpId;
            }
        }
        return 0;
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
        if (mCurrentMessage != null && mCurrentMessage.id > 0 && !mCurrentMessage.processed) {
            mCurrentPos = -1; //reset this value to forget the current position since the user quit the app.
            mCurrentMessage.processed = true;
            MessageHelper
                    .messageProcessed(this.getApplicationContext(), mCurrentMessage.id);
        }
    }

    private void showNoUnreadActivity() {
        startActivity((new Intent(this, NoUnreadActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    private void showFeedback(String message) {
        ProgressDialog.showProgressDialog(SummaryActivity.this, message);
    };

    private Runnable dismissAllNotifFeedback = new Runnable() {
        public void run() {
            dismissFeedback();
        }
    };

    private Runnable dismissNewNotifFeedback = new Runnable() {
        public void run() {
            dismissFeedback();
            if(mode == filterMode.ALL && !noUnreadMessages){
                mode = filterMode.UNREAD;
                filtering(getIntent().getIntExtra("grp_id", 0));
            } else if(noUnreadMessages) showNoUnreadActivity();
        }
    };

    private void dismissFeedback() {
        ProgressDialog.dismissProgressDialog(SummaryActivity.this);
    };
}
