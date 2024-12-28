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

public class EntrenamientoFragment extends Fragment {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_TIME_KEY = "firstTime";

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
        Button submitButton = view.findViewById(R.id.submitButton);

        welcomeText.setVisibility(View.VISIBLE);
        daysSpinner.setVisibility(View.VISIBLE);
        jobTypeSpinner.setVisibility(View.VISIBLE);
        submitButton.setVisibility(View.VISIBLE);

        Log.d(TAG, "showWelcomeForm: Configurando ArrayAdapter para los Spinners");
        ArrayAdapter<CharSequence> daysAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.training_days_array, android.R.layout.simple_spinner_item);
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> jobTypeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.job_type_array, android.R.layout.simple_spinner_item);
        jobTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Verificar si los adaptadores tienen contenido antes de asignarlos a los Spinners
        if (daysAdapter.getCount() > 0) {
            daysSpinner.setAdapter(daysAdapter);
            Log.d(TAG, "showWelcomeForm: Spinner de días lleno con opciones");
        } else {
            Toast.makeText(getContext(), "No hay opciones disponibles para el Spinner de días", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "showWelcomeForm: No hay opciones disponibles para el Spinner de días");
        }

        if (jobTypeAdapter.getCount() > 0) {
            jobTypeSpinner.setAdapter(jobTypeAdapter);
            Log.d(TAG, "showWelcomeForm: Spinner de tipo de trabajo lleno con opciones");
        } else {
            Toast.makeText(getContext(), "No hay opciones disponibles para el Spinner de tipo de trabajo", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "showWelcomeForm: No hay opciones disponibles para el Spinner de tipo de trabajo");
        }

        submitButton.setOnClickListener(v -> {
            Log.d(TAG, "submitButton: Botón de enviar clicado");

            // Guardar datos del formulario y actualizar preferencias
            SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(FIRST_TIME_KEY, false);
            editor.apply();
            Log.d(TAG, "submitButton: Preferencias actualizadas, firstTime establecido a false");

            // Ocultar formulario y mostrar contenido normal
            welcomeText.setVisibility(View.GONE);
            daysSpinner.setVisibility(View.GONE);
            jobTypeSpinner.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            Log.d(TAG, "submitButton: Formulario de bienvenida oculto, mostrando contenido normal");
        });
    }
}