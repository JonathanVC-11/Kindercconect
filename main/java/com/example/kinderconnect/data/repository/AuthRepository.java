package com.example.kinderconnect.data.repository;

import android.util.Log; // <-- AÑADIDO
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
    private static final String TAG = "AuthRepository"; // <-- AÑADIDO

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        this.userLiveData = new MutableLiveData<>();

        firebaseAuth.addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            userLiveData.postValue(user);
        });
    }

    // ... (loginUser, registerUser, saveUserToFirestore, getUserData, updateUserPhotoUrl... sin cambios) ...
    // ... (Estos métodos se mantienen)

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

    public LiveData<Resource<FirebaseUser>> registerUser(String email, String password,
                                                         String fullName, String phone, String userType) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
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

    // --- INICIO DE CÓDIGO AÑADIDO ---
    /**
     * Busca un usuario por email y (opcionalmente) por tipo de usuario.
     * USADO INTERNAMENTE POR OTROS REPOSITORIOS.
     */
    public void findUserByEmail(String email, String expectedUserType, MutableLiveData<Resource<User>> result) {
        firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        result.setValue(Resource.error("No se encontró ningún usuario con el correo: " + email, null));
                        return;
                    }

                    User user = querySnapshot.getDocuments().get(0).toObject(User.class);

                    if (user != null && expectedUserType != null && !user.getUserType().equals(expectedUserType)) {
                        result.setValue(Resource.error("El correo " + email + " no pertenece a una cuenta de Maestra.", null));
                        return;
                    }

                    result.setValue(Resource.success(user));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(e.getMessage(), null)));
    }
    // --- FIN DE CÓDIGO AÑADIDO ---


    public LiveData<Resource<Void>> updateUserPhotoUrl(String uid, String photoUrl) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("photoUrl", photoUrl)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error("Error al actualizar foto: " + e.getMessage(), null)));

        return result;
    }

    // --- (updateUserToken, updateUserTokenFireAndForget, logout, etc. sin cambios) ---

    public LiveData<Resource<Void>> updateUserToken(String uid, String token) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error("Error al actualizar token: " + e.getMessage(), null)));

        return result;
    }

    public void updateUserTokenFireAndForget(String uid, String token) {
        if (uid == null || token == null) {
            Log.e(TAG, "UID o Token nulos, no se puede actualizar FCM token.");
            return;
        }

        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token actualizado (Fire and Forget)"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar FCM Token (Fire and Forget)", e));
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