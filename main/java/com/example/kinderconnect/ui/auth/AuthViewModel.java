package com.example.kinderconnect.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.example.kinderconnect.data.model.User;
import com.example.kinderconnect.data.repository.AuthRepository;
import com.example.kinderconnect.utils.Resource;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel() {
        this.authRepository = new AuthRepository();
    }

    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        return authRepository.loginUser(email, password);
    }

    // --- CAMBIO AQUÍ ---
    // Se añadió 'String phone' a los parámetros
    public LiveData<Resource<FirebaseUser>> register(String email, String password,
                                                     String fullName, String phone, String userType) {
        // Y se pasa 'phone' a la llamada del repositorio
        return authRepository.registerUser(email, password, fullName, phone, userType);
    }

    public LiveData<Resource<User>> getUserData(String uid) {
        return authRepository.getUserData(uid);
    }

    public LiveData<Resource<Void>> resetPassword(String email) {
        return authRepository.resetPassword(email);
    }

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