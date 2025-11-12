package com.example.kinderconnect.data.repository;

import android.net.Uri;
import android.util.Log; // <-- AÑADIDO
import androidx.annotation.Nullable; // <-- AÑADIDO
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions; // <-- AÑADIDO
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
    private static final String TAG = "StudentRepository"; // <-- AÑADIDO

    public StudentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // Método helper para borrar fotos de Storage
    private void deleteFromStorage(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(mediaUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG,"Foto de alumno eliminada: " + mediaUrl))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar foto: "+ mediaUrl, e));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL inválida, no se puede eliminar: " + mediaUrl, e);
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    public LiveData<Resource<Student>> addStudent(Student student, String parentEmail, Uri imageUri) {
        MutableLiveData<Resource<Student>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        findParentByEmail(parentEmail, result, (parentId) -> {
            student.setParentId(parentId);
            if (imageUri != null) {
                uploadImageAndSaveStudent(student, imageUri, result, false);
            } else {
                saveStudentToFirestore(student, result, false);
            }
        });

        return result;
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // Nuevo método de ACTUALIZACIÓN
    public LiveData<Resource<Student>> updateStudent(Student student, String parentEmail, @Nullable Uri newImageUri) {
        MutableLiveData<Resource<Student>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // La lógica de buscar padre es la misma
        findParentByEmail(parentEmail, result, (parentId) -> {
            student.setParentId(parentId);
            if (newImageUri != null) {
                // Si hay imagen NUEVA, borramos la antigua (si existe) y subimos la nueva
                deleteFromStorage(student.getPhotoUrl()); // Borra la foto antigua
                uploadImageAndSaveStudent(student, newImageUri, result, true); // Sube la nueva
            } else {
                // Si NO hay imagen nueva, solo actualizamos Firestore
                saveStudentToFirestore(student, result, true);
            }
        });

        return result;
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    // Método helper para buscar al padre
    private void findParentByEmail(String parentEmail, MutableLiveData<Resource<Student>> result, ParentIdCallback callback) {
        String normalizedEmail = parentEmail == null ? "" : parentEmail.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.isEmpty()) {
            db.collection(COLLECTION_USERS)
                    .whereEqualTo("email", normalizedEmail)
                    .whereEqualTo("userType", "PARENT")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String parentId = querySnapshot.getDocuments().get(0).getId();
                            callback.onParentFound(parentId);
                        } else {
                            result.setValue(Resource.error("No se encontró un padre/madre con ese correo", null));
                        }
                    })
                    .addOnFailureListener(e ->
                            result.setValue(Resource.error("Error al buscar padre/madre: " + e.getMessage(), null))
                    );
        } else {
            result.setValue(Resource.error("El email del padre/madre es obligatorio", null));
        }
    }

    // Interfaz helper
    interface ParentIdCallback {
        void onParentFound(String parentId);
    }


    // --- MÉTODO MODIFICADO ---
    // Añadido boolean 'isUpdate'
    private void uploadImageAndSaveStudent(Student student, Uri imageUri, MutableLiveData<Resource<Student>> result, boolean isUpdate) {
        String fileName = "students/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            student.setPhotoUrl(uri.toString());
                            saveStudentToFirestore(student, result, isUpdate);
                        })
                )
                .addOnFailureListener(e ->
                        result.setValue(Resource.error("Error al subir imagen: " + e.getMessage(), null))
                );
    }

    // --- MÉTODO MODIFICADO ---
    // Añadido boolean 'isUpdate'
    private void saveStudentToFirestore(Student student, MutableLiveData<Resource<Student>> result, boolean isUpdate) {
        if (!isUpdate) {
            // Lógica de CREAR (la que ya tenías)
            String studentId = db.collection(COLLECTION_STUDENTS).document().getId();
            student.setStudentId(studentId);
            db.collection(COLLECTION_STUDENTS)
                    .document(studentId)
                    .set(student)
                    .addOnSuccessListener(aVoid -> result.setValue(Resource.success(student)))
                    .addOnFailureListener(e ->
                            result.setValue(Resource.error("Error al guardar: " + e.getMessage(), null))
                    );
        } else {
            // Lógica de ACTUALIZAR
            db.collection(COLLECTION_STUDENTS)
                    .document(student.getStudentId())
                    .set(student, SetOptions.merge()) // Usar SET con MERGE
                    .addOnSuccessListener(aVoid -> result.setValue(Resource.success(student)))
                    .addOnFailureListener(e ->
                            result.setValue(Resource.error("Error al actualizar: " + e.getMessage(), null))
                    );
        }
    }

    // ... (getStudentsByParent y getStudentById no cambian) ...
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


    // --- MÉTODO 'updateStudent' ELIMINADO ---
    // (Lo reemplazamos por el nuevo 'updateStudent' que acepta email y URI)
    /*
    public LiveData<Resource<Void>> updateStudent(Student student) { ... }
    */

    // --- MÉTODO 'deleteStudent' MODIFICADO ---
    public LiveData<Resource<Void>> deleteStudent(String studentId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // 1. Obtener el documento para saber la URL de la foto
        db.collection(COLLECTION_STUDENTS).document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String photoUrlToDelete = null;
                    if (documentSnapshot.exists()) {
                        Student student = documentSnapshot.toObject(Student.class);
                        if (student != null && student.getPhotoUrl() != null) {
                            photoUrlToDelete = student.getPhotoUrl();
                        }
                    }

                    final String finalPhotoUrl = photoUrlToDelete;

                    // 2. Borrar el documento de Firestore
                    db.collection(COLLECTION_STUDENTS)
                            .document(studentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // 3. Borrar la foto de Storage
                                deleteFromStorage(finalPhotoUrl);
                                result.setValue(Resource.success(null));
                            })
                            .addOnFailureListener(e ->
                                    result.setValue(Resource.error(e.getMessage(), null))
                            );
                })
                .addOnFailureListener(e -> {
                    // Si falla al obtener, igual intentamos borrar (pero no la foto)
                    Log.e(TAG, "No se pudo obtener alumno antes de borrar. Borrando solo doc.", e);
                    db.collection(COLLECTION_STUDENTS)
                            .document(studentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                            .addOnFailureListener(eDel ->
                                    result.setValue(Resource.error(eDel.getMessage(), null))
                            );
                });

        return result;
    }
}