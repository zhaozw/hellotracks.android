package com.hellotracks.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;

import com.google.android.gms.maps.model.LatLngBounds;
import com.hellotracks.Log;
import com.hellotracks.types.Bounds;
import com.hellotracks.types.LatLng;

public class SearchMap {

    public static class LocationResult {
        public LatLng position = null;
        public String displayname = null;
    }
    
    public static class DirectionsResult {
        public int distanceValue = 0;
        public String distanceText = null;
        public String startAddress = null;
        public String endAddress = null;
        public LatLng startLocation = null;
        public LatLng endLocation = null;
        public String track = null;
        public String durationText = null;
        public int durationValue = 0;
    }

    public static interface Callback<T> {
        public void onResult(boolean success, T location);
    }

    public static void asyncSearch(final Activity context, final String search, final LatLngBounds bounds,
            final Callback<LocationResult[]> callback) {
        new Async.Task<LocationResult[]>(context) {

            @Override
            public LocationResult[] async() {
                return searchGoogleMaps(search, bounds);
            }

            @Override
            public void post(LocationResult[] locations) {
                if (callback != null) {
                    callback.onResult(locations != null && locations.length > 0, locations);
                }
            }

        };
    }
    
    public static void asyncGetDirections(final Activity context, final LatLng origin, final LatLng destination,
            final Callback<DirectionsResult> callback) {
        new Async.Task<DirectionsResult>(context) {

            @Override
            public DirectionsResult async() {
                return getDirections(origin, destination);
            }

            @Override
            public void post(DirectionsResult result) {
                if (callback != null) {
                    callback.onResult(result != null, result);
                }
            }

        };
    }

    public static LocationResult searchNominatim(String string) {
        LocationResult location = null;

        try {
            String encoded = URLEncoder.encode(string, "UTF-8").replace("+", "%20");
            URL url = new URL("http://nominatim.openstreetmap.org/search/" + encoded + "?format=json");
            Log.i(url.toString());
            URLConnection connection = url.openConnection();

            BufferedReader streamReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            JSONArray array = new JSONArray(responseStrBuilder.toString());
            if (array.length() > 0) {
                JSONObject obj = array.getJSONObject(0);
                location = new LocationResult();
                location.position = new LatLng(obj.getDouble("lat"), obj.getDouble("lon"));
                location.displayname = obj.getString("display_name");
            }

        } catch (Exception exc) {
            Log.w(exc);
        } finally {
        }
        return location;
    }

    public static LocationResult[] searchGoogleMaps(String string, LatLngBounds b) {
        LocationResult[] location = null;

        try {
            String encoded = URLEncoder.encode(string, "UTF-8");

            String urlString = "http://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=" + encoded;
            if (b != null) {
                urlString += "&bounds=" + b.southwest.latitude + "," + b.southwest.longitude + "|"
                        + b.northeast.latitude + "," + b.northeast.longitude;
            }
            URL url = new URL(urlString);
            Log.i(url.toString());
            URLConnection connection = url.openConnection();

            BufferedReader streamReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            JSONObject json = new JSONObject(responseStrBuilder.toString());
            JSONArray array = json.getJSONArray("results");
            location = new LocationResult[array.length()];

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = json.getJSONArray("results").getJSONObject(i);
                JSONObject geometry = obj.getJSONObject("geometry");
                JSONObject loc = geometry.getJSONObject("location");
                location[i] = new LocationResult();
                location[i].position = new LatLng(loc.getDouble("lat"), loc.getDouble("lng"));
                location[i].displayname = obj.getString("formatted_address");
            }
        } catch (Exception exc) {
            Log.w(exc);
        } finally {
        }
        return location;
    }

    public static DirectionsResult getDirections(LatLng origin, LatLng destination) {
        try {
            String urlString = "http://maps.googleapis.com/maps/api/directions/json?sensor=false&origin=" + origin.lat
                    + "," + origin.lng + "&destination=" + destination.lat + "," + destination.lng;

            URL url = new URL(urlString);
            Log.i(url.toString());
            URLConnection connection = url.openConnection();

            BufferedReader streamReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            JSONObject json = new JSONObject(responseStrBuilder.toString());
            JSONArray routes = json.getJSONArray("routes");
            JSONObject route = routes.getJSONObject(0);
            JSONArray legs = route.getJSONArray("legs");
            JSONObject leg = legs.getJSONObject(0);
            JSONObject startLoc = leg.getJSONObject("start_location");
            JSONObject endLoc = leg.getJSONObject("end_location");
            JSONObject distance = leg.getJSONObject("distance");
            JSONObject duration = leg.getJSONObject("duration");
            
            DirectionsResult result = new DirectionsResult();
            result.track = route.getJSONObject("overview_polyline").getString("points");
            result.startLocation = new LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng"));
            result.startAddress = leg.getString("start_address");
            result.endLocation = new LatLng(endLoc.getDouble("lat"), endLoc.getDouble("lng"));
            result.endAddress = leg.getString("end_address");
            result.distanceText = distance.getString("text");
            result.distanceValue = distance.getInt("value");
            result.durationText = duration.getString("text");
            result.durationValue = duration.getInt("value");
            return result;
        } catch (Exception exc) {
            Log.w(exc);
        }
        return null;
    }
}
