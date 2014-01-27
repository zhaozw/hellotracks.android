package com.hellotracks.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.hellotracks.Logger;

public class StaticMap {

    public static class Google {

        public static URL createMap(int size, String encoded) {
            try {
                String s = size + "x" + size;
                URL url = new URL("http://maps.google.com/maps/api/staticmap?size=" + s
                        + "&sensor=true&path=weight:4|color:blue|enc:" + encoded);
                return url;
            } catch (MalformedURLException e) {
                Logger.w(e);
            }
            return null;
        }

        public static URL createMap(int size, double lat, double lng) {
            return createMap(size, size, lat, lng, 10);
        }
        
        public static URL createMap(int w, int h, double lat, double lng, int zoom) {
            try {
                String s = w + "x" + h;
                String ll = lat + "," + lng;
                return new URL("http://maps.google.com/maps/api/staticmap?center=" + ll + "&zoom=" + zoom + "&size=" + s
                        + "&markers=size:mid|label:A|color:blue|" + ll + "&sensor=true&scale=2");
            } catch (MalformedURLException e) {
                Logger.w(e);
            }
            return null;
        }

        public static URL createMap(int size, double lat, double lng, String markerUrl) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("http://maps.google.com/maps/api/staticmap?center=");
                sb.append(lat);
                sb.append(",");
                sb.append(lng);
                sb.append("&zoom=10&size=");
                sb.append(size);
                sb.append("x");
                sb.append(size);
                sb.append("&markers=icon:");
                sb.append(markerUrl);
                sb.append("|");
                sb.append(lat);
                sb.append(",");
                sb.append(lng);
                sb.append("&sensor=true");
                return new URL(sb.toString());
            } catch (MalformedURLException e) {
                Logger.w(e);
            }
            return null;
        }
    }

}
