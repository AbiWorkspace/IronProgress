package com.example.diets.fragments;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.model.ExerciseDistribution;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.Muscle;
import com.example.diets.model.TrainingDay;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.Serializable;

public class ExerciseDistributionFragment extends Fragment {
    private static final String TAG = "ExerciseDistributionFragment";
    private String selectedDistribution = null;
    private DatabaseReference mDatabase;
    private int selectedDays;
    private String userId;

    private List<RadioButton> radioButtons = new ArrayList<>();
    private List<TrainingDay> trainingDays;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_distribution, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Obtener el userId del usuario autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        LinearLayout recommendedContainer = view.findViewById(R.id.recommendedContainer);
        LinearLayout notRecommendedContainer = view.findViewById(R.id.notRecommendedContainer);
        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (selectedDistribution != null) {
                Log.d(TAG, "Distribución seleccionada: " + selectedDistribution);
                // Guardar la distribución seleccionada en un Bundle
                Bundle data = new Bundle();
                data.putString("selectedDistribution", selectedDistribution);
                data.putSerializable("trainingDays", (Serializable) trainingDays);
                data.putString("userId", userId); // Asegúrate de pasar el userId

                // Cambiar al siguiente fragmento
                ((ViewPagerActivity) getActivity()).switchToSelectExercisesFragment(data);
            } else {
                Log.d(TAG, "No se ha seleccionado ninguna distribución");
            }
        });

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            selectedDays = data.getInt("days", 3); // Valor predeterminado de 3 días si no se encuentra en el Bundle
        }

        loadExerciseDistributions(recommendedContainer, notRecommendedContainer, selectedDays);
    }

    private void loadExerciseDistributions(LinearLayout recommendedContainer, LinearLayout notRecommendedContainer, int selectedDays) {
        DatabaseReference exerciseDistRef = mDatabase.child("exerciseDistributions");
        exerciseDistRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<ExerciseDistribution> distributions = new ArrayList<>();
                    for (DataSnapshot distributionSnapshot : snapshot.getChildren()) {
                        ExerciseDistribution exerciseDistribution = distributionSnapshot.getValue(ExerciseDistribution.class);
                        if (exerciseDistribution != null) {
                            distributions.add(exerciseDistribution);
                        } else {
                            Log.d(TAG, "Distribución de ejercicios es nula");
                        }
                    }
                    List<ExerciseDistribution> filteredDistributions = ExerciseDistribution.filterByNumberOfDays(distributions, selectedDays);
                    if (filteredDistributions.isEmpty()) {
                        displayNoDistributionsMessage(recommendedContainer);
                        displayNoDistributionsMessage(notRecommendedContainer);
                    } else {
                        boolean hasRecommended = false;
                        boolean hasNotRecommended = false;
                        for (ExerciseDistribution distribution : filteredDistributions) {
                            if (distribution.isRecomendado()) {
                                displayDistributions(distribution, recommendedContainer);
                                hasRecommended = true;
                            } else {
                                displayDistributions(distribution, notRecommendedContainer);
                                hasNotRecommended = true;
                            }
                        }
                        if (!hasRecommended) {
                            displayNoDistributionsMessage(recommendedContainer);
                        }
                        if (!hasNotRecommended) {
                            displayNoDistributionsMessage(notRecommendedContainer);
                        }
                    }
                } else {
                    Log.d(TAG, "No se encontraron distribuciones de ejercicios");
                    displayNoDistributionsMessage(recommendedContainer);
                    displayNoDistributionsMessage(notRecommendedContainer);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "Error de base de datos: " + error.getMessage());
            }
        });
    }

    private void displayNoDistributionsMessage(LinearLayout container) {
        TextView noDistributionsText = new TextView(getContext());
        noDistributionsText.setText("No hay distribuciones");
        noDistributionsText.setTextColor(getResources().getColor(R.color.white));
        noDistributionsText.setPadding(8, 8, 8, 8);
        noDistributionsText.setTextSize(18);
        noDistributionsText.setTypeface(null, Typeface.BOLD);
        container.addView(noDistributionsText);
    }

    private void displayDistributions(ExerciseDistribution exerciseDistribution, LinearLayout container) {
        LinearLayout distributionLayout = new LinearLayout(getContext());
        distributionLayout.setOrientation(LinearLayout.VERTICAL);
        distributionLayout.setPadding(16, 16, 16, 16);
        distributionLayout.setBackground(getResources().getDrawable(R.drawable.distribution_background));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        distributionLayout.setLayoutParams(params);

        TextView titleText = new TextView(getContext());
        titleText.setText(exerciseDistribution.getTitle());
        titleText.setTextColor(getResources().getColor(R.color.white));
        titleText.setPadding(8, 8, 8, 8);
        titleText.setTextSize(20); // Tamaño del texto del título
        titleText.setTypeface(null, Typeface.BOLD);

        TextView daysText = new TextView(getContext());
        daysText.setText("Número de días: " + exerciseDistribution.getNumberOfDays());
        daysText.setTextColor(getResources().getColor(R.color.white));
        daysText.setPadding(8, 8, 8, 8);
        daysText.setTextSize(18); // Tamaño del texto de la cantidad de días
        daysText.setTypeface(null, Typeface.BOLD);

        TextView distributionText = new TextView(getContext());
        StringBuilder distributionDetails = new StringBuilder();
        List<TrainingDay> trainingDaysList = exerciseDistribution.getTrainingDays();
        for (int i = 0; i < trainingDaysList.size(); i++) {
            TrainingDay day = trainingDaysList.get(i);
            distributionDetails.append("Día ").append(i + 1).append(":\n");
            if (day.getMovementPatterns() != null) {
                for (MovementPattern pattern : day.getMovementPatterns()) {
                    distributionDetails.append("  - Patrón: ").append(pattern.getName()).append("\n");
                    distributionDetails.append("    Músculos: ");
                    if (pattern.getSynergistMuscles() != null) {
                        for (Muscle muscle : pattern.getSynergistMuscles()) {
                            distributionDetails.append(muscle.getName()).append(", ");
                        }
                        // Eliminar la última coma y espacio
                        if (!pattern.getSynergistMuscles().isEmpty()) {
                            distributionDetails.setLength(distributionDetails.length() - 2);
                        }
                    }
                    distributionDetails.append("\n");
                }
            }
        }
        distributionText.setText(distributionDetails.toString());
        distributionText.setTextColor(getResources().getColor(R.color.white));
        distributionText.setPadding(8, 8, 8, 8);
        distributionText.setTextSize(16);
        distributionText.setTypeface(null, Typeface.NORMAL);

        distributionLayout.addView(titleText);
        distributionLayout.addView(daysText);
        distributionLayout.addView(distributionText);

        RadioButton radioButton = new RadioButton(getContext());
        radioButton.setPadding(8, 8, 8, 8);
        radioButton.setButtonTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
        radioButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        distributionLayout.addView(radioButton);
        radioButtons.add(radioButton); // Añadir el RadioButton a la lista

        radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedDistribution = exerciseDistribution.getTitle();
                List<TrainingDay> tempTrainingDays = exerciseDistribution.getTrainingDays();
                trainingDays = new ArrayList<>(tempTrainingDays); // Asignar a la variable de instancia
                for (RadioButton rb : radioButtons) {
                    if (rb != buttonView) {
                        rb.setChecked(false);
                    }
                }
            }
        });

        container.addView(distributionLayout);
    }
}
