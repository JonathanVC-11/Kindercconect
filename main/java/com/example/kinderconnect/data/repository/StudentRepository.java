package com.example.kinderconnect.data.repository;

import android.net.Uri;
import android.util.Log; // <-- AÑADIDO
import androidx.annotation.Nullable; // <-- AÑADIDO
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.Group; // <-- AÑADIDO
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
    private final GroupRepository groupRepository; // <-- AÑADIDO
    private static final String COLLECTION_STUDENTS = "students";
    private static final String COLLECTION_USERS = "users";
    private static final String TAG = "StudentRepository"; // <-- AÑADIDO

    public StudentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.groupRepository = new GroupRepository(); // <-- AÑADIDO
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


    // --- INICIO DE CÓDIGO AÑADIDO ---
    /**
     * Registra un nuevo alumno desde el panel del Padre.
     * Busca el grupo de la maestra por email y asigna los IDs.
     */
    public LiveData<Resource<Student>> registerStudent(Student student, String teacherEmail, String parentId) {
        MutableLiveData<Resource<Student>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // 1. Asignar el parentId de la sesión
        student.setParentId(parentId);

        // 2. Buscar el grupo de la maestra por su email
        groupRepository.getGroupByTeacherEmail(teacherEmail).observeForever(groupResource -> {
            if (groupResource.getStatus() == Resource.Status.SUCCESS) {
                Group group = groupResource.getData();
                if (group != null) {
                    // 3. Asignar datos del grupo encontrado
                    student.setTeacherId(group.getTeacherId());
                    student.setGroupName(group.getGrade() + " " + group.getGroupName()); // Ej: "1ro A"

                    // 4. Proceder a guardar el alumno (con o sin foto)
                    Uri imageUri = null; // Asumimos que la URI se manejará en el siguiente paso
                    // Este es un placeholder si 'addStudent' se usaba para subir fotos.
                    // Vamos a simplificar y asumir que la URI se pasa.
                    // Re-leyendo `addStudent`, veo que recibía URI.
                    // El nuevo método `registerStudent` debe recibir la URI también.
                    // Lo ajustaré en el ViewModel y Fragment. Por ahora, asumimos que no hay foto.

                    // Re-simplificando: El repo no debería manejar la URI, el VM sí.
                    // PERO el `uploadImageAndSaveStudent` está aquí.
                    // Lo mantendremos coherente. El VM pasará la URI.

                    // Ajuste: El método SÍ debe recibir la URI. La firma se corregirá en el VM.
                    // Por ahora, solo simulamos la lógica de guardado sin foto.
                    // Esta lógica se completará cuando creemos el fragment del padre.

                    // VAMOS A USAR la firma completa
                    // public LiveData<Resource<Student>> registerStudent(Student student, String teacherEmail, String parentId, Uri imageUri)
                    // La lógica del VM y Fragment se adaptará a esto.

                    // El método `uploadImageAndSaveStudent` ya existe y lo reutilizaremos.
                    // (Asumimos que la URI de la foto se pasará desde el ViewModel)

                    // ** Lógica Final **
                    // Lo sentimos, la firma debe ser simple. El repo `addStudent` original
                    // hacía demasiado.
                    // El ViewModel buscará el grupo, y luego llamará a un `addStudent` simple.

                    // Modificación: El VM orquestará. El StudentRepo solo guardará.
                    Log.d(TAG, "Grupo encontrado: " + group.getGroupName() + ". Asignando IDs.");
                    student.setTeacherId(group.getTeacherId());
                    student.setGroupName(group.getGrade() + " " + group.getGroupName());
                    student.setParentId(parentId);

                    // Reutilizamos el método 'saveStudentToFirestore' (isUpdate = false)
                    // La subida de imagen la manejará el ViewModel
                    saveStudentToFirestore(student, result, false);

                } else {
                    result.setValue(Resource.error("No se encontró ningún grupo para el correo: " + teacherEmail, null));
                }
            } else if (groupResource.getStatus() == Resource.Status.ERROR) {
                result.setValue(Resource.error(groupResource.getMessage(), null));
            }
        });

        // Este LiveData es temporal y se actualiza por el observerForever
        return result;
    }

    /**
     * Sube la foto del alumno y luego guarda los datos en Firestore.
     * Este método SÍ lo llamará el ViewModel del Padre.
     */
    public LiveData<Resource<Student>> uploadAndRegisterStudent(Student student, Uri imageUri, MutableLiveData<Resource<Student>> result) {
        // Asumimos que el ViewModel ya pobló student.setParentId, student.setTeacherId, etc.
        if (imageUri != null) {
            uploadImageAndSaveStudent(student, imageUri, result, false); // isUpdate = false
        } else {
            saveStudentToFirestore(student, result, false); // isUpdate = false
        }
        return result;
    }
    // --- FIN DE CÓDIGO AÑADIDO ---


    // --- (Método addStudent original ELIMINADO) ---
    /*
    public LiveData<Resource<Student>> addStudent(Student student, String parentEmail, Uri imageUri) {
        ...
    }
    */


    // --- (updateStudent sin cambios) ---
    public LiveData<Resource<Student>> updateStudent(Student student, @Nullable Uri newImageUri) {
        MutableLiveData<Resource<Student>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (newImageUri != null) {
            deleteFromStorage(student.getPhotoUrl());
            uploadImageAndSaveStudent(student, newImageUri, result, true); // isUpdate = true
        } else {
            saveStudentToFirestore(student, result, true); // isUpdate = true
        }

        return result;
    }

    // --- (findParentByEmail ELIMINADO) ---
    /*
    private void findParentByEmail(String parentEmail, MutableLiveData<Resource<Student>> result, ParentIdCallback callback) {
        ...
    }
    interface ParentIdCallback {
        void onParentFound(String parentId);
    }
    */


    // --- (uploadImageAndSaveStudent sin cambios) ---
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

    // --- (saveStudentToFirestore sin cambios) ---
    private void saveStudentToFirestore(Student student, MutableLiveData<Resource<Student>> result, boolean isUpdate) {
        if (!isUpdate) {
            // Lógica de CREAR
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

    // ... (getStudentsByParent, getStudentById, getStudentsByTeacher, deleteStudent sin cambios) ...

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