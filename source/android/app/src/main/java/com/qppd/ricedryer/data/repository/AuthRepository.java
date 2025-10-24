package com.qppd.ricedryer.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.qppd.ricedryer.data.model.User;
import com.qppd.ricedryer.data.remote.FirebaseAuthManager;
import com.qppd.ricedryer.data.remote.FirebaseDataSource;

public class AuthRepository {
    private static AuthRepository instance;
    private final FirebaseAuthManager authManager;
    private final FirebaseDataSource dataSource;
    private final MutableLiveData<FirebaseUser> userLiveData;

    private AuthRepository() {
        authManager = FirebaseAuthManager.getInstance();
        dataSource = FirebaseDataSource.getInstance();
        userLiveData = new MutableLiveData<>();
        userLiveData.setValue(authManager.getCurrentUser());
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public Task<AuthResult> login(String email, String password) {
        return authManager.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> userLiveData.setValue(authResult.getUser()));
    }

    public Task<AuthResult> register(String email, String password, String name) {
        return authManager.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Create user profile in database
                        User user = new User(
                                firebaseUser.getUid(),
                                email,
                                name,
                                System.currentTimeMillis()
                        );
                        dataSource.getUserRef(firebaseUser.getUid()).setValue(user);
                        
                        // Send verification email
                        firebaseUser.sendEmailVerification();
                        
                        userLiveData.setValue(firebaseUser);
                    }
                });
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return authManager.getAuth().sendPasswordResetEmail(email);
    }

    public void logout() {
        authManager.signOut();
        userLiveData.setValue(null);
    }

    public boolean isUserLoggedIn() {
        return authManager.isUserLoggedIn();
    }

    public String getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    public FirebaseUser getCurrentUser() {
        return authManager.getCurrentUser();
    }
}
