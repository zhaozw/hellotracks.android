package com.hellotracks.types;

import java.io.Serializable;

import android.location.Location;

public class LatLng implements Serializable {

    private static final long serialVersionUID = 645687916178395417L;

    public double lat;

    public double lng;

    public LatLng() {
    }

    public LatLng(Location l) {
        if (l != null) {
            lat = l.getLatitude();
            lng = l.getLongitude();
        }
    }
    
    public LatLng(com.google.android.gms.maps.model.LatLng ll) {
        lat = ll.latitude;
        lng = ll.longitude;
    }

    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public void setLatitude(double latitude) {
        this.lat = latitude;
    }

    public void setLongitude(double longitude) {
        this.lng = longitude;
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lng;
    }

    public void set(LatLng other) {
        this.lat = other.lat;
        this.lng = other.lng;
    }

    public LatLng copy() {
        return new LatLng(lat, lng);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lat);
        sb.append(",");
        sb.append(lng);
        return sb.toString();
    }

    public com.google.android.gms.maps.model.LatLng toGoogle() {
        return new com.google.android.gms.maps.model.LatLng(lat, lng);
    }

}