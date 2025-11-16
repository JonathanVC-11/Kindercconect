package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Group {
    @DocumentId
    private String groupId;
    private String teacherId;
    private String teacherEmail;
    private String teacherName;
    private String grade; // Ej: "1ro", "2do", "3ro"
    private String groupName; // Ej: "A", "B", "C"
    @ServerTimestamp
    private Date createdAt;

    public Group() {
        // Constructor vacío para Firestore
    }

    public Group(String teacherId, String teacherEmail, String teacherName, String grade, String groupName) {
        this.teacherId = teacherId;
        this.teacherEmail = teacherEmail;
        this.teacherName = teacherName;
        this.grade = grade;
        this.groupName = groupName;
    }

    // Getters y Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherEmail() { return teacherEmail; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}