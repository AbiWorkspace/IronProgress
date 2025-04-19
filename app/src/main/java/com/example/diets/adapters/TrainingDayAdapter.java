/******************************************************************************
 *  TrainingDayAdapter.java                                                   *
 *  Botón siempre habilitado + subList segura                                  *
 *  © 2025 – Listo para copiar y pegar                                        *
 ******************************************************************************/

package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import java.util.ArrayList;
import java.util.List;

public class TrainingDayAdapter
        extends RecyclerView.Adapter<TrainingDayAdapter.ViewHolder> {

    private final List<TrainingDay> trainingDays;
    private final OnItemClickListener listener;
    private static final String TAG = "TrainingDayAdapter";

    public interface OnItemClickListener { void onItemClick(TrainingDay td); }

    public TrainingDayAdapter(List<TrainingDay> trainingDays,
                              OnItemClickListener listener) {
        this.trainingDays = (trainingDays != null) ? trainingDays : new ArrayList<>();
        this.listener     = listener;
    }

    // ───────────────── ViewHolder lifecycle ─────────────────
    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int vt) {
        Log.d(TAG,"🆕 Creando ViewHolder día.");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_training_day2,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h,int pos) {
        h.bind(trainingDays.get(pos), listener);
    }

    @Override public int getItemCount() { return trainingDays.size(); }

    // ───────────────── ViewHolder ────────────────────────────
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView   dayIndexTextView;
        private final TextView   dayTitleTextView;
        private final RecyclerView exercisesRecyclerView;
        private final Button     startWorkoutButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayIndexTextView      = itemView.findViewById(R.id.dayIndexTextView);
            dayTitleTextView      = itemView.findViewById(R.id.dayTitleTextView);
            exercisesRecyclerView = itemView.findViewById(R.id.exercisesRecyclerView);
            startWorkoutButton    = itemView.findViewById(R.id.startWorkoutButton);
        }

        public void bind(final TrainingDay td, final OnItemClickListener listener){

            /* -------- Etiquetas -------- */
            int idx = (td.getDayIndex()>0) ? td.getDayIndex()
                    : getBindingAdapterPosition()+1;
            dayIndexTextView.setText("Día " + idx);
            dayTitleTextView.setText(
                    (td.getDayTitle()!=null && !td.getDayTitle().isEmpty())
                            ? td.getDayTitle() : "Día " + idx);

            /* -------- Lista de ejercicios (máx 3, sin error si 0) -------- */
            List<UserExercise> src = (td.getExercises()!=null) ? td.getExercises()
                    : new ArrayList<>();
            List<UserExercise> limited =
                    (src.isEmpty()) ? new ArrayList<>()
                            : src.subList(0, Math.min(3, src.size()));

            if (exercisesRecyclerView.getLayoutManager()==null) {
                exercisesRecyclerView.setLayoutManager(
                        new LinearLayoutManager(itemView.getContext()));
            }

            ExerciseListAdapter exAdapter =
                    (ExerciseListAdapter) exercisesRecyclerView.getAdapter();

            if (exAdapter==null) {
                exAdapter = new ExerciseListAdapter(limited);
                exercisesRecyclerView.setAdapter(exAdapter);
            } else {
                exAdapter.updateExercises(limited);
            }

            /* -------- Botón SIEMPRE habilitado -------- */
            startWorkoutButton.setEnabled(true);
            startWorkoutButton.setOnClickListener(v -> {
                if (listener!=null) listener.onItemClick(td);
            });
        }
    }
}
