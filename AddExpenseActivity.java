package com.example.messmanagement;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etDate, etDescription, etAmount;
    private RadioGroup rgPayer;
    private RadioButton rbSelf, rbMultiple;
    private Button btnSelectMembers, btnSave;
    private ProgressBar progressBar;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Calendar selectedDate;
    private SharedPreferences userPrefs, offlinePrefs;
    private List<User> allMembers;
    private List<String> selectedMemberIds;
    private List<String> selectedMemberNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userPrefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        offlinePrefs = getSharedPreferences("OfflineExpenses", Context.MODE_PRIVATE);

        etDate = findViewById(R.id.etDate);
        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        rgPayer = findViewById(R.id.rgPayer);
        rbSelf = findViewById(R.id.rbSelf);
        rbMultiple = findViewById(R.id.rbMultiple);
        btnSelectMembers = findViewById(R.id.btnSelectMembers);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);

        allMembers = new ArrayList<>();
        selectedMemberIds = new ArrayList<>();
        selectedMemberNames = new ArrayList<>();

        selectedDate = Calendar.getInstance();
        updateDateField();

        loadMembers();

        etDate.setOnClickListener(v -> showDatePicker());

        rgPayer.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSelf) {
                btnSelectMembers.setVisibility(View.GONE);
            } else {
                btnSelectMembers.setVisibility(View.VISIBLE);
            }
        });

        btnSelectMembers.setOnClickListener(v -> showMemberSelectionDialog());
        btnSave.setOnClickListener(v -> saveExpense());

        setupBottomNavigation();
        syncOfflineExpenses();
    }

    private void loadMembers() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allMembers.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        allMembers.add(user);
                    }
                });
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

    private void showMemberSelectionDialog() {
        if (allMembers.isEmpty()) {
            Toast.makeText(this, "No members available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] memberNames = new String[allMembers.size()];
        boolean[] checkedItems = new boolean[allMembers.size()];

        for (int i = 0; i < allMembers.size(); i++) {
            memberNames[i] = allMembers.get(i).getName();
            checkedItems[i] = selectedMemberIds.contains(allMembers.get(i).getUserId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Payers");
        builder.setMultiChoiceItems(memberNames, checkedItems,
                (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                });
        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedMemberIds.clear();
            selectedMemberNames.clear();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selectedMemberIds.add(allMembers.get(i).getUserId());
                    selectedMemberNames.add(allMembers.get(i).getName());
                }
            }
            if (selectedMemberIds.size() > 0) {
                btnSelectMembers.setText(selectedMemberIds.size() + " members selected");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveExpense() {
        String description = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        List<String> payerIds = new ArrayList<>();
        List<String> payerNames = new ArrayList<>();

        if (rbSelf.isChecked()) {
            payerIds.add(mAuth.getCurrentUser().getUid());
            payerNames.add(userPrefs.getString("name", "Unknown"));
        } else {
            if (selectedMemberIds.isEmpty()) {
                Toast.makeText(this, "Please select payers", Toast.LENGTH_SHORT).show();
                return;
            }
            payerIds.addAll(selectedMemberIds);
            payerNames.addAll(selectedMemberNames);
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String date = sdf.format(selectedDate.getTime());

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM-yyyy", Locale.US);
        String monthYear = monthYearFormat.format(selectedDate.getTime());

        double amountPerPerson = amount / payerIds.size();

        String expenseId = db.collection("expenses").document().getId();

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("expenseId", expenseId);
        expenseData.put("date", date);
        expenseData.put("monthYear", monthYear);
        expenseData.put("description", description);
        expenseData.put("payers", payerIds);
        expenseData.put("payerNames", payerNames);
        expenseData.put("amount", amount);
        expenseData.put("amountPerPerson", amountPerPerson);
        expenseData.put("timestamp", System.currentTimeMillis());

        if (isNetworkAvailable()) {
            db.collection("expenses").document(expenseId).set(expenseData)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Expense saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> saveOfflineExpense(expenseData));
        } else {
            saveOfflineExpense(expenseData);
        }
    }

    private void saveOfflineExpense(Map<String, Object> expenseData) {
        String offlineExpensesJson = offlinePrefs.getString("expenses", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> offlineExpenses = gson.fromJson(offlineExpensesJson, type);

        offlineExpenses.add(expenseData);

        String updatedJson = gson.toJson(offlineExpenses);
        SharedPreferences.Editor editor = offlinePrefs.edit();
        editor.putString("expenses", updatedJson);
        editor.apply();

        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "Expense saved offline. Will sync when online.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void syncOfflineExpenses() {
        if (!isNetworkAvailable()) return;

        String offlineExpensesJson = offlinePrefs.getString("expenses", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> offlineExpenses = gson.fromJson(offlineExpensesJson, type);

        if (offlineExpenses.isEmpty()) return;

        for (Map<String, Object> expenseData : offlineExpenses) {
            String expenseId = (String) expenseData.get("expenseId");
            db.collection("expenses").document(expenseId).set(expenseData)
                    .addOnSuccessListener(aVoid -> removeOfflineExpense(expenseId));
        }
    }

    private void removeOfflineExpense(String expenseId) {
        String offlineExpensesJson = offlinePrefs.getString("expenses", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> offlineExpenses = gson.fromJson(offlineExpensesJson, type);

        offlineExpenses.removeIf(expense -> expenseId.equals(expense.get("expenseId")));

        String updatedJson = gson.toJson(offlineExpenses);
        SharedPreferences.Editor editor = offlinePrefs.edit();
        editor.putString("expenses", updatedJson);
        editor.apply();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setupBottomNavigation() {
        ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        ivHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        ivNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        ivLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                    loginPrefs.edit().clear().apply();
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
