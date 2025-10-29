package com.example.kinderconnect.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Grade {
    @DocumentId
    private String gradeId;
    private String studentId;
    private String teacherId;
    private int period; // 1, 2, or 3
    private Map<String, AreaEvaluation> evaluations;
    @ServerTimestamp
    private Date createdAt;
    private Date updatedAt;

    public static class AreaEvaluation {
        private String areaName;
        private String level; // "REQUIERE_APOYO", "EN_DESARROLLO", "ESPERADO", "SOBRESALIENTE"
        private String observations;

        public AreaEvaluation() {
            // Constructor vacío requerido por Firebase
        }

        public AreaEvaluation(String areaName, String level, String observations) {
            this.areaName = areaName;
            this.level = level;
            this.observations = observations;
        }

        // Getters y Setters
        public String getAreaName() { return areaName; }
        public void setAreaName(String areaName) { this.areaName = areaName; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getObservations() { return observations; }
        public void setObservations(String observations) { this.observations = observations; }
    }

    public Grade() {
        this.evaluations = new HashMap<>();
    }

    public Grade(String studentId, String teacherId, int period) {
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.period = period;
        this.evaluations = new HashMap<>();
    }

    // Getters y Setters
    public String getGradeId() { return gradeId; }
    public void setGradeId(String gradeId) { this.gradeId = gradeId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }

    public Map<String, AreaEvaluation> getEvaluations() { return evaluations; }
    public void setEvaluations(Map<String, AreaEvaluation> evaluations) {
        this.evaluations = evaluations;
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public void addEvaluation(String areaKey, String areaName, String level, String observations) {
        evaluations.put(areaKey, new AreaEvaluation(areaName, level, observations));
    }
}
