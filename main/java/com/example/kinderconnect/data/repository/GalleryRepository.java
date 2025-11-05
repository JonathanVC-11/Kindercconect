package com.example.kinderconnect.data.repository;

import android.net.Uri;
import android.util.Log; // ¡Importante añadir Log!

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import java.util.ArrayList; // ¡Importante para listas vacías!
import java.util.List;
import java.util.UUID; // ¡Importante para nombre único!

public class GalleryRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private static final String TAG = "GalleryRepository"; // Tag para Logs

    public GalleryRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public LiveData<Resource<String>> uploadMedia(GalleryItem galleryItem, Uri mediaUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (mediaUri == null) {
            Log.e(TAG, "Intento de subir archivo con mediaUri nulo.");
            result.setValue(Resource.error("No se seleccionó ningún archivo", null));
            return result;
        }

        // Generar un nombre de archivo único
        String extension = galleryItem.getMediaType().equals(Constants.MEDIA_IMAGE) ? ".jpg" : ".mp4";
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        String storagePath = Constants.STORAGE_GALLERY + uniqueFileName;
        StorageReference mediaRef = storage.getReference().child(storagePath);

        Log.d(TAG, "Iniciando subida a Storage: " + storagePath);

        // 1. Subir el archivo
        mediaRef.putFile(mediaUri)
                .addOnProgressListener(snapshot -> {
                    // Opcional: Calcular y mostrar progreso
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Progreso de subida: " + String.format("%.2f", progress) + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Archivo subido exitosamente a Storage.");
                    // 2. Obtener la URL de descarga
                    mediaRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "URL de descarga obtenida: " + downloadUri.toString());
                                // 3. Asignar URLs y guardar en Firestore
                                galleryItem.setMediaUrl(downloadUri.toString());
                                // Simplificación: usar la misma URL como thumbnail.
                                galleryItem.setThumbnailUrl(downloadUri.toString());
                                saveGalleryItemToFirestore(galleryItem, result);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al obtener URL de descarga", e);
                                result.setValue(Resource.error("Error al obtener URL: " + e.getMessage(), null));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al subir archivo a Storage", e);
                    result.setValue(Resource.error("Error al subir archivo: " + e.getMessage(), null));
                });

        return result;
    }

    private void saveGalleryItemToFirestore(GalleryItem galleryItem,
                                            MutableLiveData<Resource<String>> result) {
        Log.d(TAG, "Guardando metadatos en Firestore...");
        firestore.collection(Constants.COLLECTION_GALLERY)
                .add(galleryItem)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Metadatos guardados en Firestore con ID: " + documentReference.getId());
                    result.setValue(Resource.success(documentReference.getId()));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar en Firestore", e);
                    result.setValue(Resource.error("Error al guardar: " + e.getMessage(), null));
                });
    }

    public LiveData<Resource<List<GalleryItem>>> getGalleryByGroup(String groupName) {
        MutableLiveData<Resource<List<GalleryItem>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GALLERY)
                .whereEqualTo("groupName", groupName)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error al obtener galería por grupo: ", error);
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<GalleryItem> items = value.toObjects(GalleryItem.class);
                        Log.d(TAG, "Galería por grupo cargada. Items: " + items.size());
                        result.setValue(Resource.success(items));
                    } else {
                        Log.d(TAG, "Snapshot nulo al obtener galería por grupo. Devolviendo lista vacía.");
                        result.setValue(Resource.success(new ArrayList<>())); // Lista vacía si no hay valor
                    }
                });

        return result;
    }

    public LiveData<Resource<List<GalleryItem>>> getGalleryByStudent(String studentId) {
        MutableLiveData<Resource<List<GalleryItem>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GALLERY)
                // Asegúrate de que este campo exista o la query fallará silenciosamente
                .whereArrayContains("taggedStudents", studentId)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error al obtener galería por alumno: ", error);
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<GalleryItem> items = value.toObjects(GalleryItem.class);
                        Log.d(TAG, "Galería por alumno cargada. Items: " + items.size());
                        result.setValue(Resource.success(items));
                    } else {
                        Log.d(TAG, "Snapshot nulo al obtener galería por alumno. Devolviendo lista vacía.");
                        result.setValue(Resource.success(new ArrayList<>())); // Lista vacía
                    }
                });

        return result;
    }

    public LiveData<Resource<Void>> deleteGalleryItem(String itemId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // 1. Obtener documento para borrar de Storage (si tiene URL)
        firestore.collection(Constants.COLLECTION_GALLERY).document(itemId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String mediaUrlToDelete = null;
                    if (documentSnapshot.exists()) {
                        GalleryItem item = documentSnapshot.toObject(GalleryItem.class);
                        if (item != null && item.getMediaUrl() != null && !item.getMediaUrl().isEmpty()) {
                            mediaUrlToDelete = item.getMediaUrl();
                        }
                    }

                    // 2. Borrar documento de Firestore
                    deleteGalleryDocument(itemId, result, mediaUrlToDelete); // Pasar URL a borrar
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener item antes de borrar, borrando solo Firestore.", e);
                    deleteGalleryDocument(itemId, result, null); // Intentar borrar Firestore de todos modos
                });

        return result;
    }

    // Método auxiliar para borrar de Firestore y luego (si hay URL) de Storage
    private void deleteGalleryDocument(String itemId, MutableLiveData<Resource<Void>> result, @Nullable String mediaUrl) {
        firestore.collection(Constants.COLLECTION_GALLERY)
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Documento de Firestore eliminado: " + itemId);
                    // 3. Si se borró de Firestore y había URL, borrar de Storage
                    if (mediaUrl != null) {
                        deleteFromStorage(mediaUrl);
                    }
                    result.setValue(Resource.success(null)); // Éxito (Firestore borrado)
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar documento de Firestore", e);
                    result.setValue(Resource.error("Error al eliminar: " + e.getMessage(), null));
                });
    }

    // Método auxiliar para borrar de Storage
    private void deleteFromStorage(String mediaUrl) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(mediaUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG,"Archivo de Storage eliminado: " + mediaUrl))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar archivo de Storage: "+ mediaUrl, e));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL inválida, no se puede eliminar de Storage: " + mediaUrl, e);
        }
    }
}