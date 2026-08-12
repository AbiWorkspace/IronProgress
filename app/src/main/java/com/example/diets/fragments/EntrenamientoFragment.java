package com.example.diets.fragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;

public class EntrenamientoFragment extends Fragment {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_TIME_KEY = "firstTime";
    private static final String TAG = "EntrenamientoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflando el layout para EntrenamientoFragment");
        return inflater.inflate(R.layout.fragment_entrenamiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Vista creada para EntrenamientoFragment");

        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
        boolean firstTime = settings.getBoolean(FIRST_TIME_KEY, true);
        Log.d(TAG, "onViewCreated: Usuario por primera vez: " + firstTime);

        if (true) {
            showWelcomeForm(view);
        } else {
            Log.d(TAG, "onViewCreated: Mostrando contenido normal");
        }
    }

    private void showWelcomeForm(View view) {
        Log.d(TAG, "showWelcomeForm: Mostrando formulario de bienvenida");
        TextView welcomeText = view.findViewById(R.id.welcomeText);
        Spinner daysSpinner = view.findViewById(R.id.daysSpinner);
        Spinner jobTypeSpinner = view.findViewById(R.id.jobTypeSpinner);
        Spinner goalSpinner2 = view.findViewById(R.id.goalSpinner);
        Spinner experienceSpinner = view.findViewById(R.id.experienceSpinner);
        Spinner timeSpinner = view.findViewById(R.id.timeSpinner);
        Button submitButton = view.findViewById(R.id.submitButton);

        welcomeText.setVisibility(View.VISIBLE);
        daysSpinner.setVisibility(View.VISIBLE);
        jobTypeSpinner.setVisibility(View.VISIBLE);
        goalSpinner2.setVisibility(View.VISIBLE);
        experienceSpinner.setVisibility(View.VISIBLE);
        timeSpinner.setVisibility(View.VISIBLE);
        submitButton.setVisibility(View.VISIBLE);

        Log.d(TAG, "showWelcomeForm: Configurando ArrayAdapter para los Spinners");
        ArrayAdapter<CharSequence> daysAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.training_days_array, android.R.layout.simple_spinner_item);
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> jobTypeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.job_type_array, android.R.layout.simple_spinner_item);
        jobTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.goal_options2, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> experienceAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.experience_options, android.R.layout.simple_spinner_item);
        experienceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.time_availability_options, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Log.d(TAG, "showWelcomeForm: Configurando ArrayAdapter personalizado para los Spinners con texto blanco");

        daysSpinner.setAdapter(daysAdapter);
        jobTypeSpinner.setAdapter(jobTypeAdapter);
        goalSpinner2.setAdapter(goalAdapter);
        experienceSpinner.setAdapter(experienceAdapter);
        timeSpinner.setAdapter(timeAdapter);

        submitButton.setOnClickListener(v -> {
            Log.d(TAG, "submitButton: Botón de enviar clicado");

            // Recopilar datos seleccionados en los Spinners
            String goal = goalSpinner2.getSelectedItem().toString();
            String daysText = daysSpinner.getSelectedItem().toString();
            String jobType = jobTypeSpinner.getSelectedItem().toString();
            String experience = experienceSpinner.getSelectedItem().toString();
            String timeText = timeSpinner.getSelectedItem().toString();

            // Procesar valores numéricos
            int days = Integer.parseInt(daysText.replaceAll("\\D+", ""));
            int time = Integer.parseInt(timeText.replaceAll("\\D+", ""));

            // Crear un Bundle con los datos
            Bundle data = new Bundle();
            data.putString("goal", goal);
            data.putString("jobType", jobType);
            data.putString("experience", experience);
            data.putInt("days", days);
            data.putInt("time", time);

            // Llamar al método para cambiar al nuevo fragmento
            if (getActivity() instanceof ViewPagerActivity) {
                ((ViewPagerActivity) getActivity()).switchToExerciseDistributionFragment(data);
            }
        });
    }

}
