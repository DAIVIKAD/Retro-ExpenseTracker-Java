package com.retroexpense.app.utils;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class RetroAnimator {

    public interface AnimationCallback {
        void onComplete();
    }

    // -----------------------
    // Typing effect
    // -----------------------
    public static void typeText(TextView textView, String text, int delayMs,
                                AnimationCallback callback) {

        textView.setText("");
        Handler handler = new Handler(Looper.getMainLooper());

        for (int i = 0; i <= text.length(); i++) {
            final int index = i;
            handler.postDelayed(() -> {
                textView.setText(text.substring(0, index));
                if (index == text.length() && callback != null) {
                    callback.onComplete();
                }
            }, i * delayMs);
        }
    }

    // -----------------------
    // Scanning loader
    // -----------------------
    public static void scanningAnimation(TextView textView, String baseText,
                                         int durationMs, AnimationCallback callback) {

        String[] frames = {
                baseText + " [░░░░░░░░░░]",
                baseText + " [█░░░░░░░░░]",
                baseText + " [██░░░░░░░░]",
                baseText + " [███░░░░░░░]",
                baseText + " [████░░░░░░]",
                baseText + " [█████░░░░░]",
                baseText + " [██████░░░░]",
                baseText + " [███████░░░]",
                baseText + " [████████░░]",
                baseText + " [█████████░]",
                baseText + " [██████████]"
        };

        Handler handler = new Handler(Looper.getMainLooper());
        int frameDelay = durationMs / frames.length;

        for (int i = 0; i < frames.length; i++) {
            final int index = i;

            handler.postDelayed(() -> {
                textView.setText(frames[index]);
                if (index == frames.length - 1 && callback != null) {
                    callback.onComplete();
                }
            }, i * frameDelay);
        }
    }

    // -----------------------
    // Sync progress
    // -----------------------
    public static void syncProgressAnimation(TextView textView, int totalItems,
                                             AnimationCallback callback) {

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(totalItems * 300);

        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            textView.setText(AsciiHelper.generateUploadProgress(progress));
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (callback != null) callback.onComplete();
            }
        });

        animator.start();
    }

    // -----------------------
    // Blinking cursor
    // -----------------------
    public static void blinkingCursor(TextView textView, String baseText) {
        Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] showCursor = {true};

        Runnable blink = new Runnable() {
            @Override
            public void run() {
                textView.setText(showCursor[0] ? baseText + "█" : baseText + " ");
                showCursor[0] = !showCursor[0];
                handler.postDelayed(this, 500);
            }
        };

        handler.post(blink);
    }

    // -----------------------
    // Matrix rain (Hacker Mode)
    // -----------------------
    public static void matrixRain(TextView textView, int durationMs) {

        String chars = "0123456789ABCDEF$₹€£¥";
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable rain = new Runnable() {
            int iterations = 0;
            final int maxIterations = durationMs / 50;

            @Override
            public void run() {
                if (iterations < maxIterations) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 20; i++) {
                        sb.append(chars.charAt((int) (Math.random() * chars.length())));
                    }
                    textView.append(sb.toString() + "\n");
                    iterations++;
                    handler.postDelayed(this, 50);
                }
            }
        };

        handler.post(rain);
    }
}
