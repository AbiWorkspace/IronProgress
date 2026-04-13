package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.TrainingDay;

import java.util.List;

public class TrainingDayDetailAdapter extends RecyclerView.Adapter<TrainingDayDetailAdapter.ViewHolder> {
    private List<TrainingDay> trainingDays;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TrainingDay trainingDay);
    }

    public TrainingDayDetailAdapter(List<TrainingDay> trainingDays, OnItemClickListener listener) {
        this.trainingDays = trainingDays;
        this.listener = listener;
        Log.d("TrainingDayDetailAdapter", "Constructor: Adapter creado con " + trainingDays.size() + " días de entrenamiento");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("TrainingDayDetailAdapter", "onCreateViewHolder: Inflando el layout del item");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_day_detail3, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrainingDay trainingDay = trainingDays.get(position);
        Log.d("TrainingDayDetailAdapter", "onBindViewHolder: Vinculando día de entrenamiento en la posición " + position + ": " + trainingDay);
        holder.bind(trainingDay, listener);
    }

    @Override
    public int getItemCount() {
        Log.d("TrainingDayDetailAdapter", "getItemCount: Número de días de entrenamiento: " + trainingDays.size());
        return trainingDays.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView exerciseNameTextView;
        private TextView exerciseDetailsTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameTextView = itemView.findViewById(R.id.exerciseNameTextView);
            exerciseDetailsTextView = itemView.findViewById(R.id.exerciseDetailsTextView);
            Log.d("TrainingDayDetailAdapter", "ViewHolder: ViewHolder creado");
        }

        public void bind(final TrainingDay trainingDay, final OnItemClickListener listener) {
            // Aquí puedes configurar los datos del día de entrenamiento
            exerciseNameTextView.setText("Día " + trainingDay.getDayIndex());
            exerciseDetailsTextView.setText("Detalles del día de entrenamiento");

            itemView.setOnClickListener(v -> {
                Log.d("TrainingDayDetailAdapter", "bind: Día de entrenamiento clicado: " + trainingDay);
                listener.onItemClick(trainingDay);
            });
        }
    }
}