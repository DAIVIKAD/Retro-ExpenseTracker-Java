package com.retroexpense.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.retroexpense.app.core.RetroBaseActivity;
import com.retroexpense.app.models.Saving;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddSavingActivity extends RetroBaseActivity {

    private EditText amountInput;
    private EditText dateInput;
    private EditText noteInput;
    private TextView statusText;
    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_saving);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        amountInput = findViewById(R.id.amount_input);
        dateInput = findViewById(R.id.date_input);
        noteInput = findViewById(R.id.note_input);
        statusText = findViewById(R.id.status_text);

        Button saveBtn = findViewById(R.id.save_btn);
        Button cancelBtn = findViewById(R.id.cancel_btn);

        dateInput.setText(dateFormat.format(selectedDate.getTime()));
        dateInput.setOnClickListener(v -> showDatePicker());
        saveBtn.setOnClickListener(v -> saveSaving());
        cancelBtn.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);
                    dateInput.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveSaving() {
        String userId = auth.getUid();
        if (userId == null) {
            statusText.setText("ERROR: LOGIN REQUIRED");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String amountStr = amountInput.getText().toString().trim();
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

        String note = noteInput.getText().toString().trim();
        Saving saving = new Saving(userId, amount, selectedDate.getTimeInMillis(), note);
        statusText.setText("UPLOADING...");
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_add_mode));
        }

        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .document(saving.getId())
                .set(saving)
                .addOnSuccessListener(unused -> {
                    statusText.setText(getString(R.string.savings_add_success));
                    amountInput.setText("");
                    noteInput.setText("");
                    if (getNinjaOverlay() != null) {
                        getNinjaOverlay().perchOnTop(getString(R.string.ninja_add_mode));
                    }
                })
                .addOnFailureListener(error -> {
                    statusText.setText(getString(R.string.savings_add_error));
                    if (getNinjaOverlay() != null) {
                        getNinjaOverlay().perchOnTop(getString(R.string.ninja_add_mode));
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_add_mode));
        }
    }
}

