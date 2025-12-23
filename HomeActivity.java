package com.example.messmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    
    private TextView tvTotalMeals, tvTotalExpenses, tvMealRate, tvDateLabel;
    private Button btnAddMeal, btnAddExpense, btnMonthlyReport, btnMemberList;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences offlineCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        offlineCache = getSharedPreferences("OfflineCache", Context.MODE_PRIVATE);
        
        // Initialize views
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvMealRate = findViewById(R.id.tvMealRate);
        tvDateLabel = findViewById(R.id.tvDateLabel);
        btnAddMeal = findViewById(R.id.btnAddMeal);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnMonthlyReport = findViewById(R.id.btnMonthlyReport);
        btnMemberList = findViewById(R.id.btnMemberList);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);
        
        // Set today's date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String todayDate = sdf.format(new Date());
        tvDateLabel.setText("Today: " + todayDate);
        
        // Load cached data first
        loadCachedData();
        
        // Load today's data
        loadTodayData();
        
        // Load current month's meal rate
        loadMonthlyMealRate();
        
        // Set click listeners
        btnAddMeal.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, AddMealActivity.class));
        });
        
        btnAddExpense.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, AddExpenseActivity.class));
        });
        
        btnMonthlyReport.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MonthlyReportActivity.class));
        });
        
        btnMemberList.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MemberListActivity.class));
        });
        
        ivNotification.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, NotificationActivity.class));
        });
        
        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });
        
        ivHome.setOnClickListener(v -> {
            // Already on home, just refresh data
            loadTodayData();
            loadMonthlyMealRate();
        });
        
        ivLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to home
        loadTodayData();
        loadMonthlyMealRate();
    }
    
    private void loadCachedData() {
        int cachedMeals = offlineCache.getInt("todayMeals", 0);
        float cachedExpenses = offlineCache.getFloat("todayExpenses", 0f);
        float cachedRate = offlineCache.getFloat("mealRate", 0f);
        
        tvTotalMeals.setText(String.valueOf(cachedMeals));
        tvTotalExpenses.setText(String.format("৳%.2f", cachedExpenses));
        tvMealRate.setText(String.format("৳%.2f", cachedRate));
    }
    
    private void loadTodayData() {
        // Get today's date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String todayDate = sdf.format(new Date());
        
        // Load today's meals count
        db.collection("meals")
            .whereEqualTo("date", todayDate)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalMeals = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Meal meal = doc.toObject(Meal.class);
                    totalMeals += meal.getTotalMeals();
                }
                
                tvTotalMeals.setText(String.valueOf(totalMeals));
                
                // Cache the data
                SharedPreferences.Editor editor = offlineCache.edit();
                editor.putInt("todayMeals", totalMeals);
                editor.apply();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load today's meals. Using cached data.", 
                    Toast.LENGTH_SHORT).show();
            });
        
        // Load today's expenses
        db.collection("expenses")
            .whereEqualTo("date", todayDate)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalExpenses = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Expense expense = doc.toObject(Expense.class);
                    totalExpenses += expense.getAmount();
                }
                
                tvTotalExpenses.setText(String.format("৳%.2f", totalExpenses));
                
                // Cache the data
                SharedPreferences.Editor editor = offlineCache.edit();
                editor.putFloat("todayExpenses", (float) totalExpenses);
                editor.apply();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load today's expenses. Using cached data.", 
                    Toast.LENGTH_SHORT).show();
            });
    }
    
    private void loadMonthlyMealRate() {
        // Get current month and year
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);
        
        String monthYear = String.format(Locale.US, "%02d-%d", currentMonth, currentYear);
        
        // Load meals count for the month
        db.collection("meals")
            .whereEqualTo("monthYear", monthYear)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalMeals = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Meal meal = doc.toObject(Meal.class);
                    totalMeals += meal.getTotalMeals();
                }
                
                final int mealCount = totalMeals;
                
                // Load expenses for the month
                db.collection("expenses")
                    .whereEqualTo("monthYear", monthYear)
                    .get()
                    .addOnSuccessListener(expenseSnapshots -> {
                        double totalExpenses = 0;
                        for (QueryDocumentSnapshot doc : expenseSnapshots) {
                            Expense expense = doc.toObject(Expense.class);
                            totalExpenses += expense.getAmount();
                        }
                        
                        // Calculate meal rate
                        double mealRate = mealCount > 0 ? totalExpenses / mealCount : 0;
                        tvMealRate.setText(String.format("৳%.2f", mealRate));
                        
                        // Cache the data
                        SharedPreferences.Editor editor = offlineCache.edit();
                        editor.putFloat("mealRate", (float) mealRate);
                        editor.apply();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load meal rate. Using cached data.", 
                    Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Clear remember me preferences
                SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = loginPrefs.edit();
                editor.clear();
                editor.apply();
                
                // Sign out from Firebase
                mAuth.signOut();
                
                // Go to welcome screen
                Intent intent = new Intent(HomeActivity.this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("No", null)
            .show();
    }
}
