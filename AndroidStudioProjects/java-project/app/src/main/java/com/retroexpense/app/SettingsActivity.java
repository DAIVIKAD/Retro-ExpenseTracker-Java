package com.retroexpense.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.retroexpense.app.core.RetroBaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.retroexpense.app.managers.PreferencesManager;

public class SettingsActivity extends RetroBaseActivity {
    private EditText pinInput, budgetInput;
    private Switch soundEffectsSwitch, notificationsSwitch;
    private Button savePinBtn, saveBudgetBtn, logoutBtn;
    private TextView statusText;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferencesManager = new PreferencesManager(this);

        initViews();
        loadSettings();
    }

    private void initViews() {
        pinInput = findViewById(R.id.pin_input);
        budgetInput = findViewById(R.id.budget_input);
        soundEffectsSwitch = findViewById(R.id.sound_effects_switch);
        notificationsSwitch = findViewById(R.id.notifications_switch);
        savePinBtn = findViewById(R.id.save_pin_btn);
        saveBudgetBtn = findViewById(R.id.save_budget_btn);
        logoutBtn = findViewById(R.id.logout_btn);
        statusText = findViewById(R.id.status_text);

        savePinBtn.setOnClickListener(v -> savePin());
        saveBudgetBtn.setOnClickListener(v -> saveBudget());
        logoutBtn.setOnClickListener(v -> confirmLogout());

        soundEffectsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferencesManager.setSoundEffectsEnabled(isChecked));

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferencesManager.setNotificationsEnabled(isChecked));
    }

    private void loadSettings() {
        float budget = preferencesManager.getMonthlyBudget();
        if (budget > 0) {
            budgetInput.setText(String.valueOf(budget));
        }

        soundEffectsSwitch.setChecked(preferencesManager.isSoundEffectsEnabled());
        notificationsSwitch.setChecked(preferencesManager.isNotificationsEnabled());
    }

    private void savePin() {
        String pin = pinInput.getText().toString().trim();

        if (pin.length() != 4 || !pin.matches("\\d{4}")) {
            statusText.setText("ERROR: PIN MUST BE 4 DIGITS");
            return;
        }

        preferencesManager.savePin(pin);
        statusText.setText("PIN SAVED SUCCESSFULLY");
        pinInput.setText("");

        new Handler(Looper.getMainLooper()).postDelayed(() ->
                statusText.setText("SETTINGS"), 2000);
    }

    private void saveBudget() {
        String budgetStr = budgetInput.getText().toString().trim();

        if (budgetStr.isEmpty()) {
            statusText.setText("ERROR: BUDGET REQUIRED");
            return;
        }

        try {
            float budget = Float.parseFloat(budgetStr);
            preferencesManager.saveMonthlyBudget(budget);
            statusText.setText("BUDGET SAVED: $" + budget);

            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    statusText.setText("SETTINGS"), 2000);
        } catch (NumberFormatException e) {
            statusText.setText("ERROR: INVALID BUDGET FORMAT");
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("CONFIRM LOGOUT")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("YES", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    finishAffinity();
                })
                .setNegativeButton("NO", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_settings_mode));
        }
    }
}
