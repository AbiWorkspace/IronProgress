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

    private static final String TAG = "MenuPopupFragment";

    private TrainingDay trainingDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu_popup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomNavigationView navigationView = view.findViewById(R.id.navigation);

        //  Obtener TrainingDay
        Bundle args = getArguments();
        if (args != null && args.containsKey("trainingDay")) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            Log.d("MenuPopupFragment", " TrainingDay recibido en MenuPopupFragment: " + trainingDay);
        } else {
            Log.e("MenuPopupFragment", " ERROR: No se recibió TrainingDay.");
        }

        navigationView.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_exercises) {
                selectedFragment = new ExerciseSelectionDialogFragment();
            } else if (item.getItemId() == R.id.navigation_progress) {
                selectedFragment = new CreatedExercisesFragment();
            }

            if (selectedFragment != null) {
                Bundle fragmentArgs = new Bundle();
                if (trainingDay != null) {
                    fragmentArgs.putSerializable("trainingDay", trainingDay);
                    Log.d("MenuPopupFragment", " Pasando TrainingDay a " + selectedFragment.getClass().getSimpleName());
                } else {
                    Log.e("MenuPopupFragment", " TrainingDay es NULL al pasarlo al fragmento hijo.");
                }

                selectedFragment.setArguments(fragmentArgs);

                //  NO SE TOCA EL CÓDIGO EXISTENTE, solo aseguramos el `trainingDay`
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
                    Log.e("MenuPopupFragment", " trainingDay es nulo, no se pudo agregar el ejercicio.");
                }
                break;
            }
        }
        dismiss();
    }
}
