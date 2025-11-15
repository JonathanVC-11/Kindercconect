package com.example.kinderconnect.data.repository;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
// --- INICIO DE IMPORTACIONES AÑADIDAS ---
import androidx.lifecycle.MediatorLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
// --- FIN DE IMPORTACIONES AÑADIDAS ---
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.kinderconnect.data.model.Notice;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import java.util.List;

public class NoticeRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private static final String TAG = "NoticeRepository";

    public NoticeRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public LiveData<Resource<String>> publishNotice(Notice notice, Uri imageUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (imageUri != null) {
            // Si hay imagen nueva, subirla y luego guardar
            uploadImage(imageUri, notice, result, null);
        } else {
            // Si no hay imagen, solo guardar en Firestore
            saveNoticeToFirestore(notice, result);
        }

        return result;
    }

    // Modificado para aceptar un ID existente (para saber si es 'crear' o 'actualizar')
    private void uploadImage(Uri imageUri, Notice notice,
                             MutableLiveData<Resource<String>> result,
                             @Nullable String existingNoticeId) {

        String fileName = Constants.STORAGE_NOTICES + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storage.getReference().child(fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        notice.setImageUrl(uri.toString()); // Pone la NUEVA url de imagen
                        if (existingNoticeId == null) {
                            saveNoticeToFirestore(notice, result); // CREAR
                        } else {
                            updateNoticeInFirestore(notice, result); // ACTUALIZAR
                        }
                    }).addOnFailureListener(e -> result.setValue(Resource.error(
                            "Error al obtener URL: " + e.getMessage(), null)));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al subir imagen: " + e.getMessage(), null)));
    }

    private void saveNoticeToFirestore(Notice notice,
                                       MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_NOTICES)
                .add(notice) // .add() crea un ID nuevo
                .addOnSuccessListener(documentReference ->
                        result.setValue(Resource.success(documentReference.getId())))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al publicar aviso: " + e.getMessage(), null)));
    }

    // --- INICIO DE LÓGICA DE EDICIÓN AÑADIDA ---
    // Método para actualizar un documento existente
    private void updateNoticeInFirestore(Notice notice, MutableLiveData<Resource<String>> result) {
        if (notice.getNoticeId() == null) {
            result.setValue(Resource.error("ID de aviso nulo al actualizar", null));
            return;
        }

        firestore.collection(Constants.COLLECTION_NOTICES)
                .document(notice.getNoticeId()) // .document() usa el ID existente
                .set(notice, SetOptions.merge()) // .set() con merge actualiza
                .addOnSuccessListener(aVoid ->
                        result.setValue(Resource.success(notice.getNoticeId())))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al actualizar aviso: " + e.getMessage(), null)));
    }

    // Método principal para actualizar. Maneja la lógica de la imagen.
    public LiveData<Resource<String>> updateNotice(Notice notice, @Nullable Uri newImageUri, @Nullable String oldImageUrl) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (newImageUri != null) {
            // 1. Usuario seleccionó una NUEVA imagen
            // Borramos la antigua primero
            deleteFromStorage(oldImageUrl);
            // Subimos la nueva, y 'uploadImage' llamará a 'updateNoticeInFirestore'
            uploadImage(newImageUri, notice, result, notice.getNoticeId());
        } else {
            // 2. Usuario NO seleccionó imagen nueva
            // Mantenemos la 'oldImageUrl' que ya está en el objeto 'notice'
            notice.setImageUrl(oldImageUrl);
            // Solo actualizamos Firestore
            updateNoticeInFirestore(notice, result);
        }
        return result;
    }
    // --- FIN DE LÓGICA DE EDICIÓN AÑADIDA ---


    // --- INICIO DE CÓDIGO MODIFICADO ---

    /**
     * MÉTODO NUEVO: Combina avisos del grupo y de la escuela para un padre.
     */
    public LiveData<Resource<List<Notice>>> getNoticesForParent(String groupName) {
        // MediatorLiveData nos permite combinar 2 fuentes (queries)
        MediatorLiveData<Resource<List<Notice>>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading(null));

        // Listas para guardar los resultados de cada query
        HashMap<String, Notice> groupNotices = new HashMap<>();
        HashMap<String, Notice> schoolNotices = new HashMap<>();

        // Fuente 1: Avisos del GRUPO
        LiveData<Resource<List<Notice>>> groupSource = getNoticesByGroup(groupName);

        // Fuente 2: Avisos de la ESCUELA
        LiveData<Resource<List<Notice>>> schoolSource = getNoticesByScope(Constants.SCOPE_SCHOOL);

        // Función helper para combinar y emitir
        Runnable combineResults = () -> {
            HashMap<String, Notice> allNoticesMap = new HashMap<>();
            allNoticesMap.putAll(schoolNotices);
            allNoticesMap.putAll(groupNotices); // Si hay duplicados, el del grupo "gana"

            ArrayList<Notice> combinedList = new ArrayList<>(allNoticesMap.values());

            // Ordenar por fecha, el más nuevo primero
            Collections.sort(combinedList, (o1, o2) -> {
                if (o1.getPublishedAt() == null || o2.getPublishedAt() == null) return 0;
                return o2.getPublishedAt().compareTo(o1.getPublishedAt());
            });

            result.setValue(Resource.success(combinedList));
        };

        // Observar la Fuente 1 (Grupo)
        result.addSource(groupSource, resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                    groupNotices.clear();
                    for (Notice n : resource.getData()) {
                        groupNotices.put(n.getNoticeId(), n);
                    }
                    combineResults.run();
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    result.setValue(Resource.error(resource.getMessage(), null));
                }
            }
        });

        // Observar la Fuente 2 (Escuela)
        result.addSource(schoolSource, resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                    schoolNotices.clear();
                    for (Notice n : resource.getData()) {
                        schoolNotices.put(n.getNoticeId(), n);
                    }
                    combineResults.run();
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    result.setValue(Resource.error(resource.getMessage(), null));
                }
            }
        });

        return result;
    }

    /**
     * Este método ahora solo trae avisos por 'groupName'
     * (Usado por la Maestra y por 'getNoticesForParent')
     */
    public LiveData<Resource<List<Notice>>> getNoticesByGroup(String groupName) {
        MutableLiveData<Resource<List<Notice>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .whereEqualTo("groupName", groupName)
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<Notice> notices = value.toObjects(Notice.class);
                        // Asignar IDs
                        for(int i=0; i < value.getDocuments().size(); i++){
                            notices.get(i).setNoticeId(value.getDocuments().get(i).getId());
                        }
                        result.setValue(Resource.success(notices));
                    }
                });

        return result;
    }

    /**
     * MÉTODO NUEVO: Trae avisos por 'scope' (para los de "SCHOOL")
     */
    public LiveData<Resource<List<Notice>>> getNoticesByScope(String scope) {
        MutableLiveData<Resource<List<Notice>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .whereEqualTo("scope", scope) // Query por "SCHOOL"
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<Notice> notices = value.toObjects(Notice.class);
                        // Asignar IDs
                        for(int i=0; i < value.getDocuments().size(); i++){
                            notices.get(i).setNoticeId(value.getDocuments().get(i).getId());
                        }
                        result.setValue(Resource.success(notices));
                    }
                });

        return result;
    }
    // --- FIN DE CÓDIGO MODIFICADO ---


    // --- INICIO DE MÉTODOS RESTAURADOS ---
    public LiveData<Resource<List<Notice>>> getAllNotices() {
        MutableLiveData<Resource<List<Notice>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<Notice> notices = value.toObjects(Notice.class);
                        // Asignar IDs
                        for(int i=0; i < value.getDocuments().size(); i++){
                            notices.get(i).setNoticeId(value.getDocuments().get(i).getId());
                        }
                        result.setValue(Resource.success(notices));
                    }
                });

        return result;
    }

    public LiveData<Resource<Void>> markAsRead(String noticeId, String userId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .document(noticeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Notice notice = documentSnapshot.toObject(Notice.class);
                    if (notice != null) {
                        notice.markAsReadBy(userId);
                        firestore.collection(Constants.COLLECTION_NOTICES)
                                .document(noticeId)
                                .update("readBy", notice.getReadBy())
                                .addOnSuccessListener(aVoid ->
                                        result.setValue(Resource.success(null)))
                                .addOnFailureListener(e -> result.setValue(Resource.error(
                                        "Error: " + e.getMessage(), null)));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }
    // --- FIN DE MÉTODOS RESTAURADOS ---


    // --- LÓGICA DE ELIMINACIÓN MODIFICADA (de la vez pasada) ---
    // Método helper para borrar de Storage
    private void deleteFromStorage(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) return;
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(mediaUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG,"Archivo de Storage eliminado: " + mediaUrl))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar de Storage: "+ mediaUrl, e));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL inválida, no se puede eliminar: " + mediaUrl, e);
        }
    }

    public LiveData<Resource<Void>> deleteNotice(String noticeId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES).document(noticeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String imageUrlToDelete = null;
                    if (documentSnapshot.exists()) {
                        Notice notice = documentSnapshot.toObject(Notice.class);
                        if (notice != null && notice.getImageUrl() != null && !notice.getImageUrl().isEmpty()) {
                            imageUrlToDelete = notice.getImageUrl();
                        }
                    }

                    final String finalImageUrl = imageUrlToDelete;

                    firestore.collection(Constants.COLLECTION_NOTICES)
                            .document(noticeId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                result.setValue(Resource.success(null));
                                deleteFromStorage(finalImageUrl);
                            })
                            .addOnFailureListener(e -> result.setValue(Resource.error(
                                    "Error al eliminar aviso: " + e.getMessage(), null)));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener aviso antes de borrar. Borrando solo doc.", e);
                    firestore.collection(Constants.COLLECTION_NOTICES)
                            .document(noticeId)
                            .delete()
                            .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                            .addOnFailureListener(eDel -> result.setValue(Resource.error(
                                    "Error al eliminar aviso: " + eDel.getMessage(), null)));
                });

        return result;
    }
    // --- FIN DE LÓGICA DE ELIMINACIÓN ---


    // --- LÓGICA DE OBTENER POR ID (de la vez pasada) ---
    public LiveData<Resource<Notice>> getNoticeById(String noticeId) {
        MutableLiveData<Resource<Notice>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .document(noticeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Notice notice = documentSnapshot.toObject(Notice.class);
                        // Asignamos el ID del documento al objeto
                        notice.setNoticeId(documentSnapshot.getId());
                        result.setValue(Resource.success(notice));
                    } else {
                        result.setValue(Resource.error("No se encontró el aviso", null));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " .concat(e.getMessage()), null)));

        return result;
    }
}