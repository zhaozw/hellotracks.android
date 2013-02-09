package com.hellotracks.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE = "CREATE TABLE "
			+ Col.DATABASE_TABLE
			+ " (ts INTEGER PRIMARY KEY, "
			+ "lat REAL, lng REAL, alt INTEGER, head INTEGER, speed INTEGER, vacc INTEGER, hacc INTEGER, sensor INTEGER);";

	public DbHelper(Context context) {
		super(context, Col.DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		database.execSQL("DROP TABLE IF EXISTS " + Col.DATABASE_TABLE);
		onCreate(database);
	}
}
