package com.hellotracks.util;

import java.util.LinkedList;

import com.hellotracks.types.GPS;

public class TrackDecoder {

    public static GPS[] decode(int cnt, String encodedPolyline) {
        GPS[] gps = new GPS[cnt];
        int c = 0;

        int len = encodedPolyline.length();
        int index = 0;
        int lat = 0;
        int lng = 0;

        // Decode polyline according to Google's polyline decoder utility.
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            gps[c] = new GPS();
            gps[c].lat = lat / 100000.0;
            gps[c].lng = lng / 100000.0;
            c++;
        }

        return gps;
    }

    public static GPS[] decodeNow(String encodedPolyline) {
        LinkedList<GPS> list = new LinkedList<GPS>();
 
        int len = encodedPolyline.length();
        int index = 0;
        int lat = 0;
        int lng = 0;

        // Decode polyline according to Google's polyline decoder utility.
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            

            GPS gps = new GPS();
            gps.lat = lat / 100000.0;
            gps.lng = lng / 100000.0;
            list.add(gps);
        }

        return list.toArray(new GPS[0]);
    }

    public static String decode(String encodedPolyline) {

        StringBuilder sb = new StringBuilder();

        int len = encodedPolyline.length();
        int index = 0;
        int lat = 0;
        int lng = 0;

        // Decode polyline according to Google's polyline decoder utility.
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            sb.append(lat / 100000.0);
            sb.append(",");
            sb.append(lng / 100000.0);
            if (index < len) {
                sb.append("|");
            }
        }

        return sb.toString();
    }
}
