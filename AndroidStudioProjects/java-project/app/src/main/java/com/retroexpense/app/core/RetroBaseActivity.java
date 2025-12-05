package com.retroexpense.app.core;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.retroexpense.app.ui.NinjaOverlay;

/**
 * Small base activity that keeps the ninja companion attached
 * to the current screen so it can animate across navigation events.
 */
public abstract class RetroBaseActivity extends AppCompatActivity {

    private NinjaOverlay ninjaOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ninjaOverlay = NinjaOverlay.getInstance(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ninjaOverlay != null) {
            ninjaOverlay.attach(this);
        }
    }

    @Override
    protected void onPause() {
        if (ninjaOverlay != null) {
            ninjaOverlay.detach(this);
        }
        super.onPause();
    }

    protected NinjaOverlay getNinjaOverlay() {
        return ninjaOverlay;
    }
}

