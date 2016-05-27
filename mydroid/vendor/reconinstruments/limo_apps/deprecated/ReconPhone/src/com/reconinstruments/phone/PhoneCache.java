package com.reconinstruments.phone;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * @author bryan stern
 * Database wrapper for call history
 */

public class PhoneCache {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_TYPE = "type";
	public static final String KEY_SOURCE = "source";
	public static final String KEY_CONTACT = "contact";
	public static final String KEY_DATE = "date";
	public static final String KEY_BODY = "body";
	public static final String KEY_MISSED = "missed";
	private static final String DATABASE_TABLE = "todo";
	private Context context;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	
	private static PhoneCache mInstance = null;
	
	private PhoneCache(Context c) {
		context = c;
		this.open();
	}

	/** Returns the instance of PhoneCache. */
	public static synchronized PhoneCache getInstance(Context c) {
		if (mInstance == null) {
			mInstance = new PhoneCache(c);
		}
		return mInstance;
	}

	public PhoneCache open() throws SQLException {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}
	
	public void addSMS(String source, String contact, String body) {
		Date now = new java.util.Date();
		ContentValues initialValues = createContentValues(PhoneCacheBundle.TYPE_SMS, source, contact, now.getTime(), body, false);

		database.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public void addCall(String source, String contact, boolean missedCall) {
		Date now = new java.util.Date();
		ContentValues initialValues = createContentValues(PhoneCacheBundle.TYPE_CALL, source, contact, now.getTime(), "", missedCall);

		database.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public PhoneCacheBundle[] getRecentCalls(int max) {
		String[] fieldNames = new String[] { KEY_ROWID, KEY_TYPE, KEY_SOURCE, KEY_CONTACT, KEY_DATE, KEY_MISSED };
		
		Cursor c = database.query(DATABASE_TABLE, 
				fieldNames, 
				KEY_TYPE + "="+ PhoneCacheBundle.TYPE_CALL, null, null,
				null, KEY_DATE+" DESC", Integer.toString(max));
		
		PhoneCacheBundle[] bundle = new PhoneCacheBundle[c.getCount()];
		
		for(int i=0; i<c.getCount(); i++) {
			c.moveToPosition(i);
			
			Date d = new Date(c.getLong(4));
			Boolean missed = (c.getInt(5) == 1);
			
			bundle[i] = new PhoneCacheBundle(c.getInt(1), c.getString(2), c.getString(3), missed, d);
		}
		
		return bundle;
	}
	
	public PhoneCacheBundle[] getRecentSMS(int max) {
		String[] fieldNames = new String[] { KEY_ROWID, KEY_TYPE, KEY_SOURCE, KEY_CONTACT, KEY_DATE, KEY_BODY };
		
		Cursor c = database.query(DATABASE_TABLE, 
				fieldNames, 
				KEY_TYPE + "="+ PhoneCacheBundle.TYPE_SMS, null, null,
				null, KEY_DATE+" DESC", Integer.toString(max));
		
		PhoneCacheBundle[] bundle = new PhoneCacheBundle[c.getCount()];
		
		for(int i=0; i<c.getCount(); i++) {
			c.moveToPosition(i);
			
			Date d = new Date(c.getLong(4));
			
			bundle[i] = new PhoneCacheBundle(c.getInt(1), c.getString(2), c.getString(3), c.getString(5), d);
		}
		
		return bundle;
	}
	
	private ContentValues createContentValues(int type, String source, String contact, long date, String body, boolean missed) {
		ContentValues values = new ContentValues();
		values.put(KEY_TYPE, type);
		values.put(KEY_SOURCE, source);
		values.put(KEY_CONTACT, contact);
		values.put(KEY_DATE, date);
		values.put(KEY_BODY, body);
		values.put(KEY_MISSED, (missed ? 1 : 0));
		return values;
	}
	
	/*
	 * Database Helper Class
	 */
	class DatabaseHelper extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "applicationdata";
		private static final int DATABASE_VERSION = 1;
		
		private static final String DATABASE_CREATE = "create table todo (_id integer primary key autoincrement, " +
				"type int not null, " +
				"source text not null, " +
				"contact text, " +
				"date datetime not null, " +
				"body text, " +
				"missed int);";
		
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
			db.execSQL("DROP TABLE IF EXISTS todo");
			onCreate(db);
		}
		
	}

}