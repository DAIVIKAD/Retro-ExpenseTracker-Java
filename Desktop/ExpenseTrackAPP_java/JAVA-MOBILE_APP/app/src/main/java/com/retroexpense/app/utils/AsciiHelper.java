package com.retroexpense.app.utils;

public final class AsciiHelper {

    private AsciiHelper() {}

    public static String generateUploadProgress(int progress) {
        int clamped = Math.max(0, Math.min(100, progress));
        int totalBars = 10;
        int filledBars = (int) Math.round((clamped / 100f) * totalBars);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < totalBars; i++) {
            bar.append(i < filledBars ? "█" : "░");
        }
        bar.append("] ");
        bar.append(String.format("%03d", clamped)).append("%");
        return "SYNC " + bar;
    }
}


