package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.BusStatus;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint; // Asegúrate que esté importado

public class BusRepository {
    private final FirebaseFirestore firestore;

    private static final String COLLECTION_BUS = "bus_tracking";
    private static final String DOCUMENT_ROUTE = "current_status";

    public BusRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<Resource<BusStatus>> getBusStatusUpdates() {
        MutableLiveData<Resource<BusStatus>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(COLLECTION_BUS)
                .document(DOCUMENT_ROUTE)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        BusStatus busStatus = snapshot.toObject(BusStatus.class);
                        result.setValue(Resource.success(busStatus));
                    } else {
                        // --- MODIFICACIÓN AQUÍ ---
                        // Antes: resource.error("No se encontró estado del bus", null)
                        // Ahora: Devolvemos un estado "STOPPED" por defecto si no existe el doc.
                        BusStatus defaultStatus = new BusStatus();
                        defaultStatus.setStatus("STOPPED");
                        // Opcional: ponerlo en el inicio de la ruta
                        // defaultStatus.setCurrentLocation(new GeoPoint(19.4326, -99.1332));
                        result.setValue(Resource.success(defaultStatus));
                        // -------------------------
                    }
                });

        return result;
    }
}