package com.example.kinderconnect.data.repository;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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


    // --- INICIO DE MÉTODOS RESTAURADOS ---
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