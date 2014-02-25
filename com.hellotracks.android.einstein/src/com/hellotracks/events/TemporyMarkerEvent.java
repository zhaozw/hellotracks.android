package com.hellotracks.events;


public class TemporyMarkerEvent {

    public double latitude;
    public double longitude;
    public String text;
    
    public TemporyMarkerEvent(double lat, double lng, String text) {
        this.latitude = lat;
        this.longitude = lng;
        this.text = text;
    }
}
