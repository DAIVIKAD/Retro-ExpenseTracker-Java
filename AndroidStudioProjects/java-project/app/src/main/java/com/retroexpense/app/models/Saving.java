package com.retroexpense.app.models;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.UUID;

@IgnoreExtraProperties
public class Saving implements Serializable {

    private String id;
    private double amount;
    private long dateUtcMillis;
    private String note;
    private String ownerId;

    public Saving() {
    }

    public Saving(String ownerId, double amount, long dateUtcMillis, @Nullable String note) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
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
}

