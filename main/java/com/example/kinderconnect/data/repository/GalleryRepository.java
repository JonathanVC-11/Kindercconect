package com.example.kinderconnect.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.ImageUtils;
import com.example.kinderconnect.utils.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GalleryRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private static final String TAG = "GalleryRepository";

    public GalleryRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    /**
     * Sube una foto de perfil, la comprime y devuelve la URL de descarga.
     */
    public LiveData<Resource<String>> uploadProfilePicture(Context context, String userId, Uri imageUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        try {
            // 1. Comprimir la imagen
            Bitmap bitmap = ImageUtils.getBitmapFromUri(context, imageUri);
            Bitmap compressedBitmap = ImageUtils.compressBitmap(bitmap, 400, 400); // 400x400 para perfil

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] data = baos.toByteArray();

            // 2. Crear ruta de subida
            String fileName = Constants.STORAGE_PROFILES + userId + ".jpg"; // Sobrescribe la anterior
            StorageReference profileRef = storage.getReference().child(fileName);

            // 3. Subir
            profileRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        // 4. Obtener URL de descarga
                        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            result.setValue(Resource.success(uri.toString()));
                        }).addOnFailureListener(e -> {
                            result.setValue(Resource.error("No se pudo obtener URL: " + e.getMessage(), null));
                        });
                    })
                    .addOnFailureListener(e -> {
                        result.setValue(Resource.error("No se pudo subir la foto: " + e.getMessage(), null));
                    });

        } catch (IOException e) {
            result.setValue(Resource.error("Error al procesar la imagen: " + e.getMessage(), null));
        }

        return result;
    }
    // ----------------------------

    public LiveData<Resource<String>> uploadMedia(GalleryItem galleryItem, Uri mediaUri, Context context) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (mediaUri == null) {
            Log.e(TAG, "Intento de subir archivo con mediaUri nulo.");
            result.setValue(Resource.error("No se seleccionó ningún archivo", null));
            return result;
        }

        String extension = galleryItem.getMediaType().equals(Constants.MEDIA_IMAGE) ? ".jpg" : ".mp4";
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        String storagePath = Constants.STORAGE_GALLERY + uniqueFileName;
        StorageReference originalMediaRef = storage.getReference().child(storagePath);

        Log.d(TAG, "Iniciando subida a Storage: " + storagePath);

        originalMediaRef.putFile(mediaUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Progreso de subida: " + String.format("%.2f", progress) + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Archivo original subido exitosamente a Storage.");
                    originalMediaRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "URL de descarga (original) obtenida: " + downloadUri.toString());

                                galleryItem.setMediaUrl(downloadUri.toString());

                                if (galleryItem.getMediaType().equals(Constants.MEDIA_IMAGE)) {
                                    Log.d(TAG, "Es imagen, iniciando subida de thumbnail...");
                                    uploadThumbnail(context, mediaUri, galleryItem, result);
                                } else {
                                    Log.d(TAG, "Es video, guardando en Firestore sin thumbnail.");
                                    galleryItem.setThumbnailUrl(null);
                                    saveGalleryItemToFirestore(galleryItem, result);
                                }
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

    private void uploadThumbnail(Context context, Uri originalImageUri, GalleryItem galleryItem, MutableLiveData<Resource<String>> result) {
        try {
            Bitmap bitmap = ImageUtils.getBitmapFromUri(context, originalImageUri);
            Bitmap compressedBitmap = ImageUtils.compressBitmap(bitmap, 400, 400);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            String thumbFileName = "thumb_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference thumbRef = storage.getReference().child(Constants.STORAGE_GALLERY + thumbFileName);

            Log.d(TAG, "Subiendo thumbnail: " + thumbFileName);
            thumbRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        thumbRef.getDownloadUrl().addOnSuccessListener(thumbUri -> {
                            Log.d(TAG, "Thumbnail subido, URL: " + thumbUri.toString());
                            galleryItem.setThumbnailUrl(thumbUri.toString());
                            saveGalleryItemToFirestore(galleryItem, result);
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Error al obtener URL del thumbnail", e);
                            result.setValue(Resource.error("Error al subir thumbnail: " + e.getMessage(), null));
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al subir bytes del thumbnail", e);
                        result.setValue(Resource.error("Error al subir thumbnail: " + e.getMessage(), null));
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error al crear bitmap para thumbnail", e);
            result.setValue(Resource.error("Error al procesar imagen: " + e.getMessage(), null));
        }
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
                        result.setValue(Resource.success(new ArrayList<>()));
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
                        result.setValue(Resource.success(new ArrayList<>()));
                    }
                });

        return result;
    }

    public LiveData<Resource<Void>> deleteGalleryItem(String itemId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_GALLERY).document(itemId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String mediaUrlToDelete = null;
                    String thumbUrlToDelete = null;

                    if (documentSnapshot.exists()) {
                        GalleryItem item = documentSnapshot.toObject(GalleryItem.class);
                        if (item != null) {
                            if (item.getMediaUrl() != null && !item.getMediaUrl().isEmpty()) {
                                mediaUrlToDelete = item.getMediaUrl();
                            }
                            if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isEmpty()) {
                                thumbUrlToDelete = item.getThumbnailUrl();
                            }
                        }
                    }

                    deleteGalleryDocument(itemId, result, mediaUrlToDelete, thumbUrlToDelete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener item antes de borrar, borrando solo Firestore.", e);
                    deleteGalleryDocument(itemId, result, null, null);
                });

        return result;
    }

    private void deleteGalleryDocument(String itemId, MutableLiveData<Resource<Void>> result, @Nullable String mediaUrl, @Nullable String thumbUrl) {
        firestore.collection(Constants.COLLECTION_GALLERY)
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Documento de Firestore eliminado: " + itemId);
                    if (mediaUrl != null) {
                        deleteFromStorage(mediaUrl);
                    }
                    if (thumbUrl != null) {
                        deleteFromStorage(thumbUrl);
                    }
                    result.setValue(Resource.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar documento de Firestore", e);
                    result.setValue(Resource.error("Error al eliminar: " + e.getMessage(), null));
                });
    }

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