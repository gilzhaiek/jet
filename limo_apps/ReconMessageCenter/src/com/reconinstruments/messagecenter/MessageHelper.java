//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.messagecenter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.util.Pair;

import com.reconinstruments.messagecenter.MessageDBSchema;
import com.reconinstruments.messagecenter.MessagesProvider;
import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;
import com.reconinstruments.utils.MessageCenterUtils;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.MessageCenterUtils;

/**
 * <code>MessageHelper</code> contains helper functions to construct
 * Group, Category and Message Objects given their index from the
 * database It essentially provides an object access interface to the
 * database
 */
public class MessageHelper {
    private static final String TAG = MessageHelper.class.getSimpleName();

    public static final String[] CATEGORIES_VIEW_ALL_FIELDS = new String[] {
        CatSchema._ID, CatSchema.COL_DESCRIPTION, CatSchema.COL_ICON,
        GrpSchema.COL_APK, MsgSchema.COL_TEXT, MsgSchema.COL_TIMESTAMP,
        "UnreadCount", CatSchema.COL_VIEWER_INTENT, "GroupUri" };

    public static class CategoryViewData {
        public CategoryViewData(Context context, Cursor cursor) {
            catId = cursor.getInt(0);
            catDesc = cursor.getString(1);
            catIcon = cursor.getInt(2);
            apk = cursor.getString(3);
            lastMsgText = cursor.getString(4);
            lastMsgDate = new Date(cursor.getLong(5));
            unrdCount = cursor.getInt(6);
            viewIntent = MessageCenterUtils.BytesToIntent(cursor.getBlob(7));
            grpUri = cursor.getString(8);
            if (grpUri.equals("com.reconinstruments.stats")) {
                catDesc = ConversionUtil
                    .convertUnitString(
                                       catDesc,
                                       SettingsUtil.getUnits(context) == SettingsUtil.RECON_UINTS_METRIC);
            }
        }

        public int catId;
        public String catDesc;
        public int catIcon;
        public String apk;

        public String lastMsgText;
        public Date lastMsgDate;
        public int unrdCount;

        public Intent viewIntent;
        public String grpUri;
    }

    public static final String[] GROUPS_VIEW_ALL_FIELDS = new String[] {
        GrpSchema._ID, GrpSchema.COL_APK, GrpSchema.COL_DESCRIPTION,
        GrpSchema.COL_ICON, "LastMessage" + MsgSchema.COL_TEXT,
        "LastMessage" + MsgSchema.COL_TIMESTAMP, "UnreadCount" };

    public static class GroupViewData {
        public GroupViewData(Cursor cursor) {
            grpId = cursor.getInt(0);
            apk = cursor.getString(1);
            grpDesc = cursor.getString(2);
            grpIcon = cursor.getInt(3);

            lastMsgText = cursor.getString(4);
            lastMsgDate = new Date(cursor.getLong(5));
            unrdCount = cursor.getInt(6);
        }

        public int grpId;
        public String apk;
        public String grpDesc;
        public int grpIcon;

        public String lastMsgText;
        public Date lastMsgDate;
        public int unrdCount;
    }

    public static final String[] MESSAGES_ALL_FIELDS = new String[] {
        MsgSchema._ID, MsgSchema.COL_TIMESTAMP, MsgSchema.COL_TEXT,
        MsgSchema.COL_PROCESSED, CatSchema.COL_DESCRIPTION,
        CatSchema.COL_PRESS_INTENT, CatSchema.COL_PRESS_CAPTION,
        MsgSchema.COL_CATEGORY_ID, CatSchema.COL_GROUP_ID };

    public static class MessageViewData {
        public MessageViewData(Cursor cursor) {
            id = cursor.getInt(0);
            date = new Date(cursor.getLong(1));
            text = cursor.getString(2);
            processed = cursor.getInt(3) == 1;
            catDesc = cursor.getString(4);
            pressIntent = MessageCenterUtils.BytesToIntent(cursor.getBlob(5));
            pressCaption = cursor.getString(6);
            catId = cursor.getInt(7);
            grpId = cursor.getInt(8);
        }

        public int id;
        public Date date;
        public String text;
        public boolean processed;
        public String catDesc;
        public Intent pressIntent;
        public String pressCaption;
        public int catId;
        public int grpId;
    }

    public static CursorLoader getCategoriesByGroupId(Context context,
                                                      int groupId) {
        CursorLoader cursorLoader = null;
        String catSelect = MessageDBSchema.CatSchema.COL_GROUP_ID + " = "
            + groupId;
        String order = "Timestamp DESC";

        cursorLoader = new CursorLoader(context,
                                        MessagesProvider.CATEGORIES_VIEW_URI,
                                        CATEGORIES_VIEW_ALL_FIELDS, catSelect, null, order);
        return cursorLoader;
    }

    public static GroupViewData getGroupById(Context context, int id) {
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = GrpSchema._ID + " = " + id;
        Cursor cursor = contentResolver.query(
                MessagesProvider.GROUPS_VIEW_URI,
                GROUPS_VIEW_ALL_FIELDS,
                msgSelect, null, null);
        GroupViewData group = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();
            group = new GroupViewData(cursor);
        }
        cursor.close();
        return group;
    }

    public static CursorLoader getGroups(Context context) {
        CursorLoader cursorLoader = null;
        String order = "LastMessageTimestamp DESC";
        cursorLoader = new CursorLoader(context,
                                        MessagesProvider.GROUPS_VIEW_URI, GROUPS_VIEW_ALL_FIELDS, null,
                                        null, order);
        return cursorLoader;
    }

    public static void clearAll(Context context) {
        context.getContentResolver().delete(MessagesProvider.GROUPS_URI, null,
                                            null);
        context.getContentResolver().delete(MessagesProvider.CATEGORIES_URI,
                                            null, null);
        context.getContentResolver().delete(MessagesProvider.MESSAGES_URI,
                                            null, null);
    }

    public static MessageViewData[] getMessagesByCategoryId(Context context,
                                                            int categoryId) {
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = MsgSchema.COL_CATEGORY_ID + " = " + categoryId;
        Cursor cursor = contentResolver.query(
                                              MessagesProvider.MESSAGES_VIEW_URI, MESSAGES_ALL_FIELDS,
                                              msgSelect, null, null);
        MessageViewData[] messages = new MessageViewData[cursor.getCount()];
        for (int i = 0; i < messages.length; i++) {
            cursor.moveToNext();
            messages[i] = new MessageViewData(cursor);
        }
        cursor.close();
        return messages;
    }

    public static MessageViewData getMessagesById(Context context, int id) {
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = MsgSchema._ID + " = " + id;
        Cursor cursor = contentResolver.query(
                                              MessagesProvider.MESSAGES_VIEW_URI, MESSAGES_ALL_FIELDS,
                                              msgSelect, null, null);
        MessageViewData message = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();
            message = new MessageViewData(cursor);
        }
        cursor.close();
        return message;
    }

    public static CategoryViewData getCategoryById(Context context, int id) {
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = CatSchema._ID + " = " + id;
        Cursor cursor = contentResolver.query(
                MessagesProvider.CATEGORIES_VIEW_URI,
                CATEGORIES_VIEW_ALL_FIELDS,
                msgSelect, null, null);
        CategoryViewData category = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();
            category = new CategoryViewData(context, cursor);
        }
        cursor.close();
        return category;
    }

    public static void messageProcessed(Context context, int id) {
        ContentValues values = new ContentValues();
        values.put(MessageDBSchema.MsgSchema.COL_PROCESSED, 1);
        String msgSelect = "_id=" + id;
        context.getContentResolver().update(MessagesProvider.MESSAGES_URI,
                                            values, msgSelect, null);
    }
    
    /**
     * specified for the Jet platform only
     * get the most recent messages grouped by group and category, order by timestamp desc
     */
    public static MessageViewData[] getRecentMessages(Context context){
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                                              MessagesProvider.RECENT_MESSAGES_VIEW_URI, MESSAGES_ALL_FIELDS,
                                              null, null, null);
        List<MessageViewData> msgs = new ArrayList<MessageViewData>();
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();
            MessageViewData data = new MessageViewData(cursor);
            msgs.add(data);
        }
        cursor.close();
        return msgs.toArray(new MessageViewData[msgs.size()]);
    }
    
    /**
     * specified for the Jet platform only
     * get all of the message with a specified group and category description
     * the size is the unread count under a group and description
     */
    public static MessageViewData[] getMessagesByGroupIdAndCategoryDescription(Context context, int groupId, String description){
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = MessageDBSchema.CatSchema.COL_GROUP_ID + " = " + groupId + " AND " + MessageDBSchema.CatSchema.COL_DESCRIPTION + " = '" + description + "'";
        String order = "Timestamp DESC";
        Cursor cursor = contentResolver.query(
                                              MessagesProvider.MESSAGES_VIEW_URI, MESSAGES_ALL_FIELDS,
                                              msgSelect, null, order);
        MessageViewData[] messages = new MessageViewData[cursor.getCount()];
        for (int i = 0; i < messages.length; i++) {
            cursor.moveToNext();
            messages[i] = new MessageViewData(cursor);
        }
        cursor.close();
        return messages;
    }
    
    /**
     * specified for the Jet platform only, used for filtering notification
     * get all of the message under a specified group
     */
    public static MessageViewData[] getMessagesByGroupId(Context context, int groupId){
        ContentResolver contentResolver = context.getContentResolver();
        String msgSelect = null;
        if(groupId > 0){
            msgSelect = MessageDBSchema.CatSchema.COL_GROUP_ID + " = " + groupId + " GROUP BY GroupID, Description";
        }else{
            msgSelect = MessageDBSchema.CatSchema.COL_GROUP_ID + " > " + 0 + " GROUP BY GroupID, Description";
        }
        String order = "Timestamp DESC";
        Cursor cursor = contentResolver.query(
                                              MessagesProvider.MESSAGES_VIEW_URI, MESSAGES_ALL_FIELDS,
                                              msgSelect, null, order);
        MessageViewData[] messages = new MessageViewData[cursor.getCount()];
        for (int i = 0; i < messages.length; i++) {
            cursor.moveToNext();
            messages[i] = new MessageViewData(cursor);
        }
        cursor.close();
        return messages;
    }
    
    /**
     * specified for the Jet platform only
     * get all of the group info
     * @param context
     * @return
     */
    public static GroupViewData[] getGroupView(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String order = "LastMessageTimestamp DESC";
        Cursor cursor = contentResolver.query(
                                              MessagesProvider.GROUPS_VIEW_URI, GROUPS_VIEW_ALL_FIELDS,
                                              null, null, order);
        GroupViewData[] groups = new GroupViewData[cursor.getCount()];
        for (int i = 0; i < groups.length; i++) {
            cursor.moveToNext();
            groups[i] = new GroupViewData(cursor);
        }
        cursor.close();
        return groups;
    }
}
