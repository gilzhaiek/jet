package com.reconinstruments.messagecenter;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MessagePriority;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;

public class MessagesProvider extends ContentProvider {

	private static final String TAG = "NotificationsProvider";

	public static final String AUTHORITY = "com.reconinstruments.messagecenter";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri MESSAGES_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + MsgSchema.TABLE);
	public static final Uri CATEGORIES_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + CatSchema.TABLE);
	public static final Uri GROUPS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + GrpSchema.TABLE);

	public static final Uri MESSAGES_VIEW_URI = Uri.parse("content://"
			+ AUTHORITY + "/messages_view");
	public static final Uri CATEGORIES_VIEW_URI = Uri.parse("content://"
			+ AUTHORITY + "/categories_view");
	public static final Uri GROUPS_VIEW_URI = Uri.parse("content://"
			+ AUTHORITY + "/groups_view");

	//specified for the Jet platform only
   public static final Uri RECENT_MESSAGES_VIEW_URI = Uri.parse("content://"
            + AUTHORITY + "/recent_messages_by_group_and_category");
	
	
	private static final int URI_MESSAGES = 1;
	private static final int URI_CATEGORIES = 2;
	private static final int URI_GROUPS = 3;

	private static SQLiteDatabase database;
	private static MessageDatabase dbHelper;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, MsgSchema.TABLE, URI_MESSAGES);
		uriMatcher.addURI(AUTHORITY, CatSchema.TABLE, URI_CATEGORIES);
		uriMatcher.addURI(AUTHORITY, GrpSchema.TABLE, URI_GROUPS);
	}

	@Override
	public boolean onCreate() {
		Log.d(TAG, "onCreate() called");

		// Attempt to initialize the database
		dbHelper = new MessageDatabase(getContext());

		try {
			database = dbHelper.getWritableDatabase();
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			return false;
		}

		return database != null;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case -1:
			throw new IllegalArgumentException("Unknown URI " + uri);
		default:
			return "vnd.android.cursor.item/vnd.reconinstruments.notifications";
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// Log.d(TAG, "insert() called, uri: "+uri);

		String table = uri.getLastPathSegment();

		// Insert row or replace if conflict
		long rowId = database.replace(table, null, values);

		// Notify all listeners
		getContext().getContentResolver().notifyChange(uri, null);

		// because all three of these URIs use data from all three tables,
		// notify each one
		getContext().getContentResolver().notifyChange(MESSAGES_VIEW_URI, null);
		getContext().getContentResolver().notifyChange(CATEGORIES_VIEW_URI,
				null);
		getContext().getContentResolver().notifyChange(GROUPS_VIEW_URI, null);
		
        getContext().getContentResolver().notifyChange(RECENT_MESSAGES_VIEW_URI, null);
        
		if (rowId > 0) {
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Log.d(TAG, "delete() called, uri: "+uri);

		String table = uri.getLastPathSegment();

		int count = database.delete(table, selection, selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);

		getContext().getContentResolver().notifyChange(MESSAGES_VIEW_URI, null);
		getContext().getContentResolver().notifyChange(CATEGORIES_VIEW_URI,
				null);
		getContext().getContentResolver().notifyChange(GROUPS_VIEW_URI, null);
		
        getContext().getContentResolver().notifyChange(RECENT_MESSAGES_VIEW_URI, null);

		return count;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Log.d(TAG, "query() called, uri: "+uri);
		// Log.d(TAG, "selection: "+selection);

		String table = uri.getLastPathSegment();

		Cursor cursor = database.query(table, projection, selection,
				selectionArgs, null, null, sortOrder);

		// ---register to watch a content URI for changes---
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		// Log.d(TAG, "returning "+cursor.getCount()+" items");

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// Log.d(TAG, "update() called, uri: "+uri);

		String table = uri.getLastPathSegment();

		int count = database.update(table, values, selection, selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);

		// because all three of these URIs use data from all three tables,
		// notify each one
		getContext().getContentResolver().notifyChange(MESSAGES_VIEW_URI, null);
		getContext().getContentResolver().notifyChange(CATEGORIES_VIEW_URI,
				null);
		getContext().getContentResolver().notifyChange(GROUPS_VIEW_URI, null);
		
        getContext().getContentResolver().notifyChange(RECENT_MESSAGES_VIEW_URI, null);

		return count;
	}
}
