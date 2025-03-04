package com.example.diets.adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.UserExercise;
import java.util.List;

public class TrainingDetailAdapter extends RecyclerView.Adapter<TrainingDetailAdapter.ViewHolder> {
    private List<UserExercise> exercises;

    public TrainingDetailAdapter(List<UserExercise> exercises) {
        this.exercises = exercises;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_detailss, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserExercise exercise = exercises.get(position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView exerciseNameTextView;
        private EditText repsEditText;
        private EditText weightEditText;
        private EditText rpeEditText;
        private Button saveButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameTextView = itemView.findViewById(R.id.exerciseNameTextView);
            repsEditText = itemView.findViewById(R.id.repsEditText);
            weightEditText = itemView.findViewById(R.id.weightEditText);
            rpeEditText = itemView.findViewById(R.id.rpeEditText);
            saveButton = itemView.findViewById(R.id.saveButton);
        }

        public void bind(UserExercise exercise) {
            exerciseNameTextView.setText(exercise.getName());
            repsEditText.setText(String.valueOf(exercise.getReps()));
            weightEditText.setText(String.valueOf(exercise.getWeight()));
            rpeEditText.setText(String.valueOf(exercise.getRpe()));

            saveButton.setOnClickListener(v -> {
                // Guardar los valores ingresados
                exercise.setReps(Integer.parseInt(repsEditText.getText().toString()));
                exercise.setWeight(Double.parseDouble(weightEditText.getText().toString()));
                exercise.setRpe(Double.parseDouble(rpeEditText.getText().toString()));
                // Aquí puedes agregar la lógica para guardar los datos en Firebase
            });
        }
    }
}