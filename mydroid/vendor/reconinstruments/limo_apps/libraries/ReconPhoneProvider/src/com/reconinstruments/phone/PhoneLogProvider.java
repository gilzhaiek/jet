package com.reconinstruments.phone;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author Bryan Stern
 * Database wrapper & Content Provider for call history
 */


public class PhoneLogProvider extends ContentProvider {

	public static final String PROVIDER_NAME = "com.reconinstruments.phone";
	public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME);
	
	private static final int URI_CALLS = 0;
	private static final int URI_SMS = 1;
	
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TYPE = "type";
	public static final String KEY_SOURCE = "source";
	public static final String KEY_CONTACT = "contact";
	public static final String KEY_DATE = "date";
	public static final String KEY_BODY = "body";
	public static final String KEY_MISSED = "missed";
	public static final String KEY_INCOMING = "incoming";
	public static final String KEY_NOTIF_ID = "notif_id"; // The id of the notification posted for this call history
	
	public static final int TYPE_SMS = 1;
	public static final int TYPE_CALL = 2;
	
	private static final String DATABASE_TABLE = "recon_phone";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	
	private static final UriMatcher uriMatcher;
	static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "calls", URI_CALLS);
        uriMatcher.addURI(PROVIDER_NAME, "sms", URI_SMS);      
    }

	public boolean onCreate() {
		Context context = getContext();
	    dbHelper = new DatabaseHelper(context);
	    try {
	    	database = dbHelper.getWritableDatabase();
	    } catch(Exception e) {
	    	Log.e("PhoneLogProvider", e.toString());
	    	return false;
	    }
	    return (database == null)? false:true;
	}
	
	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		int count = database.delete(DATABASE_TABLE, whereClause, whereArgs);

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		// ---get all calls---
		case URI_CALLS:
			return "vnd.android.cursor.dir/vnd.reconinstruments.calls";
			// ---get all sms---
		case URI_SMS:
			return "vnd.android.cursor.dir/vnd.reconinstruments.sms";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d("PhoneCache", values.toString());
		
		// ---add a new record---
		long rowID = database.insert(DATABASE_TABLE, null, values);

		// ---if added successfully---
		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);

		if (uriMatcher.match(uri) == URI_CALLS)
			sqlBuilder.appendWhere(KEY_TYPE + " = " + TYPE_CALL);
		else if (uriMatcher.match(uri) == URI_SMS)
			sqlBuilder.appendWhere(KEY_TYPE + " = " + TYPE_SMS);

		if (sortOrder == null || sortOrder == "")
			sortOrder = KEY_DATE + " DESC";

		Cursor c = sqlBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

		// ---register to watch a content URI for changes---
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		int count = 0;
		
		count = database.update(DATABASE_TABLE, arg1, arg2, arg3);
		getContext().getContentResolver().notifyChange(CONTENT_URI, null);
		
		return count;
	}
	
	public static ContentValues createContentValues(int type, String source, String contact, long date, String body, boolean missed, boolean incoming, int notification_id) {
		ContentValues values = new ContentValues();
		values.put(KEY_TYPE, type);
		values.put(KEY_SOURCE, source);
		values.put(KEY_CONTACT, contact);
		values.put(KEY_DATE, date);
		values.put(KEY_BODY, body);
		values.put(KEY_MISSED, (missed ? 1 : 0));
		values.put(KEY_INCOMING, (incoming ? 1 : 0));
		values.put(KEY_NOTIF_ID, notification_id);
		return values;
	}
	
	public static ArrayList<Bundle> cursorToBundleList(Cursor c) {
		ArrayList<Bundle> bundleList = new ArrayList<Bundle>(c.getCount());
		if(c.moveToFirst()) {
			do {
				Bundle b = new Bundle();
				b.putInt("id", c.getInt(c.getColumnIndex(KEY_ROWID)));
				b.putInt("type", c.getInt(c.getColumnIndex(KEY_TYPE)));
				b.putString("source", c.getString(c.getColumnIndex(KEY_SOURCE)));
				b.putString("contact", c.getString(c.getColumnIndex(KEY_CONTACT)));
				b.putLong("date", c.getLong(c.getColumnIndex(KEY_DATE)));
				b.putString("body", c.getString(c.getColumnIndex(KEY_BODY)));
				b.putBoolean("missed", c.getInt(c.getColumnIndex(KEY_MISSED)) == 1);
				b.putBoolean("replied", c.getInt(c.getColumnIndex(KEY_INCOMING)) == 1);
				b.putInt(KEY_NOTIF_ID, c.getInt(c.getColumnIndex(KEY_NOTIF_ID)));
				Uri _uri = ContentUris.withAppendedId(CONTENT_URI, c.getInt(c.getColumnIndex(KEY_ROWID)));
				b.putString("uri", _uri.toString());
				
				bundleList.add(b);
			} while (c.moveToNext());
		}
		return bundleList;
	}
	
	/*
	 * Database Helper Class
	 */
	class DatabaseHelper extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "applicationdata";
		private static final int DATABASE_VERSION = 5;
		private static final String TABLE_NAME = "recon_phone";
		
		private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
				KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				KEY_TYPE + " INT NOT NULL, " +
				KEY_SOURCE + " TEXT NOT NULL, " +
				KEY_CONTACT + " TEXT, " +
				KEY_DATE + " DATETIME NOT NULL, " +
				KEY_BODY + " TEXT, " +
				KEY_MISSED + " INT, " +
				KEY_INCOMING + " INT, " + 
				KEY_NOTIF_ID + " INT );";
		
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(DatabaseHelper.class.getName(),
					"Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
		
	}

}