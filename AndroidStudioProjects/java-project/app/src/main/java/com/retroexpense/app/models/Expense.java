package com.retroexpense.app.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.UUID;

@IgnoreExtraProperties
public class Expense implements Serializable {
    private String id;
    private String category;
    private double amount;
    private long dateUtcMillis;
    private String note;
    private String ownerId;

    // Required empty constructor for Firebase / Gson
    public Expense() {
    }

    public Expense(String category, double amount, long dateUtcMillis, @Nullable String note) {
        this(null, category, amount, dateUtcMillis, note);
    }

    public Expense(@Nullable String ownerId, String category, double amount, long dateUtcMillis, @Nullable String note) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.category = category;
        this.amount = amount;
        this.dateUtcMillis = dateUtcMillis;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDateUtcMillis() {
        return dateUtcMillis;
    }

    public void setDateUtcMillis(long dateUtcMillis) {
        this.dateUtcMillis = dateUtcMillis;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    @Nullable
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable String ownerId) {
        this.ownerId = ownerId;
    }

    @NonNull
    @Override
    public String toString() {
        return category + " - $" + amount;
    }
}


