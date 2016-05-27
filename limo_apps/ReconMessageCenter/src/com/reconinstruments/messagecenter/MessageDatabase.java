package com.reconinstruments.messagecenter;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.reconinstruments.messagecenter.MessageDBSchema.CatSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;

public class MessageDatabase extends SQLiteOpenHelper {
	private static final String TAG = MessageDatabase.class.getSimpleName();

	public static final String DATABASE_NAME = "messagecenter";
	public static final int DATABASE_VERSION = 35;

	private Context mContext = null;
	SQLiteDatabase db;

	public MessageDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;

		try {
			db = getWritableDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");

		super.onOpen(db);

		// exec statements stored in code
		for (String SQL : sqlStatements) {
			db.execSQL(SQL);
		}
		// extra statements from external files
		execSQLFromAsset(db, "views.sql");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade");

		execSQLFromAsset(db, "dropall.sql");
		onCreate(db);
	}

	public void execSQLFromAsset(SQLiteDatabase db, String file) {
		try {
			InputStream sqlAsset = this.mContext.getAssets().open(file);
			String[] sql = convertStreamToStrings(sqlAsset);

			for (String statement : sql) {
				if (statement.trim().length() > 0) {
					try {
						db.execSQL(statement);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] convertStreamToStrings(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		String string = s.hasNext() ? s.next() : "";
		return string.split(";");
	}

	public static final String SQL_CREATE_GROUPS = "CREATE TABLE "
			+ GrpSchema.TABLE + " (" + GrpSchema._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + GrpSchema.COL_URI
			+ " TEXT NOT NULL, " + GrpSchema.COL_APK + " TEXT NOT NULL, "
			+ GrpSchema.COL_ICON + " INTEGER, " + GrpSchema.COL_DESCRIPTION
			+ " TEXT, " + "UNIQUE(" + GrpSchema.COL_URI + ")" +

			" )";

	public static final String SQL_CREATE_CATEGORIES = "CREATE TABLE "
			+ CatSchema.TABLE + " (" + CatSchema._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + CatSchema.COL_URI
			+ " TEXT NOT NULL, " + CatSchema.COL_GROUP_ID
			+ " INTEGER NOT NULL, " + CatSchema.COL_ICON + " INTEGER, "
			+ CatSchema.COL_AGGREGATABLE + " NUMERIC, "
			+ CatSchema.COL_DESCRIPTION + " TEXT, " +

			CatSchema.COL_PRESS_INTENT + " BLOB, " + CatSchema.COL_HOLD_INTENT
			+ " BLOB, " + CatSchema.COL_VIEWER_INTENT + " BLOB, "
			+ CatSchema.COL_PRESS_CAPTION + " INTEGER, "
			+ CatSchema.COL_HOLD_CAPTION + " INTEGER, " + "UNIQUE("
			+ CatSchema.COL_GROUP_ID + ", " + CatSchema.COL_URI + ") "
			+ "FOREIGN KEY(" + CatSchema.COL_GROUP_ID + ") REFERENCES "
			+ GrpSchema.TABLE + "(" + GrpSchema._ID + ") ON DELETE CASCADE"
			+ " )";

	public static final String SQL_CREATE_MESSAGES = "CREATE TABLE "
			+ MsgSchema.TABLE + " (" + MsgSchema._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ MsgSchema.COL_CATEGORY_ID + " INTEGER NOT NULL, "
			+ MsgSchema.COL_PRIORITY + " INTEGER, " + MsgSchema.COL_TEXT
			+ " TEXT, " + MsgSchema.COL_TIMESTAMP + " INTEGER, "
			+ MsgSchema.COL_ICON + " INTEGER, " + MsgSchema.COL_PROCESSED
			+ " INTEGER, " + MsgSchema.COL_EXTRA + " TEXT, " + "FOREIGN KEY("
			+ MsgSchema.COL_CATEGORY_ID + ") REFERENCES " + CatSchema.TABLE
			+ "(" + CatSchema._ID + ") ON DELETE CASCADE" + " )";

	public static final String[] ALL_TABLES = { MsgSchema.TABLE,
			CatSchema.TABLE, GrpSchema.TABLE };
	public static final String[] ALL_TRIGGERS = { "del_group", "del_category" };

	// because ON DELETE CASCADE doesn't work (android sqlite problem)
	public static final String SQL_CREATE_TRIGGER_DELETE_GROUP = "CREATE TRIGGER del_group "
			+ "BEFORE DELETE ON "
			+ GrpSchema.TABLE
			+ " "
			+ "FOR EACH ROW BEGIN "
			+ "DELETE FROM "
			+ CatSchema.TABLE
			+ " WHERE " + CatSchema.COL_GROUP_ID + " = OLD._id; " + "END;";
	public static final String SQL_CREATE_TRIGGER_DELETE_CATEGORY = "CREATE TRIGGER del_category "
			+ "BEFORE DELETE ON "
			+ CatSchema.TABLE
			+ " "
			+ "FOR EACH ROW BEGIN "
			+ "DELETE FROM "
			+ MsgSchema.TABLE
			+ " WHERE " + MsgSchema.COL_CATEGORY_ID + " = OLD._id; " + "END;";

	// the following are executed when the database is created
	public static final String[] sqlStatements = { "PRAGMA foreign_keys=ON;",
			"PRAGMA recursive_triggers=ON;", SQL_CREATE_GROUPS,
			SQL_CREATE_CATEGORIES, SQL_CREATE_MESSAGES,
			SQL_CREATE_TRIGGER_DELETE_GROUP, SQL_CREATE_TRIGGER_DELETE_CATEGORY };
}
