package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.kinderconnect.utils.Resource;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.Timestamp;

public class BusTrackingRepository {

    private static final String COLLECTION_NAME = "bus_tracking";
    private static final String DOCUMENT_ID = "current_status";
    private final FirebaseFirestore firestore;
    private final DocumentReference statusDocRef;

    public BusTrackingRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.statusDocRef = firestore.collection(COLLECTION_NAME).document(DOCUMENT_ID);
    }

    // Actualiza (o crea) el estado del bus en Firestore
    public LiveData<Resource<Void>> updateBusStatus(String status) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("lastUpdateTime", Timestamp.now()); // Siempre actualiza la hora
        // Si el estado es ACTIVE, también guarda la hora de inicio
        if ("ACTIVE".equals(status)) {
            updates.put("startTime", Timestamp.now());
        }

        // Usamos set (que sobreescribe o crea)
        statusDocRef.set(updates)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error("Error al actualizar estado: " + e.getMessage(), null)));

        return result;
    }

    // Obtiene el estado actual del bus una sola vez
    public LiveData<Resource<String>> getCurrentBusStatus() {
        MutableLiveData<Resource<String>> statusLiveData = new MutableLiveData<>();
        statusLiveData.setValue(Resource.loading(null));

        statusDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String status = document.getString("status");
                    // Si el campo status no existe o es nulo, default a INACTIVE
                    statusLiveData.setValue(Resource.success(status != null ? status : "INACTIVE"));
                } else {
                    // Si el documento 'current_status' no existe, asumimos INACTIVE
                    statusLiveData.setValue(Resource.success("INACTIVE"));
                }
            } else {
                statusLiveData.setValue(Resource.error("Error al obtener estado: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), null));
            }
        });
        return statusLiveData;
    }
}