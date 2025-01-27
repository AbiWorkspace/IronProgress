package com.example.diets.adapters;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.fragments.ExerciseDetailsFragment;
import com.example.diets.fragments.SelectExercisesFragment;
import com.example.diets.model.Exercise;
import java.util.List;
public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
    private final List<Exercise> exercises;
    private final List<Exercise> selectedExercises;
    private final OnExerciseClickListener onExerciseClickListener;
    private final Fragment parentFragment;

    public ExercisesAdapter(List<Exercise> exercises, List<Exercise> selectedExercises, OnExerciseClickListener onExerciseClickListener, Fragment parentFragment) {
        this.exercises = exercises;
        this.selectedExercises = selectedExercises;
        this.onExerciseClickListener = onExerciseClickListener;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view, onExerciseClickListener, parentFragment);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.exerciseName.setText(exercise.getName());
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedExercises.contains(exercise));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedExercises.add(exercise);
            } else {
                selectedExercises.remove(exercise);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView exerciseName;
        CheckBox checkBox;
        OnExerciseClickListener onExerciseClickListener;
        Fragment parentFragment;

        ViewHolder(View itemView, OnExerciseClickListener onExerciseClickListener, Fragment parentFragment) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exerciseName);
            checkBox = itemView.findViewById(R.id.checkBox);
            this.onExerciseClickListener = onExerciseClickListener;
            this.parentFragment = parentFragment;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onExerciseClickListener.onExerciseClick(getAdapterPosition());

            // Reemplazar el fragmento cuando se haga clic en un ejercicio
            int position = getAdapterPosition();
            Exercise exercise = ((ExercisesAdapter) getBindingAdapter()).exercises.get(position);
            ExerciseDetailsFragment fragment = ExerciseDetailsFragment.newInstance(exercise);

            // Obtener el FragmentManager desde el contexto del fragmento padre
            if (parentFragment != null) {
                FragmentTransaction transaction = parentFragment.getParentFragmentManager().beginTransaction();
                SelectExercisesFragment selectExercisesFragment = new SelectExercisesFragment();
                transaction.replace(R.id.fragment_container, selectExercisesFragment);
                transaction.commit();
            } else {
                Log.e(TAG, "onClick: parentFragment es nulo");
            }
        }
    }

    public interface OnExerciseClickListener {
        void onExerciseClick(int position);
    }
}