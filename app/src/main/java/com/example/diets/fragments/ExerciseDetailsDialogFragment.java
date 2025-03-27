package com.example.diets.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.model.ExerciseSeries;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseDetailsDialogFragment extends DialogFragment {
    private static final String ARG_EXERCISE = "exerciseId";
    private static final String TAG = "ExerciseDetailsDialogFragment";

    private LineChart chartExerciseProgress;
    private DatabaseReference databaseReference;
    private Spinner spinnerRpeFilter;
    private List<ExerciseSeries> allSeriesData;

    /**
     * Método estático para crear una nueva instancia del fragmento, recibiendo el ID del ejercicio.
     */
    public static ExerciseDetailsDialogFragment newInstance(String exerciseId) {
        Log.d(TAG + " - newInstance", "Creando una nueva instancia del fragmento con el ID del ejercicio: " + exerciseId);

        ExerciseDetailsDialogFragment fragment = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE, exerciseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + " - onCreate", "Inicializando el fragmento y configurando el estilo del diálogo.");
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG + " - onCreate", "Referencia a Firebase obtenida correctamente.");

        allSeriesData = new ArrayList<>();
        Log.d(TAG + " - onCreate", "Lista de series inicializada: allSeriesData vacía.");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG + " - onCreateView", "Inflando la vista del fragmento.");
        View view = inflater.inflate(R.layout.dialog_exercise_details, container, false);

        // Referencias a vistas
        ImageView imageExercise         = view.findViewById(R.id.imageExercise);
        TextView  textExerciseName      = view.findViewById(R.id.textExerciseName);
        TextView  textDescription       = view.findViewById(R.id.textDescription);
        TextView  textMusclesWorked     = view.findViewById(R.id.textMusclesWorked);
        TextView  textSynergistMuscles  = view.findViewById(R.id.textSynergistMuscles);
        TextView  textRM                = view.findViewById(R.id.textRM);
        TextView  textPersonalRecord    = view.findViewById(R.id.textPersonalRecord);
        chartExerciseProgress           = view.findViewById(R.id.chartExerciseProgress);
        Button    btnClose              = view.findViewById(R.id.btnClose);
        spinnerRpeFilter               = view.findViewById(R.id.spinnerRpeFilter);

        Log.d(TAG + " - onCreateView", "Vistas inicializadas correctamente.");

        // Verificar usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "⚠️ Error: Usuario no autenticado. No se cargará nada.");
            return view;
        }
        String userId = user.getUid();
        Log.d(TAG, "Usuario autenticado con UID: " + userId);

        // Obtener ID del ejercicio desde argumentos
        String exerciseId = (getArguments() != null) ? getArguments().getString(ARG_EXERCISE) : null;
        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "⚠️ Error: ID del ejercicio no válido.");
            return view;
        }
        Log.d(TAG, "Obteniendo datos del ejercicio con ID: " + exerciseId);

        // Referencia al ejercicio en Firebase
        DatabaseReference exerciseRef = databaseReference
                .child("users")
                .child(userId)
                .child("userExercises")
                .child(exerciseId);

        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "⚠️ Error: No se encontró el ejercicio en Firebase con ID: " + exerciseId);
                    return;
                }

                // Obtener datos de la BD
                String exerciseName      = snapshot.child("name").getValue(String.class);
                String description       = snapshot.child("description").getValue(String.class);
                String musclesWorkedStr  = snapshot.child("musclesWorked").getValue(String.class);
                Integer currentLoad      = snapshot.child("currentLoad").getValue(Integer.class);
                Integer maxReps          = snapshot.child("maxReps").getValue(Integer.class);
                String imageUrl          = snapshot.child("image").getValue(String.class);

                // Listado de sinergistas
                List<String> synergyList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.child("synergistMuscles").getChildren()) {
                    String muscle = ds.getValue(String.class);
                    if (muscle != null) synergyList.add(muscle);
                }

                Log.d(TAG + " - onDataChange", "Ejercicio: " + exerciseName);
                Log.d(TAG + " - onDataChange", "Descripción: " + description);
                Log.d(TAG + " - onDataChange", "Músculos trabajados: " + musclesWorkedStr);
                Log.d(TAG + " - onDataChange", "Músculos sinérgicos: " + synergyList);
                Log.d(TAG + " - onDataChange", "Carga actual: " + currentLoad + " kg");
                Log.d(TAG + " - onDataChange", "Repeticiones máximas: " + maxReps);

                // Asignar datos a la UI
                textExerciseName.setText(exerciseName);
                textDescription.setText(description);
                textMusclesWorked.setText("Músculos trabajados: " + capitalizeWords(musclesWorkedStr));
                textSynergistMuscles.setText("Músculos sinérgicos: " + TextUtils.join(", ", synergyList));

                // Cargar imagen con Glide
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(imageExercise);
                    Log.d(TAG + " - onDataChange", "Imagen cargada correctamente desde: " + imageUrl);
                } else {
                    imageExercise.setImageResource(R.drawable.ic_placeholder);
                    Log.e(TAG + " - onDataChange", "URL de imagen no válida, usando imagen por defecto.");
                }

                // Calcular RM estimado
                if (currentLoad == null || currentLoad <= 0) currentLoad = 1;
                if (maxReps == null || maxReps <= 0) maxReps = 1;

                double estimatedRM = currentLoad * (1 + (maxReps / 30.0));
                textRM.setText("RM Estimado: " + String.format("%.2f", estimatedRM) + " kg");
                Log.d(TAG + " - onDataChange", "RM Estimado calculado: " + estimatedRM);

                // Ahora descargamos las series
                fetchSeriesData(exerciseId, textPersonalRecord);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG + " - onDataChange", "❌ Error al obtener el ejercicio", error.toException());
            }
        });

        // Botón Cerrar
        btnClose.setOnClickListener(v -> {
            Log.d(TAG, "Botón de cerrar presionado, cerrando el diálogo.");
            dismiss();
        });

        // Evento de selección en el Spinner (filtrar RPE)
        spinnerRpeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selectedRpeStr = parent.getItemAtPosition(position).toString();
                Log.d(TAG + " - SpinnerRpe", "Se seleccionó RPE: " + selectedRpeStr);
                try {
                    float rpeValue = Float.parseFloat(selectedRpeStr);
                    filterAndDrawChartByRpe(rpeValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG + " - SpinnerRpe", "Error parseando RPE seleccionado: " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG + " - SpinnerRpe", "No se seleccionó ningún valor RPE en el Spinner.");
            }
        });

        // Configuraciones iniciales de la gráfica para un look más profesional
        configureChartLookAndFeel();

        return view;
    }

    /**
     * Configura la apariencia y comportamiento inicial de la gráfica para que luzca más profesional.
     */
    private void configureChartLookAndFeel() {
        Log.d(TAG + " - configureChartLookAndFeel", "Aplicando configuraciones de look & feel a la gráfica.");

        // Descripción
        Description description = new Description();
        description.setText("Progreso de Cargas (RM Estimado)");
        description.setTextColor(Color.WHITE);
        description.setTextSize(12f);
        chartExerciseProgress.setDescription(description);

        // Fondo de la gráfica
        chartExerciseProgress.setDrawGridBackground(false);
        chartExerciseProgress.setBackgroundColor(Color.TRANSPARENT);

        // Escala y gestos
        chartExerciseProgress.setDragEnabled(true);
        chartExerciseProgress.setScaleEnabled(true);
        chartExerciseProgress.setPinchZoom(true);

        // Configurar leyenda
        Legend legend = chartExerciseProgress.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setForm(Legend.LegendForm.CIRCLE);

        // Configurar eje X
        XAxis xAxis = chartExerciseProgress.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        // Configurar eje Y izquierdo
        YAxis leftAxis = chartExerciseProgress.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);

        // Desactivar eje Y derecho si no se usa
        YAxis rightAxis = chartExerciseProgress.getAxisRight();
        rightAxis.setEnabled(false);
    }

    /**
     * Descarga todas las series y las almacena en allSeriesData. Luego dibuja la gráfica con todas.
     */
    private void fetchSeriesData(String exerciseId, TextView textPersonalRecord) {
        Log.d(TAG + " - fetchSeriesData", "Iniciando descarga de series para el ejercicio con ID: " + exerciseId);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG + " - fetchSeriesData", "⚠️ Usuario no autenticado, no se pueden obtener las series.");
            return;
        }

        String userId = user.getUid();
        DatabaseReference seriesRef = databaseReference
                .child("users")
                .child(userId)
                .child("userExercises")
                .child(exerciseId)
                .child("seriesData");

        seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double personalRecord = 0;
                allSeriesData.clear();
                Log.d(TAG + " - fetchSeriesData", "Descargando datos de series...");

                for (DataSnapshot seriesSnapshot : dataSnapshot.getChildren()) {
                    Integer seriesNumber = seriesSnapshot.child("seriesNumber").getValue(Integer.class);
                    Integer reps        = seriesSnapshot.child("reps").getValue(Integer.class);
                    Double weight       = seriesSnapshot.child("weight").getValue(Double.class);
                    Double rpe          = seriesSnapshot.child("rpe").getValue(Double.class);

                    if (seriesNumber != null && reps != null && weight != null && rpe != null) {
                        ExerciseSeries newSeries = new ExerciseSeries(seriesNumber, reps, weight, rpe);
                        allSeriesData.add(newSeries);

                        personalRecord = Math.max(personalRecord, weight);
                        Log.d(TAG + " - fetchSeriesData",
                                "Serie cargada - Num:" + seriesNumber +
                                        ", Reps:" + reps + ", Peso:" + weight + ", RPE:" + rpe);
                    } else {
                        Log.w(TAG + " - fetchSeriesData", "Datos de serie incompletos, se omite este nodo.");
                    }
                }

                Log.d(TAG + " - fetchSeriesData", "Se descargaron " + allSeriesData.size() + " series en total.");
                textPersonalRecord.setText("Personal Record: " + String.format("%.2f", personalRecord) + " kg");

                // Dibujar con TODAS las series inicialmente
                drawProgressChart(allSeriesData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG + " - fetchSeriesData", "❌ Error al obtener datos de series", databaseError.toException());
            }
        });
    }

    /**
     * Filtra la lista 'allSeriesData' por el RPE especificado y llama a 'drawProgressChart'.
     */
    private void filterAndDrawChartByRpe(float rpeFilter) {
        Log.d(TAG + " - filterAndDrawChartByRpe", "Filtrando por RPE: " + rpeFilter);

        if (allSeriesData.isEmpty()) {
            Log.d(TAG + " - filterAndDrawChartByRpe", "No hay series en allSeriesData para filtrar.");
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }

        List<ExerciseSeries> filteredSeries = new ArrayList<>();
        for (ExerciseSeries s : allSeriesData) {
            float rpeValue = (float) s.getRpe();
            // Comparamos con el RPE seleccionado
            if (rpeValue == rpeFilter) {
                filteredSeries.add(s);
            }
        }

        Log.d(TAG + " - filterAndDrawChartByRpe",
                "Se encontraron " + filteredSeries.size() + " series con RPE = " + rpeFilter);

        drawProgressChart(filteredSeries);
    }

    /**
     * Dibuja la gráfica recibiendo una lista de series a mostrar (ya sea filtradas o completas).
     */
    private void drawProgressChart(List<ExerciseSeries> seriesData) {
        Log.d(TAG + " - drawProgressChart", "Dibujando la gráfica con " + seriesData.size() + " series.");

        if (seriesData.isEmpty()) {
            chartExerciseProgress.setNoDataText("No hay datos suficientes");
            chartExerciseProgress.setNoDataTextColor(Color.YELLOW);
            Log.d(TAG + " - drawProgressChart", "No hay suficientes datos para la gráfica. Limpiando la gráfica...");
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }

        // Creamos los puntos (x=seriesNumber, y=RM estimado)
        List<Entry> entries = new ArrayList<>();
        for (ExerciseSeries series : seriesData) {
            float x = (float) series.getSeriesNumber();
            float y = (float) (series.getWeight() * (1 + (series.getReps() / 30.0))); // RM estimado
            entries.add(new Entry(x, y));

            Log.d(TAG + " - drawProgressChart",
                    "Agregando Entry => serieNum:" + x + ", RM estimado:" + y);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Progreso RPE");
        dataSet.setColor(Color.GREEN);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.GREEN);
        dataSet.setDrawValues(true);

        // Personalizar ejes, textos, leyenda, etc. (ya configurado en configureChartLookAndFeel())
        LineData lineData = new LineData(dataSet);
        chartExerciseProgress.setData(lineData);
        chartExerciseProgress.invalidate();

        Log.d(TAG + " - drawProgressChart", "Gráfica actualizada con " + seriesData.size() + " series.");
    }

    /**
     * Capitaliza la primera letra de cada palabra en el string.
     */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String[] words = text.split("\\s+");
        StringBuilder capitalizedText = new StringBuilder();

        for (String word : words) {
            if (word.length() > 1) {
                capitalizedText.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            } else {
                capitalizedText.append(word.toUpperCase()).append(" ");
            }
        }
        return capitalizedText.toString().trim();
    }
}
