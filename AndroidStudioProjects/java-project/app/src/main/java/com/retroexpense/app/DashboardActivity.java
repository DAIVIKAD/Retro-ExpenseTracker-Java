package com.retroexpense.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.retroexpense.app.core.RetroBaseActivity;
import com.retroexpense.app.managers.PreferencesManager;
import com.retroexpense.app.models.Expense;
import com.retroexpense.app.models.Saving;
import com.retroexpense.app.utils.HealthScoreCalculator;
import com.retroexpense.app.utils.OfflineCache;
import com.retroexpense.app.utils.OfflineCacheManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends RetroBaseActivity {

    private TextView todayTotalText;
    private TextView monthTotalText;
    private TextView pendingStatusText;
    private OfflineCacheManager cacheManager;
    private OfflineCache offlineCache;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    private final SimpleDateFormat dayLabelFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    private static final int TREND_DAYS = 7;
    private static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
    private PreferencesManager preferencesManager;
    private double lastMonthTotal = 0;
    private double lastTodayTotal = 0;
    private LineChart trendChart;
    private final List<Saving> latestSavings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        cacheManager = new OfflineCacheManager(this);
        offlineCache = new OfflineCache(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        preferencesManager = new PreferencesManager(this);

        initViews();
        attachActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSummary();
    }

    private void initViews() {
        todayTotalText = findViewById(R.id.today_total);
        monthTotalText = findViewById(R.id.month_total);
        pendingStatusText = findViewById(R.id.pending_status);
        trendChart = findViewById(R.id.trend_chart);
        configureTrendChart();
    }

    private void attachActions() {
        Button addExpenseBtn = findViewById(R.id.btn_add_expense);
        Button listBtn = findViewById(R.id.btn_expense_list);
        Button settingsBtn = findViewById(R.id.btn_settings);
        Button logoutBtn = findViewById(R.id.btn_logout);
        Button syncBtn = findViewById(R.id.btn_sync);
        Button refreshBtn = findViewById(R.id.btn_refresh);
        Button healthBtn = findViewById(R.id.btn_health);
        Button hackerBtn = findViewById(R.id.btn_hacker_mode);
        Button addSavingsBtn = findViewById(R.id.btn_add_savings);
        Button viewSavingsBtn = findViewById(R.id.btn_view_savings);

        addExpenseBtn.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        listBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseListActivity.class)));

        addSavingsBtn.setOnClickListener(v ->
                startActivity(new Intent(this, AddSavingActivity.class)));

        viewSavingsBtn.setOnClickListener(v ->
                startActivity(new Intent(this, SavingsListActivity.class)));

        settingsBtn.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        syncBtn.setOnClickListener(v -> syncPendingExpenses());
        refreshBtn.setOnClickListener(v -> {
            loadSummary();
            Toast.makeText(this, "Dashboard data refreshed", Toast.LENGTH_SHORT).show();
        });

        healthBtn.setOnClickListener(v -> showHealthStatus());

        hackerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, HackerModeActivity.class)));
    }

    private void loadSummary() {
        String userId = auth.getUid();
        if (userId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        pendingStatusText.setText(formatPendingLabel(cacheManager.getPendingCount()));
        todayTotalText.setText("TODAY: LOADING...");
        monthTotalText.setText("MONTH: LOADING...");
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_loading_scanning));
        }

        firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .get()
                .addOnSuccessListener(querySnapshot -> handleRemoteSummary(userId, querySnapshot.getDocuments()))
                .addOnFailureListener(e -> handleOfflineSummary(userId));
    }

    private void applySummary(double todayTotal, double monthTotal) {
        lastTodayTotal = todayTotal;
        lastMonthTotal = monthTotal;
        todayTotalText.setText("TODAY: " + currencyFormat.format(todayTotal));
        monthTotalText.setText("MONTH: " + currencyFormat.format(monthTotal));
    }

    private void syncPendingExpenses() {
        int pending = cacheManager.getPendingCount();
        if (pending == 0) {
            pendingStatusText.setText(formatPendingLabel(0));
            return;
        }

        pendingStatusText.setText("SYNCING ▸ " + pending);
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().showLoadingMessage(getString(R.string.ninja_loading_scanning));
        }
        cacheManager.syncPendingExpenses((successCount, totalCount) ->
                runOnUiThread(() -> {
                    pendingStatusText.setText("SYNC COMPLETE ▸ " + successCount + "/" + totalCount);
                    if (getNinjaOverlay() != null) {
                        getNinjaOverlay().perchOnTop(getString(R.string.ninja_ready));
                    }
                    loadSummary();
                }));
    }

    private void handleRemoteSummary(String userId, List<DocumentSnapshot> documents) {
        double todayTotal = 0;
        double monthTotal = 0;
        List<Expense> expenses = new ArrayList<>();
        long[] boundaries = getDateBoundaries();
        long todayStart = boundaries[0];
        long monthStart = boundaries[1];

        for (DocumentSnapshot snapshot : documents) {
            Expense expense = snapshot.toObject(Expense.class);
            if (expense == null) {
                continue;
            }
            if (expense.getOwnerId() == null) {
                expense.setOwnerId(userId);
            }
            expenses.add(expense);

            long date = expense.getDateUtcMillis();
            if (date >= todayStart) {
                todayTotal += expense.getAmount();
            }
            if (date >= monthStart) {
                monthTotal += expense.getAmount();
            }
        }

        offlineCache.cacheExpenses(expenses);
        applySummary(todayTotal, monthTotal);
        pendingStatusText.setText(formatPendingLabel(cacheManager.getPendingCount()));
        fetchSavingsForChart(userId, expenses);
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_ready));
        }
    }

    private void handleOfflineSummary(String userId) {
        List<Expense> cached = offlineCache.getCachedExpensesForUser(userId);
        long[] boundaries = getDateBoundaries();
        long todayStart = boundaries[0];
        long monthStart = boundaries[1];
        double todayTotal = 0;
        double monthTotal = 0;

        for (Expense expense : cached) {
            if (expense == null) continue;
            long date = expense.getDateUtcMillis();
            if (date >= todayStart) {
                todayTotal += expense.getAmount();
            }
            if (date >= monthStart) {
                monthTotal += expense.getAmount();
            }
        }

        applySummary(todayTotal, monthTotal);
        updateTrendChart(cached, latestSavings);
        pendingStatusText.setText(getString(R.string.pending_panel_offline) + " • " + formatPendingLabel(cacheManager.getPendingCount()));
        if (getNinjaOverlay() != null) {
            getNinjaOverlay().perchOnTop(getString(R.string.ninja_ready));
        }
    }

    private void fetchSavingsForChart(String userId, List<Expense> expenses) {
        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    latestSavings.clear();
                    for (DocumentSnapshot snapshot : querySnapshot) {
                        Saving saving = snapshot.toObject(Saving.class);
                        if (saving != null) {
                            if (saving.getOwnerId() == null) {
                                saving.setOwnerId(userId);
                            }
                            latestSavings.add(saving);
                        }
                    }
                    updateTrendChart(expenses, latestSavings);
                })
                .addOnFailureListener(e -> updateTrendChart(expenses, latestSavings));
    }

    private void configureTrendChart() {
        if (trendChart == null) return;
        trendChart.setNoDataText(getString(R.string.dashboard_chart_empty));
        trendChart.setNoDataTextColor(color(R.color.terminal_green_dim));
        trendChart.getDescription().setEnabled(false);
        trendChart.setTouchEnabled(false);
        trendChart.setDragEnabled(false);
        trendChart.setScaleEnabled(false);
        trendChart.setPinchZoom(false);
        trendChart.setDrawGridBackground(false);

        Legend legend = trendChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(color(R.color.terminal_green_bright));
        legend.setForm(Legend.LegendForm.LINE);

        XAxis xAxis = trendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(color(R.color.terminal_green_bright));
        xAxis.setAxisLineColor(color(R.color.terminal_green));
        xAxis.setGridColor(color(R.color.terminal_green_dim));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(TREND_DAYS, true);

        YAxis leftAxis = trendChart.getAxisLeft();
        leftAxis.setTextColor(color(R.color.terminal_green_bright));
        leftAxis.setAxisLineColor(color(R.color.terminal_green));
        leftAxis.setGridColor(color(R.color.terminal_green_dim));
        leftAxis.setGranularity(1f);
        leftAxis.setLabelCount(5, true);
        trendChart.getAxisRight().setEnabled(false);
    }

    private void updateTrendChart(List<Expense> expenses, List<Saving> savings) {
        if (trendChart == null) return;
        if ((expenses == null || expenses.isEmpty()) && (savings == null || savings.isEmpty())) {
            trendChart.clear();
            trendChart.invalidate();
            return;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        startCal.add(Calendar.DAY_OF_YEAR, -(TREND_DAYS - 1));
        long windowStart = startCal.getTimeInMillis();

        Calendar labelCal = (Calendar) startCal.clone();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            labels.add(dayLabelFormat.format(labelCal.getTime()));
            labelCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        float[] expenseTotals = new float[TREND_DAYS];
        float[] savingsTotals = new float[TREND_DAYS];
        for (Expense expense : expenses) {
            if (expense == null) continue;
            long date = expense.getDateUtcMillis();
            if (date < windowStart) continue;
            int index = (int) ((date - windowStart) / DAY_MILLIS);
            if (index >= 0 && index < TREND_DAYS) {
                expenseTotals[index] += (float) expense.getAmount();
            }
        }

        if (savings != null) {
            for (Saving saving : savings) {
                if (saving == null) continue;
                long date = saving.getDateUtcMillis();
                if (date < windowStart) continue;
                int index = (int) ((date - windowStart) / DAY_MILLIS);
                if (index >= 0 && index < TREND_DAYS) {
                    savingsTotals[index] += (float) saving.getAmount();
                }
            }
        }

        List<Entry> expenseEntries = new ArrayList<>();
        List<Entry> savingsEntries = new ArrayList<>();
        boolean hasExpenseData = false;
        boolean hasSavingsData = false;
        for (int i = 0; i < TREND_DAYS; i++) {
            expenseEntries.add(new Entry(i, expenseTotals[i]));
            savingsEntries.add(new Entry(i, savingsTotals[i]));
            if (expenseTotals[i] > 0f) {
                hasExpenseData = true;
            }
            if (savingsTotals[i] > 0f) {
                hasSavingsData = true;
            }
        }

        LineData lineData = new LineData();
        if (hasExpenseData) {
            LineDataSet expenseSet = new LineDataSet(expenseEntries, getString(R.string.expense_chart_label));
            expenseSet.setColor(color(R.color.terminal_green));
            expenseSet.setCircleColor(color(R.color.terminal_green_bright));
            expenseSet.setLineWidth(2f);
            expenseSet.setCircleRadius(3f);
            expenseSet.setDrawValues(false);
            expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            expenseSet.setDrawFilled(true);
            expenseSet.setFillColor(Color.parseColor("#3300FF00"));
            lineData.addDataSet(expenseSet);
        }

        if (hasSavingsData) {
            LineDataSet savingsSet = new LineDataSet(savingsEntries, getString(R.string.savings_chart_label));
            savingsSet.setColor(color(R.color.terminal_yellow));
            savingsSet.setCircleColor(color(R.color.terminal_yellow));
            savingsSet.setLineWidth(2f);
            savingsSet.setCircleRadius(3f);
            savingsSet.setDrawValues(false);
            savingsSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            savingsSet.setDrawFilled(true);
            savingsSet.setFillColor(Color.parseColor("#33FFFF00"));
            lineData.addDataSet(savingsSet);
        }

        if (lineData.getDataSetCount() == 0) {
            trendChart.clear();
            trendChart.invalidate();
            return;
        }

        trendChart.setData(lineData);

        XAxis xAxis = trendChart.getXAxis();
        List<String> axisLabels = new ArrayList<>(labels);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = Math.round(value);
                if (index >= 0 && index < axisLabels.size()) {
                    return axisLabels.get(index);
                }
                return "";
            }
        });

        trendChart.invalidate();
    }

    private String formatPendingLabel(int count) {
        return String.format(Locale.getDefault(), "SYNC QUEUE ▸ %d", count);
    }

    private int color(int resId) {
        return ContextCompat.getColor(this, resId);
    }

    private void showHealthStatus() {
        float budget = preferencesManager.getMonthlyBudget();
        if (budget <= 0) {
            Toast.makeText(this, "Set a monthly budget in Settings first.", Toast.LENGTH_LONG).show();
            return;
        }

        double monthSpend = lastMonthTotal;
        if (monthSpend == 0) {
            monthSpend = computeMonthSpendFromCache();
        }
        int pending = cacheManager.getPendingCount();
        int score = HealthScoreCalculator.calculateScore(budget, monthSpend, pending);
        String verdict = HealthScoreCalculator.verdictForScore(score);

        new AlertDialog.Builder(this)
                .setTitle(R.string.health_dialog_title)
                .setMessage(getString(R.string.health_dialog_message, score, verdict))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private double computeMonthSpendFromCache() {
        long monthStart = getDateBoundaries()[1];
        double total = 0;
        String userId = auth.getUid();
        for (Expense expense : offlineCache.getCachedExpensesForUser(userId)) {
            if (expense.getDateUtcMillis() >= monthStart) {
                total += expense.getAmount();
            }
        }
        return total;
    }

    private long[] getDateBoundaries() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long monthStart = calendar.getTimeInMillis();
        return new long[]{todayStart, monthStart};
    }
}

