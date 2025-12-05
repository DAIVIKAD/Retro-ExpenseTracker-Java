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
import com.retroexpense.app.models.Expense;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends ArrayAdapter<Expense> {

    public interface ExpenseActionListener {
        void onEdit(Expense expense);

        void onDelete(Expense expense);
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final ExpenseActionListener listener;

    public ExpenseAdapter(@NonNull Context context,
                          @NonNull List<Expense> expenses,
                          @NonNull ExpenseActionListener listener) {
        super(context, 0, expenses);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_expense, parent, false);
        }

        Expense expense = getItem(position);
        if (expense == null) {
            return view;
        }

        TextView primary = view.findViewById(R.id.expense_primary);
        TextView note = view.findViewById(R.id.expense_note);
        Button editBtn = view.findViewById(R.id.btn_edit_expense);
        Button deleteBtn = view.findViewById(R.id.btn_delete_expense);

        String primaryText = expense.getCategory() + " • $" + String.format(Locale.getDefault(), "%.2f", expense.getAmount())
                + " • " + dateFormat.format(expense.getDateUtcMillis());
        primary.setText(primaryText);

        if (expense.getNote() != null && !expense.getNote().isEmpty()) {
            note.setVisibility(View.VISIBLE);
            note.setText("> " + expense.getNote());
        } else {
            note.setVisibility(View.GONE);
        }

        editBtn.setOnClickListener(v -> listener.onEdit(expense));
        deleteBtn.setOnClickListener(v -> listener.onDelete(expense));

        return view;
    }
}

