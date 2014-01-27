package com.hellotracks.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MarkerEntry {
    private int index;
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
    
    public boolean isPlace() {
        return radius >= 0;
    }
    
    public boolean isMe() {
        return index == 0;
    }
    
    public MarkerEntry(int index) {
        this.index = index;
    }

    public boolean isPerson() {
        return !isPlace();
    }
}
