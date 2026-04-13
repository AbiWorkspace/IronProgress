package com.example.diets.activitys;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.diets.model.UserExercise;


public class SharedViewModel extends ViewModel {
    private final MutableLiveData<UserExercise> selectedExercise = new MutableLiveData<>();

    public void selectExercise(UserExercise exercise) {
        selectedExercise.setValue(exercise);
    }

    public LiveData<UserExercise> getSelectedExercise() {
        return selectedExercise;
    }
}