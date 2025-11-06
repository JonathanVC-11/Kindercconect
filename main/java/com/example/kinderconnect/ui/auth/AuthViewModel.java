package com.example.kinderconnect.ui.auth;

import android.content.Context; // <-- AÑADIDO
import android.net.Uri; // <-- AÑADIDO
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.example.kinderconnect.data.model.User;
import com.example.kinderconnect.data.repository.AuthRepository;
import com.example.kinderconnect.data.repository.GalleryRepository; // <-- AÑADIDO
import com.example.kinderconnect.utils.Resource;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final GalleryRepository galleryRepository; // <-- AÑADIDO
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel() {
        this.authRepository = new AuthRepository();
        this.galleryRepository = new GalleryRepository(); // <-- AÑADIDO
    }

    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        return authRepository.loginUser(email, password);
    }

    public LiveData<Resource<FirebaseUser>> register(String email, String password,
                                                     String fullName, String phone, String userType) {
        return authRepository.registerUser(email, password, fullName, phone, userType);
    }

    public LiveData<Resource<User>> getUserData(String uid) {
        return authRepository.getUserData(uid);
    }

    public LiveData<Resource<Void>> resetPassword(String email) {
        return authRepository.resetPassword(email);
    }

    // --- NUEVOS MÉTODOS AÑADIDOS ---
    /**
     * Sube una imagen de perfil a Storage y devuelve la URL.
     */
    public LiveData<Resource<String>> uploadProfilePicture(Context context, String userId, Uri imageUri) {
        return galleryRepository.uploadProfilePicture(context, userId, imageUri);
    }

    /**
     * Actualiza el campo 'photoUrl' del usuario en Firestore.
     */
    public LiveData<Resource<Void>> updateUserPhotoUrl(String uid, String photoUrl) {
        return authRepository.updateUserPhotoUrl(uid, photoUrl);
    }
    // ---------------------------------

    public LiveData<FirebaseUser> getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public void logout() {
        authRepository.logout();
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message) {
        errorMessage.setValue(message);
    }
}