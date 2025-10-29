package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GalleryItem {
    @DocumentId
    private String itemId;
    private String teacherId;
    private String mediaUrl;
    private String thumbnailUrl;
    private String mediaType; // "IMAGE" or "VIDEO"
    private String description;
    private List<String> taggedStudents;
    private String groupName;
    private String location;
    private double latitude;
    private double longitude;
    @ServerTimestamp
    private Date uploadedAt;

    public GalleryItem() {
        this.taggedStudents = new ArrayList<>();
    }

    public GalleryItem(String teacherId, String mediaUrl, String mediaType, String description) {
        this.teacherId = teacherId;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.description = description;
        this.taggedStudents = new ArrayList<>();
    }

    // Getters y Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTaggedStudents() { return taggedStudents; }
    public void setTaggedStudents(List<String> taggedStudents) {
        this.taggedStudents = taggedStudents;
    }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }

    public void addTaggedStudent(String studentId) {
        if (taggedStudents == null) {
            taggedStudents = new ArrayList<>();
        }
        if (!taggedStudents.contains(studentId)) {
            taggedStudents.add(studentId);
        }
    }

    public boolean isStudentTagged(String studentId) {
        return taggedStudents != null && taggedStudents.contains(studentId);
    }
}
