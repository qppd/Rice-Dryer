package com.qppd.ricedryer.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.qppd.ricedryer.R;
import com.qppd.ricedryer.databinding.ActivityLoginBinding;
import com.qppd.ricedryer.ui.main.MainActivity;
import com.qppd.ricedryer.utils.Constants;
import com.qppd.ricedryer.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        setupObservers();
        setupListeners();
        checkRememberMe();
    }

    private void setupObservers() {
        viewModel.getLoginResult().observe(this, result -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);

            if (result.isSuccess()) {
                if (binding.checkRememberMe.isChecked()) {
                    preferences.edit()
                            .putBoolean(Constants.KEY_REMEMBER_ME, true)
                            .apply();
                }
                navigateToMain();
            } else {
                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void checkRememberMe() {
        boolean rememberMe = preferences.getBoolean(Constants.KEY_REMEMBER_ME, false);
        if (rememberMe && viewModel.isUserLoggedIn()) {
            navigateToMain();
        }
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation
        if (!ValidationUtils.isValidEmail(email)) {
            binding.etEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            binding.etPassword.setError(getString(R.string.error_invalid_password));
            return;
        }

        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        // Attempt login
        viewModel.login(email, password);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
