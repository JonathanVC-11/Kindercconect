package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.example.kinderconnect.data.model.User;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<FirebaseUser> userLiveData;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();

        // Habilitar persistencia offline (está habilitado por defecto en Android)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        this.userLiveData = new MutableLiveData<>();

        // Observar cambios en el estado de autenticación
        firebaseAuth.addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            userLiveData.postValue(user);
        });
    }

    public LiveData<Resource<FirebaseUser>> loginUser(String email, String password) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        result.setValue(Resource.success(user));
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error al iniciar sesión";
                        result.setValue(Resource.error(errorMessage, null));
                    }
                });

        return result;
    }

    // --- CAMBIO AQUÍ ---
    // Se añadió 'String phone' a los parámetros
    public LiveData<Resource<FirebaseUser>> registerUser(String email, String password,
                                                         String fullName, String phone, String userType) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {

                            // --- CAMBIO AQUÍ ---
                            // Se pasa 'phone' al constructor del User
                            User user = new User(firebaseUser.getUid(), email, fullName, userType, phone);
                            saveUserToFirestore(user, result, firebaseUser);
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error al registrar usuario";
                        result.setValue(Resource.error(errorMessage, null));
                    }
                });

        return result;
    }

    private void saveUserToFirestore(User user, MutableLiveData<Resource<FirebaseUser>> result,
                                     FirebaseUser firebaseUser) {
        firestore.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(firebaseUser)))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al guardar usuario: " + e.getMessage(), null)));
    }

    public LiveData<Resource<User>> getUserData(String uid) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        result.setValue(Resource.success(user));
                    } else {
                        result.setValue(Resource.error("Usuario no encontrado", null));
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error al obtener datos: " + e.getMessage(), null)));

        return result;
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    public LiveData<FirebaseUser> getCurrentUser() {
        return userLiveData;
    }

    public FirebaseUser getCurrentUserSync() {
        return firebaseAuth.getCurrentUser();
    }

    public LiveData<Resource<Void>> resetPassword(String email) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        result.setValue(Resource.success(null));
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error al enviar email";
                        result.setValue(Resource.error(errorMessage, null));
                    }
                });

        return result;
    }
}