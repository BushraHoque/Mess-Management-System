# Mess-Management-System
Android app for bachelor mess management with smart meal tracking, expense logging, automated rate calculations, real-time sync, and full offline support.
​

#Features
Daily Meal Tracking: Log breakfast (0.5), lunch (1.0), dinner (1.0) with calendar picker and auto-total calculation.
​

Expense Management: Add expenses with multi-payer split, date, description, and payer tracking.
​

Automated Calculations: Real-time meal rate (Total Expenses / Total Meals) and individual dues (User Meals × Rate - Amount Paid).
​

Monthly Reports: Summary, expense lists, member details with color-coded balances (red: owes, green: overpaid).
​

Real-Time Sync: Firebase Firestore with WebSocket updates across devices.
​

Offline Mode: Full functionality with local cache and auto-sync queue on reconnect.
​

Push Notifications: Daily missed meal and payment due reminders via AlarmManager.
​

User Auth & Profile: Secure Firebase login, editable phone/password, member list.
​

Home Dashboard: Today's meals/expenses, current rate, quick actions.
​

Tech Stack
Frontend: Android (Java, XML, Material Design), min SDK 24.
​

Backend: Firebase (Auth, Firestore, Analytics).
​

Local Storage: SharedPreferences for offline caching.
​

Libraries: Gson, Joda-Time, RecyclerView, Material Components.
​

Optimizations: Hybrid caching, indexed queries (monthYear), battery-efficient notifications (<2% daily drain).
​

Architecture
Offline-first with Firebase sync:

Cache-first reads for instant UI.

Offline writes queue to SharedPreferences.

Auto-sync on reconnect (FIFO, max 100 entries).
​

Database Collections: users, meals (with monthYear index), expenses.
​

Open in Android Studio.

Add your Firebase config (google-services.json).

Build and run (API 24+).
​

Screenshots
See screenshots/ folder for UI examples (dashboard, reports, offline mode).
​

Limitations
No edit/delete, Android-only, single mess per user, Firebase free tier limits.
​

Future Work
Edit/delete, iOS/web, payments, multi-mess, PDF exports, analytics.

