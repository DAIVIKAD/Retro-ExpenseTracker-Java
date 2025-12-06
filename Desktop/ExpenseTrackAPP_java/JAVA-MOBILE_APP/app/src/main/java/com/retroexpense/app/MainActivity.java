package com.retroexpense.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.retroexpense.app.R;
import com.retroexpense.app.managers.PreferencesManager;
import com.retroexpense.app.core.RetroBaseActivity;

public class MainActivity extends RetroBaseActivity {
    private FirebaseAuth mAuth;
    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            Intent intent;

            if (currentUser != null) {
                PreferencesManager preferencesManager = new PreferencesManager(this);
                boolean hasPIN = preferencesManager.hasPin();

                if (hasPIN) {
                    intent = new Intent(MainActivity.this, PinLockActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, DashboardActivity.class);
                }
            } else {
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_boot_sequence));
        }
    }
}
