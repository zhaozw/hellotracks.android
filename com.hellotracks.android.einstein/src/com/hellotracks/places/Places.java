package com.hellotracks.places;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.hellotracks.Logger;
import com.hellotracks.R;

public class Places {

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    public static final String API_KEY = "AIzaSyDI2RBDivFTcFSDa4s4SbV4kGJRpksB_eE";

    public static class PlacesAutoCompleteAdapter extends ArrayAdapter<Result> implements Filterable {
        private ArrayList<Result> resultList = new ArrayList<Result>();
        private Activity mActivity;
        private double lat, lng;

        public PlacesAutoCompleteAdapter(Activity activity, int textViewResourceId, double lat, double lng) {
            super(activity, textViewResourceId);
            this.mActivity = activity;
            this.lat = lat;
            this.lng = lng;
        }

        public View getView(int index, View convertView, ViewGroup parent) {
            final View vi = convertView == null ? mActivity.getLayoutInflater().inflate(R.layout.list_item_places_automcomplete,
                    null) : convertView;

            TextView textDesc = (TextView) vi.findViewById(R.id.textDescription);
            TextView textTitle = (TextView) vi.findViewById(R.id.textTitle);

            String text = resultList.get(index).description;
            int idx = text.indexOf(",");
            if (idx > 0) {
                textTitle.setVisibility(View.VISIBLE);
                textTitle.setText(text.substring(0, idx));
                textDesc.setText(text.substring(idx + 1));
            } else {
                textTitle.setVisibility(View.GONE);
                textDesc.setText(text);
            }

            vi.findViewById(R.id.imageView).setVisibility(
                    "establishment".equals(resultList.get(index).type) ? View.VISIBLE : View.GONE);
            return vi;
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public Result getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        Logger.i("constaintfilter=" + constraint.toString());
                        resultList = autocomplete(constraint.toString(), lat, lng);

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                }
            };
            return filter;
        }
    }

    public static class Result {
        public String type;
        public String description;
        public String reference;

        @Override
        public String toString() {
            return description;
        }
    }

    public static ArrayList<Result> autocomplete(String input, double latitude, double longitude) {
        ArrayList<Result> resultList = null;
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?sensor=false&key=" + API_KEY);
            sb.append("&location=" + latitude + "," + longitude);
            sb.append("&language=" + Locale.getDefault().getLanguage());
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            Logger.i("search=" + sb);

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Logger.e("Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Logger.e("Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            String res = jsonResults.toString();
            Logger.i("answer=" + res);
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(res);
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<Result>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                Result r = new Result();
                r.description = predsJsonArray.getJSONObject(i).getString("description");
                r.reference = predsJsonArray.getJSONObject(i).getString("reference");
                r.type = predsJsonArray.getJSONObject(i).getJSONArray("types").getString(0);
                resultList.add(r);
            }
        } catch (JSONException e) {
            Logger.e("Cannot process JSON results", e);
        }

        return resultList;
    }
}
