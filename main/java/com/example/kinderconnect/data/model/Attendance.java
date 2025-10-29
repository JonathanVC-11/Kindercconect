package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Attendance {
    @DocumentId
    private String attendanceId;
    private String studentId;
    private String teacherId;
    private Date attendanceDate;
    private String status; // "PRESENT", "LATE", "ABSENT"
    private String notes;
    @ServerTimestamp
    private Date recordedAt;
    private boolean parentNotified;

    public Attendance() {
        // Constructor vacío requerido por Firebase
    }

    public Attendance(String studentId, String teacherId, Date attendanceDate, String status) {
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.parentNotified = false;
    }

    // Getters y Setters
    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public Date getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(Date attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Date recordedAt) { this.recordedAt = recordedAt; }

    public boolean isParentNotified() { return parentNotified; }
    public void setParentNotified(boolean parentNotified) { this.parentNotified = parentNotified; }
}
