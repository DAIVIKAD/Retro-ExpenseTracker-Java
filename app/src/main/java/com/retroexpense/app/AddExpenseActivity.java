package com.retroexpense.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.retroexpense.app.core.RetroBaseActivity;
import com.retroexpense.app.models.Expense;
import com.retroexpense.app.utils.OfflineCache;
import com.retroexpense.app.utils.OfflineCacheManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends RetroBaseActivity {

    private Spinner categorySpinner;
    private EditText amountInput;
    private EditText dateInput;
    private EditText noteInput;
    private TextView statusText;
    private TextView receiptView;

    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private OfflineCache offlineCache;
    private OfflineCacheManager cacheManager;
    private FirebaseFirestore firestore;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categories;
    private String otherOption;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        offlineCache = new OfflineCache(this);
        cacheManager = new OfflineCacheManager(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        otherOption = getString(R.string.category_other);

        initViews();
        setUpSpinner();
        setCurrentDate();
        attachListeners();
    }

    private void initViews() {
        categorySpinner = findViewById(R.id.category_spinner);
        amountInput = findViewById(R.id.amount_input);
        dateInput = findViewById(R.id.date_input);
        noteInput = findViewById(R.id.note_input);
        statusText = findViewById(R.id.status_text);
        receiptView = findViewById(R.id.receipt_view);
    }

    private void setUpSpinner() {
        String[] baseCategories = getResources().getStringArray(R.array.expense_categories);
        categories = new ArrayList<>();
        Collections.addAll(categories, baseCategories);

        categoryAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        categoryAdapter.setDropDownViewResource(R.layout.spinner_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = categories.get(position);
                if (otherOption.equalsIgnoreCase(selected)) {
                    promptCustomCategory();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void setCurrentDate() {
        dateInput.setText(displayFormat.format(selectedDate.getTime()));
    }

    private void attachListeners() {
        dateInput.setOnClickListener(v -> showDatePicker());

        Button saveBtn = findViewById(R.id.save_btn);
        Button cancelBtn = findViewById(R.id.cancel_btn);

        saveBtn.setOnClickListener(v -> saveExpense());
        cancelBtn.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);
                    setCurrentDate();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveExpense() {
        String category = categorySpinner.getSelectedItem().toString();
        String amountStr = amountInput.getText().toString().trim();
        String note = noteInput.getText().toString().trim();
        String userId = auth.getUid();

        if (userId == null) {
            statusText.setText("ERROR: LOGIN REQUIRED");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            statusText.setText("ERROR: AMOUNT REQUIRED");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            statusText.setText("ERROR: INVALID AMOUNT");
            return;
        }

        if (amount <= 0) {
            statusText.setText("ERROR: AMOUNT MUST BE > 0");
            return;
        }

        Expense expense = new Expense(userId, category, amount, selectedDate.getTimeInMillis(), note);
        statusText.setText("UPLOADING...");
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_loading_scanning));
        }
        firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .document(expense.getId())
                .set(expense)
                .addOnSuccessListener(unused -> handleSuccess(expense))
                .addOnFailureListener(error -> handleFailure(expense, error));
    }

    private void handleSuccess(Expense expense) {
        statusText.setText("EXPENSE SAVED");
        cacheForHistory(expense);
        showReceipt(expense, true);
        clearInputs();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_add_mode));
        }
    }

    private void handleFailure(Expense expense, Exception error) {
        cacheManager.addPendingExpense(expense);
        cacheForHistory(expense);
        String message = error != null ? error.getMessage() : "UNKNOWN ERROR";
        statusText.setText("OFFLINE MODE. QUEUED (" + message + ")");
        showReceipt(expense, false);
        clearInputs();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_add_mode));
        }
    }

    private void cacheForHistory(Expense expense) {
        String userId = auth.getUid();
        List<Expense> current = new ArrayList<>(offlineCache.getCachedExpensesForUser(userId));
        current.add(expense);
        offlineCache.cacheExpenses(current);
    }

    private void clearInputs() {
        amountInput.setText("");
        noteInput.setText("");
        categorySpinner.setSelection(0);
        selectedDate.setTimeInMillis(System.currentTimeMillis());
        setCurrentDate();
    }

    private void showReceipt(Expense expense, boolean live) {
        String receiptId = expense.getId() != null && expense.getId().length() >= 8
                ? expense.getId().substring(0, 8)
                : expense.getId();

        String receipt = "╔════════════════════════╗\n" +
                "║   RETROEXPENSE RECEIPT ║\n" +
                "╠════════════════════════╣\n" +
                "║ ID: " + receiptId + "\n" +
                "║ CATEGORY: " + expense.getCategory() + "\n" +
                "║ DATE: " + displayFormat.format(new java.util.Date(expense.getDateUtcMillis())) + "\n" +
                "║ AMOUNT: $" + String.format(Locale.getDefault(), "%.2f", expense.getAmount()) + "\n" +
                "║ STATUS: " + (live ? "SYNCED" : "QUEUED") + "\n" +
                "╚════════════════════════╝";

        receiptView.setText(receipt);
        receiptView.setVisibility(View.VISIBLE);
    }

    private void promptCustomCategory() {
        final EditText input = new EditText(this);
        input.setHint(R.string.custom_category_hint);
        input.setTextColor(getResources().getColor(R.color.terminal_green, getTheme()));
        input.setHintTextColor(getResources().getColor(R.color.terminal_green_dim, getTheme()));
        input.setBackgroundResource(R.drawable.terminal_border);
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle(R.string.custom_category_title)
                .setView(input)
                .setPositiveButton(R.string.custom_category_positive, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(value)) {
                        int otherIndex = categories.indexOf(otherOption);
                        if (otherIndex < 0) {
                            categories.add(value);
                            otherIndex = categories.size() - 1;
                        } else {
                            categories.add(otherIndex, value);
                        }
                        categoryAdapter.notifyDataSetChanged();
                        categorySpinner.setSelection(otherIndex);
                    } else {
                        categorySpinner.setSelection(0);
                    }
                })
                .setNegativeButton(R.string.custom_category_negative, (dialog, which) -> categorySpinner.setSelection(0))
                .setOnDismissListener(dialog -> {
                    if (categorySpinner.getSelectedItem().toString().equalsIgnoreCase(otherOption)) {
                        categorySpinner.setSelection(0);
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_add_mode));
        }
    }
}

