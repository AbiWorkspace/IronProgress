package com.example.diets.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.WeightRecord;

import java.util.List;
import java.util.Locale;

public class WeightAdapter
        extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {

    public interface OnEditRequested {
        void onEdit(WeightRecord record, int position);
    }

    private final List<WeightRecord> data;
    private final OnEditRequested listener;

    public WeightAdapter(List<WeightRecord> data, OnEditRequested l) {
        this.data = data;
        this.listener = l;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vType) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_weight_record, p, false);
        return new WeightViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder h, int pos) {
        WeightRecord r = data.get(pos);
        h.weightText.setText(String.format(Locale.getDefault(),"%.1f kg", r.getWeight()));
        h.kcalText.setText(String.format(Locale.getDefault(),"%.0f kcal", r.getCalories()));
        h.dateText.setText(r.getDate());

        h.itemView.setOnClickListener(v -> listener.onEdit(r, pos));
    }

    @Override public int getItemCount() { return data.size(); }

    static class WeightViewHolder extends RecyclerView.ViewHolder {
        TextView weightText, kcalText, dateText;
        WeightViewHolder(@NonNull View v) {
            super(v);
            weightText = v.findViewById(R.id.weightText);
            kcalText   = v.findViewById(R.id.kcalText);
            dateText   = v.findViewById(R.id.dateText);
        }
    }
}
