package com.hellotracks.types;

import java.util.Date;

public class Waypoint extends LatLng {

	private static final long serialVersionUID = 7192915427013182263L;

	public long ts = System.currentTimeMillis();

	public Waypoint() {

	}

	public Waypoint(long ts, double lat, double lng) {
		setLatitude(lat);
		setLongitude(lng);
		this.ts = ts;
	}

	public long getTimestamp() {
		return ts;
	}

	public void setTimestamp(long ts) {
		this.ts = ts;
	}

	@Override
	public String toString() {
		return new Date(ts) + " " + lat + " " + lng;
	}

}
