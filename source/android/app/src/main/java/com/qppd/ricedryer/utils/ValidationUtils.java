package com.qppd.ricedryer.utils;

import android.text.TextUtils;
import android.util.Patterns;

public class ValidationUtils {
    
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    public static boolean isStrongPassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < 8) {
            return false;
        }
        
        boolean hasUpperCase = !password.equals(password.toLowerCase());
        boolean hasLowerCase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        
        return hasUpperCase && hasLowerCase && hasDigit;
    }

    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.trim().length() >= 2;
    }

    public static boolean isValidPairingCode(String code) {
        return !TextUtils.isEmpty(code) && code.matches("\\d{6}");
    }

    public static boolean isValidTemperature(float temp) {
        return temp >= Constants.TEMP_MIN && temp <= Constants.TEMP_MAX;
    }

    public static String getPasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) {
            return "Empty";
        } else if (password.length() < 6) {
            return "Too Short";
        } else if (password.length() < 8) {
            return "Weak";
        } else if (isStrongPassword(password)) {
            return "Strong";
        } else {
            return "Medium";
        }
    }
}
