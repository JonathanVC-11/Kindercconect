package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class BusStatus {
    private String status; // "STOPPED", "ACTIVE", "FINISHED"
    private GeoPoint currentLocation;

    // --- CAMPOS CORREGIDOS Y AÑADIDOS ---
    @ServerTimestamp
    private Date lastUpdateTime; // Coincide con el nombre en BusTrackingRepository
    @ServerTimestamp
    private Date startTime; // Añadido
    @ServerTimestamp
    private Date endTime; // Añadido

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

    // --- MÉTODOS CORREGIDOS Y AÑADIDOS ---
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

}