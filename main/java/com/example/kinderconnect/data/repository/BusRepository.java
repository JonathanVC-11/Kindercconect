package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.kinderconnect.data.model.BusStatus;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.FirebaseFirestore;

public class BusRepository {
    private final FirebaseFirestore firestore;
    private static final String COLLECTION_BUS = "bus";
    private static final String DOCUMENT_ROUTE = "route";

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
                        result.setValue(Resource.error("No se encontró estado del bus", null));
                    }
                });

        return result;
    }
}