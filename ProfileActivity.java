package com.example.messmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvUserId;
    private Button btnChangePhone, btnChangePassword;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userPrefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvUserId = findViewById(R.id.tvUserId);
        btnChangePhone = findViewById(R.id.btnChangePhone);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);

        setupBottomNavigation();
        loadProfile();

        btnChangePhone.setOnClickListener(v -> showChangePhoneDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadProfile() {
        String name = userPrefs.getString("name", "N/A");
        String email = userPrefs.getString("email", "N/A");
        String phone = userPrefs.getString("phone", "N/A");
        String userId = mAuth.getCurrentUser().getUid();

        tvName.setText("Name: " + name);
        tvEmail.setText("Email: " + email);
        tvPhone.setText("Phone: " + phone);
        tvUserId.setText("User ID: " + userId);
    }

    private void showChangePhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Phone Number");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint("Enter new phone number");
        String currentPhone = userPrefs.getString("phone", "");
        input.setText(currentPhone);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPhone = input.getText().toString().trim();

            if (newPhone.isEmpty()) {
                Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPhone.length() < 10) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePhoneNumber(newPhone);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updatePhoneNumber(String newPhone) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .update("phone", newPhone)
                .addOnSuccessListener(aVoid -> {
                    // Update local storage
                    SharedPreferences.Editor editor = userPrefs.edit();
                    editor.putString("phone", newPhone);
                    editor.apply();

                    // Update UI
                    tvPhone.setText("Phone: " + newPhone);

                    Toast.makeText(this, "Phone number updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordInput.setHint("Current Password");
        layout.addView(currentPasswordInput);

        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setHint("New Password");
        layout.addView(newPasswordInput);

        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setHint("Confirm New Password");
        layout.addView(confirmPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Please enter current password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePassword(currentPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Update password
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update password: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        ivProfile.setOnClickListener(v -> {
            // Already on profile page
        });

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
        });

        ivNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        ivLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = loginPrefs.edit();
                    editor.clear();
                    editor.apply();

                    mAuth.signOut();

                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
