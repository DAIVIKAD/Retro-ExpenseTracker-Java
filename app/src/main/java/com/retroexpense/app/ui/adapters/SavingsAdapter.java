package com.retroexpense.app.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.retroexpense.app.R;
import com.retroexpense.app.models.Saving;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SavingsAdapter extends ArrayAdapter<Saving> {

    public interface SavingsActionListener {
        void onEdit(Saving saving);

        void onDelete(Saving saving);
    }

    private final SavingsActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public SavingsAdapter(@NonNull Context context,
                          @NonNull List<Saving> data,
                          @NonNull SavingsActionListener listener) {
        super(context, 0, data);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_saving, parent, false);
        }

        Saving saving = getItem(position);
        if (saving == null) {
            return view;
        }

        TextView primary = view.findViewById(R.id.saving_primary);
        TextView note = view.findViewById(R.id.saving_note);
        Button editBtn = view.findViewById(R.id.btn_edit_saving);
        Button deleteBtn = view.findViewById(R.id.btn_delete_saving);

        primary.setText("$" + String.format(Locale.getDefault(), "%.2f", saving.getAmount()) +
                " • " + dateFormat.format(saving.getDateUtcMillis()));

        if (saving.getNote() != null && !saving.getNote().isEmpty()) {
            note.setVisibility(View.VISIBLE);
            note.setText("> " + saving.getNote());
        } else {
            note.setVisibility(View.GONE);
        }

        editBtn.setOnClickListener(v -> listener.onEdit(saving));
        deleteBtn.setOnClickListener(v -> listener.onDelete(saving));

        return view;
    }
}

