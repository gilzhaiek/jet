package com.reconinstruments.dashlauncher.notifications;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NotificationsDatabase extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "notificationdata";
	private static final int DATABASE_VERSION = 2;

	public static final String TABLE_NAME = "notifications";

	
	public static final String KEY_ROWID			= "_id";
	public static final String KEY_NOTIFICATION_ID 	= "notif_id"; // A unique id for the type of notification, 
																		// supplied by the app sending this notification. 
																		// E.g. phone app can discriminate between text and call notifications
	public static final String KEY_DATE 			= "date";			// date added
	public static final String KEY_SINGLETON_PERSISTANT	= "singleton"; 		// should this notification be single permanent instance
	public static final String KEY_DATA				= "data";			// bundle containing intent, intent extras icon, label, text, and values
	
	private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_NAME + " ("
			+ KEY_ROWID				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ KEY_NOTIFICATION_ID 	+ " INTEGER NOT NULL UNIQUE, "
			+ KEY_DATE				+ " DATETIME NOT NULL, "
			+ KEY_SINGLETON_PERSISTANT	+ " BOOLEAN NOT NULL, "
			+ KEY_DATA				+ " BLOB NOT NULL ); ";

	public NotificationsDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(NotificationsDatabase.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}
}
