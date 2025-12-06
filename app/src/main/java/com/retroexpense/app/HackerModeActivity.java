package com.retroexpense.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.retroexpense.app.core.RetroBaseActivity;
import com.retroexpense.app.utils.OfflineCacheManager;
import com.retroexpense.app.utils.RetroAnimator;

public class HackerModeActivity extends RetroBaseActivity {

    private TextView matrixOutput;
    private TextView statusLine;
    private TextView pendingLine;
    private OfflineCacheManager cacheManager;
    private boolean autoStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hacker_mode);

        cacheManager = new OfflineCacheManager(this);

        matrixOutput = findViewById(R.id.matrix_output);
        statusLine = findViewById(R.id.status_line);
        pendingLine = findViewById(R.id.pending_line);

        Button startBtn = findViewById(R.id.btn_start_matrix);
        Button syncBtn = findViewById(R.id.btn_sync_matrix);
        Button exitBtn = findViewById(R.id.btn_exit_matrix);

        updatePendingDisplay();

        startBtn.setOnClickListener(v -> startMatrixSequence());
        syncBtn.setOnClickListener(v -> triggerSync());
        exitBtn.setOnClickListener(v -> finish());
    }

    private void startMatrixSequence() {
        matrixOutput.setText("");
        statusLine.setText("INITIATING MATRIX STREAM...");
        RetroAnimator.matrixRain(matrixOutput, 6000);
        RetroAnimator.scanningAnimation(statusLine, "TRACE", 2400, this::updatePendingDisplay);
    }

    private void triggerSync() {
        statusLine.setText("SYNCING OFFLINE QUEUE...");
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_hacker_watch));
        }
        cacheManager.syncPendingExpenses((successCount, totalCount) -> runOnUiThread(() -> {
            statusLine.setText("SYNC RESULT " + successCount + "/" + totalCount);
            updatePendingDisplay();
            if (getNinjaOverlay() != null) {
                getNinjaOverlay().perchOnTop(getString(R.string.ninja_hacker_watch));
            }
        }));
    }

    private void updatePendingDisplay() {
        pendingLine.setText("QUEUE SIZE: " + cacheManager.getPendingCount());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!autoStarted) {
            startMatrixSequence();
            autoStarted = true;
        }
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_hacker_watch));
        }
    }
}
