package com.hellotracks.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.hellotracks.Log;
import com.hellotracks.types.GPS;

public class DbAdapter {

	// Database fields

	private Context context;
	private SQLiteDatabase database;
	private DbHelper dbHelper;

	public DbAdapter(Context context) {
		this.context = context;
	}

	public DbAdapter open() throws SQLException {
		dbHelper = new DbHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}

	public long insertGPS(GPS gps) {
		long start = System.currentTimeMillis();
		ContentValues initialValues = createContentValues(gps);
		long rowId = database.insert(Col.DATABASE_TABLE, null, initialValues);
		long time = System.currentTimeMillis() - start;
		Log.d("inserting gps in " + time + " ms");
		return rowId;
	}

	public GPS selectLastGPS() {
		try {
			Cursor cursor = selectAllGPS();
			if (cursor.moveToLast()) {
				GPS gps = new GPS();
				gps.ts = cursor.getLong(Col.TS.ordinal());
				gps.lat = cursor.getDouble(Col.LAT.ordinal());
				gps.lng = cursor.getDouble(Col.LNG.ordinal());
				gps.alt = cursor.getInt(Col.ALT.ordinal());
				gps.head = cursor.getInt(Col.HEAD.ordinal());
				gps.speed = cursor.getInt(Col.SPEED.ordinal());
				gps.vacc = cursor.getInt(Col.VACC.ordinal());
				gps.hacc = cursor.getInt(Col.HACC.ordinal());
				gps.sensor = cursor.getInt(Col.SENSOR.ordinal());
			}
		} catch (Exception exc) {
		}
		return null;
	}

	public boolean deleteGPS(long to) {
		long start = System.currentTimeMillis();
		boolean result = database.delete(Col.DATABASE_TABLE, Col.TS.name() + "<=" + to, null) > 0;
		long time = System.currentTimeMillis() - start;
		Log.d("deleting gps in " + time + " ms");
		return result;
	}

	public Cursor selectAllGPS() {
		return database.query(Col.DATABASE_TABLE, Col.names(), null, null,
				null, null, Col.TS.name());
	}

	public int count() {
		return (int) DatabaseUtils
				.queryNumEntries(database, Col.DATABASE_TABLE);
	}

	public GPS[] selectGPS(int max) {
		try {
			long start = System.currentTimeMillis();
			Cursor cursor = selectAllGPS();

			if (cursor.getCount() == 0)
				return new GPS[0];

			GPS[] data = new GPS[Math.min(cursor.getCount(), max)];

			int i = 0;
			cursor.moveToFirst();
			do {
				GPS gps = new GPS();
				gps.ts = cursor.getLong(Col.TS.ordinal());
				gps.lat = cursor.getDouble(Col.LAT.ordinal());
				gps.lng = cursor.getDouble(Col.LNG.ordinal());
				gps.alt = cursor.getInt(Col.ALT.ordinal());
				gps.head = cursor.getInt(Col.HEAD.ordinal());
				gps.speed = cursor.getInt(Col.SPEED.ordinal());
				gps.vacc = cursor.getInt(Col.VACC.ordinal());
				gps.hacc = cursor.getInt(Col.HACC.ordinal());
				gps.sensor = cursor.getInt(Col.SENSOR.ordinal());
				data[i++] = gps;
				cursor.moveToNext();
			} while (i < max && !cursor.isAfterLast());
			long time = System.currentTimeMillis() - start;
			Log.d("selecting gps in " + time + " ms | count=" + data.length);
			return data;
		} catch (Exception exc) {
			Log.w(exc);
			return new GPS[0];
		}
	}

	private ContentValues createContentValues(GPS gps) {
		ContentValues values = new ContentValues();
		values.put(Col.TS.name(), gps.ts);
		values.put(Col.LAT.name(), gps.lat);
		values.put(Col.LNG.name(), gps.lng);
		values.put(Col.ALT.name(), gps.alt);
		values.put(Col.HEAD.name(), gps.head);
		values.put(Col.SPEED.name(), gps.speed);
		values.put(Col.VACC.name(), gps.vacc);
		values.put(Col.HACC.name(), gps.hacc);
		values.put(Col.SENSOR.name(), gps.sensor);
		return values;
	}
}
