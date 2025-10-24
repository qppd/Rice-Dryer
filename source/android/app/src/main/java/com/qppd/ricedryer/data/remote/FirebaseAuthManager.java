package com.qppd.ricedryer.data.remote;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseAuthManager {
    private static FirebaseAuthManager instance;
    private final FirebaseAuth mAuth;

    private FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void signOut() {
        mAuth.signOut();
    }
}
