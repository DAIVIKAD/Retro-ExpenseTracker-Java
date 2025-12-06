package com.retroexpense.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.retroexpense.app.models.Expense;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OfflineCache {

    private static final String PREFS_NAME = "RetroExpenseCache";
    private static final String KEY_PENDING_EXPENSES = "pending_expenses";
    private static final String KEY_CACHED_EXPENSES = "cached_expenses";

    private SharedPreferences prefs;
    private Gson gson;

    public OfflineCache(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // -------------------
    // Pending (offline)
    // -------------------

    public void addPendingExpense(Expense expense) {
        List<Expense> pending = getPendingExpenses();
        pending.add(expense);
        savePendingExpenses(pending);
    }

    public List<Expense> getPendingExpenses() {
        String json = prefs.getString(KEY_PENDING_EXPENSES, "[]");
        Type type = new TypeToken<ArrayList<Expense>>() {}.getType();
        List<Expense> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    public void removePendingExpense(Expense expense) {
        List<Expense> pending = getPendingExpenses();
        pending.removeIf(e -> e.getId() != null && e.getId().equals(expense.getId()));
        savePendingExpenses(pending);
    }

    public void clearPendingExpenses() {
        savePendingExpenses(new ArrayList<>());
    }

    private void savePendingExpenses(List<Expense> expenses) {
        String json = gson.toJson(expenses);
        prefs.edit().putString(KEY_PENDING_EXPENSES, json).apply();
    }

    // -------------------
    // Cached Data
    // -------------------

    public void cacheExpenses(List<Expense> expenses) {
        String json = gson.toJson(expenses);
        prefs.edit().putString(KEY_CACHED_EXPENSES, json).apply();
    }

    public List<Expense> getCachedExpenses() {
        String json = prefs.getString(KEY_CACHED_EXPENSES, "[]");
        Type type = new TypeToken<ArrayList<Expense>>() {}.getType();
        List<Expense> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    public List<Expense> getCachedExpensesForUser(@Nullable String ownerId) {
        List<Expense> all = getCachedExpenses();
        if (ownerId == null) {
            return all;
        }
        List<Expense> filtered = new ArrayList<>();
        for (Expense expense : all) {
            if (expense != null && (expense.getOwnerId() == null || ownerId.equals(expense.getOwnerId()))) {
                filtered.add(expense);
            }
        }
        return filtered;
    }

    public void clearCache() {
        prefs.edit().remove(KEY_CACHED_EXPENSES).apply();
    }

    // -------------------
    // Sync helpers
    // -------------------

    public int getPendingCount() {
        return getPendingExpenses().size();
    }

    public boolean hasPendingSync() {
        return getPendingCount() > 0;
    }
}
