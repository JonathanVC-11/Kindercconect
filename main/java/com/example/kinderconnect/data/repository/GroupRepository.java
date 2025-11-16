package com.example.kinderconnect.data.repository;

import android.util.Log; // <-- AÑADIDO
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.Group;
import com.example.kinderconnect.data.model.Student; // <-- AÑADIDO
import com.example.kinderconnect.data.model.User; // <-- AÑADIDO
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot; // <-- AÑADIDO
import com.google.firebase.firestore.WriteBatch; // <-- AÑADIDO

public class GroupRepository {
    private final FirebaseFirestore firestore;
    private final AuthRepository authRepository; // <-- AÑADIDO
    private static final String TAG = "GroupRepository"; // <-- AÑADIDO

    public GroupRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.authRepository = new AuthRepository(); // <-- AÑADIDO
    }

    /**
     * Obtiene el grupo que pertenece a una maestra.
     */
    public LiveData<Resource<Group>> getGroupByTeacher(String teacherId) {
        MutableLiveData<Resource<Group>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GROUPS)
                .whereEqualTo("teacherId", teacherId)
                .limit(1) // Una maestra solo debe tener un grupo
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        Group group = value.getDocuments().get(0).toObject(Group.class);
                        if (group != null) {
                            group.setGroupId(value.getDocuments().get(0).getId());
                            result.setValue(Resource.success(group));
                        }
                    } else {
                        // Es normal que no tenga grupo, devolvemos success con null
                        result.setValue(Resource.success(null));
                    }
                });
        return result;
    }

    /**
     * Crea un nuevo grupo, verificando primero que no exista y que la maestra no tenga ya uno.
     */
    public LiveData<Resource<Group>> createGroup(Group group) {
        MutableLiveData<Resource<Group>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // 1. Verificar que la maestra no tenga ya un grupo
        firestore.collection(Constants.COLLECTION_GROUPS)
                .whereEqualTo("teacherId", group.getTeacherId())
                .get()
                .addOnSuccessListener(teacherCheck -> {
                    if (!teacherCheck.isEmpty()) {
                        result.setValue(Resource.error("Ya tienes un grupo registrado. Contacta al administrador para reasignarlo.", null));
                        return;
                    }

                    // 2. Verificar que el Grado y Grupo no existan
                    firestore.collection(Constants.COLLECTION_GROUPS)
                            .whereEqualTo("grade", group.getGrade())
                            .whereEqualTo("groupName", group.getGroupName())
                            .get()
                            .addOnSuccessListener(groupCheck -> {
                                if (!groupCheck.isEmpty()) {
                                    // ¡Duplicado!
                                    result.setValue(Resource.error("El grupo " + group.getGrade() + " - " + group.getGroupName() + " ya existe y está asignado a otra maestra.", null));
                                } else {
                                    // 3. No hay duplicados, crear el grupo
                                    firestore.collection(Constants.COLLECTION_GROUPS)
                                            .add(group)
                                            .addOnSuccessListener(docRef -> {
                                                group.setGroupId(docRef.getId());
                                                result.setValue(Resource.success(group));
                                            })
                                            .addOnFailureListener(e -> result.setValue(Resource.error("Error al crear el grupo: " + e.getMessage(), null)));
                                }
                            })
                            .addOnFailureListener(e -> result.setValue(Resource.error("Error al verificar duplicados: " + e.getMessage(), null)));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error("Error al verificar maestra: " + e.getMessage(), null)));

        return result;
    }

    /**
     * Busca un grupo por el email de la maestra.
     * Esto lo usará el Padre al registrar un alumno.
     */
    public LiveData<Resource<Group>> getGroupByTeacherEmail(String teacherEmail) {
        MutableLiveData<Resource<Group>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GROUPS)
                .whereEqualTo("teacherEmail", teacherEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(value -> {
                    if (value != null && !value.isEmpty()) {
                        Group group = value.getDocuments().get(0).toObject(Group.class);
                        if (group != null) {
                            group.setGroupId(value.getDocuments().get(0).getId());
                            result.setValue(Resource.success(group));
                        }
                    } else {
                        result.setValue(Resource.error("No se encontró ningún grupo asignado a ese correo.", null));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
        return result;
    }


    // --- INICIO DE CÓDIGO AÑADIDO ---
    /**
     * Transfiere un grupo y todos sus alumnos a una nueva maestra.
     */
    public LiveData<Resource<Void>> transferGroup(Group currentGroup, String newTeacherEmail) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (currentGroup == null || currentGroup.getGroupId() == null) {
            result.setValue(Resource.error("Error: El grupo actual es inválido.", null));
            return result;
        }

        // 1. Buscar a la nueva maestra por email
        MutableLiveData<Resource<User>> newTeacherResult = new MutableLiveData<>();
        authRepository.findUserByEmail(newTeacherEmail, Constants.USER_TYPE_TEACHER, newTeacherResult);

        newTeacherResult.observeForever(userResource -> {
            if (userResource.getStatus() == Resource.Status.LOADING) return;

            if (userResource.getStatus() == Resource.Status.ERROR) {
                result.setValue(Resource.error(userResource.getMessage(), null));
                return;
            }

            User newTeacher = userResource.getData();
            if (newTeacher == null) {
                result.setValue(Resource.error("No se pudo obtener la información de la nueva maestra.", null));
                return;
            }

            // 2. Verificar que la nueva maestra NO tenga ya un grupo
            firestore.collection(Constants.COLLECTION_GROUPS)
                    .whereEqualTo("teacherId", newTeacher.getUid())
                    .get()
                    .addOnSuccessListener(groupCheck -> {
                        if (!groupCheck.isEmpty()) {
                            result.setValue(Resource.error("Error: La maestra " + newTeacher.getFullName() + " ya tiene un grupo asignado.", null));
                            return;
                        }

                        // 3. Todo en orden, proceder con la transferencia
                        transferData(currentGroup, newTeacher, result);
                    })
                    .addOnFailureListener(e -> result.setValue(Resource.error("Error al verificar grupo destino: " + e.getMessage(), null)));

        });

        return result;
    }

    private void transferData(Group currentGroup, User newTeacher, MutableLiveData<Resource<Void>> result) {
        Log.d(TAG, "Iniciando transferencia del grupo " + currentGroup.getGroupId() + " a " + newTeacher.getUid());

        // 4. Buscar todos los alumnos de la maestra actual
        firestore.collection(Constants.COLLECTION_STUDENTS)
                .whereEqualTo("teacherId", currentGroup.getTeacherId())
                .get()
                .addOnSuccessListener(studentSnapshot -> {
                    WriteBatch batch = firestore.batch();

                    // 5. Actualizar el documento del Grupo
                    batch.update(
                            firestore.collection(Constants.COLLECTION_GROUPS).document(currentGroup.getGroupId()),
                            "teacherId", newTeacher.getUid(),
                            "teacherEmail", newTeacher.getEmail(),
                            "teacherName", newTeacher.getFullName()
                    );
                    Log.d(TAG, "Batch: Actualizando documento del grupo...");

                    // 6. Actualizar cada alumno
                    for (QueryDocumentSnapshot studentDoc : studentSnapshot) {
                        batch.update(studentDoc.getReference(), "teacherId", newTeacher.getUid());
                    }
                    Log.d(TAG, "Batch: Actualizando " + studentSnapshot.size() + " alumnos...");

                    // 7. Ejecutar el batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "¡Transferencia completada!");
                                result.setValue(Resource.success(null));
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al ejecutar el batch de transferencia", e);
                                result.setValue(Resource.error("Error al transferir: " + e.getMessage(), null));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al buscar alumnos para transferir", e);
                    result.setValue(Resource.error("Error al buscar alumnos: " + e.getMessage(), null));
                });
    }
    // --- FIN DE CÓDIGO AÑADIDO ---
}