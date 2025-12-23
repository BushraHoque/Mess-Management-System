package com.example.messmanagement;

import java.util.List;

public class Expense {
    private String expenseId;
    private String date;
    private String description;
    private List<String> payers;
    private List<String> payerNames;
    private double amount;
    private double amountPerPerson;
    private long timestamp;

    public Expense() {
        // Empty constructor needed for Firebase
    }

    public Expense(String expenseId, String date, String description,
                   List<String> payers, List<String> payerNames,
                   double amount, double amountPerPerson, long timestamp) {
        this.expenseId = expenseId;
        this.date = date;
        this.description = description;
        this.payers = payers;
        this.payerNames = payerNames;
        this.amount = amount;
        this.amountPerPerson = amountPerPerson;
        this.timestamp = timestamp;
    }

    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPayers() {
        return payers;
    }

    public void setPayers(List<String> payers) {
        this.payers = payers;
    }

    public List<String> getPayerNames() {
        return payerNames;
    }

    public void setPayerNames(List<String> payerNames) {
        this.payerNames = payerNames;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAmountPerPerson() {
        return amountPerPerson;
    }

    public void setAmountPerPerson(double amountPerPerson) {
        this.amountPerPerson = amountPerPerson;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
