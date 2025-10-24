package com.qppd.ricedryer.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qppd.ricedryer.data.repository.AuthRepository;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final MutableLiveData<AuthResult> loginResult;
    private final MutableLiveData<AuthResult> registerResult;
    private final MutableLiveData<AuthResult> resetPasswordResult;

    public AuthViewModel() {
        authRepository = AuthRepository.getInstance();
        loginResult = new MutableLiveData<>();
        registerResult = new MutableLiveData<>();
        resetPasswordResult = new MutableLiveData<>();
    }

    public LiveData<AuthResult> getLoginResult() {
        return loginResult;
    }

    public LiveData<AuthResult> getRegisterResult() {
        return registerResult;
    }

    public LiveData<AuthResult> getResetPasswordResult() {
        return resetPasswordResult;
    }

    public void login(String email, String password) {
        authRepository.login(email, password)
                .addOnSuccessListener(authResult -> {
                    loginResult.setValue(new AuthResult(true, null));
                })
                .addOnFailureListener(e -> {
                    loginResult.setValue(new AuthResult(false, e.getMessage()));
                });
    }

    public void register(String email, String password, String name) {
        authRepository.register(email, password, name)
                .addOnSuccessListener(authResult -> {
                    registerResult.setValue(new AuthResult(true, null));
                })
                .addOnFailureListener(e -> {
                    registerResult.setValue(new AuthResult(false, e.getMessage()));
                });
    }

    public void resetPassword(String email) {
        authRepository.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    resetPasswordResult.setValue(new AuthResult(true, null));
                })
                .addOnFailureListener(e -> {
                    resetPasswordResult.setValue(new AuthResult(false, e.getMessage()));
                });
    }

    public boolean isUserLoggedIn() {
        return authRepository.isUserLoggedIn();
    }

    public static class AuthResult {
        private final boolean success;
        private final String error;

        public AuthResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }
}
