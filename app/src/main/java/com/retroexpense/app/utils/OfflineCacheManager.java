package com.retroexpense.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.retroexpense.app.models.Expense;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OfflineCacheManager {
    private static final String PREFS_NAME = "RetroExpenseCache";
    private static final String KEY_PENDING = "pending_expenses";
    private final SharedPreferences prefs;
    private final Gson gson;
    private final FirebaseAuth auth;

    public OfflineCacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.auth = FirebaseAuth.getInstance();
    }

    public void addPendingExpense(Expense expense) {
        if (expense.getOwnerId() == null) {
            expense.setOwnerId(auth.getUid());
        }
        List<Expense> pending = getStoredPendingExpenses();
        pending.add(expense);
        savePendingExpenses(pending);
    }

    public List<Expense> getPendingExpenses() {
        List<Expense> stored = getStoredPendingExpenses();
        String uid = auth.getUid();
        if (uid == null) {
            return stored;
        }
        List<Expense> filtered = new ArrayList<>();
        for (Expense expense : stored) {
            if (expense != null && (expense.getOwnerId() == null || uid.equals(expense.getOwnerId()))) {
                filtered.add(expense);
            }
        }
        return filtered;
    }

    private void savePendingExpenses(List<Expense> expenses) {
        String json = gson.toJson(expenses);
        prefs.edit().putString(KEY_PENDING, json).apply();
    }

    public void syncPendingExpenses(OnSyncCompleteListener listener) {
        List<Expense> pending = new ArrayList<>(getPendingExpenses());

        if (pending.isEmpty()) {
            listener.onComplete(0, 0);
            return;
        }

        String uid = auth.getUid();
        if (uid == null) {
            listener.onComplete(0, pending.size());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference userCollection = db.collection("users")
                .document(uid)
                .collection("expenses");
        int totalCount = pending.size();
        final int[] successCount = {0};
        final int[] processedCount = {0};

        for (Expense expense : pending) {
            String docId = expense.getId() != null ? expense.getId() : userCollection.document().getId();
            DocumentReference documentReference = userCollection.document(docId);
            if (expense.getId() == null) {
                updatePendingExpenseId(expense, docId);
            }
            expense.setId(docId);

            documentReference
                    .set(expense)
                    .addOnSuccessListener(ignored -> {
                        successCount[0]++;
                        processedCount[0]++;
                        removePendingExpense(expense);

                        if (processedCount[0] == totalCount) {
                            listener.onComplete(successCount[0], totalCount);
                        }
                    })
                    .addOnFailureListener(e -> {
                        processedCount[0]++;

                        if (processedCount[0] == totalCount) {
                            listener.onComplete(successCount[0], totalCount);
                        }
                    });
        }
    }

    public void clearPendingExpenses() {
        prefs.edit().remove(KEY_PENDING).apply();
    }

    public int getPendingCount() {
        return getPendingExpenses().size();
    }

    private void removePendingExpense(Expense expense) {
        List<Expense> pending = getStoredPendingExpenses();
        pending.removeIf(e -> isSameExpense(e, expense));
        savePendingExpenses(pending);
    }

    private void updatePendingExpenseId(Expense target, String newId) {
        List<Expense> pending = getStoredPendingExpenses();
        boolean updated = false;
        for (Expense expense : pending) {
            if (isSameExpense(expense, target)) {
                expense.setId(newId);
                updated = true;
                break;
            }
        }
        if (updated) {
            savePendingExpenses(pending);
        }
    }

    private List<Expense> getStoredPendingExpenses() {
        String json = prefs.getString(KEY_PENDING, "[]");
        Type type = new TypeToken<List<Expense>>(){}.getType();
        List<Expense> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    private boolean isSameExpense(Expense a, Expense b) {
        if (a == null || b == null) return false;
        if (a.getOwnerId() != null || b.getOwnerId() != null) {
            if (!Objects.equals(a.getOwnerId(), b.getOwnerId())) {
                return false;
            }
        }
        if (a.getId() != null && b.getId() != null) {
            return Objects.equals(a.getId(), b.getId());
        }
        return Objects.equals(a.getCategory(), b.getCategory()) &&
                Double.compare(a.getAmount(), b.getAmount()) == 0 &&
                a.getDateUtcMillis() == b.getDateUtcMillis() &&
                Objects.equals(a.getNote(), b.getNote());
    }

    public interface OnSyncCompleteListener {
        void onComplete(int successCount, int totalCount);
    }
}
