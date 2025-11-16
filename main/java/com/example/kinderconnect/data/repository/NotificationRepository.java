package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.Notification;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class NotificationRepository {

    private final FirebaseFirestore firestore;

    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Obtiene todas las notificaciones para un usuario específico, en tiempo real.
     */
    public LiveData<Resource<List<Notification>>> getNotificationsForUser(String userId) {
        MutableLiveData<Resource<List<Notification>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50) // Limitar a las 50 más recientes
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    if (value != null) {
                        List<Notification> notifications = value.toObjects(Notification.class);
                        result.setValue(Resource.success(notifications));
                    } else {
                        result.setValue(Resource.success(null));
                    }
                });
        return result;
    }

    /**
     * Marca una notificación específica como leída.
     */
    public LiveData<Resource<Void>> markNotificationAsRead(String notificationId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /**
     * Marca todas las notificaciones no leídas de un usuario como leídas.
     */
    public LiveData<Resource<Void>> markAllAsRead(String userId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = firestore.batch();
                    if (querySnapshot.isEmpty()) {
                        result.setValue(Resource.success(null)); // No había nada que marcar
                        return;
                    }

                    for (var doc : querySnapshot.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                            .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}