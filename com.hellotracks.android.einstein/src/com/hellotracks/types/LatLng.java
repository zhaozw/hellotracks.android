package com.hellotracks.types;

import java.io.Serializable;

public class LatLng implements Serializable {

	private static final long serialVersionUID = 645687916178395417L;

	public double lat;

	public double lng;

	public LatLng() {
	}

	public LatLng(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public void setLatitude(double latitude) {
		this.lat = latitude;
	}

	public void setLongitude(double longitude) {
		this.lng = longitude;
	}

	public double getLatitude() {
		return lat;
	}

	public double getLongitude() {
		return lng;
	}

	public void set(LatLng other) {
		this.lat = other.lat;
		this.lng = other.lng;
	}

	public LatLng copy() {
		return new LatLng(lat, lng);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(lat);
		sb.append(",");
		sb.append(lng);
		return sb.toString();
	}

}