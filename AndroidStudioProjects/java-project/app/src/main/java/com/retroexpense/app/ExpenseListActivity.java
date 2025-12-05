package com.retroexpense.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.retroexpense.app.core.RetroBaseActivity;
import com.retroexpense.app.models.Expense;
import com.retroexpense.app.ui.adapters.ExpenseAdapter;
import com.retroexpense.app.utils.OfflineCache;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ExpenseListActivity extends RetroBaseActivity implements ExpenseAdapter.ExpenseActionListener {

    private ListView listView;
    private TextView statusText;
    private ExpenseAdapter adapter;
    private final List<Expense> expenses = new ArrayList<>();
    private OfflineCache offlineCache;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        offlineCache = new OfflineCache(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        listView = findViewById(R.id.expense_list);
        statusText = findViewById(R.id.status_text);

        adapter = new ExpenseAdapter(this, expenses, this);
        listView.setAdapter(adapter);
    }

    private void loadExpenses() {
        String userId = auth.getUid();
        if (userId == null) {
            statusText.setText("LOGIN REQUIRED TO VIEW EXPENSES");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        statusText.setText("FETCHING EXPENSES...");
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_list_mode));
        }
        firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Expense> fresh = new ArrayList<>();
                    for (QueryDocumentSnapshot snapshot : querySnapshot) {
                        Expense expense = snapshot.toObject(Expense.class);
                        if (expense != null) {
                            if (expense.getOwnerId() == null) {
                                expense.setOwnerId(userId);
                            }
                            fresh.add(expense);
                        }
                    }
                    updateList(fresh);
                    offlineCache.cacheExpenses(fresh);
                    if (getNinjaOverlay() != null) {
                        getNinjaOverlay().perchOnTop(getString(R.string.ninja_list_mode));
                    }
                })
                .addOnFailureListener(error -> {
                    List<Expense> cached = offlineCache.getCachedExpensesForUser(userId);
                    updateList(cached);
                    statusText.setText("OFFLINE MODE • SHOWING CACHED DATA (" + cached.size() + ")");
                    if (getNinjaOverlay() != null) {
                        getNinjaOverlay().perchOnTop(getString(R.string.ninja_list_mode));
                    }
                });
    }

    private void updateList(List<Expense> incoming) {
        expenses.clear();
        if (incoming != null) {
            expenses.addAll(incoming);
        }
        Collections.sort(expenses, Comparator.comparingLong(Expense::getDateUtcMillis).reversed());
        adapter.notifyDataSetChanged();
        statusText.setText(expenses.isEmpty() ? "NO EXPENSES FOUND" : "TOTAL: " + expenses.size());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_list_mode));
        }
    }

    @Override
    public void onEdit(Expense expense) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_transaction, null, false);
        EditText amountInput = dialogView.findViewById(R.id.input_amount);
        EditText noteInput = dialogView.findViewById(R.id.input_note);
        amountInput.setText(String.valueOf(expense.getAmount()));
        noteInput.setText(expense.getNote());

        new AlertDialog.Builder(this)
                .setTitle(R.string.expense_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save_button, (dialog, which) -> {
                    String amountStr = amountInput.getText().toString().trim();
                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    expense.setAmount(amount);
                    expense.setNote(noteInput.getText().toString().trim());
                    commitExpenseUpdate(expense);
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    @Override
    public void onDelete(Expense expense) {
        String userId = auth.getUid();
        if (userId == null || expense.getId() == null) return;

        firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .document(expense.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    expenses.removeIf(e -> e.getId().equals(expense.getId()));
                    adapter.notifyDataSetChanged();
                    statusText.setText(expenses.isEmpty() ? "NO EXPENSES FOUND" : "TOTAL: " + expenses.size());
                });
    }

    private void commitExpenseUpdate(Expense expense) {
        String userId = auth.getUid();
        if (userId == null || expense.getId() == null) return;

        firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .document(expense.getId())
                .set(expense)
                .addOnSuccessListener(unused -> {
                    Collections.sort(expenses, Comparator.comparingLong(Expense::getDateUtcMillis).reversed());
                    adapter.notifyDataSetChanged();
                });
    }
}

