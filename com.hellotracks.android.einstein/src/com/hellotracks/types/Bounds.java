package com.hellotracks.types;

public class Bounds {

	private LatLng southWest;

	private LatLng northEast;

	public Bounds(LatLng... points) {
		extend(points);
	}

	public boolean isOK() {
		return southWest != null && northEast != null;
	}

	public Bounds(LatLng southWest, LatLng northEast) {
		double lat1 = Math.min(southWest.lat, northEast.lat);
		double lng1 = Math.min(southWest.lng, northEast.lng);
		this.southWest = new LatLng(lat1, lng1);

		double lat2 = Math.max(southWest.lat, northEast.lat);
		double lng2 = Math.max(southWest.lng, northEast.lng);
		this.northEast = new LatLng(lat2, lng2);
	}

	public LatLng getNorthEast() {
		return northEast;
	}

	public LatLng getSouthWest() {
		return southWest;
	}

	public void clear() {
		northEast = null;
		northEast = null;
	}

	public void extend(LatLng point) {
		if (southWest == null)
			southWest = new LatLng(point.lat, point.lng);
		if (northEast == null)
			northEast = new LatLng(point.lat, point.lng);

		if (point.lat < southWest.lat)
			southWest.lat = point.lat;
		if (point.lng < southWest.lng)
			southWest.lng = point.lng;

		if (point.lat > northEast.lat)
			northEast.lat = point.lat;
		if (point.lng > northEast.lng)
			northEast.lng = point.lng;
	}

	public void extend(LatLng... points) {
		for (LatLng point : points) {
			extend(point);
		}
	}

	public Bounds copy() {
		Bounds copy = new Bounds();
		copy.southWest = southWest.copy();
		copy.northEast = northEast.copy();
		return copy;
	}
}
