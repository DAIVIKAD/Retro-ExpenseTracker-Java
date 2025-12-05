package com.retroexpense.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import com.retroexpense.app.core.RetroBaseActivity;
import com.retroexpense.app.managers.PreferencesManager;

public class PinLockActivity extends RetroBaseActivity {
    private TextView pinDisplay, statusText;
    private StringBuilder currentPin;
    private String storedPin;
    private static final int PIN_LENGTH = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        PreferencesManager preferencesManager = new PreferencesManager(this);
        storedPin = preferencesManager.getPin();

        currentPin = new StringBuilder();

        pinDisplay = findViewById(R.id.pin_display);
        statusText = findViewById(R.id.status_text);

        setupNumpad();
    }

    private void setupNumpad() {
        int[] buttonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < buttonIds.length; i++) {
            final int digit = i;
            Button btn = findViewById(buttonIds[i]);
            btn.setOnClickListener(v -> addDigit(String.valueOf(digit)));
        }

        findViewById(R.id.btn_clear).setOnClickListener(v -> clearPin());
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteDigit());
    }

    private void addDigit(String digit) {
        if (currentPin.length() < PIN_LENGTH) {
            currentPin.append(digit);
            updateDisplay();

            if (currentPin.length() == PIN_LENGTH) {
                verifyPin();
            }
        }
    }

    private void deleteDigit() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updateDisplay();
        }
    }

    private void clearPin() {
        currentPin.setLength(0);
        updateDisplay();
        statusText.setText("ENTER PIN");
    }

    private void updateDisplay() {
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < PIN_LENGTH; i++) {
            if (i < currentPin.length()) {
                display.append("█ ");
            } else {
                display.append("░ ");
            }
        }
        pinDisplay.setText(display.toString());
    }

    private void verifyPin() {
        if (currentPin.toString().equals(storedPin)) {
            statusText.setText("ACCESS GRANTED");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }, 500);
        } else {
            statusText.setText("ACCESS DENIED");
            new Handler(Looper.getMainLooper()).postDelayed(this::clearPin, 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_pin_mode));
        }
    }
}
