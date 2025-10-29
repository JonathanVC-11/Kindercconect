package com.example.kinderconnect.data.repository;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StudentRepository {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private static final String COLLECTION_STUDENTS = "students";
    private static final String COLLECTION_USERS = "users";

    public StudentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public LiveData<Resource<Student>> addStudent(Student student, String parentEmail, Uri imageUri) {
        MutableLiveData<Resource<Student>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        String normalizedEmail = parentEmail == null ? "" : parentEmail.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.isEmpty()) {
            db.collection(COLLECTION_USERS)
                    .whereEqualTo("email", normalizedEmail)
                    .whereEqualTo("userType", "PARENT")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String parentId = querySnapshot.getDocuments().get(0).getId();
                            student.setParentId(parentId);
                            if (imageUri != null) {
                                uploadImageAndSaveStudent(student, imageUri, result);
                            } else {
                                saveStudentToFirestore(student, result);
                            }
                        } else {
                            result.setValue(Resource.error("No se encontró un padre/madre con ese correo", null));
                        }
                    })
                    .addOnFailureListener(e ->
                            result.setValue(Resource.error("Error al buscar padre/madre: " + e.getMessage(), null))
                    );
        } else {
            if (imageUri != null) {
                uploadImageAndSaveStudent(student, imageUri, result);
            } else {
                saveStudentToFirestore(student, result);
            }
        }

        return result;
    }

    private void uploadImageAndSaveStudent(Student student, Uri imageUri, MutableLiveData<Resource<Student>> result) {
        String fileName = "students/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            student.setPhotoUrl(uri.toString());
                            saveStudentToFirestore(student, result);
                        })
                )
                .addOnFailureListener(e ->
                        result.setValue(Resource.error("Error al subir imagen: " + e.getMessage(), null))
                );
    }

    private void saveStudentToFirestore(Student student, MutableLiveData<Resource<Student>> result) {
        String studentId = db.collection(COLLECTION_STUDENTS).document().getId();
        student.setStudentId(studentId);
        db.collection(COLLECTION_STUDENTS)
                .document(studentId)
                .set(student)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(student)))
                .addOnFailureListener(e ->
                        result.setValue(Resource.error("Error al guardar: " + e.getMessage(), null))
                );
    }

// Obtiene la lista de estudiantes vinculados a un padre
public LiveData<Resource<List<Student>>> getStudentsByParent(String parentId) {
    MutableLiveData<Resource<List<Student>>> result = new MutableLiveData<>();
    result.setValue(Resource.loading(null));

    db.collection(COLLECTION_STUDENTS)
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    result.setValue(Resource.error(error.getMessage(), null));
                    return;
                }

                List<Student> students = new ArrayList<>();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        Student student = doc.toObject(Student.class);
                        students.add(student);
                    }
                }
                result.setValue(Resource.success(students));
            });

    return result;
}

// Obtiene un estudiante por su ID
public LiveData<Resource<Student>> getStudentById(String studentId) {
    MutableLiveData<Resource<Student>> result = new MutableLiveData<>();
    result.setValue(Resource.loading(null));

    db.collection(COLLECTION_STUDENTS)
            .document(studentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Student student = documentSnapshot.toObject(Student.class);
                    result.setValue(Resource.success(student));
                } else {
                    result.setValue(Resource.error("No se encontró el alumno", null));
                }
            })
            .addOnFailureListener(e ->
                    result.setValue(Resource.error(e.getMessage(), null))
            );

    return result;
}

    // Obtener estudiantes por profesor
    public LiveData<Resource<List<Student>>> getStudentsByTeacher(String teacherId) {
        MutableLiveData<Resource<List<Student>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_STUDENTS)
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    List<Student> students = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Student student = doc.toObject(Student.class);
                            students.add(student);
                        }
                    }
                    result.setValue(Resource.success(students));
                });

        return result;
    }

    // Actualizar estudiante
    public LiveData<Resource<Void>> updateStudent(Student student) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_STUDENTS)
                .document(student.getStudentId())
                .set(student)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e ->
                        result.setValue(Resource.error(e.getMessage(), null))
                );

        return result;
    }

    // Eliminar estudiante
    public LiveData<Resource<Void>> deleteStudent(String studentId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        db.collection(COLLECTION_STUDENTS)
                .document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e ->
                        result.setValue(Resource.error(e.getMessage(), null))
                );

        return result;
    }

}