package com.example.messmanagement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin;
    private TextView tvSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences loginPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize SharedPreferences
        loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        // 🔔 ADD NOTIFICATION PERMISSION REQUEST HERE (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        progressBar = findViewById(R.id.progressBar);

        // Check if remember me was enabled
        boolean rememberMe = loginPrefs.getBoolean("rememberMe", false);
        if (rememberMe) {
            String savedEmail = loginPrefs.getString("email", "");
            String savedPassword = loginPrefs.getString("password", "");
            etEmail.setText(savedEmail);
            etPassword.setText(savedPassword);
            cbRememberMe.setChecked(true);
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter valid email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Login with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save credentials if remember me is checked
                        if (cbRememberMe.isChecked()) {
                            SharedPreferences.Editor editor = loginPrefs.edit();
                            editor.putBoolean("rememberMe", true);
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.apply();
                        } else {
                            // Clear saved credentials
                            SharedPreferences.Editor editor = loginPrefs.edit();
                            editor.clear();
                            editor.apply();
                        }

                        // 🔔 OPTIONAL: Initialize notifications here as well
                        NotificationHelper.createNotificationChannel(LoginActivity.this);
                        NotificationHelper.scheduleDailyNotifications(LoginActivity.this);

                        // Load user data to local storage
                        String userId = mAuth.getCurrentUser().getUid();
                        loadUserData(userId);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");

                        // Save to local storage
                        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("userId", userId);
                        editor.putString("name", name);
                        editor.putString("email", email);
                        editor.putString("phone", phone);
                        editor.apply();

                        // Initialize notification system after user data is loaded
                        NotificationHelper.createNotificationChannel(this);
                        NotificationHelper.scheduleDailyNotifications(this);

                    }

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                })

                .addOnFailureListener(e -> {

                    // 🔔 OPTIONAL: Initialize notifications here as well
                    NotificationHelper.createNotificationChannel(LoginActivity.this);
                    NotificationHelper.scheduleDailyNotifications(LoginActivity.this);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                });
    }
}
