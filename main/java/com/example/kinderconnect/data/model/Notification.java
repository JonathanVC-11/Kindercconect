package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {

    @DocumentId
    private String notificationId;
    private String userId; // El ID del usuario que RECIBE la notificación
    private String title;
    private String body;
    private String type; // Ej: "ATTENDANCE", "NEW_STUDENT", "NOTICE", "BUS_ROUTE"
    private boolean isRead;
    @ServerTimestamp
    private Date timestamp;

    public Notification() {
        // Constructor vacío para Firestore
    }

    // Constructor útil para crear notificaciones (ej. en Cloud Functions)
    public Notification(String userId, String title, String body, String type) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.isRead = false;
    }

    // Getters y Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}