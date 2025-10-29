package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Notice {
    @DocumentId
    private String noticeId;
    private String teacherId;
    private String teacherName;
    private String title;
    private String description;
    private String category; // "TAREA", "EVENTO", "RECORDATORIO", "URGENTE"
    private String scope; // "GROUP", "SCHOOL"
    private String groupName;
    private Date validUntil;
    private String imageUrl;
    private String documentUrl;
    @ServerTimestamp
    private Date publishedAt;
    private List<String> readBy;

    public Notice() {
        this.readBy = new ArrayList<>();
    }

    public Notice(String teacherId, String title, String description, String category, String scope) {
        this.teacherId = teacherId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.scope = scope;
        this.readBy = new ArrayList<>();
    }

    // Getters y Setters
    public String getNoticeId() { return noticeId; }
    public void setNoticeId(String noticeId) { this.noticeId = noticeId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Date getValidUntil() { return validUntil; }
    public void setValidUntil(Date validUntil) { this.validUntil = validUntil; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    public List<String> getReadBy() { return readBy; }
    public void setReadBy(List<String> readBy) { this.readBy = readBy; }

    public boolean isReadByUser(String userId) {
        return readBy != null && readBy.contains(userId);
    }

    public void markAsReadBy(String userId) {
        if (readBy == null) {
            readBy = new ArrayList<>();
        }
        if (!readBy.contains(userId)) {
            readBy.add(userId);
        }
    }
}
