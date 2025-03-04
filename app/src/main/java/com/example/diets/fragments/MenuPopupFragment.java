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
import com.example.diets.model.TrainingDay;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MenuPopupFragment extends DialogFragment {
    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise exercise);
    }

    private OnExerciseSelectedListener listener;
    private TrainingDay trainingDay;

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
        navigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            Bundle args = getArguments();

            if (args != null) {
                trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            }

            if (item.getItemId() == R.id.navigation_exercises) {
                selectedFragment = new ExerciseSelectionDialogFragment();
            } else if (item.getItemId() == R.id.navigation_progress) {
                selectedFragment = new CreatedExercisesFragment();
            }

            if (selectedFragment != null) {
                if (args != null) {
                    selectedFragment.setArguments(args);
                }

                // ✅ Configurar el listener para devolver ejercicios seleccionados
                if (selectedFragment instanceof ExerciseSelectionDialogFragment) {
                    ((ExerciseSelectionDialogFragment) selectedFragment).setOnExercisesSelectedListener(
                            (selectedExercises, day) -> sendExercisesToParentFragment(selectedExercises, day)
                    );
                } else if (selectedFragment instanceof CreatedExercisesFragment) {
                    ((CreatedExercisesFragment) selectedFragment).setOnExerciseSelectedListener(
                            this::sendExerciseToParentFragment
                    );
                }

                getChildFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            navigationView.setSelectedItemId(R.id.navigation_exercises);
        }
    }

    public void sendExercisesToParentFragment(List<Exercise> selectedExercises, TrainingDay trainingDay) {
        FragmentManager fragmentManager = getParentFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();

        for (Fragment fragment : fragments) {
            if (fragment instanceof SelectExercisesFragment) {
                SelectExercisesFragment selectExercisesFragment = (SelectExercisesFragment) fragment;
                for (Exercise exercise : selectedExercises) {
                    selectExercisesFragment.addSelectedExercise(exercise, trainingDay);
                }
                break;
            }
        }
        dismiss();
    }

    public void sendExerciseToParentFragment(Exercise exercise) {
        FragmentManager fragmentManager = getParentFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();

        for (Fragment fragment : fragments) {
            if (fragment instanceof SelectExercisesFragment) {
                SelectExercisesFragment selectExercisesFragment = (SelectExercisesFragment) fragment;
                if (trainingDay != null) {
                    selectExercisesFragment.addSelectedExercise(exercise, trainingDay);
                } else {
                    Log.e("MenuPopupFragment", "❌ trainingDay es nulo, no se pudo agregar el ejercicio.");
                }
                break;
            }
        }
        dismiss();
    }
}
