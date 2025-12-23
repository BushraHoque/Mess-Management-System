package com.example.messmanagement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DailyReminderReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("CHECK_MEAL_ENTRY".equals(action)) {
            checkMissedMealEntry(context);
        } else if ("CHECK_PAYMENT_DUE".equals(action)) {
            checkPaymentDue(context);
        }
    }
    
    private void checkMissedMealEntry(Context context) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return; // User not logged in
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Get yesterday's date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String yesterdayDate = sdf.format(calendar.getTime());
        
        // Check if user added meal yesterday
        db.collection("meals")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", yesterdayDate)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    // No meal entry for yesterday
                    String message = "You missed entering your meal yesterday (" + yesterdayDate + ")";
                    NotificationHelper.showMealReminderNotification(context, message);
                }
            });
    }
    
    private void checkPaymentDue(Context context) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return; // User not logged in
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Get current month
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);
        String monthYear = String.format(Locale.US, "%02d-%d", currentMonth, currentYear);
        
        // Calculate user's dues
        db.collection("meals")
            .whereEqualTo("userId", userId)
            .whereEqualTo("monthYear", monthYear)
            .get()
            .addOnSuccessListener(mealSnapshots -> {
                int totalMeals = 0;
                for (QueryDocumentSnapshot doc : mealSnapshots) {
                    Meal meal = doc.toObject(Meal.class);
                    totalMeals += meal.getTotalMeals();
                }
                
                final int userMeals = totalMeals;
                
                // Get all meals to calculate meal rate
                db.collection("meals")
                    .whereEqualTo("monthYear", monthYear)
                    .get()
                    .addOnSuccessListener(allMealSnapshots -> {
                        int totalAllMeals = 0;
                        for (QueryDocumentSnapshot doc : allMealSnapshots) {
                            Meal meal = doc.toObject(Meal.class);
                            totalAllMeals += meal.getTotalMeals();
                        }
                        
                        final int finalTotalMeals = totalAllMeals;
                        
                        // Get all expenses
                        db.collection("expenses")
                            .whereEqualTo("monthYear", monthYear)
                            .get()
                            .addOnSuccessListener(expenseSnapshots -> {
                                double totalExpenses = 0;
                                double userPaid = 0;
                                
                                for (QueryDocumentSnapshot doc : expenseSnapshots) {
                                    Expense expense = doc.toObject(Expense.class);
                                    totalExpenses += expense.getAmount();
                                    
                                    if (expense.getPayers() != null && 
                                        expense.getPayers().contains(userId)) {
                                        userPaid += expense.getAmountPerPerson();
                                    }
                                }
                                
                                // Calculate dues
                                double mealRate = finalTotalMeals > 0 ? 
                                    totalExpenses / finalTotalMeals : 0;
                                double userBill = userMeals * mealRate;
                                double dueBalance = userBill - userPaid;
                                
                                if (dueBalance > 1.0) { // If due is more than ৳1
                                    String message = String.format(
                                        "You have a pending payment of ৳%.2f for this month", 
                                        dueBalance
                                    );
                                    NotificationHelper.showPaymentDueNotification(context, message);
                                }
                            });
                    });
            });
    }
}
