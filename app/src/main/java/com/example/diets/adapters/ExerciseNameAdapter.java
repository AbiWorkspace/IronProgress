package com.example.diets.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.fragments.TrainingDetailFragment;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseNameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_DAY = 0;
    private static final int VIEW_TYPE_EXERCISE = 1;

    private List<Object> items;

    public ExerciseNameAdapter(List<Object> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof TrainingDay) {
            return VIEW_TYPE_DAY;
        } else {
            return VIEW_TYPE_EXERCISE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DAY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_day2, parent, false);
            return new DayViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_name, parent, false);
            return new ExerciseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_DAY) {
            TrainingDay trainingDay = (TrainingDay) items.get(position);
            ((DayViewHolder) holder).bind(trainingDay);
        } else {
            UserExercise exercise = (UserExercise) items.get(position);
            ((ExerciseViewHolder) holder).bind(exercise);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        private TextView dayIndexTextView;
        private TextView dayTitleTextView;
        private RecyclerView exercisesRecyclerView;
        private Button startWorkoutButton;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayIndexTextView = itemView.findViewById(R.id.dayIndexTextView);
            dayTitleTextView = itemView.findViewById(R.id.dayTitleTextView);
            exercisesRecyclerView = itemView.findViewById(R.id.exercisesRecyclerView);
            startWorkoutButton = itemView.findViewById(R.id.startWorkoutButton);
        }

        public void bind(TrainingDay trainingDay) {
            dayIndexTextView.setText("Día " + trainingDay.getDayIndex());
            dayTitleTextView.setText(trainingDay.getDayTitle());

            // Configurar el RecyclerView interno para los ejercicios
            ExerciseNameAdapter exerciseAdapter = new ExerciseNameAdapter(new ArrayList<>(trainingDay.getExercises()));
            exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            exercisesRecyclerView.setAdapter(exerciseAdapter);

            startWorkoutButton.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putSerializable("exercises", new ArrayList<>(trainingDay.getExercises()));
                ViewPagerActivity activity = (ViewPagerActivity) itemView.getContext();
                activity.switchToTrainingDetailFragment(bundle);
            });
        }
    }

    public static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private TextView exerciseNameTextView;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameTextView = itemView.findViewById(R.id.exerciseNameTextView);
        }

        public void bind(UserExercise exercise) {
            exerciseNameTextView.setText(exercise.getName());
        }
    }
}