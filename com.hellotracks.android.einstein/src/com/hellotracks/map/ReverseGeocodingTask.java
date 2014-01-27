package com.hellotracks.map;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.ui.IconGenerator;

class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
    Context mContext;
    Marker mMarker;

    public ReverseGeocodingTask(Context context, Marker m) {
        super();
        mContext = context;
        mMarker = m;
    }

    // Finding address using reverse geocoding
    @Override
    protected String doInBackground(LatLng... params) {
        Geocoder geocoder = new Geocoder(mContext);
        double latitude = params[0].latitude;
        double longitude = params[0].longitude;
        List<Address> addresses = null;
        String addressText = "";
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            Thread.sleep(500);

            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                addressText = address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return addressText;
    }

    @Override
    protected void onPostExecute(String addressText) {
        if (addressText != null) {
            IconGenerator gen = new IconGenerator(mContext);
            gen.setStyle(IconGenerator.STYLE_PURPLE);
            gen.setContentRotation(-90);
            Bitmap bmp = gen.makeIcon(addressText);
            mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
            mMarker.setTitle(addressText);
        }
    }
}