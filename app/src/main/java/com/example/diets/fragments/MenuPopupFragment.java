package com.example.diets.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MenuPopupFragment extends DialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu_popup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomNavigationView navigationView = view.findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
            }
        });

        // Set default fragment
        if (savedInstanceState == null) {
            navigationView.setSelectedItemId(R.id.navigation_exercises);
        }
    }
}