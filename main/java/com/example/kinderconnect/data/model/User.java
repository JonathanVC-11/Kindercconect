package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    @DocumentId
    private String uid;
    private String email;
    private String fullName;
    private String userType; // "TEACHER" or "PARENT"
    private String phone;    // <-- Este campo
    private String photoUrl;
    private String schoolId;
    @ServerTimestamp
    private Date createdAt;
    private boolean isActive;

    public User() {
        // Constructor vacío requerido por Firebase
    }

    // --- CONSTRUCTOR CORREGIDO ---
    // Añadimos 'String phone' como parámetro
    public User(String uid, String email, String fullName, String userType, String phone) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.userType = userType;
        this.phone = phone; // <-- Y lo asignamos aquí
        this.isActive = true;
    }

    // Getters y Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getSchoolId() { return schoolId; }
    public void setSchoolId(String schoolId) { this.schoolId = schoolId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}