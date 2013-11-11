package com.hellotracks.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MarkerEntry {
    public int index;
    public String account;
    public String json;
    public long timestamp;
    public String url;
    public String name;
    public LatLng point;
    public int radius;
    public String info;
    public int accuracy;
    public Marker marker;
    
    public MarkerEntry(int index) {
        this.index = index;
    }
}
