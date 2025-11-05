package com.example.kinderconnect.data.repository;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.kinderconnect.data.model.Notice;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import java.util.List;

public class NoticeRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public NoticeRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public LiveData<Resource<String>> publishNotice(Notice notice, Uri imageUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (imageUri != null) {
            uploadImage(imageUri, notice, result);
        } else {
            saveNoticeToFirestore(notice, result);
        }

        return result;
    }

    private void uploadImage(Uri imageUri, Notice notice,
                             MutableLiveData<Resource<String>> result) {
        String fileName = Constants.STORAGE_NOTICES + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storage.getReference().child(fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        notice.setImageUrl(uri.toString());
                        saveNoticeToFirestore(notice, result);
                    }).addOnFailureListener(e -> result.setValue(Resource.error(
                            "Error al obtener URL: " + e.getMessage(), null)));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al subir imagen: " + e.getMessage(), null)));
    }

    private void saveNoticeToFirestore(Notice notice,
                                       MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_NOTICES)
                .add(notice)
                .addOnSuccessListener(documentReference ->
                        result.setValue(Resource.success(documentReference.getId())))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al publicar aviso: " + e.getMessage(), null)));
    }

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

    public LiveData<Resource<Void>> deleteNotice(String noticeId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .document(noticeId)
                .delete()
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }

    // --- ¡¡NUEVO MÉTODO AÑADIDO AQUÍ!! ---
    public LiveData<Resource<Notice>> getNoticeById(String noticeId) {
        MutableLiveData<Resource<Notice>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTICES)
                .document(noticeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Notice notice = documentSnapshot.toObject(Notice.class);
                        result.setValue(Resource.success(notice));
                    } else {
                        result.setValue(Resource.error("No se encontró el aviso", null));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }
}