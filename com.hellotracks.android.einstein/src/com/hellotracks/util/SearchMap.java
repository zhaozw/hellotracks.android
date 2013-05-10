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

    public static class Result {
        public LatLng position = null;
        public String displayname = null;
    }

    public static interface Callback {
        public void onResult(boolean success, Result[] location);
    }

    public static void asyncSearch(final Activity context, final String search, final LatLngBounds bounds,
            final Callback callback) {
        new Async.Task<Result[]>(context) {

            @Override
            public Result[] async() {
                return searchGoogleMaps(search, bounds);
            }

            @Override
            public void post(Result[] locations) {
                if (callback != null) {
                    callback.onResult(locations != null && locations.length > 0, locations);
                }
            }

        };
    }

    public static Result searchNominatim(String string) {
        Result location = null;

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
                location = new Result();
                location.position = new LatLng(obj.getDouble("lat"), obj.getDouble("lon"));
                location.displayname = obj.getString("display_name");
            }

        } catch (Exception exc) {
            Log.w(exc);
        } finally {
        }
        return location;
    }

    public static Result[] searchGoogleMaps(String string, LatLngBounds b) {
        Result[] location = null;

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
            location = new Result[array.length()];

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = json.getJSONArray("results").getJSONObject(i);
                JSONObject geometry = obj.getJSONObject("geometry");
                JSONObject loc = geometry.getJSONObject("location");
                location[i] = new Result();
                location[i].position = new LatLng(loc.getDouble("lat"), loc.getDouble("lng"));
                location[i].displayname = obj.getString("formatted_address");
            }
        } catch (Exception exc) {
            Log.w(exc);
        } finally {
        }
        return location;
    }
}
