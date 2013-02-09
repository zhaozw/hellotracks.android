/**
 * Reimplementation of Mark McClures Javascript PolylineEncoder
 * All the mathematical logic is more or less copied by McClure
 *  
 * @author Mark Rambow
 * @e-mail markrambow[at]gmail[dot]com
 * @version 0.1
 * 
 */

package com.hellotracks.util;

import java.util.ArrayList;

import com.hellotracks.types.Track;
import com.hellotracks.types.Waypoint;

public class TrackEncoder {

	private static int floor1e5(double coordinate) {
		return (int) Math.floor(coordinate * 1e5);
	}

	private static String encodeSignedNumber(int num) {
		int sgn_num = num << 1;
		if (num < 0) {
			sgn_num = ~(sgn_num);
		}
		return (encodeNumber(sgn_num));
	}

	private static String encodeNumber(int num) {
		StringBuffer encodeString = new StringBuffer();

		while (num >= 0x20) {
			int nextValue = (0x20 | (num & 0x1f)) + 63;
			encodeString.append((char) (nextValue));
			num >>= 5;
		}

		num += 63;
		encodeString.append((char) (num));

		return encodeString.toString();
	}

	public static String encodePoints(Track track) {
		StringBuffer encodedPoints = new StringBuffer();

		ArrayList<Waypoint> list = new ArrayList<Waypoint>();
		for (Waypoint wp : track.getWaypoints()) {
			list.add(wp);
		}

		int plat = 0;
		int plng = 0;
		int counter = 0;

		int listSize = list.size();

		Waypoint trackpoint;

		for (int i = 0; i < listSize; i++) {
			counter++;
			trackpoint = list.get(i);

			int late5 = floor1e5(trackpoint.lat);
			int lnge5 = floor1e5(trackpoint.lng);

			int dlat = late5 - plat;
			int dlng = lnge5 - plng;

			plat = late5;
			plng = lnge5;

			encodedPoints.append(encodeSignedNumber(dlat)).append(
					encodeSignedNumber(dlng));
		}

		return encodedPoints.toString();
	}

	public static String encodeLevels(int size, int level) {
		StringBuffer encodedLevels = new StringBuffer();
		for (int i = 0; i < size; i++) {
			encodedLevels.append(encodeNumber(level));
		}
		return encodedLevels.toString();
	}

}
