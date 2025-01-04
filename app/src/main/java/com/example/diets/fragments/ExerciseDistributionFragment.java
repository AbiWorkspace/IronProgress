package com.example.diets.fragments;


import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.example.diets.R;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ExerciseDistributionFragment extends Fragment {
    private static final String TAG = "ExerciseDistributionFragment";
    private String selectedDistribution = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_distribution, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        LinearLayout distributionContainer = view.findViewById(R.id.distributionContainer);
        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (selectedDistribution != null) {
                Log.d(TAG, "Distribución seleccionada: " + selectedDistribution);
                // Aquí puedes agregar la lógica para guardar la distribución seleccionada
            } else {
                Log.d(TAG, "No se ha seleccionado ninguna distribución");
            }
        });

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

            if (distributionContainer != null) {
                Log.e(TAG, "distributionContainer is not null");
                RadioGroup radioGroup = new RadioGroup(getContext());
                radioGroup.setOrientation(RadioGroup.VERTICAL);
                Log.e(TAG, "RadioGroup created");

                for (String distribution : distributions) {
                    Log.e(TAG, "Processing distribution: " + distribution);
                    LinearLayout distributionLayout = new LinearLayout(getContext());
                    distributionLayout.setOrientation(LinearLayout.HORIZONTAL);
                    distributionLayout.setPadding(16, 16, 16, 16);
                    distributionLayout.setBackground(getResources().getDrawable(R.drawable.distribution_background));
                    Log.e(TAG, "LinearLayout for distribution created");

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 16);
                    distributionLayout.setLayoutParams(params);
                    Log.e(TAG, "LayoutParams set for LinearLayout");

                    RadioButton radioButton = new RadioButton(getContext());
                    radioButton.setText("");
                    radioButton.setPadding(8, 8, 8, 8);
                    radioButton.setButtonTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
                    radioButton.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    Log.e(TAG, "RadioButton created and styled");

                    TextView distributionText = new TextView(getContext());
                    distributionText.setText(distribution);
                    distributionText.setTextColor(getResources().getColor(R.color.white));
                    distributionText.setPadding(8, 8, 8, 8);
                    distributionText.setTextSize(16);
                    distributionText.setTypeface(null, Typeface.BOLD);
                    Log.e(TAG, "TextView created and styled");

                    distributionLayout.addView(radioButton);
                    Log.e(TAG, "RadioButton added to LinearLayout");
                    distributionLayout.addView(distributionText);
                    Log.e(TAG, "TextView added to LinearLayout");

                    radioGroup.addView(distributionLayout);
                    Log.e(TAG, "LinearLayout added to RadioGroup");

                    // Log the state of the RadioButton
                    radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        Log.e(TAG, "RadioButton " + distribution + " isChecked: " + isChecked);
                        if (isChecked) {
                            selectedDistribution = distribution;
                            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                                LinearLayout layout = (LinearLayout) radioGroup.getChildAt(i);
                                RadioButton rb = (RadioButton) layout.getChildAt(0);
                                if (rb != buttonView) {
                                    rb.setChecked(false);
                                }
                            }
                        }
                    });
                }

                distributionContainer.addView(radioGroup);
                Log.e(TAG, "RadioGroup added to distributionContainer");
                Log.d(TAG, "Distributions set to individual containers with RadioButtons");
            } else {
                Log.e(TAG, "distributionContainer is null");
            }
        } else {
            Log.e(TAG, "No se pasaron argumentos al fragmento.");
        }
    }
    private List<String> generateTrainingDistributions(String goal, int days, int time, String jobType, String experience) {
        List<String> distributions = new ArrayList<>();

        if (days >= 6) {
            addSixDayDistributions(goal, time, distributions);
        } else if (days >= 5) {
            addFiveDayDistributions(goal, time, distributions);
        } else if (days >= 4) {
            addFourDayDistributions(goal, time, distributions);
        } else if (days >= 3) {
            addThreeDayDistributions(goal, time, distributions);
        } else {
            Log.e(TAG, "No hay suficientes días para una distribución adecuada");
        }

        return distributions;
    }

    private void addSixDayDistributions(String goal, int time, List<String> distributions) {
        if (isStrengthOrHypertrophy(goal)) {
            if (time < 60) {
                distributions.add("Push-Pull-Legs (Empuje-Tirón-Piernas) - Menos de 1 hora:\nLunes: Empuje (pecho, hombros, tríceps).\nMartes: Tirón (espalda, bíceps).\nMiércoles: Piernas (cuádriceps, isquiotibiales, glúteos).\nJueves: Descanso (no cuenta).\nViernes: Empuje (pecho, hombros, tríceps).\nSábado: Tirón (espalda, bíceps).\nDomingo: Piernas (cuádriceps, isquiotibiales, glúteos).");
            } else {
                distributions.add("Push-Pull-Legs (Empuje-Tirón-Piernas):\nLunes: Empuje (pecho, hombros, tríceps).\nMartes: Tirón (espalda, bíceps).\nMiércoles: Piernas (cuádriceps, isquiotibiales, glúteos).\nJueves: Descanso (no cuenta).\nViernes: Empuje (pecho, hombros, tríceps).\nSábado: Tirón (espalda, bíceps).\nDomingo: Piernas (cuádriceps, isquiotibiales, glúteos).");
            }
            distributions.add("PHAT (Power Hypertrophy Adaptive Training):\nLunes: Fuerza parte superior.\nMartes: Fuerza parte inferior.\nMiércoles: Descanso (no cuenta).\nJueves: Hipertrofia parte superior (pecho, espalda).\nViernes: Hipertrofia parte inferior (piernas, glúteos).\nSábado: Hipertrofia parte superior (hombros, brazos).\nDomingo: Descanso (no cuenta).");
            distributions.add("5/3/1 de Jim Wendler:\nLunes: Press de banca.\nMartes: Sentadilla.\nMiércoles: Descanso (no cuenta).\nJueves: Press militar.\nViernes: Peso muerto.\nSábado y Domingo: Descanso (no cuenta).");
        }
    }

    private void addFiveDayDistributions(String goal, int time, List<String> distributions) {
        if (isStrengthOrHypertrophy(goal)) {
            if (time < 60) {
                distributions.add("Fuerza-Hipertrofia (Strength-Hypertrophy Split) - Menos de 1 hora:\nLunes: Fuerza (sentadillas, press de banca, peso muerto).\nMartes: Hipertrofia (pecho, espalda, hombros).\nMiércoles: Descanso (no cuenta).\nJueves: Fuerza (sentadillas, press de banca, peso muerto).\nViernes: Hipertrofia (piernas, brazos).\nSábado y Domingo: Descanso (no cuenta).");
            } else {
                distributions.add("Fuerza-Hipertrofia (Strength-Hypertrophy Split):\nLunes: Fuerza (sentadillas, press de banca, peso muerto).\nMartes: Hipertrofia (pecho, espalda, hombros).\nMiércoles: Descanso (no cuenta).\nJueves: Fuerza (sentadillas, press de banca, peso muerto).\nViernes: Hipertrofia (piernas, brazos).\nSábado y Domingo: Descanso (no cuenta).");
            }
            distributions.add("Entrenamiento de Volumen Alemán (GVT):\nLunes: Pecho y espalda.\nMartes: Piernas y abdominales.\nMiércoles: Descanso (no cuenta).\nJueves: Brazos y hombros.\nViernes: Piernas y abdominales.\nSábado y Domingo: Descanso (no cuenta).");
            distributions.add("Westside Barbell:\nLunes: Fuerza máxima (press de banca).\nMiércoles: Fuerza máxima (sentadilla/peso muerto).\nViernes: Fuerza dinámica (press de banca).\nSábado: Fuerza dinámica (sentadilla/peso muerto).\nMartes y Jueves: Accesorios (hombros, brazos).\nDomingo: Descanso (no cuenta).");
            distributions.add("Push-Pull-Legs (Empuje-Tirón-Piernas):\nLunes: Empuje (pecho, hombros, tríceps).\nMartes: Tirón (espalda, bíceps).\nMiércoles: Piernas (cuádriceps, isquiotibiales, glúteos).\nJueves: Empuje (pecho, hombros, tríceps).\nViernes: Tirón (espalda, bíceps).\nSábado: Piernas (cuádriceps, isquiotibiales, glúteos).\nDomingo: Descanso (no cuenta).");
        }
    }

    private void addFourDayDistributions(String goal, int time, List<String> distributions) {
        if (isStrengthOrHypertrophy(goal)) {
            if (time < 60) {
                distributions.add("Upper-Lower (Superior-Inferior) - Menos de 1 hora:\nLunes: Parte superior (pecho, espalda, hombros, brazos).\nMartes: Parte inferior (piernas, glúteos).\nMiércoles: Descanso (no cuenta).\nJueves: Parte superior (pecho, espalda, hombros, brazos).\nViernes: Parte inferior (piernas, glúteos).\nSábado y Domingo: Descanso (no cuenta).");
            } else {
                distributions.add("Upper-Lower (Superior-Inferior):\nLunes: Parte superior (pecho, espalda, hombros, brazos).\nMartes: Parte inferior (piernas, glúteos).\nMiércoles: Descanso (no cuenta).\nJueves: Parte superior (pecho, espalda, hombros, brazos).\nViernes: Parte inferior (piernas, glúteos).\nSábado y Domingo: Descanso (no cuenta).");
            }
            distributions.add("PHUL (Power Hypertrophy Upper Lower):\nLunes: Fuerza parte superior.\nMartes: Fuerza parte inferior.\nJueves: Hipertrofia parte superior.\nViernes: Hipertrofia parte inferior.\nMiércoles, Sábado y Domingo: Descanso (no cuenta).");
            distributions.add("Full Body (Cuerpo Completo):\nLunes: Full Body (fuerza).\nMiércoles: Full Body (hipertrofia).\nViernes: Full Body (fuerza).\nDomingo: Full Body (hipertrofia).");
            distributions.add("Push-Pull-Legs (Empuje-Tirón-Piernas):\nLunes: Empuje (pecho, hombros, tríceps).\nMartes: Tirón (espalda, bíceps).\nJueves: Piernas (cuádriceps, isquiotibiales, glúteos).\nViernes: Empuje (pecho, hombros, tríceps).\nSábado: Tirón (espalda, bíceps).\nDomingo: Piernas (cuádriceps, isquiotibiales, glúteos).");
        }
    }

    private void addThreeDayDistributions(String goal, int time, List<String> distributions) {
        if (isStrengthOrHypertrophy(goal)) {
            if (time < 60) {
                distributions.add("Full Body - Menos de 1 hora:\nLunes: Full Body.\nMiércoles: Full Body.\nViernes: Full Body.");
            } else {
                distributions.add("Full Body:\nLunes: Full Body.\nMiércoles: Full Body.\nViernes: Full Body.");
            }
            distributions.add("Powerlifting Split:\nLunes: Sentadilla.\nMiércoles: Press de banca.\nViernes: Peso muerto.");
            distributions.add("Push-Pull-Legs (Empuje-Tirón-Piernas):\nLunes: Empuje (pecho, hombros, tríceps).\nMiércoles: Tirón (espalda, bíceps).\nViernes: Piernas (cuádriceps, isquiotibiales, glúteos).");
        }
    }
    private boolean isStrengthOrHypertrophy(String goal) {
        return "fuerza".equalsIgnoreCase(goal) || "Aumentar fuerza".equalsIgnoreCase(goal) || "hipertrofia".equalsIgnoreCase(goal) || "Aumentar masa muscular".equalsIgnoreCase(goal);
    }
}
