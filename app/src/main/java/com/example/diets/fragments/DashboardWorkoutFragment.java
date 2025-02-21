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
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.TrainingDayAdapter;
import com.example.diets.model.ExerciseDistribution;
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
import java.util.Collections;
import java.util.List;
public class DashboardWorkoutFragment extends Fragment {
    private RecyclerView recyclerView;
    private TrainingDayAdapter adapter;
    private List<TrainingDay> trainingDays;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DashboardWorkoutFragment", "onCreateView: Inflando el layout del fragmento");
        View view = inflater.inflate(R.layout.fragment_dashboardfragment, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTrainingDays);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Obtener los días de entrenamiento del usuario
        getTrainingDaysFromUser();

        return view;
    }

    private void getTrainingDaysFromUser() {
        Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: Iniciando la obtención de días de entrenamiento");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: Usuario autenticado con userId " + userId);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("userMovementPatterns");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Datos recibidos de Firebase");
                    trainingDays = new ArrayList<>();
                    Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Número de hijos en snapshot: " + snapshot.getChildrenCount());
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Procesando snapshot: " + dataSnapshot);
                        UserMovementPattern userMovementPattern = dataSnapshot.getValue(UserMovementPattern.class);
                        if (userMovementPattern != null) {
                            TrainingDay trainingDay = new TrainingDay();
                            trainingDay.setDayIndex(userMovementPattern.getId());
                            trainingDay.setMovementPatterns(Collections.singletonList(userMovementPattern));
                            trainingDays.add(trainingDay);
                            Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Día de entrenamiento añadido: " + trainingDay.getDayIndex());
                        } else {
                            Log.e("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: userMovementPattern es nulo");
                        }
                    }
                    Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Total días de entrenamiento añadidos: " + trainingDays.size());
                    if (trainingDays.isEmpty()) {
                        Log.w("DashboardWorkoutFragment", "getTrainingDaysFromUser: No se encontraron días de entrenamiento");
                    }
                    // Configurar el adaptador del RecyclerView
                    adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                        // Manejar el clic en un día de entrenamiento o en el botón "Empezar"
                        Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Día de entrenamiento clicado: " + trainingDay);
                        ((ViewPagerActivity) getActivity()).switchToTrainingDayDetailFragment(trainingDay);
                    });
                    recyclerView.setAdapter(adapter);
                    Log.d("DashboardWorkoutFragment", "getTrainingDaysFromUser: onDataChange: Adapter configurado con los días de entrenamiento");
                    adapter.notifyDataSetChanged(); // Notificar al adaptador que los datos han cambiado
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("DashboardWorkoutFragment", "getTrainingDaysFromUser: onCancelled: Error al obtener los días de entrenamiento", error.toException());
                }
            });
        } else {
            Log.e("DashboardWorkoutFragment", "getTrainingDaysFromUser: Usuario no autenticado");
        }
    }
}

