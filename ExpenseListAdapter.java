package com.example.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ExpenseListAdapter extends RecyclerView.Adapter<ExpenseListAdapter.ExpenseViewHolder> {
    
    private List<Expense> expenseList;

    public ExpenseListAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        
        holder.tvDate.setText(expense.getDate());
        holder.tvDescription.setText(expense.getDescription());
        holder.tvAmount.setText(String.format("৳%.2f", expense.getAmount()));
        
        // Show payer names
        if (expense.getPayerNames() != null && !expense.getPayerNames().isEmpty()) {
            String payers = String.join(", ", expense.getPayerNames());
            holder.tvPayers.setText("Paid by: " + payers);
        } else {
            holder.tvPayers.setText("Paid by: Unknown");
        }
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDescription, tvAmount, tvPayers;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvDescription = itemView.findViewById(R.id.tvExpenseDescription);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvPayers = itemView.findViewById(R.id.tvExpensePayers);
        }
    }

    public void updateList(List<Expense> newList) {
        expenseList = newList;
        notifyDataSetChanged();
    }
}
