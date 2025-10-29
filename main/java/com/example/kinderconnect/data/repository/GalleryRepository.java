package com.example.kinderconnect.data.repository;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import java.util.List;

public class GalleryRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public GalleryRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public LiveData<Resource<String>> uploadMedia(GalleryItem galleryItem, Uri mediaUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (mediaUri != null) {
            uploadMediaFile(mediaUri, galleryItem, result);
        } else {
            result.setValue(Resource.error("No se seleccionó ningún archivo", null));
        }

        return result;
    }

    private void uploadMediaFile(Uri mediaUri, GalleryItem galleryItem,
                                 MutableLiveData<Resource<String>> result) {
        String extension = galleryItem.getMediaType().equals(Constants.MEDIA_IMAGE) ? ".jpg" : ".mp4";
        String fileName = Constants.STORAGE_GALLERY + System.currentTimeMillis() + extension;
        StorageReference mediaRef = storage.getReference().child(fileName);

        mediaRef.putFile(mediaUri)
                .addOnSuccessListener(taskSnapshot -> {
                    mediaRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        galleryItem.setMediaUrl(uri.toString());
                        galleryItem.setThumbnailUrl(uri.toString());
                        saveGalleryItemToFirestore(galleryItem, result);
                    }).addOnFailureListener(e -> result.setValue(Resource.error(
                            "Error al obtener URL: " + e.getMessage(), null)));
                })
                .addOnProgressListener(snapshot -> {
                    // Aquí podrías actualizar el progreso de subida
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al subir archivo: " + e.getMessage(), null)));
    }

    private void saveGalleryItemToFirestore(GalleryItem galleryItem,
                                            MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_GALLERY)
                .add(galleryItem)
                .addOnSuccessListener(documentReference ->
                        result.setValue(Resource.success(documentReference.getId())))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al guardar: " + e.getMessage(), null)));
    }

    public LiveData<Resource<List<GalleryItem>>> getGalleryByGroup(String groupName) {
        MutableLiveData<Resource<List<GalleryItem>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GALLERY)
                .whereEqualTo("groupName", groupName)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<GalleryItem> items = value.toObjects(GalleryItem.class);
                        result.setValue(Resource.success(items));
                    }
                });

        return result;
    }

    public LiveData<Resource<List<GalleryItem>>> getGalleryByStudent(String studentId) {
        MutableLiveData<Resource<List<GalleryItem>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GALLERY)
                .whereArrayContains("taggedStudents", studentId)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<GalleryItem> items = value.toObjects(GalleryItem.class);
                        result.setValue(Resource.success(items));
                    }
                });

        return result;
    }

    public LiveData<Resource<Void>> deleteGalleryItem(String itemId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GALLERY)
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }
}
