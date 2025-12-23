package com.example.messmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;

public class NotificationActivity extends AppCompatActivity {

    private TextView tvNotifications;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvNotifications = findViewById(R.id.tvNotifications);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);

        setupBottomNavigation();
        loadNotifications();
    }

    private void loadNotifications() {
        StringBuilder notifications = new StringBuilder();

        String userId = mAuth.getCurrentUser().getUid();
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);
        String monthYear = String.format("%02d-%d", currentMonth, currentYear);

        notifications.append("📢 NOTIFICATIONS\n\n");

        // Check for missing meal entries (last 3 days)
        db.collection("meals")
                .whereEqualTo("userId", userId)
                .whereEqualTo("monthYear", monthYear)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.size() == 0) {
                        notifications.append("⚠️ No meal entries this month!\n\n");
                    }

                    // Check for due payments
                    checkDuePayments(userId, monthYear, notifications);
                });
    }

    private void checkDuePayments(String userId, String monthYear, StringBuilder notifications) {
        // Calculate if user has dues
        db.collection("meals")
                .whereEqualTo("userId", userId)
                .whereEqualTo("monthYear", monthYear)
                .get()
                .addOnSuccessListener(mealSnapshots -> {
                    int totalMeals = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : mealSnapshots) {
                        Meal meal = doc.toObject(Meal.class);
                        totalMeals += meal.getTotalMeals();
                    }

                    if (totalMeals > 0) {
                        notifications.append("ℹ️ You have consumed ").append(totalMeals)
                                .append(" meals this month\n\n");
                    }

                    notifications.append("✅ All notifications loaded");
                    tvNotifications.setText(notifications.toString());
                });
    }

    private void setupBottomNavigation() {
        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
        });

        ivNotification.setOnClickListener(v -> {
            // Already on notification page
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
