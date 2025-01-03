package com.example.diets.fragments;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.example.diets.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

        Bundle args = getArguments();
        TextView distributionTextView = view.findViewById(R.id.distributionTextView);

        if (args != null) {
            String goal = args.getString("goal");
            String jobType = args.getString("jobType");
            String experience = args.getString("experience");
            int days = args.getInt("days", 0);
            int time = args.getInt("time", 0);

            Log.d(TAG, "Goal: " + goal);
            Log.d(TAG, "Job Type: " + jobType);
            Log.d(TAG, "Experience: " + experience);
            Log.d(TAG, "Days: " + days);
            Log.d(TAG, "Time: " + time);

            List<String> distributions = generateTrainingDistributions(goal, days, time, jobType, experience);

            if (distributionTextView != null) {
                StringBuilder allDistributions = new StringBuilder();
                for (String distribution : distributions) {
                    allDistributions.append(distribution).append("\n\n");
                }

                distributionTextView.setText(allDistributions.toString());
                distributionTextView.setMovementMethod(new ScrollingMovementMethod());
                Log.d(TAG, "Distributions set to TextView");
                Log.d(TAG, "Distributions: " + allDistributions.toString());
            } else {
                Log.e(TAG, "distributionTextView is null");
            }
        } else {
            Log.e(TAG, "No se pasaron argumentos al fragmento.");
            if (distributionTextView != null) {
                distributionTextView.setText("Texto de prueba");
                distributionTextView.setMovementMethod(new ScrollingMovementMethod());
                Log.d(TAG, "Texto de prueba establecido en TextView");
            } else {
                Log.e(TAG, "distributionTextView is null incluso sin argumentos");
            }
        }
    }

    private List<String> generateTrainingDistributions(String goal, int days, int time, String jobType, String experience) {
        List<String> distributions = new ArrayList<>();
        Random random = new Random();

        if ("fuerza".equalsIgnoreCase(goal) || "Aumentar fuerza".equalsIgnoreCase(goal)) {
            distributions.add(generateDynamicPowerliftingDistribution(days, time, "Push-Pull", jobType, experience, random));
            distributions.add(generateDynamicPowerliftingDistribution(days, time, "Push-Pull-Legs", jobType, experience, random));
            distributions.add(generateDynamicPowerliftingDistribution(days, time, "Full Body", jobType, experience, random));
        } else if ("hipertrofia".equalsIgnoreCase(goal) || "Aumentar masa muscular".equalsIgnoreCase(goal)) {
            distributions.add(generateDynamicHypertrophyDistribution(days, time, "Push-Pull", jobType, experience, random));
            distributions.add(generateDynamicHypertrophyDistribution(days, time, "Push-Pull-Legs", jobType, experience, random));
            distributions.add(generateDynamicHypertrophyDistribution(days, time, "Full Body", jobType, experience, random));
        } else {
            Log.e(TAG, "Goal no reconocido: " + goal);
        }

        return distributions;
    }

    private String generateDynamicPowerliftingDistribution(int days, int time, String type, String jobType, String experience, Random random) {
        StringBuilder distribution = new StringBuilder();
        List<String> pushExercises = List.of("Press de banca", "Press militar", "Fondos", "Press inclinado");
        List<String> pullExercises = List.of("Remo con barra", "Dominadas", "Curls de bíceps", "Face pulls");
        List<String> legExercises = List.of("Sentadillas", "Peso muerto", "Prensa de piernas", "Zancadas");

        int timePerDay = time / days;

        distribution.append("Distribución para Fuerza (Powerlifting) - ").append(type).append(":\n");

        for (int i = 1; i <= days; i++) {
            distribution.append("Día ").append(i).append(":\n");

            if ("Push-Pull".equals(type)) {
                if (i % 2 == 1) {
                    distribution.append("Push:\n");
                    addRandomExercises(distribution, pushExercises, random);
                } else {
                    distribution.append("Pull:\n");
                    addRandomExercises(distribution, pullExercises, random);
                }
            } else if ("Push-Pull-Legs".equals(type)) {
                if (i % 3 == 1) {
                    distribution.append("Push:\n");
                    addRandomExercises(distribution, pushExercises, random);
                } else if (i % 3 == 2) {
                    distribution.append("Pull:\n");
                    addRandomExercises(distribution, pullExercises, random);
                } else {
                    distribution.append("Legs:\n");
                    addRandomExercises(distribution, legExercises, random);
                }
            } else if ("Full Body".equals(type)) {
                distribution.append("Full Body:\n");
                addRandomExercises(distribution, pushExercises, random);
                addRandomExercises(distribution, pullExercises, random);
                addRandomExercises(distribution, legExercises, random);
            } else {
                Log.e(TAG, "Tipo de distribución no reconocido: " + type);
            }

            distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
        }

        return distribution.toString();
    }

    private String generateDynamicHypertrophyDistribution(int days, int time, String type, String jobType, String experience, Random random) {
        StringBuilder distribution = new StringBuilder();
        List<String> pushExercises = List.of("Press de pecho", "Press de hombros", "Extensiones de tríceps", "Aperturas con mancuernas");
        List<String> pullExercises = List.of("Remo con mancuerna", "Dominadas", "Curls de bíceps", "Encogimientos de trapecio");
        List<String> legExercises = List.of("Sentadillas", "Peso muerto", "Prensa de piernas", "Elevaciones de talones");

        int timePerDay = time / days;

        distribution.append("Distribución para Hipertrofia - ").append(type).append(":\n");

        for (int i = 1; i <= days; i++) {
            distribution.append("Día ").append(i).append(":\n");

            if ("Push-Pull".equals(type)) {
                if (i % 2 == 1) {
                    distribution.append("Push:\n");
                    addRandomExercises(distribution, pushExercises, random);
                } else {
                    distribution.append("Pull:\n");
                    addRandomExercises(distribution, pullExercises, random);
                }
            } else if ("Push-Pull-Legs".equals(type)) {
                if (i % 3 == 1) {
                    distribution.append("Push:\n");
                    addRandomExercises(distribution, pushExercises, random);
                } else if (i % 3 == 2) {
                    distribution.append("Pull:\n");
                    addRandomExercises(distribution, pullExercises, random);
                } else {
                    distribution.append("Legs:\n");
                    addRandomExercises(distribution, legExercises, random);
                }
            } else if ("Full Body".equals(type)) {
                distribution.append("Full Body:\n");
                addRandomExercises(distribution, pushExercises, random);
                addRandomExercises(distribution, pullExercises, random);
                addRandomExercises(distribution, legExercises, random);
            } else {
                Log.e(TAG, "Tipo de distribución no reconocido: " + type);
            }

            distribution.append("Tiempo estimado: ").append(timePerDay).append(" minutos\n\n");
        }

        return distribution.toString();
    }
    private void addRandomExercises(StringBuilder distribution, List<String> exercises, Random random) {
        int numExercises = random.nextInt(2) + 2; // 2-3 ejercicios aleatorios
        for (int i = 0; i < numExercises; i++) {
            String exercise = exercises.get(random.nextInt(exercises.size()));
            if (!distribution.toString().contains(exercise)) {
                distribution.append("- ").append(exercise).append("\n");
            } else {
                Log.e(TAG, "Ejercicio duplicado: " + exercise);
            }
        }
    }
}