//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.messagecenter;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MessagePriority;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;
import com.reconinstruments.utils.MessageCenterUtils;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.SettingsUtil;

import com.reconinstruments.messagecenter.R;

import com.reconinstruments.os.hardware.screen.HUDScreenManager;


/**
 * Use the APIs in <code>ReconMessageAPI</code> to post Recon
 * notifications. You need android.permission.SYSTEM_ALERT_WINDOW for
 * your app for these notifications to work properly
 *
 */
public class ReconMessageAPI {

    private static final String TAG = "ReconMessageAPI";
        
    public static final String AUTHORITY = "com.reconinstruments.messagecenter";
    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY);
    public static final Uri MESSAGES_URI =
        Uri.parse("content://" + AUTHORITY + "/" + MsgSchema.TABLE);
    public static final Uri CATEGORIES_URI =
        Uri.parse("content://" + AUTHORITY + "/" + CatSchema.TABLE);
    public static final Uri GROUPS_URI =
        Uri.parse("content://" + AUTHORITY + "/" + GrpSchema.TABLE);

    public static final Uri MESSAGES_VIEW_URI =
        Uri.parse("content://" + AUTHORITY + "/messages_view");
    public static final Uri CATEGORIES_VIEW_URI =
        Uri.parse("content://" + AUTHORITY + "/categories_view");
    public static final Uri GROUPS_VIEW_URI =
        Uri.parse("content://" + AUTHORITY + "/groups_view");
        
    //specified for the Jet platform only
    public static final Uri RECENT_MESSAGES_VIEW_URI =
        Uri.parse("content://"+ AUTHORITY +
                  "/recent_messages_by_group_and_category");

    private static Handler mHandler = new Handler();
    public static final int NOTIFICATION_SHORT_DURATION = 3;
    public static final int NOTIFICATION_LONG_DURATION = 5;

    private static HUDScreenManager mScreenMgr = null;
    private static final int SCREEN_ON_DELAY = 5; // 5 seconds

    public enum NotificationType {
        NONE,PASSIVE,INTERACTIVE }


    /**
     * Deprecated, use postNotification(ReconNotification
     * notification,boolean update,boolean
     * showInteractiveNotification)
     */
    public static void postNotification(Context context,String grpUri,
                                        String grpDesc,int grpIcon,
                                        String catUri,String catDesc,
                                        int catIcon,Intent pressIntent,
                                        Intent holdIntent,Intent viewerIntent,
                                        String pressCaption,
                                        String holdCaption,boolean aggregatable,
                                        String messageText,
                                        MessagePriority priority){

        postNotification(context, grpUri, grpDesc, grpIcon,catUri, catDesc,
                         catIcon, pressIntent, holdIntent, viewerIntent,
                         pressCaption,holdCaption,aggregatable, messageText,
                         priority,null);
    }
        
    /**
     * Deprecated, use postNotification(ReconNotification
     * notification,boolean update,boolean
     * showInteractiveNotification)
     */
    public static void postNotification(Context context,String grpUri,
                                        String grpDesc,int grpIcon,
                                        String catUri,String catDesc,
                                        int catIcon,Intent pressIntent,
                                        Intent holdIntent, Intent viewerIntent,
                                        String pressCaption,String holdCaption,
                                        boolean aggregatable,
                                        String messageText,
                                        MessagePriority priority,String extra){

        ReconNotification notification =
            new ReconNotification(context,grpUri,grpDesc,grpIcon,catUri,catDesc,
                                  catIcon,messageText);
        
        notification.setPressAction(pressIntent, pressCaption);
        notification.setHoldAction(holdIntent, holdCaption);
        notification.overrideMessageViewer(viewerIntent);
        notification.setExtra(extra);
                
        postNotification(notification,false,
                         (priority ==
                          MessagePriority.MESSAGE_PRIORITY_IMPORTANT));
    }

    public static void markMessageRead(Context context,String select){
        ContentValues values = new ContentValues();
        values.put(MessageDBSchema.MsgSchema.COL_PROCESSED, true);
        context.getContentResolver().update(MESSAGES_URI, values, select, null);
    }
        
    public static void markMessageRead(Context context,int msgId){
        String msgSelect = "_id="+msgId;
        markMessageRead(context,msgSelect);
    }

    public static void markAllMessagesInCategoryAsRead (Context context,
                                                        int category_id) {
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = MsgSchema.COL_CATEGORY_ID+" = "+category_id;
        Cursor cursor = contentResolver.query(MESSAGES_VIEW_URI,
                                              new String[]{MsgSchema._ID},
                                              msgSelect, null, null);

        for(int i=0;i<cursor.getCount();i++){   
            cursor.moveToNext();
            int id = cursor.getInt(0);
            markMessageRead(context, id);
        }
        cursor.close();
    }

        

    /**
     * Describe <code>postNotification</code> method here. This is the
     * most general function for posting notifications, others are
     * polymorphism as convenience around this
     *
     * @param notification a <code>ReconNotification</code> value
     * @param update a <code>boolean</code> value
     * @param type a <code>NotificationType</code> value
     * @param shouldUpdateGroupsAndCats a <code>boolean</code> value
     * @param overwrittenDesc a <code>String</code> value
     * @return an <code>Uri</code> value
     */
    public static Uri postNotification(ReconNotification notification,
                                       boolean update,
                                       NotificationType type,
                                       boolean shouldUpdateGroupsAndCats,
                                       String overwrittenDesc) {
        ContentResolver resolver = notification.context.getContentResolver();
        int grpId = getGroupId(resolver,notification.grpUri);
        if(grpId==-1){
            ContentValues group = notification.getGroupValues();
            Log.d(TAG, "posting group");
            Uri uri = resolver.insert(GROUPS_URI, group);
            Log.d(TAG, "got uri: "+uri.toString());
            grpId = Integer.parseInt(uri.getLastPathSegment());
        } else if (shouldUpdateGroupsAndCats) {
            ContentValues group = notification.getGroupValues();
            resolver.update(GROUPS_URI, group,
                            MessageDBSchema.GrpSchema._ID+"="+grpId, null);
        }
                
        int catId = getCategoryId(resolver,grpId,notification.catUri);
        if(catId==-1){
            ContentValues category = notification.getCategoryValues(grpId);
            Log.d(TAG, "posting category");
            Uri uri = resolver.insert(CATEGORIES_URI, category);
            Log.d(TAG, "got uri: "+uri.toString());
            catId = Integer.parseInt(uri.getLastPathSegment());
        } else if (shouldUpdateGroupsAndCats) {
            ContentValues category = notification.getCategoryValues(grpId);
            resolver.update(CATEGORIES_URI, category,
                            MessageDBSchema.CatSchema._ID+"="+catId, null);
        }

        ContentValues message = notification.getMessageValues(catId);
        int msgId = -1;
        Uri msgUri = null;
        if(update){
            //Log.v(TAG,"attempt to update database");
            msgId = getMessageId(resolver,catId);
            if(msgId!=-1){
                Log.v(TAG,"msgId != -1");
                resolver.update(MESSAGES_URI, message,
                                MessageDBSchema.MsgSchema._ID+"="+msgId, null);
                msgUri = Uri.withAppendedPath(MESSAGES_URI, ""+msgId);
            }
        }
        if(!update||msgId==-1){
            //Log.v(TAG,"msgId==-1");
            msgUri = resolver.insert(MESSAGES_URI, message);
            msgId = Integer.parseInt(msgUri.getLastPathSegment());
        }

        wakeupScreen();
        switch(type) {
        case INTERACTIVE:
            showInteractiveNotification(notification, msgId, overwrittenDesc);
            break;
        case PASSIVE:
            showPassiveNotification(notification.context,
                                    notification.messageText,
                                    NOTIFICATION_SHORT_DURATION,
                                    false); // We bleep next line
            break;
        case NONE:
            return msgUri;
        }
        playSoundIfShould(notification.context,notification.mp);
        return msgUri;
    }

    /**
     * The simplest of all <code>postNotification</code> methods. Most
     * people only need this
     *
     * @param notification a <code>ReconNotification</code> value
     * @param type a <code>NotificationType</code> value
     * @return an <code>Uri</code> value
     */
    public static Uri postNotification(ReconNotification notification,
                                       NotificationType type) {
        return postNotification(notification, false, type,true,null);
    }

    /**
     * Post a notification to the message center
     *
     * @param notification notification to post
     * @param update whether to replace a single message
     * @param showInteractiveNotification show as an interactive
     * notification
     * @param overwrittenDesc the text to overwrite the desc on the alert
     */

    public static Uri postNotification(ReconNotification notification,
                                       boolean update,
                                       boolean showInteractiveNotification,
                                       String overwrittenDesc){
        if(overwrittenDesc == null || overwrittenDesc.length() == 0){
            return postNotification(notification, update,
                                    showInteractiveNotification, true);
        }
        return postNotification(notification, update,
                                showInteractiveNotification,
                                true, overwrittenDesc);
    }



    /**
     * Describe <code>postNotification</code> method here.
     *
     * @param notification a <code>ReconNotification</code> value
     * @param update a <code>boolean</code> value
     * @param showInteractiveNotification a <code>boolean</code> is
     * interacitve or not. If no, then NO notification will be shown
     * @param shouldUpdateGroupsAndCats a <code>boolean</code> value
     * @param overwrittenDesc a <code>String</code> value
     * @return an <code>Uri</code> value
     */
    public static Uri postNotification(ReconNotification notification,
                                       boolean update,
                                       boolean showInteractiveNotification,
                                       boolean shouldUpdateGroupsAndCats,
                                       String overwrittenDesc) {
        return postNotification(notification, update,
                                showInteractiveNotification ?
                                NotificationType.INTERACTIVE:
                                NotificationType.NONE,
                                shouldUpdateGroupsAndCats,
                                overwrittenDesc);
    }

    /**
     * Post a notification to the message center
     *
     * @param notification notification to post
sgener     * @param update whether to replace a single message
     * @param showInteractiveNotification show as an interactive
     * notification
     */
    public static Uri postNotification(ReconNotification notification,
                                       boolean update,
                                       boolean showInteractiveNotification) {
        return postNotification(notification, update,
                                showInteractiveNotification, true);
    }


    /**
     * Describe <code>postNotification</code> method here.
     *
     * @param notification a <code>ReconNotification</code> value
     * @param update a <code>boolean</code> value
     * @param showInteractiveNotification a <code>boolean</code> value
     * @param shouldUpdateGroupsAndCats a <code>boolean</code> value
     * @return an <code>Uri</code> value
     */
    public static Uri postNotification(ReconNotification notification,
                                       boolean update,
                                       boolean showInteractiveNotification,
                                       boolean shouldUpdateGroupsAndCats){
        return postNotification(notification,update,
                                showInteractiveNotification,
                                shouldUpdateGroupsAndCats,null);
    }

    private static int getGroupId(ContentResolver resolver,String grpUri){
        int grpId = -1;
        Cursor grpCursor =
            resolver.query(GROUPS_URI, new String[]{GrpSchema._ID},
                           GrpSchema.COL_URI+"='"+grpUri+"'", null, null);
        if(grpCursor.moveToFirst()){
            grpId = grpCursor.getInt(0);
            Log.d(TAG, "group "+grpUri+" already registered: "+grpId);
        } else Log.d(TAG, "group "+grpUri+" not registered");
        grpCursor.close();

        return grpId;
    }

    private static int getCategoryId(ContentResolver resolver,int grpId,
                                     String catUri){
        int catId = -1;
        String catSelect =CatSchema.COL_GROUP_ID+"="+grpId+" AND "+
            CatSchema.COL_URI+"='"+catUri+"'";
        Cursor catCursor = resolver.query(CATEGORIES_URI,
                                          new String[]{CatSchema._ID},
                                          catSelect, null, null);
        if(catCursor.moveToFirst()){
            catId = catCursor.getInt(0);
            Log.d(TAG, "category "+catUri+" already registered: "+catId);
        }  else Log.d(TAG, "category "+catUri+" not registered");
        catCursor.close();

        return catId;
    }
    // get id of first message in category
    private static int getMessageId(ContentResolver resolver, int catId) {
        int msgId = -1;
        String msgSelect = CatSchema._ID+"="+catId;
        Cursor msgCursor = resolver.query(MESSAGES_URI,
                                          new String[]{MsgSchema._ID},
                                          msgSelect, null, null);
        if(msgCursor.moveToFirst()){
            msgId = msgCursor.getInt(0);
        }
        msgCursor.close();

        return msgId;
    }

    public static class ReconNotification{
        Context context;
        String grpUri;
        String grpDesc;
        int grpIcon;
        String catUri;
        String catDesc;
        int catIcon;
        String messageText;
                
        Intent pressIntent = null;
        String pressCaption = null;
        Intent holdIntent = null;
        String holdCaption = null;
        Intent viewerIntent = null;
                
        String extra;
                
        MediaPlayer mp = null;  

        /**
         * Create a notification for the message center
         *
         * @param grpUri unique identifier for the top-level group
         * this notification belongs to
         * @param grpDesc group description
         * @param grpIcon group icon resource id for an image/drawable
         * @param subGrpName identifier for the subgroup, unique for
         * this group
         * @param subGrpDesc subgroup description
         * @param subGrpIcon subgroup icon resource id
         * @param messageText notification text
         */
        public ReconNotification(Context context,String grpUri,String grpDesc,
                                 int grpIcon,String subGrpName,
                                 String subGrpDesc,int subGrpIcon,
                                 String messageText){
            this.context = context;
            this.grpUri = grpUri;
            this.grpDesc = grpDesc;
            this.grpIcon = grpIcon;
            this.catUri = subGrpName;
            this.catDesc = subGrpDesc;
            this.catIcon = subGrpIcon;
            this.messageText = messageText;
                        
            //Init the notification sound 
            mp = MediaPlayer.create(this.context, R.raw.bleep);
        }
        /**
         * Set the action associated with a center button press
         *
         * @param pressIntent intent to launch
         * @param pressCaption user visible intent description
         */
        public void setPressAction(Intent pressIntent,String pressCaption){

            this.pressIntent = pressIntent;
            this.pressCaption = pressCaption;
        }

        /**
         * Set the action associated with a center button long press
         *
         * @param pressIntent intent to launch
         * @param pressCaption user visible intent description
         */
        public void setHoldAction(Intent holdIntent,String holdCaption){

            this.holdIntent = holdIntent;
            this.holdCaption = holdCaption;
        }

        /**
         * Set a custom intent to be launched when subgroup is
         * selected to override the default message viewer
         */
        public void overrideMessageViewer(Intent viewerIntent){

            this.viewerIntent = viewerIntent;
        }
                
        /**
         * Attach extra data to the message
         */
        public void setExtra(String extra){

            this.extra = extra;
        }
                
        ContentValues getGroupValues(){
            ContentValues values = new ContentValues();
            values.put(MessageDBSchema.GrpSchema.COL_URI, grpUri);
            values.put(MessageDBSchema.GrpSchema.COL_APK,
                       context.getPackageName());
            values.put(MessageDBSchema.GrpSchema.COL_DESCRIPTION, grpDesc);
            values.put(MessageDBSchema.GrpSchema.COL_ICON, grpIcon);
            return values;
        }
                
        ContentValues getCategoryValues(int group_id){
            ContentValues values = new ContentValues();
            values.put(MessageDBSchema.CatSchema.COL_GROUP_ID, group_id);
            values.put(MessageDBSchema.CatSchema.COL_URI, catUri);
            values.put(MessageDBSchema.CatSchema.COL_DESCRIPTION, catDesc);
            values.put(MessageDBSchema.CatSchema.COL_ICON, catIcon);
                        
            values.put(MessageDBSchema.CatSchema.COL_PRESS_INTENT,
                       MessageCenterUtils.IntentToBytes(pressIntent));
            values.put(MessageDBSchema.CatSchema.COL_HOLD_INTENT,
                       MessageCenterUtils.IntentToBytes(holdIntent));
            values.put(MessageDBSchema.CatSchema.COL_VIEWER_INTENT,
                       MessageCenterUtils.IntentToBytes(viewerIntent));

            values.put(MessageDBSchema.CatSchema.COL_PRESS_CAPTION,
                       pressCaption);
            values.put(MessageDBSchema.CatSchema.COL_HOLD_CAPTION, holdCaption);
            return values;
        }

        ContentValues getMessageValues(int cat_id){
            ContentValues values = new ContentValues();
            values.put(MessageDBSchema.MsgSchema.COL_CATEGORY_ID, cat_id);
            values.put(MessageDBSchema.MsgSchema.COL_TEXT, messageText);
            values.put(MessageDBSchema.MsgSchema.COL_TIMESTAMP,
                       new Date().getTime());
            //values.put(MessageDBSchema.MsgSchema.COL_PRIORITY, priority.name());
            values.put(MessageDBSchema.MsgSchema.COL_EXTRA, extra);
            values.put(MessageDBSchema.MsgSchema.COL_PROCESSED, 0);
            return values;
        }
    }
        
    /**
     * specified for the Jet platform only
     * get the most recent unread message count
     * @param context
     * @return
     */
    public static int getMostRecentUnreadMessageCount(Context context){
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect =MsgSchema.COL_PROCESSED + " IS NULL OR " +
            MsgSchema.COL_PROCESSED + " =0";
        Cursor msgCursor = contentResolver.query(RECENT_MESSAGES_VIEW_URI,
                                                 new String[]{MsgSchema._ID},
                                                 msgSelect, null, null);
        int count = msgCursor.getCount();
        msgCursor.close();
        return count;
    }
        
    /**
     * show interactive notification
     * @param notif a <code>ReconNotification</code> value
     * @param msgId an <code>int</code> value
     * @param overwrittenDesc a <code>String</code> value
     */
    public static void showInteractiveNotification(ReconNotification notification,
                                                   int msgId,
                                                   String overwrittenDesc){
        Intent i =
            new Intent ("com.reconinstruments.messagecenter.MessageAlert");
        i.putExtra("ViewerIntent",notification.viewerIntent);
        i.putExtra("message_id",msgId);
        if(overwrittenDesc != null && overwrittenDesc.length() > 0){
            i.putExtra("overwritten_desc",overwrittenDesc);
        }
        i.putExtra("group_uri",notification.grpUri);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.context.startActivity(i);
    }

    /**
     * show passive notification
     * @param context
     * @param text
     * @param duration an <code>int</code> value
     */
    public static void showPassiveNotification(Context context, String text,
                                               int duration){
        showPassiveNotification(context, text, duration, true);
    }

    /**
     * Describe <code>showPassiveNotification</code> method here.
     *
     * @param context a <code>Context</code> value
     * @param text a <code>String</code> value
     * @param duration an <code>int</code> value
     * @param bleep a <code>boolean</code> value if should bleep
     */
    public static void showPassiveNotification(Context context, String text,
                                               int duration, boolean bleep) {
        mHandler.removeCallbacks(null); //ensure to remove callbacks
        LayoutInflater inflater =(LayoutInflater)context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout rl =(RelativeLayout) inflater
            .inflate(R.layout.passive_notification, null);
        TextView passiveText = (TextView)rl.findViewById(R.id.passive_text);
        // Typeface semiboldTypeface =
        //     UIUtils.getFontFromRes(context, R.raw.opensans_semibold);
        // passiveText.setTypeface(semiboldTypeface);
        passiveText.setText(text);
        passiveText.setFocusable(true);
        if(duration == 0){
            duration = NOTIFICATION_SHORT_DURATION;
        }
        addView(context, rl, 46, duration);
        if (!bleep) return;
        if (sGenericMp == null) {
            sGenericMp = MediaPlayer.create(context, R.raw.bleep);
        }
        playSoundIfShould(context,sGenericMp);
    }

    private static MediaPlayer sGenericMp= null;
        
    private static void addView(final Context context, final RelativeLayout rl,
                                int height, int duration){
        final WindowManager wm = (WindowManager) context
            .getSystemService(android.content.Context.WINDOW_SERVICE);
        try{
            wm.removeView(rl); //ensure to remove the old view
        }catch(Exception e){
            //do nothing, ignore
        }
        
        
        WindowManager.LayoutParams params = null;
        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT,
                                                height,
                                                WindowManager.LayoutParams
                                                .TYPE_SYSTEM_OVERLAY,
                                                WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                                                WindowManager.LayoutParams
                                                .FLAG_WATCH_OUTSIDE_TOUCH|
                                                WindowManager.LayoutParams
                                                .FLAG_LAYOUT_IN_SCREEN,
                                                PixelFormat.TRANSLUCENT);

        params.dimAmount = 0.7f;
        params.gravity = Gravity.BOTTOM;
        wm.addView(rl, params);
        
        mHandler.postDelayed(new Runnable() {
                public void run() {
                    try{
                        wm.removeView(rl);
                    }catch(Exception e){
                        //do nothing, ignore
                    }
                }
            }, duration * 1000);
    }
    
    private static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId =
            context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    
    private static void playSoundIfShould(Context c, MediaPlayer mp) {
        if (SettingsUtil
            .getCachableSystemIntOrSet(c, SettingsUtil
                                       .SHOULD_NOTIFICATION_SOUNDS,1)==1) {
            mp.start();
        }
    }

    public static void wakeupScreen() {
        if (mScreenMgr == null) {
            mScreenMgr = HUDScreenManager.getInstance();
        }

        if (mScreenMgr != null) {
            mScreenMgr.forceScreenOn(SCREEN_ON_DELAY, false);
        }
    }
}
