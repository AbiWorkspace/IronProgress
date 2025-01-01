package com.example.diets.fragments;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.diets.R;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ExerciseDistributionFragment extends Fragment {
    private static final String TAG = "ExerciseDistributionFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_distribution, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recibir los datos del formulario
        Bundle args = getArguments();
        if (args != null) {
            String goal = args.getString("goal");
            int days = args.getInt("days", 0);
            int time = args.getInt("time", 0);

            Log.d(TAG, "Goal: " + goal);
            Log.d(TAG, "Days: " + days);
            Log.d(TAG, "Time: " + time);

            // Generar distribuciones de entrenamiento
            List<String> distributions = generateTrainingDistributions(goal, days, time);

            // Mostrar las distribuciones generadas
            TextView distributionTextView = view.findViewById(R.id.distributionTextView);
            StringBuilder allDistributions = new StringBuilder();
            for (String distribution : distributions) {
                allDistributions.append(distribution).append("\n\n");
            }

            if (distributionTextView != null) {
                distributionTextView.setText(allDistributions.toString());
                Log.d(TAG, "Distributions set to TextView");
            } else {
                Log.e(TAG, "distributionTextView is null");
            }

            Log.d(TAG, "Distributions: " + allDistributions.toString());
        }
    }

    private List<String> generateTrainingDistributions(String goal, int days, int time) {
        List<String> distributions = new ArrayList<>();

        if ("fuerza".equalsIgnoreCase(goal)) {
            distributions.add(generatePowerliftingDistribution(days, time, "Push-Pull"));
            distributions.add(generatePowerliftingDistribution(days, time, "Push-Pull-Legs"));
            distributions.add(generatePowerliftingDistribution(days, time, "Full Body"));
        } else if ("hipertrofia".equalsIgnoreCase(goal)) {
            distributions.add(generateHypertrophyDistribution(days, time, "Push-Pull"));
            distributions.add(generateHypertrophyDistribution(days, time, "Push-Pull-Legs"));
            distributions.add(generateHypertrophyDistribution(days, time, "Full Body"));
        }

        return distributions;
    }

    private String generatePowerliftingDistribution(int days, int time, String type) {
        StringBuilder distribution = new StringBuilder();
        List<String> pushExercises = List.of("Press de banca", "Press militar", "Fondos");
        List<String> pullExercises = List.of("Remo con barra", "Dominadas", "Curls de bíceps");
        List<String> legExercises = List.of("Sentadillas", "Peso muerto", "Prensa de piernas");

        int timePerDay = time / days;

        distribution.append("Distribución para Fuerza (Powerlifting) - ").append(type).append(":\n");

        switch (type) {
            case "Push-Pull":
                for (int i = 1; i <= days; i++) {
                    distribution.append("Día ").append(i).append(":\n");
                    if (i % 2 == 1) {
                        distribution.append("Push:\n");
                        pushExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    } else {
                        distribution.append("Pull:\n");
                        pullExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    }
                    distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
                }
                break;
            case "Push-Pull-Legs":
                for (int i = 1; i <= days; i++) {
                    distribution.append("Día ").append(i).append(":\n");
                    if (i % 3 == 1) {
                        distribution.append("Push:\n");
                        pushExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    } else if (i % 3 == 2) {
                        distribution.append("Pull:\n");
                        pullExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    } else {
                        distribution.append("Legs:\n");
                        legExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    }
                    distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
                }
                break;
            case "Full Body":
                for (int i = 1; i <= days; i++) {
                    distribution.append("Día ").append(i).append(":\n");
                    distribution.append("Full Body:\n");
                    pushExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    pullExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    legExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
                }
                break;
        }

        return distribution.toString();
    }

    private String generateHypertrophyDistribution(int days, int time, String type) {
        StringBuilder distribution = new StringBuilder();
        List<String> pushExercises = List.of("Press de pecho", "Press de hombros", "Extensiones de tríceps");
        List<String> pullExercises = List.of("Remo con mancuerna", "Dominadas", "Curls de bíceps");
        List<String> legExercises = List.of("Sentadillas", "Peso muerto", "Prensa de piernas");

        int timePerDay = time / days;

        distribution.append("Distribución para Hipertrofia - ").append(type).append(":\n");

        switch (type) {
            case "Push-Pull":
                for (int i = 1; i <= days; i++) {
                    distribution.append("Día ").append(i).append(":\n");
                    if (i % 2 == 1) {
                        distribution.append("Push:\n");
                        pushExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    } else {
                        distribution.append("Pull:\n");
                        pullExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    }
                    distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
                }
                break;
            case "Push-Pull-Legs":
                for (int i = 1; i <= days; i++) {
                    distribution.append("Día ").append(i).append(":\n");
                    if (i % 3 == 1) {
                        distribution.append("Push:\n");
                        pushExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    } else if (i % 3 == 2) {
                        distribution.append("Pull:\n");
                        pullExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    } else {
                        distribution.append("Legs:\n");
                        legExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    }
                    distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
                }
                break;
            case "Full Body":
                for (int i = 1; i <= days; i++) {
                    distribution.append("Día ").append(i).append(":\n");
                    distribution.append("Full Body:\n");
                    pushExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    pullExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    legExercises.forEach(exercise -> distribution.append("- ").append(exercise).append("\n"));
                    distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
                }
                break;
        }

        return distribution.toString();
    }
}