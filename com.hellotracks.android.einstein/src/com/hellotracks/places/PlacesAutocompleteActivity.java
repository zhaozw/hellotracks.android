package com.hellotracks.places;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.hellotracks.R;
import com.hellotracks.places.Places.Result;

public class PlacesAutocompleteActivity extends SherlockActivity implements OnItemClickListener {


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
        autoCompView.setAdapter(new Places.PlacesAutoCompleteAdapter(this, R.layout.list_item_places_automcomplete, latitude, longitude));
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

        String text = result.description;
        int idx = text.indexOf(",");
        if (idx > 0) {
            data.putExtra("description", text.substring(0, idx));
        } else {
            data.putExtra("description", result.description);
        }

        setResult(RESULT_OK, data);
        finish();
    }



    

   

    

}
