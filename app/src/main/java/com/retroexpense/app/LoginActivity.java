package com.retroexpense.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.retroexpense.app.core.RetroBaseActivity;

public class LoginActivity extends RetroBaseActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private TextView statusText;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        initViews();

        Button loginBtn = findViewById(R.id.login_btn);
        Button registerBtn = findViewById(R.id.register_btn);

        loginBtn.setOnClickListener(v -> attemptAuth(false));
        registerBtn.setOnClickListener(v -> attemptAuth(true));
    }

    private void initViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void attemptAuth(boolean isRegisterFlow) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            statusText.setText("ERROR: EMAIL AND PASSWORD REQUIRED");
            return;
        }

        if (password.length() < 6) {
            statusText.setText("ERROR: PASSWORD MUST BE 6+ CHARACTERS");
            return;
        }

        showLoading(true);

        if (isRegisterFlow) {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> handleAuthResult(task.isSuccessful(), task.getException(), true));
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> handleAuthResult(task.isSuccessful(), task.getException(), false));
        }
    }

    private void handleAuthResult(boolean success, Exception error, boolean isRegisterFlow) {
        showLoading(false);

        if (success) {
            statusText.setText(isRegisterFlow ? "USER CREATED. ACCESS GRANTED." : "ACCESS GRANTED.");
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                navigateToDashboard();
            }
        } else {
            String prefix = isRegisterFlow ? "REGISTER FAILED: " : "LOGIN FAILED: ";
            statusText.setText(prefix + (error != null ? error.getMessage() : "UNKNOWN ERROR"));
        }
    }

    private void navigateToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_ready));
        }
    }
}

