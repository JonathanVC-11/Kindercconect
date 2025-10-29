package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.kinderconnect.data.model.Grade;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import java.util.Date;
import java.util.List;

public class GradeRepository {
    private final FirebaseFirestore firestore;

    public GradeRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<Resource<String>> saveGrade(Grade grade) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        grade.setUpdatedAt(new Date());

        // Verificar si ya existe una evaluación para ese estudiante y periodo
        firestore.collection(Constants.COLLECTION_GRADES)
                .whereEqualTo("studentId", grade.getStudentId())
                .whereEqualTo("period", grade.getPeriod())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Actualizar evaluación existente
                        String gradeId = querySnapshot.getDocuments().get(0).getId();
                        updateExistingGrade(gradeId, grade, result);
                    } else {
                        // Crear nueva evaluación
                        createNewGrade(grade, result);
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al verificar calificaciones: " + e.getMessage(), null)));

        return result;
    }

    private void updateExistingGrade(String gradeId, Grade grade,
                                     MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_GRADES)
                .document(gradeId)
                .set(grade)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(gradeId)))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al actualizar: " + e.getMessage(), null)));
    }

    private void createNewGrade(Grade grade, MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_GRADES)
                .add(grade)
                .addOnSuccessListener(documentReference ->
                        result.setValue(Resource.success(documentReference.getId())))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al guardar: " + e.getMessage(), null)));
    }

    public LiveData<Resource<List<Grade>>> getGradesByStudent(String studentId) {
        MutableLiveData<Resource<List<Grade>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GRADES)
                .whereEqualTo("studentId", studentId)
                .orderBy("period", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<Grade> grades = value.toObjects(Grade.class);
                        result.setValue(Resource.success(grades));
                    }
                });

        return result;
    }

    public LiveData<Resource<Grade>> getGradeByStudentAndPeriod(String studentId, int period) {
        MutableLiveData<Resource<Grade>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GRADES)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("period", period)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Grade grade = querySnapshot.getDocuments().get(0).toObject(Grade.class);
                        result.setValue(Resource.success(grade));
                    } else {
                        result.setValue(Resource.success(null));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }
}
