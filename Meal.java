package com.example.messmanagement;

public class Meal {
    private String mealId;
    private String userId;
    private String userName;
    private String date;
    private boolean breakfast;
    private boolean lunch;
    private boolean dinner;
    private long timestamp;

    public Meal() {
        // Empty constructor needed for Firebase
    }

    public Meal(String mealId, String userId, String userName, String date,
                boolean breakfast, boolean lunch, boolean dinner, long timestamp) {
        this.mealId = mealId;
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.timestamp = timestamp;
    }

    public String getMealId() {
        return mealId;
    }

    public void setMealId(String mealId) {
        this.mealId = mealId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isBreakfast() {
        return breakfast;
    }

    public void setBreakfast(boolean breakfast) {
        this.breakfast = breakfast;
    }

    public boolean isLunch() {
        return lunch;
    }

    public void setLunch(boolean lunch) {
        this.lunch = lunch;
    }

    public boolean isDinner() {
        return dinner;
    }

    public void setDinner(boolean dinner) {
        this.dinner = dinner;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getTotalMeals() {
        int count = 0;
        if (breakfast) count++;
        if (lunch) count++;
        if (dinner) count++;
        return count;
    }
}
