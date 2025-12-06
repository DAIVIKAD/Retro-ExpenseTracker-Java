package com.retroexpense.app.utils;

public final class HealthScoreCalculator {

    private HealthScoreCalculator() {}

    public static int calculateScore(double monthlyBudget, double monthlySpend, int pendingItems) {
        if (monthlyBudget <= 0) {
            return 50;
        }

        double utilization = monthlySpend / monthlyBudget;
        int baseScore;

        if (utilization <= 0.5) {
            baseScore = 95;
        } else if (utilization <= 0.8) {
            baseScore = 80;
        } else if (utilization <= 1.0) {
            baseScore = 65;
        } else {
            baseScore = 40;
        }

        baseScore -= Math.min(20, pendingItems * 2);
        baseScore = Math.max(0, Math.min(100, baseScore));
        return baseScore;
    }

    public static String verdictForScore(int score) {
        if (score >= 85) return "SYSTEM STABLE";
        if (score >= 70) return "OPTIMIZE SPENDING";
        if (score >= 50) return "WARNING: HIGH USAGE";
        return "CRITICAL: CUT EXPENSES";
    }
}
