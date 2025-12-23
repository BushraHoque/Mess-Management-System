package com.example.messmanagement;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemberReportAdapter extends RecyclerView.Adapter<MemberReportAdapter.ReportViewHolder> {
    
    private List<MemberReport> reportList;

    public MemberReportAdapter(List<MemberReport> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_member_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        MemberReport report = reportList.get(position);
        
        holder.tvName.setText(report.userName);
        holder.tvMealCount.setText(String.valueOf(report.mealCount));
        holder.tvMealBill.setText(String.format("৳%.2f", report.mealBill));
        holder.tvAmountPaid.setText(String.format("৳%.2f", report.amountPaid));
        holder.tvDueBalance.setText(String.format("৳%.2f", report.dueBalance));
        
        // Color code the due balance
        if (report.dueBalance > 0) {
            holder.tvDueBalance.setTextColor(Color.RED);
        } else if (report.dueBalance < 0) {
            holder.tvDueBalance.setTextColor(Color.GREEN);
        } else {
            holder.tvDueBalance.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMealCount, tvMealBill, tvAmountPaid, tvDueBalance;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReportName);
            tvMealCount = itemView.findViewById(R.id.tvReportMealCount);
            tvMealBill = itemView.findViewById(R.id.tvReportMealBill);
            tvAmountPaid = itemView.findViewById(R.id.tvReportAmountPaid);
            tvDueBalance = itemView.findViewById(R.id.tvReportDueBalance);
        }
    }

    public void updateList(List<MemberReport> newList) {
        reportList = newList;
        notifyDataSetChanged();
    }
}
