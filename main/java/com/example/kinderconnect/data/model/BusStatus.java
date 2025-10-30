package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class BusStatus {
    private String status; // "STOPPED", "ACTIVE", "FINISHED"
    private GeoPoint currentLocation;
    @ServerTimestamp
    private Date lastUpdate;

    public BusStatus() {
        // Constructor vacío
    }

    // Getters y Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public GeoPoint getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(GeoPoint currentLocation) {
        this.currentLocation = currentLocation;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}