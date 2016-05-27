package com.reconinstruments.dashlauncher.notifications;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class NotificationsProvider extends ContentProvider {

	private static final String TAG = "NotificationsProvider";

	public static final String AUTHORITY = "com.reconinstruments.dashlauncher.notifications";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri NOTIFICATION_URI = Uri.parse("content://" + AUTHORITY + "/" + NotificationsDatabase.TABLE_NAME );

	private static final int URI_NOTIFICATIONS = 1;
	
	private static SQLiteDatabase database;
	private static NotificationsDatabase dbHelper;

	private static final UriMatcher uriMatcher;
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, NotificationsDatabase.TABLE_NAME, URI_NOTIFICATIONS);
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.v(TAG, "delete() called");

		int count;

		switch (uriMatcher.match(uri)) {
		case URI_NOTIFICATIONS:
			count = database.delete(NotificationsDatabase.TABLE_NAME, selection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
			case URI_NOTIFICATIONS :
				return "vnd.android.cursor.item/vnd.reconinstruments.notifications";
			default :
				throw new IllegalArgumentException("Unknown URI "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//Log.v(TAG, "insert() called");

		// Check to see if value URI
		switch (uriMatcher.match(uri)){
			case URI_NOTIFICATIONS :
				if (values == null)
					throw new IllegalArgumentException("Value is null");
				break;
			default :
				throw new IllegalArgumentException("Unknown URI " + uri); 
		}	

		// Insert row or replace if conflict
		long rowId = database.replace(NotificationsDatabase.TABLE_NAME, null, values);
		
		// Notify all listeners
		getContext().getContentResolver().notifyChange(uri, null);
		
		if (rowId > 0) {
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		Log.v(TAG, "onCreate() called");

		// Attempt to initialize the database
		dbHelper = new NotificationsDatabase(getContext());
		
		try {
			database = dbHelper.getWritableDatabase();
		} catch(Exception e) {
			Log.e(TAG, e.toString());
			return false;
		}
		
		return database != null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		Log.v(TAG, "query() called");

		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(NotificationsDatabase.TABLE_NAME);

		if (sortOrder == null || sortOrder == "")
			sortOrder = NotificationsDatabase.KEY_DATE + " DESC";

		Cursor c = sqlBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

		// ---register to watch a content URI for changes---
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		
		int count;
		switch (uriMatcher.match(uri)) {
		case URI_NOTIFICATIONS:
			count = database.update(NotificationsDatabase.TABLE_NAME, values, selection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

}
