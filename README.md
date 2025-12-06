# Retro Expense Tracker (Java / Android)

Retro Expense Tracker is an Android app built in **Java** with a **retro terminal-inspired UI**.  
It helps users log their daily expenses and savings, see trends, and understand how “healthy” their spending habits are, with support for **offline cache + Firebase sync**.

---

## ✨ Features

- 🔐 **Authentication**
  - Login & logout using **Firebase Authentication**
  - Session handling and redirect to `LoginActivity` on logout

- 📊 **Dashboard (DashboardActivity)**
  - Shows **Today’s total spending**
  - Shows **This month’s total spending**
  - Displays **pending sync status** for offline expenses
  - **7-day trend line chart** using MPAndroidChart
  - Buttons to quickly:
    - Add a new expense
    - View expense list
    - Add savings
    - View savings
    - Open settings
    - View health status
    - Trigger manual sync
    - Open **Hacker Mode** (fun retro / hacker-style screen)

- 💸 **Expense Management**
  - `AddExpenseActivity` – add a new expense (amount, category, date, note, etc.)
  - `ExpenseListActivity` – view all expenses in a list
  - Uses `Expense` model and Firestore for persistent storage
  - Offline cache for expenses when network is not available

- 💰 **Savings Management**
  - `AddSavingActivity` – add a saving entry
  - `SavingsListActivity` – view all savings entries
  - Uses `Saving` model in the `com.retroexpense.app.models` package

- 🧠 **Spending Health & Analytics**
  - `HealthScoreCalculator` utility calculates a **spending “health” score** based on expenses and savings
  - `DashboardActivity` uses this to show status text (e.g., good / needs improvement)
  - Trend graph over the last N days using MPAndroidChart

- 🌐 **Offline Support + Sync**
  - `OfflineCache` and `OfflineCacheManager` manage locally cached expenses/savings
  - A **Sync** button on the dashboard tries to push any pending items to Firestore
  - Useful when the user is offline and later regains internet

- ⚙️ **Settings & Preferences**
  - `SettingsActivity` – app configuration and options
  - `PreferencesManager` – wraps SharedPreferences for storing user settings / local flags
  - `RetroBaseActivity` – common base activity with shared retro styling/logic

- 🧑‍💻 **Hacker Mode**
  - `HackerModeActivity` – special screen with hacker/terminal-like UI for fun
  - Can be launched from the dashboard

---

## 🧱 Tech Stack

- **Language:** Java  
- **Platform:** Android (Android Studio project)  
- **Backend:** Firebase Firestore  
- **Auth:** Firebase Authentication  
- **Charting:** MPAndroidChart (line chart for trends)  
- **Storage / Utils:**
  - Firestore for cloud data
  - Offline cache manager for local pending data
  - SharedPreferences via `PreferencesManager`

---

## 📂 Project Structure (high level)

```text
AndroidStudioProjects/
└── java-project/
    ├── app/
    │   ├── src/
    │   │   ├── main/
    │   │   │   ├── java/com/retroexpense/app/
    │   │   │   │   ├── DashboardActivity.java
    │   │   │   │   ├── AddExpenseActivity.java
    │   │   │   │   ├── ExpenseListActivity.java
    │   │   │   │   ├── AddSavingActivity.java
    │   │   │   │   ├── SavingsListActivity.java
    │   │   │   │   ├── SettingsActivity.java
    │   │   │   │   ├── HackerModeActivity.java
    │   │   │   │   ├── LoginActivity.java
    │   │   │   │   ├── core/RetroBaseActivity.java
    │   │   │   │   ├── managers/PreferencesManager.java
    │   │   │   │   ├── models/Expense.java
    │   │   │   │   ├── models/Saving.java
    │   │   │   │   ├── utils/HealthScoreCalculator.java
    │   │   │   │   ├── utils/OfflineCache.java
    │   │   │   │   └── utils/OfflineCacheManager.java
    │   │   │   └── res/ (layouts, drawables, themes, etc.)
    │   ├── build.gradle
    │   └── ...
    ├── settings.gradle
    ├── gradle.properties
    └── gradlew / gradlew.bat
