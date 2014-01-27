package com.hellotracks.types;

import android.location.Location;

import com.hellotracks.Logger;

public class GPS extends Waypoint {

	private static final long serialVersionUID = 3844291424306374921L;

	public static final int SENSOR_TEST = -1;
	public static final int SENSOR_UNKOWN = 0;
	public static final int SENSOR_GPS = 1;
	public static final int SENSOR_NETWORK = 2;
	public static final int SENSOR_HUMAN = 3;
	public static final int SENSOR_BROWSER = 4;
	public static final int SENSOR_TRACKEND = 5;

	public int alt = -1;

	public int head = -1;

	public int speed = -1;

	public int hacc = -1;

	public int vacc = -1;

	public int sensor = 0;

	public int getAltitude() {
		return alt;
	}

	public int getSpeed() {
		return speed;
	}

	public int getHeading() {
		return head;
	}

	public void setSensor(int sensor) {
		this.sensor = sensor;
	}

	public int getSensor() {
		return sensor;
	}

	public void setAccuracyHorizontal(int accuracy) {
		hacc = accuracy;
	}

	public void setAccuracyVertical(int accuracy) {
		vacc = accuracy;
	}

	public int getAccuracyHorizontal() {
		return hacc;
	}

	public int getAccuracyVertical() {
		return vacc;
	}

	public void setHeading(int head) {
		this.head = head;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void setAltitude(int alt) {
		this.alt = alt;
	}

	public void set(GPS other) {
		this.lat = other.lat;
		this.lng = other.lng;
		this.ts = other.ts;
		this.alt = other.alt;
		this.head = other.head;
		this.speed = other.speed;
		this.hacc = other.hacc;
		this.vacc = other.vacc;
	}

	@Override
	public GPS copy() {
		GPS other = new GPS();
		other.set(this);
		return other;
	}

	@Override
	public String toString() {
		return "{" + lat + "," + lng + "}";
	}
	
	public static GPS fromLocation(Location loc) {
	    GPS gps = new GPS();
        gps.ts = loc.getTime();
        gps.lat = loc.getLatitude();
        gps.lng = loc.getLongitude();
        gps.alt = (int) loc.getAltitude();
        gps.hacc = (int) loc.getAccuracy();
        gps.speed = (int) (loc.getSpeed() * 3.6);
        gps.head = (int) loc.getBearing();
        gps.hacc = (int) loc.getAccuracy();

        // fix for some mobile phones, unkown why loc.ts is one day before
        // current ts
        if (gps.ts > System.currentTimeMillis()) {
            gps.ts = System.currentTimeMillis();
        }

        if (loc.getAccuracy() < 60) {
            gps.sensor = GPS.SENSOR_GPS;
        } else {
            gps.sensor = GPS.SENSOR_NETWORK;
        }
        return gps;
	}

}
