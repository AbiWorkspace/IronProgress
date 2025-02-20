package com.example.diets.fragments;

import static java.security.AccessController.getContext;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.adapters.ExerciseAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
public class TrainingDayDetailFragment extends Fragment {
    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private TrainingDay trainingDay;

    public static TrainingDayDetailFragment newInstance(TrainingDay trainingDay) {
        Log.d("TrainingDayDetailFragment", "newInstance: Creando nueva instancia con trainingDay: " + trainingDay);
        TrainingDayDetailFragment fragment = new TrainingDayDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("trainingDay", trainingDay);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("TrainingDayDetailFragment", "onCreateView: Inflando el layout del fragmento");
        View view = inflater.inflate(R.layout.fragment_training_day_detail, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() != null) {
            trainingDay = (TrainingDay) getArguments().getSerializable("trainingDay");
            Log.d("TrainingDayDetailFragment", "onCreateView: Datos del trainingDay recibidos: " + trainingDay);
            List<UserExercise> userExercises = convertToUserExercises(trainingDay.getExercises());
            adapter = new ExerciseAdapter(userExercises, this);
            recyclerView.setAdapter(adapter);
            Log.d("TrainingDayDetailFragment", "onCreateView: Adapter configurado con los ejercicios del día de entrenamiento");
        } else {
            Log.e("TrainingDayDetailFragment", "onCreateView: No se recibieron argumentos");
        }

        return view;
    }

    private List<UserExercise> convertToUserExercises(List<Exercise> exercises) {
        Log.d("TrainingDayDetailFragment", "convertToUserExercises: Convirtiendo ejercicios a UserExercises");
        List<UserExercise> userExercises = new ArrayList<>();
        for (Exercise exercise : exercises) {
            UserExercise userExercise = new UserExercise();
            userExercise.setId(exercise.getId());
            userExercise.setName(exercise.getName());
            userExercise.setEquipmentType(exercise.getEquipmentType());
            userExercise.setExerciseType(exercise.getExerciseType());
            userExercise.setMusclesWorked(exercise.getMusclesWorked());
            userExercise.setLoadIntensity(exercise.getLoadIntensity());
            userExercise.setImage(exercise.getImage());
            userExercise.setDescription(exercise.getDescription());
            userExercise.setMovementType(exercise.getMovementType());
            userExercise.setSynergistMuscles(exercise.getSynergistMuscles());
            userExercise.setGoals(exercise.getGoals());
            userExercise.setMovementPatterns(exercise.getMovementPatterns());
            userExercises.add(userExercise);
            Log.d("TrainingDayDetailFragment", "convertToUserExercises: Ejercicio convertido: " + userExercise);
        }
        return userExercises;
    }

    public void saveUserProgress(UserExercise exercise) {
        Log.d("TrainingDayDetailFragment", "saveUserProgress: Guardando progreso del ejercicio: " + exercise);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("userExercises").child(String.valueOf(exercise.getId()));
            userRef.setValue(exercise)
                    .addOnSuccessListener(aVoid -> Log.d("TrainingDayDetailFragment", "saveUserProgress: Progreso guardado exitosamente"))
                    .addOnFailureListener(e -> Log.e("TrainingDayDetailFragment", "saveUserProgress: Error al guardar el progreso", e));
        } else {
            Log.e("TrainingDayDetailFragment", "saveUserProgress: Usuario no autenticado");
        }
    }
}