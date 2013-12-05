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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.hellotracks.Log;
import com.hellotracks.R;

public class PlacesAutocompleteActivity extends SherlockActivity implements OnItemClickListener {

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    public static final String API_KEY = "AIzaSyDI2RBDivFTcFSDa4s4SbV4kGJRpksB_eE";

    private double latitude;
    private double longitude;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.disappear);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.grow_from_top, 0);
        super.onCreate(savedInstanceState);

        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        setContentView(R.layout.places_autocomplete);

        final AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item_places_automcomplete));
        autoCompView.setOnItemClickListener(this);
    }

    public void onClose(View view) {
        finish();
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Result result = (Result) adapterView.getItemAtPosition(position);
        Toast.makeText(this, result.description, Toast.LENGTH_SHORT).show();
        Intent data = new Intent();
        data.putExtra("reference", result.reference);
        data.putExtra("description", result.description);
        setResult(RESULT_OK, data);
        finish();
    }

    private class Result {
        String type;
        String description;
        String reference;

        @Override
        public String toString() {
            return description;
        }
    }

    private ArrayList<Result> resultList = null;

    private ArrayList<Result> autocomplete(String input) {

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?sensor=false&key=" + API_KEY);
            sb.append("&location=" + latitude + "," + longitude);
            sb.append("&language=" + Locale.getDefault().getLanguage());
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

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
            Log.e("Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e("Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
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
            Log.e("Cannot process JSON results", e);
        }

        return resultList;
    }

    private class PlacesAutoCompleteAdapter extends ArrayAdapter<Result> implements Filterable {
        private ArrayList<Result> resultList;

        public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public View getView(int index, View convertView, ViewGroup parent) {
            final View vi = convertView == null ? getLayoutInflater().inflate(R.layout.list_item_places_automcomplete,
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
                        resultList = autocomplete(constraint.toString());

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
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

}
