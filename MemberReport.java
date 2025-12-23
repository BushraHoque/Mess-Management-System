package com.example.messmanagement;

public class MemberReport {
    public String userId;
    public String userName;
    public int mealCount;
    public double mealBill;
    public double amountPaid;
    public double dueBalance;

    public MemberReport() {
        this.userId = "";
        this.userName = "";
        this.mealCount = 0;
        this.mealBill = 0.0;
        this.amountPaid = 0.0;
        this.dueBalance = 0.0;
    }

    public MemberReport(String userId, String userName, int mealCount, 
                       double mealBill, double amountPaid, double dueBalance) {
        this.userId = userId;
        this.userName = userName;
        this.mealCount = mealCount;
        this.mealBill = mealBill;
        this.amountPaid = amountPaid;
        this.dueBalance = dueBalance;
    }
}
