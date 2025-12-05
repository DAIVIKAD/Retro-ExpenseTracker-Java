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
import com.retroexpense.app.models.Saving;
import com.retroexpense.app.ui.adapters.SavingsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SavingsListActivity extends RetroBaseActivity implements SavingsAdapter.SavingsActionListener {

    private ListView listView;
    private TextView statusText;
    private SavingsAdapter adapter;
    private final List<Saving> savings = new ArrayList<>();
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_list);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.savings_list);
        statusText = findViewById(R.id.status_text);

        adapter = new SavingsAdapter(this, savings, this);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavings();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_list_mode));
        }
    }

    private void loadSavings() {
        String userId = auth.getUid();
        if (userId == null) {
            statusText.setText("LOGIN REQUIRED");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        statusText.setText("FETCHING SAVINGS...");
        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    savings.clear();
                    for (QueryDocumentSnapshot snapshot : querySnapshot) {
                        Saving saving = snapshot.toObject(Saving.class);
                        if (saving != null) {
                            if (saving.getOwnerId() == null) {
                                saving.setOwnerId(userId);
                            }
                            savings.add(saving);
                        }
                    }
                    Collections.sort(savings, Comparator.comparingLong(Saving::getDateUtcMillis).reversed());
                    adapter.notifyDataSetChanged();
                    statusText.setText(savings.isEmpty() ? "NO SAVINGS FOUND" : "TOTAL: " + savings.size());
                })
                .addOnFailureListener(e -> statusText.setText("FAILED TO LOAD SAVINGS"));
    }

    @Override
    public void onEdit(Saving saving) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_transaction, null, false);
        EditText amountInput = dialogView.findViewById(R.id.input_amount);
        EditText noteInput = dialogView.findViewById(R.id.input_note);
        amountInput.setText(String.valueOf(saving.getAmount()));
        noteInput.setText(saving.getNote());

        new AlertDialog.Builder(this)
                .setTitle(R.string.savings_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save_button, (dialog, which) -> {
                    String amountStr = amountInput.getText().toString().trim();
                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException ex) {
                        return;
                    }
                    saving.setAmount(amount);
                    saving.setNote(noteInput.getText().toString().trim());
                    commitSavingUpdate(saving);
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    @Override
    public void onDelete(Saving saving) {
        String userId = auth.getUid();
        if (userId == null || saving.getId() == null) return;

        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .document(saving.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    savings.removeIf(s -> s.getId().equals(saving.getId()));
                    adapter.notifyDataSetChanged();
                });
    }

    private void commitSavingUpdate(Saving saving) {
        String userId = auth.getUid();
        if (userId == null || saving.getId() == null) return;

        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .document(saving.getId())
                .set(saving)
                .addOnSuccessListener(unused -> {
                    Collections.sort(savings, Comparator.comparingLong(Saving::getDateUtcMillis).reversed());
                    adapter.notifyDataSetChanged();
                });
    }
}

