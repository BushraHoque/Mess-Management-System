package com.example.messmanagement;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddMealActivity extends AppCompatActivity {

    private EditText etDate;
    private CheckBox cbBreakfast, cbLunch, cbDinner;
    private Button btnSave;
    private ProgressBar progressBar;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Calendar selectedDate;
    private SharedPreferences userPrefs, offlinePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize SharedPreferences
        userPrefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        offlinePrefs = getSharedPreferences("OfflineMeals", Context.MODE_PRIVATE);

        // Initialize views
        etDate = findViewById(R.id.etDate);
        cbBreakfast = findViewById(R.id.cbBreakfast);
        cbLunch = findViewById(R.id.cbLunch);
        cbDinner = findViewById(R.id.cbDinner);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);

        // Set current date
        selectedDate = Calendar.getInstance();
        updateDateField();

        // Date picker
        etDate.setOnClickListener(v -> showDatePicker());

        // Save button
        btnSave.setOnClickListener(v -> saveMeal());

        // Setup bottom navigation
        setupBottomNavigation();

        // Try to sync offline meals when activity starts
        syncOfflineMeals();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateField();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        etDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void saveMeal() {
        // Validation
        if (!cbBreakfast.isChecked() && !cbLunch.isChecked() && !cbDinner.isChecked()) {
            Toast.makeText(this, "Please select at least one meal", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        String userId = mAuth.getCurrentUser().getUid();
        String userName = userPrefs.getString("name", "Unknown");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String date = sdf.format(selectedDate.getTime());

        // Get month-year for filtering
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM-yyyy", Locale.US);
        String monthYear = monthYearFormat.format(selectedDate.getTime());

        // Create meal object
        String mealId = db.collection("meals").document().getId();

        Map<String, Object> mealData = new HashMap<>();
        mealData.put("mealId", mealId);
        mealData.put("userId", userId);
        mealData.put("userName", userName);
        mealData.put("date", date);
        mealData.put("monthYear", monthYear);
        mealData.put("breakfast", cbBreakfast.isChecked());
        mealData.put("lunch", cbLunch.isChecked());
        mealData.put("dinner", cbDinner.isChecked());
        mealData.put("timestamp", System.currentTimeMillis());

        // Check internet connectivity
        if (isNetworkAvailable()) {
            // Save to Firebase
            db.collection("meals").document(mealId).set(mealData)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Meal saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // If online save fails, save offline
                        saveOfflineMeal(mealData);
                    });
        } else {
            // Save offline
            saveOfflineMeal(mealData);
        }
    }

    private void saveOfflineMeal(Map<String, Object> mealData) {
        // Get existing offline meals
        String offlineMealsJson = offlinePrefs.getString("meals", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> offlineMeals = gson.fromJson(offlineMealsJson, type);

        // Add new meal
        offlineMeals.add(mealData);

        // Save back to SharedPreferences
        String updatedJson = gson.toJson(offlineMeals);
        SharedPreferences.Editor editor = offlinePrefs.edit();
        editor.putString("meals", updatedJson);
        editor.apply();

        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "Meal saved offline. Will sync when online.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void syncOfflineMeals() {
        if (!isNetworkAvailable()) {
            return;
        }

        String offlineMealsJson = offlinePrefs.getString("meals", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> offlineMeals = gson.fromJson(offlineMealsJson, type);

        if (offlineMeals.isEmpty()) {
            return;
        }

        // Sync each meal
        for (Map<String, Object> mealData : offlineMeals) {
            String mealId = (String) mealData.get("mealId");
            db.collection("meals").document(mealId).set(mealData)
                    .addOnSuccessListener(aVoid -> {
                        // Remove from offline storage after successful sync
                        removeOfflineMeal(mealId);
                    });
        }
    }

    private void removeOfflineMeal(String mealId) {
        String offlineMealsJson = offlinePrefs.getString("meals", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> offlineMeals = gson.fromJson(offlineMealsJson, type);

        // Remove the synced meal
        offlineMeals.removeIf(meal -> mealId.equals(meal.get("mealId")));

        // Save back
        String updatedJson = gson.toJson(offlineMeals);
        SharedPreferences.Editor editor = offlinePrefs.edit();
        editor.putString("meals", updatedJson);
        editor.apply();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setupBottomNavigation() {
        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
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
