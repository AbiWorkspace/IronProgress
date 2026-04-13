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
import com.example.diets.adapters.TrainingDayAdapter;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
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

public class TrainingDayDetailFragment extends Fragment {
    private static final String ARG_TRAINING_DAY = "training_day";
    private TrainingDay trainingDay;
    private RecyclerView recyclerView;
    private TrainingDayAdapter adapter;
    private List<TrainingDay> trainingDays;

    public static TrainingDayDetailFragment newInstance(TrainingDay trainingDay) {
        TrainingDayDetailFragment fragment = new TrainingDayDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRAINING_DAY, trainingDay);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            trainingDay = (TrainingDay) getArguments().getSerializable(ARG_TRAINING_DAY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training_day_detail, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTrainingDayDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Cargar la información del día de entrenamiento
        loadTrainingDayDetails();

        return view;
    }

    private void loadTrainingDayDetails() {
        Log.d("TrainingDayDetailFragment", "loadTrainingDayDetails: Cargando detalles del día de entrenamiento");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d("TrainingDayDetailFragment", "loadTrainingDayDetails: Usuario autenticado con userId " + userId);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("userMovementPatterns").child(String.valueOf(trainingDay.getDayIndex()));

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("TrainingDayDetailFragment", "loadTrainingDayDetails: onDataChange: Datos recibidos de Firebase");
                    trainingDays = new ArrayList<>();
                    UserMovementPattern userMovementPattern = snapshot.getValue(UserMovementPattern.class);
                    if (userMovementPattern != null) {
                        trainingDay.setMovementPatterns(Collections.singletonList(userMovementPattern));
                        trainingDays.add(trainingDay);
                        Log.d("TrainingDayDetailFragment", "loadTrainingDayDetails: onDataChange: Día de entrenamiento añadido: " + trainingDay.getDayIndex());
                    } else {
                        Log.e("TrainingDayDetailFragment", "loadTrainingDayDetails: onDataChange: userMovementPattern es nulo");
                    }

                    // Configurar el adaptador del RecyclerView
                    adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                        // Manejar el clic en un día de entrenamiento
                        Log.d("TrainingDayDetailFragment", "loadTrainingDayDetails: onDataChange: Día de entrenamiento clicado: " + trainingDay);
                        // Aquí puedes manejar el clic en el día de entrenamiento si es necesario
                    });
                    recyclerView.setAdapter(adapter);
                    Log.d("TrainingDayDetailFragment", "loadTrainingDayDetails: onDataChange: Adapter configurado con los días de entrenamiento");
                    adapter.notifyDataSetChanged(); // Notificar al adaptador que los datos han cambiado
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TrainingDayDetailFragment", "loadTrainingDayDetails: onCancelled: Error al obtener los días de entrenamiento", error.toException());
                }
            });
        } else {
            Log.e("TrainingDayDetailFragment", "loadTrainingDayDetails: Usuario no autenticado");
        }
    }

    // Define el método saveUserProgress
    public void saveUserProgress(UserExercise exercise) {
        // Implementa la lógica para guardar el progreso del usuario
        Log.d("TrainingDayDetailFragment", "saveUserProgress: Guardando progreso del usuario para el ejercicio: " + exercise);
        // Aquí puedes añadir la lógica para guardar el progreso en Firebase u otra base de datos
    }
}
