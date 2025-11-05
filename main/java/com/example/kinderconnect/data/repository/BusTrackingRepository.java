package com.example.kinderconnect.data.repository;

import android.util.Log; // ¡Añadido!
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.kinderconnect.utils.Resource;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint; // ¡Importante!
import com.google.firebase.firestore.SetOptions; // ¡Importante para merge!

public class BusTrackingRepository {

    private static final String COLLECTION_NAME = "bus_tracking";
    private static final String DOCUMENT_ID = "current_status";
    private final FirebaseFirestore firestore;
    private final DocumentReference statusDocRef;
    private static final String TAG = "BusTrackingRepo"; // Tag para Logs

    public BusTrackingRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.statusDocRef = firestore.collection(COLLECTION_NAME).document(DOCUMENT_ID);
    }

    // Actualiza (o crea) el estado del bus en Firestore
    public LiveData<Resource<Void>> updateBusStatus(String status) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        Log.d(TAG, "Actualizando estado del bus a: " + status);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("lastUpdateTime", Timestamp.now()); // Siempre actualiza la hora

        if ("ACTIVE".equals(status)) {
            updates.put("startTime", Timestamp.now());
            // No ponemos ubicación inicial aquí, dejamos que el servicio la ponga
        } else if ("FINISHED".equals(status) || "STOPPED".equals(status)) {
            // Opcional: Podrías borrar la ubicación al finalizar o detener
            // updates.put("currentLocation", FieldValue.delete());
            updates.put("endTime", Timestamp.now());
        }

        // Usamos set con merge=true. Esto crea el documento si no existe,
        // o actualiza solo los campos especificados si ya existe.
        statusDocRef.set(updates, SetOptions.merge()) // Usar merge
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Estado del bus actualizado exitosamente en Firestore a " + status);
                    result.setValue(Resource.success(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar estado del bus en Firestore", e);
                    result.setValue(Resource.error("Error al actualizar estado: " + e.getMessage(), null));
                });

        return result;
    }

    /**
     * Actualiza solo la ubicación actual del bus y la hora de última actualización.
     * Usa update() para no afectar otros campos como status o startTime.
     * Fallará si el documento 'current_status' no existe.
     */
    public void updateBusLocation(GeoPoint location) {
        Map<String, Object> locationUpdate = new HashMap<>();
        locationUpdate.put("currentLocation", location);
        locationUpdate.put("lastUpdateTime", Timestamp.now());

        statusDocRef.update(locationUpdate)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ubicación actualizada: " + location.getLatitude() + "," + location.getLongitude()))
                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar ubicación (¿Documento existe?)", e));
        // No retornamos LiveData aquí, es una operación rápida desde el servicio.
    }


    // Obtiene el estado actual del bus una sola vez
    public LiveData<Resource<String>> getCurrentBusStatus() {
        MutableLiveData<Resource<String>> statusLiveData = new MutableLiveData<>();
        statusLiveData.setValue(Resource.loading(null));
        Log.d(TAG, "Obteniendo estado actual del bus...");

        statusDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                String statusResult = "STOPPED"; // Estado por defecto si no se encuentra
                if (document != null && document.exists()) {
                    String status = document.getString("status");
                    if (status != null) {
                        statusResult = status;
                    }
                    Log.d(TAG, "Documento encontrado. Estado: " + statusResult);
                } else {
                    Log.w(TAG, "Documento 'current_status' no existe. Asumiendo estado STOPPED.");
                }
                statusLiveData.setValue(Resource.success(statusResult));
            } else {
                Log.e(TAG, "Error al obtener estado del bus desde Firestore", task.getException());
                statusLiveData.setValue(Resource.error("Error al obtener estado: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), null));
            }
        });
        return statusLiveData;
    }
}