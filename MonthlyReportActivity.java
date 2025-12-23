package com.example.messmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonthlyReportActivity extends AppCompatActivity {

    private Spinner spinnerMonth, spinnerYear;
    private Button btnGenerate;
    private TextView tvTotalMeals, tvTotalExpenses, tvMealRate, tvTotalMembers;
    private RecyclerView rvMemberDetails, rvExpenseList;
    private ProgressBar progressBar;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<MemberReport> memberReports;
    private List<Expense> expenseList;
    private MemberReportAdapter memberAdapter;
    private ExpenseListAdapter expenseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        memberReports = new ArrayList<>();
        expenseList = new ArrayList<>();

        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnGenerate = findViewById(R.id.btnGenerate);
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvMealRate = findViewById(R.id.tvMealRate);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        rvMemberDetails = findViewById(R.id.rvMemberDetails);
        rvExpenseList = findViewById(R.id.rvExpenseList);
        progressBar = findViewById(R.id.progressBar);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);

        // Setup RecyclerViews
        rvMemberDetails.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberReportAdapter(memberReports);
        rvMemberDetails.setAdapter(memberAdapter);

        rvExpenseList.setLayoutManager(new LinearLayoutManager(this));
        expenseAdapter = new ExpenseListAdapter(expenseList);
        rvExpenseList.setAdapter(expenseAdapter);

        setupSpinners();
        setupBottomNavigation();

        btnGenerate.setOnClickListener(v -> generateReport());
    }

    private void setupSpinners() {
        // Month names instead of numbers
        String[] months = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(years.size() - 2);

        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));
    }

    private void generateReport() {
        int monthIndex = spinnerMonth.getSelectedItemPosition() + 1;
        String monthNumber = String.format("%02d", monthIndex);
        String year = (String) spinnerYear.getSelectedItem();
        String monthYear = monthNumber + "-" + year;

        progressBar.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);

        loadReportData(monthYear);
    }

    private void loadReportData(String monthYear) {
        Map<String, MemberReport> reportMap = new HashMap<>();

        db.collection("users").get()
                .addOnSuccessListener(userSnapshots -> {
                    for (QueryDocumentSnapshot userDoc : userSnapshots) {
                        User user = userDoc.toObject(User.class);
                        MemberReport report = new MemberReport();
                        report.userId = user.getUserId();
                        report.userName = user.getName();
                        report.mealCount = 0;
                        report.amountPaid = 0.0;
                        reportMap.put(user.getUserId(), report);
                    }

                    loadMeals(monthYear, reportMap);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMeals(String monthYear, Map<String, MemberReport> reportMap) {
        db.collection("meals")
                .whereEqualTo("monthYear", monthYear)
                .get()
                .addOnSuccessListener(mealSnapshots -> {
                    int totalMeals = 0;
                    for (QueryDocumentSnapshot mealDoc : mealSnapshots) {
                        Meal meal = mealDoc.toObject(Meal.class);
                        String userId = meal.getUserId();
                        if (reportMap.containsKey(userId)) {
                            MemberReport report = reportMap.get(userId);
                            report.mealCount += meal.getTotalMeals();
                            totalMeals += meal.getTotalMeals();
                        }
                    }

                    final int finalTotalMeals = totalMeals;
                    loadExpenses(monthYear, reportMap, finalTotalMeals);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, "Failed to load meals", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExpenses(String monthYear, Map<String, MemberReport> reportMap, int totalMeals) {
        db.collection("expenses")
                .whereEqualTo("monthYear", monthYear)
                .get()
                .addOnSuccessListener(expenseSnapshots -> {
                    double totalExpenses = 0.0;
                    expenseList.clear();

                    for (QueryDocumentSnapshot expenseDoc : expenseSnapshots) {
                        Expense expense = expenseDoc.toObject(Expense.class);
                        totalExpenses += expense.getAmount();
                        expenseList.add(expense);

                        List<String> payers = expense.getPayers();
                        double amountPerPerson = expense.getAmountPerPerson();

                        for (String payerId : payers) {
                            if (reportMap.containsKey(payerId)) {
                                MemberReport report = reportMap.get(payerId);
                                report.amountPaid += amountPerPerson;
                            }
                        }
                    }

                    double mealRate = totalMeals > 0 ? totalExpenses / totalMeals : 0;

                    // Calculate meal bills and due balances
                    for (MemberReport report : reportMap.values()) {
                        report.mealBill = report.mealCount * mealRate;
                        report.dueBalance = report.mealBill - report.amountPaid;
                    }

                    displayReport(totalMeals, totalExpenses, mealRate, reportMap);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, "Failed to load expenses", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayReport(int totalMeals, double totalExpenses, double mealRate,
                               Map<String, MemberReport> reportMap) {
        // Update summary with proper labels
        tvTotalMeals.setText("Total Meals: " + totalMeals);
        tvTotalExpenses.setText("Total Expenses: ৳" + String.format("%.2f", totalExpenses));
        tvMealRate.setText("Meal Rate: ৳" + String.format("%.2f", mealRate));
        tvTotalMembers.setText("Total Members: " + reportMap.size());

        // Update member details RecyclerView
        memberReports.clear();
        memberReports.addAll(reportMap.values());
        memberAdapter.updateList(memberReports);

        // Update expense list RecyclerView
        expenseAdapter.updateList(expenseList);

        progressBar.setVisibility(View.GONE);
        btnGenerate.setEnabled(true);

        if (memberReports.isEmpty()) {
            Toast.makeText(this, "No data found for this month", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Report generated successfully!", Toast.LENGTH_SHORT).show();
        }
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
