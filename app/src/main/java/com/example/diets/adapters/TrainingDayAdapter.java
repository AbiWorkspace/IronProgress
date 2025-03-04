package com.example.diets.adapters;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.example.diets.model.Exercise;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class TrainingDayAdapter extends RecyclerView.Adapter<TrainingDayAdapter.ViewHolder> {
    private List<TrainingDay> trainingDays;
    private OnItemClickListener listener;
    private DatabaseReference databaseReference;

    public interface OnItemClickListener {
        void onItemClick(TrainingDay trainingDay);
    }

    public TrainingDayAdapter(List<TrainingDay> trainingDays, OnItemClickListener listener) {
        this.trainingDays = trainingDays;
        this.listener = listener;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("exercises");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_day2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrainingDay trainingDay = trainingDays.get(position);
        holder.bind(trainingDay, listener, databaseReference);
    }

    @Override
    public int getItemCount() {
        return trainingDays.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView dayIndexTextView;
        private LinearLayout exercisesLayout;
        private Button startWorkoutButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayIndexTextView = itemView.findViewById(R.id.dayIndexTextView);
            exercisesLayout = itemView.findViewById(R.id.exercisesLayout);
            startWorkoutButton = itemView.findViewById(R.id.startWorkoutButton);
        }

        public void bind(final TrainingDay trainingDay, final OnItemClickListener listener, DatabaseReference exercisesRef) {
            dayIndexTextView.setText("Día " + trainingDay.getDayIndex());
            exercisesLayout.removeAllViews();

            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                pattern.getExercises(exercisesRef, new MovementPattern.OnExercisesLoadedListener() {
                    @Override
                    public void onExercisesLoaded(List<Exercise> exercises) {
                        for (Exercise exercise : exercises) {
                            UserExercise userExercise = new UserExercise(exercise);
                            View exerciseView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_exercise_detailss, exercisesLayout, false);
                            TextView exerciseNameTextView = exerciseView.findViewById(R.id.exerciseNameTextView);
                            EditText weightEditText = exerciseView.findViewById(R.id.weightEditText);
                            EditText repsEditText = exerciseView.findViewById(R.id.repsEditText);
                            EditText rpeEditText = exerciseView.findViewById(R.id.rpeEditText);
                            Button saveButton = exerciseView.findViewById(R.id.saveButton);

                            if (exerciseNameTextView != null) {
                                exerciseNameTextView.setText(userExercise.getName());
                            }
                            if (weightEditText != null) {
                                weightEditText.setText(String.valueOf(userExercise.getWeight()));
                            }
                            if (repsEditText != null) {
                                repsEditText.setText(String.valueOf(userExercise.getReps()));
                            }
                            if (rpeEditText != null) {
                                rpeEditText.setText(String.valueOf(userExercise.getRpe()));
                            }

                            saveButton.setOnClickListener(v -> {
                                if (weightEditText != null && repsEditText != null && rpeEditText != null) {
                                    userExercise.setWeight(Double.parseDouble(weightEditText.getText().toString()));
                                    userExercise.setReps(Integer.parseInt(repsEditText.getText().toString()));
                                    userExercise.setRpe(Double.parseDouble(rpeEditText.getText().toString()));
                                    // Aquí puedes añadir la lógica para guardar el ejercicio en la base de datos
                                }
                            });

                            exercisesLayout.addView(exerciseView);
                        }
                    }
                });
            }

            itemView.setOnClickListener(v -> listener.onItemClick(trainingDay));
            startWorkoutButton.setOnClickListener(v -> listener.onItemClick(trainingDay));
        }
    }
}