package com.example.diets.fragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.google.android.material.bottomnavigation.BottomNavigationView;
public class MenuPopupFragment extends DialogFragment {
    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise exercise);
    }

    private OnExerciseSelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnExerciseSelectedListener) {
            listener = (OnExerciseSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnExerciseSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu_popup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomNavigationView navigationView = view.findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            Bundle args = getArguments(); // Obtener los argumentos del fragmento

            if (itemId == R.id.navigation_exercises) {
                selectedFragment = new ExerciseSelectionDialogFragment();
            } else if (itemId == R.id.navigation_progress) {
                selectedFragment = new CreatedExercisesFragment();
            }

            if (selectedFragment != null) {
                if (args != null) {
                    selectedFragment.setArguments(args); // Pasar los argumentos al fragmento seleccionado
                }
                getChildFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            navigationView.setSelectedItemId(R.id.navigation_exercises);
        }
    }

    public void exerciseSelected(Exercise exercise) {
        Log.d(TAG, "exerciseSelected: Ejercicio seleccionado: " + exercise.getName());

        if (listener != null) {
            Log.d(TAG, "exerciseSelected: Llamando al listener con el ejercicio seleccionado");
            listener.onExerciseSelected(exercise);
        } else {
            Log.d(TAG, "exerciseSelected: Listener es nulo");
        }

        // Pasar el ejercicio seleccionado a SelectExercisesFragment
        FragmentManager fragmentManager = getParentFragmentManager();
        Log.d(TAG, "exerciseSelected: Obteniendo FragmentManager: " + fragmentManager);

        SelectExercisesFragment selectExercisesFragment = (SelectExercisesFragment) fragmentManager.findFragmentByTag("f0");
        if (selectExercisesFragment != null) {
            Log.d(TAG, "exerciseSelected: SelectExercisesFragment encontrado");
            selectExercisesFragment.addSelectedExercise(exercise);
            Log.d(TAG, "exerciseSelected: Ejercicio añadido: " + exercise.getName());
        } else {
            Log.d(TAG, "exerciseSelected: SelectExercisesFragment es nulo");
        }
    }
}