package com.example.diets.fragments;


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
import com.example.diets.adapters.ExerciseNameAdapter;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserMovementPattern;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DashboardWorkoutFragment extends Fragment {
    private RecyclerView recyclerView;
    private ExerciseNameAdapter adapter;
    private List<Object> items;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboardfragment, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTrainingDays);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Obtener los días de entrenamiento y ejercicios del usuario
        getTrainingDaysAndExercisesFromUser();

        return view;
    }

    private void getTrainingDaysAndExercisesFromUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("userMovementPatterns");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    items = new ArrayList<>();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        UserMovementPattern userMovementPattern = dataSnapshot.getValue(UserMovementPattern.class);
                        if (userMovementPattern != null) {
                            TrainingDay trainingDay = new TrainingDay();
                            trainingDay.setDayIndex(items.size() + 1);
                            trainingDay.setDayTitle(userMovementPattern.getName());
                            trainingDay.setExercises(new ArrayList<>(userMovementPattern.getUserExercises()));
                            items.add(trainingDay);
                        }
                    }
                    adapter = new ExerciseNameAdapter(items);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("DashboardWorkoutFragment", "Error al obtener los días de entrenamiento y ejercicios", error.toException());
                }
            });
        }
    }
}