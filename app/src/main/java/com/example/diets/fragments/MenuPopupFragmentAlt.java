package com.example.diets.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
public class MenuPopupFragmentAlt extends DialogFragment {

    private SelectExercisesFragment.OnExercisesSelectedListener listener;
    private TrainingDay trainingDay;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof SelectExercisesFragment.OnExercisesSelectedListener) {
            listener = (SelectExercisesFragment.OnExercisesSelectedListener) parentFragment;
            Log.d("MenuPopupFragmentAlt", "✅ Listener obtenido desde el fragmento padre.");
        } else if (context instanceof SelectExercisesFragment.OnExercisesSelectedListener) {
            listener = (SelectExercisesFragment.OnExercisesSelectedListener) context;
            Log.d("MenuPopupFragmentAlt", "✅ Listener obtenido desde la actividad.");
        } else {
            throw new RuntimeException("El fragmento o la actividad debe implementar OnExercisesSelectedListener");
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

        Bundle args = getArguments();
        if (args != null && args.containsKey("trainingDay")) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            Log.d("MenuPopupFragmentAlt", "📌 TrainingDay recibido: " + trainingDay);
        }

        navigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_exercises) {
                selectedFragment = new ExerciseSelectionDialogFragmentAlt();
            } else if (item.getItemId() == R.id.navigation_progress) {
                selectedFragment = new CreatedExercisesFragmentAlt();
            }

            if (selectedFragment != null) {
                Bundle fragmentArgs = new Bundle();
                fragmentArgs.putSerializable("trainingDay", trainingDay);
                selectedFragment.setArguments(fragmentArgs);

                if (selectedFragment instanceof ExerciseSelectionDialogFragmentAlt) {
                    ((ExerciseSelectionDialogFragmentAlt) selectedFragment).setOnExercisesSelectedListener(
                            (selectedExercises, day) -> sendExercisesToParentFragment(selectedExercises, day)
                    );
                } else if (selectedFragment instanceof CreatedExercisesFragmentAlt) {
                    ((CreatedExercisesFragmentAlt) selectedFragment).setOnExercisesSelectedListener(
                            (selectedExercises, day) -> sendExercisesToParentFragment(selectedExercises, day)
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
        if (listener != null) {
            for (Exercise exercise : selectedExercises) {
                listener.onExercisesSelected(selectedExercises, trainingDay);

            }
        }
        dismiss();
    }
}
