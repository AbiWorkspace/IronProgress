package com.example.diets.viewmodel;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.diets.model.UserExercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Conserva la lista de ejercicios incluso si la actividad se destruye
 * por falta de memoria o cambio de configuración.
 */
public class TrainingDetailViewModel extends ViewModel {

    private static final String KEY_EXERCISES = "exercises";
    private final SavedStateHandle state;
    private final MutableLiveData<List<UserExercise>> exercisesLive;

    public TrainingDetailViewModel(SavedStateHandle state) {
        this.state = state;
        // Restaura cuando el proceso fue matado
        List<UserExercise> cached = state.get(KEY_EXERCISES);
        exercisesLive = new MutableLiveData<>(cached != null ? cached : new ArrayList<>());
    }

    public LiveData<List<UserExercise>> getExercises() {
        return exercisesLive;
    }

    public void setExercises(List<UserExercise> list) {
        exercisesLive.setValue(list);
        state.set(KEY_EXERCISES, list);     // guarda en SavedStateHandle
    }

    /* Conveniencia para añadir sin repetir lógica en Fragmento */
    public void addExercise(UserExercise ex) {
        List<UserExercise> current = new ArrayList<>(exercisesLive.getValue());
        current.add(ex);
        setExercises(current);
    }
}
