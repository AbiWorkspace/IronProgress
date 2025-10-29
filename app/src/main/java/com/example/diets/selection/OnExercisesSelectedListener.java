package com.example.diets.selection;

import com.example.diets.model.Exercise;
import com.example.diets.model.TrainingDay;
import java.util.List;

public interface OnExercisesSelectedListener {
    void onExercisesSelected(List<Exercise> exercises, TrainingDay trainingDay);
}
