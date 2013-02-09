package com.hellotracks.types;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeSet;

import com.hellotracks.util.GeoUtils;
import com.hellotracks.util.TrackEncoder;

@SuppressWarnings("serial")
public class Track implements Serializable {
	public static final int DEFAULT_SIZE = 100000;

	private int maxSize = DEFAULT_SIZE;

	private double distance = -1;

	private static class WaypointComparator implements Comparator<Waypoint>,
			Serializable {
		@Override
		public int compare(Waypoint wp1, Waypoint wp2) {
			return new Long(wp1.ts).compareTo(wp2.ts);
		}
	}

	private TreeSet<GPS> waypoints = new TreeSet<GPS>(new WaypointComparator());

	public Track() {
	}

	public boolean isLive(long time) {
		return System.currentTimeMillis() - waypoints.last().ts < time;
	}

	public Track(int size) {
		maxSize = size;
	}

	public void add(GPS p) {
		if (p == null)
			return;
//		while (waypoints.size() > maxSize)
//			waypoints.remove(waypoints.first());
		
		
//		if (waypoints.size() > 5) {
//			GPS last = lastAny();
//			if (Math.abs(p.ts - last.ts) >= 1000) {
//				waypoints.add(p);
//			}
//		} else {
			waypoints.add(p);
//		}
		distance = -1;
	}

	public void remove(GPS p) {
		waypoints.remove(p);
		distance = -1;
	}

	public double distance() {
		if (distance < 0) {
			double meter = 0;
			Waypoint[] cp = getWaypoints();
			for (int i = 1; i < cp.length; i++) {
				meter += GeoUtils.distanceInMeter(cp[i - 1].lat, cp[i - 1].lng,
						cp[i].lat, cp[i].lng);
			}
			distance = meter;
		}
		return distance;
	}

	public GPS firstAny() {
		try {
			return waypoints.first();
		} catch (Exception exc) {
			return null;
		}
	}

	public GPS lastAny() {
		try {
			return waypoints.last();
		} catch (Exception exc) {
			return null;
		}
	}

	public GPS[] getWaypoints() {
		return waypoints.toArray(new GPS[0]);
	}

	public void clear() {
		waypoints.clear();
		distance = -1;
	}

	public int size() {
		return waypoints.size();
	}

	public LinkedList<Double> getLatitudes() {
		LinkedList<Double> list = new LinkedList<Double>();
		for (Waypoint cp : getWaypoints()) {
			list.add(cp.lat);
		}
		return list;
	}

	public LinkedList<Double> getLongitudes() {
		LinkedList<Double> list = new LinkedList<Double>();
		for (Waypoint cp : getWaypoints()) {
			list.add(cp.lng);
		}
		return list;
	}

	private static SimpleDateFormat TIMEFORMAT = new SimpleDateFormat("HH:mm");
	private static SimpleDateFormat DATEFORMAT = new SimpleDateFormat(
			"yyyy-MM-dd");

	public String getDate() {
		return format(DATEFORMAT, waypoints.first(), null);
	}

	public String getTime() {
		return format(TIMEFORMAT, waypoints.first(), waypoints.last());
	}

	private static String format(DateFormat format, Waypoint first,
			Waypoint last) {
		StringBuilder sb = new StringBuilder();
		if (first != null) {
			try {
				sb.append(format.format(first.ts));
			} catch (Exception exc) {
				// ignore
			}
		}
		if (last != null) {
			try {
				sb.append("-");
				sb.append(format.format(last.ts));
			} catch (Exception exc) {
				// ignore
			}
		}
		return sb.toString();
	}

	public long duration() {
		return waypoints.last().ts - waypoints.first().ts;
	}

	public String getName() {
		if (waypoints.isEmpty())
			return "-";
		StringBuilder sb = new StringBuilder();
		sb.append(getTime());
		sb.append(" (");
		sb.append(waypoints.size());
		if (waypoints.size() == 1)
			sb.append(" point)");
		else
			sb.append(" points)");
		return sb.toString();
	}

	public String encodePoints() {
		return TrackEncoder.encodePoints(this);
	}

	public String encodeTimes() {
		StringBuilder sb = new StringBuilder();
		Waypoint[] points = getWaypoints();
		long abs = 0;
		long rel = 0;
		for (int i = 0; i < points.length; i++) {
			if (abs == 0) {
				abs = points[i].ts;
				sb.append(abs / 1000);
			} else {
				rel = points[i].ts - abs;
				sb.append("|");
				sb.append(rel / 1000);
				abs = points[i].ts;
			}
		}
		return sb.toString();
	}

	public String encodeAltitudes() {
		StringBuilder sb = new StringBuilder();
		GPS[] points = getWaypoints();
		for (int i = 0; i < points.length; i++) {
			if (i > 0)
				sb.append("|");
			sb.append(points[i].alt);
		}
		return sb.toString();
	}

	public String encodeSpeeds() {
		StringBuilder sb = new StringBuilder();
		GPS[] points = getWaypoints();
		for (int i = 0; i < points.length; i++) {
			if (i > 0)
				sb.append("|");
			sb.append(points[i].speed);
		}
		return sb.toString();
	}

	public String encodeHeadings() {
		StringBuilder sb = new StringBuilder();
		GPS[] points = getWaypoints();
		for (int i = 0; i < points.length; i++) {
			if (i > 0)
				sb.append("|");
			sb.append(points[i].head);
		}
		return sb.toString();
	}

	public String encodeVAccs() {
		StringBuilder sb = new StringBuilder();
		GPS[] points = getWaypoints();
		for (int i = 0; i < points.length; i++) {
			if (i > 0)
				sb.append("|");
			sb.append(points[i].vacc);
		}
		return sb.toString();
	}

	public String encodeHAccs() {
		StringBuilder sb = new StringBuilder();
		GPS[] points = getWaypoints();
		for (int i = 0; i < points.length; i++) {
			if (i > 0)
				sb.append("|");
			sb.append(points[i].hacc);
		}
		return sb.toString();
	}

	public String encodeSensors() {
		StringBuilder sb = new StringBuilder();
		GPS[] points = getWaypoints();
		for (int i = 0; i < points.length; i++) {
			if (i > 0)
				sb.append("|");
			sb.append(points[i].sensor);
		}
		return sb.toString();
	}

}
