package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssignLoadFragment extends Fragment {
    private static final String TAG = "AssignLoadFragment";
    private List<TrainingDay> trainingDays;
    private String userGoal;
    private LinearLayout containerLayout;
    private Map<String, UserExercise> userExercises = new HashMap<>();
    private DatabaseReference mDatabase;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assign_load, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerLayout = view.findViewById(R.id.containerLayout);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Obtener el userId del usuario autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Log.e(TAG, "Usuario no autenticado");
            return;
        }

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            userGoal = data.getString("userGoal");
            loadUserExercises();
        }
    }

    private void loadUserExercises() {
        if (userId == null) {
            Log.e(TAG, "userId es nulo en loadUserExercises");
            return;
        }
        DatabaseReference userExercisesRef = mDatabase.child("users").child(userId).child("userExercises");
        userExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    UserExercise userExercise = exerciseSnapshot.getValue(UserExercise.class);
                    if (userExercise != null) {
                        userExercises.put(userExercise.getName(), userExercise);
                    }
                }
                listExercises();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar los ejercicios del usuario: " + error.getMessage());
            }
        });
    }

    private void listExercises() {
        if (containerLayout == null) {
            Log.e(TAG, "containerLayout is null");
            return;
        }

        Set<Exercise> uniqueExercises = new HashSet<>();
        for (TrainingDay trainingDay : trainingDays) {
            uniqueExercises.addAll(trainingDay.getExercises());
        }

        for (Exercise exercise : uniqueExercises) {
            View exerciseView = LayoutInflater.from(getContext()).inflate(R.layout.exercise_item_load, containerLayout, false);

            TextView exerciseNameView = exerciseView.findViewById(R.id.exerciseName);
            exerciseNameView.setText(exercise.getName());

            ImageButton statusButton = exerciseView.findViewById(R.id.statusButton);
            UserExercise userExercise = userExercises.getOrDefault(exercise.getName(), new UserExercise(exercise));
            userExercises.put(exercise.getName(), userExercise);

            // Configurar el icono inicial del botón según el estado del ejercicio
            updateStatusButtonIcon(statusButton, userExercise);

            statusButton.setOnClickListener(v -> {
                // Cambiar el estado del ejercicio y actualizar el icono
                toggleExerciseStatus(userExercise);
                updateStatusButtonIcon(statusButton, userExercise);
            });

            exerciseView.setOnClickListener(v -> {
                // Abrir ExerciseDetailsLoadFragment al hacer clic en el ejercicio
                openExerciseDetailsLoadFragment(userExercise);
            });

            containerLayout.addView(exerciseView);
        }
    }

    private void updateStatusButtonIcon(ImageButton button, UserExercise userExercise) {
        // Cambiar el icono del botón según el estado del ejercicio
        if (userExercise.isCompleted()) {
            button.setImageResource(R.drawable.ic_completed_status);
        } else {
            button.setImageResource(R.drawable.ic_default_status);
        }
    }

    private void toggleExerciseStatus(UserExercise userExercise) {
        // Cambiar el estado del ejercicio
        userExercise.setCompleted(!userExercise.isCompleted());
    }

    private void openExerciseDetailsLoadFragment(UserExercise userExercise) {
        ExerciseDetailsLoadFragment fragment = new ExerciseDetailsLoadFragment();
        Bundle data = new Bundle();
        data.putSerializable("userExercise", userExercise);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void saveAssignedLoads() {
        DatabaseReference userExercisesRef = mDatabase.child("users").child(userId).child("userExercises");
        userExercisesRef.setValue(userExercises)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cargas asignadas guardadas exitosamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar las cargas asignadas", e);
                    Toast.makeText(getContext(), "Error al guardar las cargas asignadas", Toast.LENGTH_SHORT).show();
                });
    }
}