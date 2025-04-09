package com.example.diets.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.core.content.ContextCompat;
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
                Bundle data = new Bundle();
                data.putString("selectedDistribution", selectedDistribution);
                data.putSerializable("trainingDays", (Serializable) trainingDays);
                data.putString("userId", userId);
                ((ViewPagerActivity) getActivity()).switchToSelectExercisesFragment(data);
            } else {
                Log.d(TAG, "No se ha seleccionado ninguna distribución");
            }
        });

        Bundle data = getArguments();
        if (data != null) {
            selectedDays = data.getInt("days", 3);
        }

        loadExerciseDistributions(recommendedContainer, notRecommendedContainer, selectedDays);
    }

    private void loadExerciseDistributions(LinearLayout recommendedContainer, LinearLayout notRecommendedContainer, int selectedDays) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Usuario no autenticado. No se puede acceder a la base de datos.");
            return;
        }

        DatabaseReference exerciseDistRef = mDatabase.child("exerciseDistributions");
        exerciseDistRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ No se encontraron distribuciones de ejercicios en la base de datos.");
                    displayNoDistributionsMessage(recommendedContainer);
                    displayNoDistributionsMessage(notRecommendedContainer);
                    return;
                }

                List<ExerciseDistribution> distributions = new ArrayList<>();
                for (DataSnapshot distributionSnapshot : snapshot.getChildren()) {
                    ExerciseDistribution exerciseDistribution = distributionSnapshot.getValue(ExerciseDistribution.class);
                    if (exerciseDistribution != null) {
                        distributions.add(exerciseDistribution);
                    } else {
                        Log.w(TAG, "⚠️ Distribución de ejercicios nula en la base de datos.");
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error de base de datos: " + error.getMessage());
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

    private void displayDistributions(ExerciseDistribution distribution, LinearLayout container) {
        Context context = getContext();

        LinearLayout distributionLayout = new LinearLayout(context);
        distributionLayout.setOrientation(LinearLayout.VERTICAL);
        distributionLayout.setPadding(12, 12, 12, 12);
        distributionLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.distribution_background));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        distributionLayout.setLayoutParams(params);

        TextView titleText = new TextView(context);
        titleText.setText(distribution.getTitle());
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(20);
        titleText.setTypeface(null, Typeface.BOLD);
        distributionLayout.addView(titleText);

        TextView daysText = new TextView(context);
        daysText.setText("Número de días: " + distribution.getNumberOfDays());
        daysText.setTextColor(Color.LTGRAY);
        daysText.setTextSize(16);
        daysText.setPadding(0, 0, 0, 8);
        distributionLayout.addView(daysText);

        for (int i = 0; i < distribution.getTrainingDays().size(); i++) {
            TrainingDay day = distribution.getTrainingDays().get(i);

            LinearLayout dayLayout = new LinearLayout(context);
            dayLayout.setOrientation(LinearLayout.VERTICAL);
            dayLayout.setPadding(8, 8, 8, 8);
            dayLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.day_card_background));

            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dayParams.setMargins(0, 8, 0, 8); // espaciado más compacto
            dayLayout.setLayoutParams(dayParams);

            TextView dayTitle = new TextView(context);
            dayTitle.setText("Día " + (i + 1) + " • Patrón: " + day.getMovementPatterns().get(0).getName());
            dayTitle.setTextColor(Color.WHITE);
            dayTitle.setTypeface(null, Typeface.BOLD);
            dayTitle.setTextSize(16);
            dayTitle.setPadding(0, 0, 0, 8);
            dayLayout.addView(dayTitle);

            // Layout oculto para músculos
            LinearLayout muscleInfoLayout = new LinearLayout(context);
            muscleInfoLayout.setOrientation(LinearLayout.VERTICAL);
            muscleInfoLayout.setVisibility(View.GONE);

            MovementPattern pattern = day.getMovementPatterns().get(0);

            TextView workedText = new TextView(context);
            workedText.setText("Trabajados: · " + TextUtils.join(" · ", pattern.getMusclesWorked()));
            workedText.setTextColor(Color.GRAY);
            workedText.setTextSize(14);
            muscleInfoLayout.addView(workedText);

            TextView synergistsText = new TextView(context);
            synergistsText.setText("Sinergistas: · " + TextUtils.join(" · ", pattern.getSynergistMuscles()));
            synergistsText.setTextColor(Color.GRAY);
            synergistsText.setTextSize(14);
            synergistsText.setPadding(0, 8, 0, 8);
            muscleInfoLayout.addView(synergistsText);

            TextView toggleText = new TextView(context);
            toggleText.setText("Ver más");
            toggleText.setTextColor(Color.parseColor("#B0BEC5"));
            toggleText.setTypeface(null, Typeface.NORMAL);
            toggleText.setTextSize(13);
            toggleText.setPadding(0, 4, 0, 4);

            final boolean[] expanded = {false};
            toggleText.setOnClickListener(v -> {
                if (!expanded[0]) {
                    muscleInfoLayout.setVisibility(View.VISIBLE);
                    toggleText.setText("Ver menos");
                    expanded[0] = true;
                } else {
                    muscleInfoLayout.setVisibility(View.GONE);
                    toggleText.setText("Ver más");
                    expanded[0] = false;
                }
            });

            dayLayout.addView(toggleText);
            dayLayout.addView(muscleInfoLayout);
            distributionLayout.addView(dayLayout);

            // Divider entre días
            View divider = new View(context);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            dividerParams.setMargins(0, 12, 0, 4);
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(Color.parseColor("#2F2F2F"));
            distributionLayout.addView(divider);
        }

        RadioButton radioButton = new RadioButton(context);
        radioButton.setPadding(8, 8, 8, 8);
        radioButton.setButtonTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
        distributionLayout.addView(radioButton);
        radioButtons.add(radioButton);

        radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedDistribution = distribution.getTitle();
                trainingDays = new ArrayList<>(distribution.getTrainingDays());
                for (RadioButton rb : radioButtons) {
                    if (rb != buttonView) rb.setChecked(false);
                }
            }
        });

        container.addView(distributionLayout);
    }

}
