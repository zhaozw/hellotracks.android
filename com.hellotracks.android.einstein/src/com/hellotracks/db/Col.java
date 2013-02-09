package com.hellotracks.db;

public enum Col {
	TS, LAT, LNG, ALT, HEAD, SPEED, VACC, HACC, SENSOR;

	public static final String DATABASE_NAME = "gpsdb";
	public static final String DATABASE_TABLE = "gps";

	public static String[] names() {
		Col[] values = Col.values();
		String[] names = new String[values.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = values[i].name();
		}
		return names;
	}

}