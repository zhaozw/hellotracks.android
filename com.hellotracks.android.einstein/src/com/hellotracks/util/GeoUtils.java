package com.hellotracks.util;

import com.hellotracks.types.Bounds;
import com.hellotracks.types.LatLng;

public class GeoUtils {

	public static double EARTH_RADIUS = 6378137; // in meter

	public static boolean isInside(LatLng point, LatLng... area) {
		boolean isInPolygon = false;

		int j = area.length - 1;

		for (int i = 0; i < area.length; i++) {
			LatLng gpi = area[i];
			LatLng gpj = area[j];
			if (gpi.getLongitude() < point.lng
					&& gpj.getLongitude() >= point.lng
					|| gpj.getLongitude() < point.lng
					&& gpi.getLongitude() >= point.lng) {

				if (gpi.getLatitude() + (point.lng - gpi.getLongitude())
						/ (gpj.getLongitude() - gpi.getLongitude())
						* (gpj.getLatitude() - gpi.getLatitude()) < point.lat) {
					isInPolygon = !isInPolygon;
				}
			}
			j = i;
		}
		return isInPolygon;
	}

	public static double distanceInMeter(double lat1, double lng1, double lat2,
			double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
				* Math.sin(dLon / 2);
		double c = 2 * Math.asin(Math.sqrt(a));
		double dist = EARTH_RADIUS * c;
		return dist;
	}

	public static double distanceInMeter(LatLng p1, LatLng p2) {
		return distanceInMeter(p1.lat, p1.lng, p2.lat, p2.lng);
	}

	private static LatLng getDestLatLng(LatLng latLng, double bearing,
			double distance) {
		double lat1 = Math.toRadians(latLng.lat);
		double lng1 = Math.toRadians(latLng.lng);
		double brng = bearing * Math.PI / 180.0;
		double dDivR = distance / EARTH_RADIUS;
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dDivR)
				+ Math.cos(lat1) * Math.sin(dDivR) * Math.cos(brng));
		double lng2 = lng1
				+ Math.atan2(Math.sin(brng) * Math.sin(dDivR) * Math.cos(lat1),
						Math.cos(dDivR) - Math.sin(lat1) * Math.sin(lat2));
		return new LatLng(lat2 / Math.PI * 180.0, lng2 / Math.PI * 180.0);
	}

	public static Bounds getBoundaries(LatLng center, double radius) {
		double hypotenuse = Math.sqrt(2.0 * radius * radius);
		LatLng sw = getDestLatLng(center, 225.0, hypotenuse);
		LatLng ne = getDestLatLng(center, 45.0, hypotenuse);
		return new Bounds(sw, ne);
	}

}