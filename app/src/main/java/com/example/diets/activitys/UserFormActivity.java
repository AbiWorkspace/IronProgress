package com.example.diets.activitys;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.diets.R;

public class UserFormActivity extends AppCompatActivity {

    private Spinner genderSpinner;
    private Spinner activityLevelSpinner;
    private Spinner goalSpinner;
    private Spinner weightSpinner;
    private Spinner heightSpinner;
    private Spinner ageSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_form);

        genderSpinner = findViewById(R.id.genderSpinner);
        activityLevelSpinner = findViewById(R.id.activityLevelSpinner);
        goalSpinner = findViewById(R.id.goalSpinner);
        weightSpinner = findViewById(R.id.weightSpinner);
        heightSpinner = findViewById(R.id.heightSpinner);
        ageSpinner = findViewById(R.id.ageSpinner);

        // Configuración de los Spinners
        configureSpinners();

        // Configuración del botón de ayuda
        findViewById(R.id.helpButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showActivityLevelInfoDialog();
            }
        });
    }

    private void configureSpinners() {
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> activityLevelAdapter = ArrayAdapter.createFromResource(this,
                R.array.activity_level_options, android.R.layout.simple_spinner_item);
        activityLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityLevelSpinner.setAdapter(activityLevelAdapter);

        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(this,
                R.array.goal_options, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalSpinner.setAdapter(goalAdapter);

        // Puedes agregar adaptadores similares para weightSpinner, heightSpinner y ageSpinner
    }

    private void showActivityLevelInfoDialog() {
        // Crear un nuevo diálogo
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_activity_level_info); // Layout personalizado
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Configurar el botón de cerrar
        Button closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Mostrar el diálogo
        dialog.show();
    }
}
