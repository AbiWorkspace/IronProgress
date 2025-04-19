package com.example.diets.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.adapters.ImagePagerAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExerciseDetailsDialogFragment extends DialogFragment {
    private static final String ARG_EXERCISE = "exerciseId";
    private static final String TAG = "ExerciseDetailsDialogFragment";

    private LineChart chartExerciseProgress;
    private DatabaseReference databaseReference;
    private Spinner spinnerRpeFilter;
    private List<ExerciseSeries> allSeriesData;
    private String base64ImageString;  // ✅ Guardará la imagen base64 limpia para su uso global
    private ViewPager2 imageCarousel;

    private String exerciseKey;
    private String dayKey;
    private String exerciseIndex; // Este es el ID real del ejercicio en /userExercises
    private List<Bitmap> carouselBitmaps = new ArrayList<>();

    // Vistas para MovementType, EquipmentType, ExerciseType, PatternName
    private TextView textMovementType, textEquipmentType, textExerciseType, textPatternName;

    /**
     * Método estático para crear una nueva instancia del fragmento, recibiendo el ID del ejercicio y el dayKey.
     */
    public static ExerciseDetailsDialogFragment newInstance(String exerciseIndex, String dayKey) {
        ExerciseDetailsDialogFragment fragment = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString("exerciseIndex", exerciseIndex);
        args.putString("dayKey", dayKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG + " - onCreate", "[🧠Lógica] Iniciando método onCreate.");

        if (getArguments() != null) {
            exerciseIndex = getArguments().getString("exerciseIndex");
            dayKey = getArguments().getString("dayKey");
            Log.d(TAG + " - onCreate", "[📦Args] Claves recibidas - dayKey: " + dayKey + ", exerciseIndex: " + exerciseIndex);
        } else {
            Log.e(TAG + " - onCreate", "[⚠️Validación] getArguments() es null. No se recibieron claves.");
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);
        Log.d(TAG + " - onCreate", "[🎨Estilo] Estilo del diálogo aplicado.");

        databaseReference = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG + " - onCreate", "[🔥Firebase] Referencia a Firebase inicializada correctamente.");

        allSeriesData = new ArrayList<>();
        Log.d(TAG + " - onCreate", "[🧠Lógica] Lista allSeriesData creada vacía.");
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG + " - onCreateView", "Inflando la vista del fragmento.");
        View view = inflater.inflate(R.layout.dialog_exercise_details, container, false);

        // Referencias a vistas existentes

        TextView  textExerciseName     = view.findViewById(R.id.textExerciseName);
        TextView  textDescription      = view.findViewById(R.id.textDescription);
        TextView  textMusclesWorked    = view.findViewById(R.id.textMusclesWorked);
        TextView  textSynergistMuscles = view.findViewById(R.id.textSynergistMuscles);
        TextView  textRM               = view.findViewById(R.id.textRM);
        TextView  textPersonalRecord   = view.findViewById(R.id.textPersonalRecord);
        imageCarousel = view.findViewById(R.id.imageCarousel);

        chartExerciseProgress          = view.findViewById(R.id.chartExerciseProgress);
        spinnerRpeFilter               = view.findViewById(R.id.spinnerRpeFilter);

        Button btnClose                = view.findViewById(R.id.btnClose);

        // NUEVAS vistas para MovementType, EquipmentType, ExerciseType, PatternName
        textMovementType  = view.findViewById(R.id.textMovementType);
        textEquipmentType = view.findViewById(R.id.textEquipmentType);
        textExerciseType  = view.findViewById(R.id.textExerciseType);
        textPatternName   = view.findViewById(R.id.textPatternName);

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
        String exerciseId = exerciseIndex;

        if (exerciseId == null || exerciseId.isEmpty()) {
            Log.e(TAG, "⚠️ Error: ID del ejercicio no válido.");
            return view;
        }
        Log.d(TAG, "Obteniendo datos del ejercicio con ID: " + exerciseId);

        // Referencia a /userExercises/<exerciseId>
        DatabaseReference ref = databaseReference
                .child("users")
                .child(userId)
                .child("userExercises")
                .child(exerciseId);

        // Lectura del ejercicio en /userExercises/<exerciseId>
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "⚠️ Error: No se encontró el ejercicio en Firebase con ID: " + exerciseId);
                    return;
                }

                // 1) Leer datos principales
                String exerciseName      = snapshot.child("name").getValue(String.class);
                String description       = snapshot.child("description").getValue(String.class);
                String musclesWorkedStr  = snapshot.child("musclesWorked").getValue(String.class);

                // 2) Leer MovementType, EquipmentType, ExerciseType
                String movementType      = snapshot.child("movementType").getValue(String.class);
                String equipmentType     = snapshot.child("equipmentType").getValue(String.class);
                String exerciseType      = snapshot.child("exerciseType").getValue(String.class);

                // 3) Leer pesos y reps "globales"
                Integer currentLoad      = snapshot.child("currentLoad").getValue(Integer.class);
                Integer maxReps          = snapshot.child("maxReps").getValue(Integer.class);

                // 4) Leer sinergistas
                List<String> synergyList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.child("synergistMuscles").getChildren()) {
                    String muscle = ds.getValue(String.class);
                    if (muscle != null) synergyList.add(muscle);
                }

                // 5) Asignar datos a la UI
                textExerciseName.setText(exerciseName);
                textDescription.setText(description);
                textMusclesWorked.setText("Músculos trabajados: " + capitalizeWords(musclesWorkedStr));
                textSynergistMuscles.setText("Músculos sinérgicos: " + TextUtils.join(", ", synergyList));

                // 6) Lógica del carrusel 🔥
                ViewPager2 imageCarousel = getView().findViewById(R.id.imageCarousel);  // Asegúrate que exista en el layout
                DatabaseReference imagesRef = FirebaseDatabase.getInstance()
                        .getReference("exerciseImages");

                imagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("name").getValue(String.class);
                            if (name != null && name.equalsIgnoreCase(exerciseName)) {
                                Log.d(TAG, "✅ Nombre coincide para cargar imágenes extra: " + name);

                                List<Bitmap> bitmaps = new ArrayList<>();
                                for (DataSnapshot imageSnap : child.child("images").getChildren()) {
                                    String base64 = imageSnap.getValue(String.class);
                                    if (base64 != null && !base64.trim().isEmpty()) {
                                        try {
                                            byte[] bytes = android.util.Base64.decode(base64.replaceAll("\\s", ""), android.util.Base64.DEFAULT);
                                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                            if (bmp != null) {
                                                bitmaps.add(bmp);
                                                Log.d(TAG, "🖼 Imagen agregada correctamente al carrusel.");
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "❌ Error al decodificar imagen base64.", e);
                                        }
                                    }
                                }
                                carouselBitmaps.clear();
                                carouselBitmaps.addAll(bitmaps);
                                if (!bitmaps.isEmpty()) {
                                    ImagePagerAdapter adapter = new ImagePagerAdapter();
                                    adapter.submit(bitmaps);
                                    imageCarousel.setAdapter(adapter);

                                    Log.d(TAG, "📸 Carrusel de imágenes cargado correctamente con " + bitmaps.size() + " imágenes.");
                                } else {
                                    Log.w(TAG, "⚠️ No se encontraron imágenes válidas para el ejercicio.");
                                }


                                imageCarousel.setOnClickListener(v -> {
                                    int currentItem = imageCarousel.getCurrentItem();
                                    if (carouselBitmaps != null && !carouselBitmaps.isEmpty()) {
                                        Bitmap selectedImage = carouselBitmaps.get(currentItem);
                                        if (selectedImage != null) {
                                            // 🔥 Convertimos el Bitmap a Base64 para pasarlo al fragmento
                                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                            String base64Selected = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.DEFAULT);

                                            ImageFullscreenDialogFragment dialog = ImageFullscreenDialogFragment.newInstance(base64Selected);
                                            dialog.show(getParentFragmentManager(), "ImageFullscreenDialog");
                                        } else {
                                            Log.w(TAG, "⚠️ La imagen actual del carrusel es null.");
                                        }
                                    } else {
                                        Log.w(TAG, "❌ No hay imágenes cargadas en el carrusel.");
                                    }
                                });

                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error al consultar exerciseImages.", error.toException());
                    }
                });

                // 7) Mostrar info adicional
                textMovementType.setText("Tipo de Movimiento: " + (movementType != null ? movementType : "desconocido"));
                textEquipmentType.setText("Equipamiento: " + (equipmentType != null ? equipmentType : "desconocido"));
                textExerciseType.setText("Tipo de Ejercicio: " + (exerciseType != null ? exerciseType : "desconocido"));

                // 8) Calcular RM
                if (currentLoad == null || currentLoad <= 0) currentLoad = 1;
                if (maxReps == null || maxReps <= 0) maxReps = 1;
                double estimatedRM = currentLoad * (1 + (maxReps / 30.0));
                textRM.setText("RM Estimado: " + String.format("%.2f", estimatedRM) + " kg");

                // 9) Patrón de movimiento
                if (dayKey != null && !dayKey.isEmpty()) {
                    DatabaseReference patternRef = databaseReference
                            .child("users")
                            .child(userId)
                            .child("userMovementPatterns")
                            .child(dayKey)
                            .child("name");

                    patternRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String patternName = snapshot.getValue(String.class);
                                if (patternName != null && !patternName.isEmpty()) {
                                    textPatternName.setText("Patrón de Movimiento: " + patternName);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "❌ Error al leer patrón", error.toException());
                        }
                    });
                }

                // 10) Series
                fetchSeriesData(exerciseId, textPersonalRecord);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al obtener el ejercicio", error.toException());
            }
        });





        // Botón Cerrar
        btnClose.setOnClickListener(v -> {
            Log.d(TAG, "Botón de cerrar presionado, cerrando el diálogo.");
            dismiss();
        });

        // Spinner RPE
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

        // Configuraciones iniciales de la gráfica
        configureChartLookAndFeel();

        return view;
    }

    /**
     * Configura la apariencia y comportamiento inicial de la gráfica para que luzca más profesional.
     */

    /**
     * Descarga todas las series desde /userExercises/<exerciseId>/seriesData
     * y actualiza la gráfica y estadísticas.
     */
    private void fetchSeriesData(String exerciseId, TextView textPersonalRecord) {
        Log.d(TAG + " - fetchSeriesData", "📥 Iniciando descarga de series: /userExercises/" + exerciseId + "/seriesData");

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

        allSeriesData.clear();

        seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double personalRecord = 0;
                double bestRm = 0;
                ExerciseSeries bestSeries = null;

                for (DataSnapshot seriesSnapshot : dataSnapshot.getChildren()) {
                    Integer seriesNumber = seriesSnapshot.child("seriesNumber").getValue(Integer.class);
                    Integer reps = seriesSnapshot.child("reps").getValue(Integer.class);
                    Double weight = seriesSnapshot.child("weight").getValue(Double.class);
                    Double rpe = seriesSnapshot.child("rpe").getValue(Double.class);

                    if (seriesNumber != null && reps != null && weight != null && rpe != null) {
                        ExerciseSeries newSeries = new ExerciseSeries(seriesNumber, reps, weight, rpe);
                        allSeriesData.add(newSeries);
                        personalRecord = Math.max(personalRecord, weight);

                        double rm = weight * (1 + (reps / 30.0));
                        if (rm > bestRm) {
                            bestRm = rm;
                            bestSeries = newSeries;
                        }

                        Log.d(TAG, "✅ Serie cargada: #" + seriesNumber + ", Reps=" + reps + ", Peso=" + weight + ", RPE=" + rpe + ", RM=" + String.format("%.2f", rm));
                    } else {
                        Log.w(TAG, "⚠️ Datos de serie incompletos, se omite.");
                    }
                }

                Log.d(TAG, "📊 Total series descargadas: " + allSeriesData.size());

                // Personal Record
                textPersonalRecord.setText("Personal Record: " + String.format("%.2f", personalRecord) + " kg");
                textPersonalRecord.setTextColor(Color.parseColor("#BBBBBB")); // Blanco estándar

                // Volumen Total
                double totalVolume = 0.0;
                for (ExerciseSeries s : allSeriesData) {
                    totalVolume += s.getWeight() * s.getReps();
                }
                TextView textVolume = getView().findViewById(R.id.textVolume);
                textVolume.setText("Volumen Total: " + String.format("%.2f", totalVolume) + " kg");
                textVolume.setTextColor(Color.parseColor("#BBBBBB")); // Gris claro profesional

                // Mejor Serie
                TextView textBestSeries = getView().findViewById(R.id.textBestSeries);
                if (bestSeries != null) {
                    String resumen = "Mejor Serie: #" + bestSeries.getSeriesNumber()
                            + " | " + bestSeries.getWeight() + " kg x " + bestSeries.getReps()
                            + " reps (1RM: " + String.format("%.2f", bestRm) + " kg)";
                    textBestSeries.setText(resumen);
                    textBestSeries.setTextColor(Color.parseColor("#BBBBBB")); // Rojo vibrante suave, profesional
                    Log.d(TAG, "🏆 Mejor Serie detectada: " + resumen);
                } else {
                    textBestSeries.setText("Mejor Serie: No disponible");
                    textBestSeries.setTextColor(Color.parseColor("#BBBBBB"));
                    Log.w(TAG, "⚠️ No se detectó mejor serie.");
                }

                // Dibujar gráfica optimizada visualmente
                drawProgressChart(allSeriesData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al obtener datos de series", error.toException());
            }
        });
    }


    /**
     * Filtra la lista 'allSeriesData' por el RPE especificado y llama a 'drawProgressChart'.
     */
    private void filterAndDrawChartByRpe(float rpeFilter) {
        Log.d(TAG + " - filterAndDrawChartByRpe", "Filtrando por RPE: " + rpeFilter);

        if (allSeriesData.isEmpty()) {
            Log.d(TAG, "No hay series en allSeriesData para filtrar.");
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }

        List<ExerciseSeries> filteredSeries = new ArrayList<>();
        for (ExerciseSeries s : allSeriesData) {
            float rpeValue = (float) s.getRpe();
            if (rpeValue == rpeFilter) {
                filteredSeries.add(s);
            }
        }
        Log.d(TAG, "Se encontraron " + filteredSeries.size() + " series con RPE = " + rpeFilter);

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
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        float bestRM = 0;
        Entry bestEntry = null;

        for (ExerciseSeries series : seriesData) {
            float x = (float) series.getSeriesNumber();
            float y = (float) (series.getWeight() * (1 + (series.getReps() / 30.0)));
            Entry entry = new Entry(x, y);
            entries.add(entry);

            if (y > bestRM) {
                bestRM = y;
                bestEntry = entry;
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Progreso RPE");
        dataSet.setColor(Color.GREEN);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.GREEN);
        dataSet.setDrawValues(true);

        if (bestEntry != null) {
            // Pintar mejor punto en rojo
            List<Entry> bestEntries = new ArrayList<>();
            bestEntries.add(bestEntry);
            LineDataSet highlightSet = new LineDataSet(bestEntries, "Mejor Serie");
            highlightSet.setColor(Color.RED);
            highlightSet.setCircleColor(Color.RED);
            highlightSet.setCircleRadius(6f);
            highlightSet.setDrawValues(false);
            highlightSet.setDrawCircles(true);

            LineData lineData = new LineData(dataSet, highlightSet);
            chartExerciseProgress.setData(lineData);
        } else {
            chartExerciseProgress.setData(new LineData(dataSet));
        }

        chartExerciseProgress.invalidate();
    }



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

        // Leyenda
        Legend legend = chartExerciseProgress.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setForm(Legend.LegendForm.CIRCLE);

        // EJE X (Series)
        XAxis xAxis = chartExerciseProgress.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Serie " + (int) value;
            }
        });

        // EJE Y IZQUIERDO (RM Estimado)
        YAxis leftAxis = chartExerciseProgress.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f kg", value);
            }
        });

        // Desactivar eje Y derecho
        YAxis rightAxis = chartExerciseProgress.getAxisRight();
        rightAxis.setEnabled(false);
    }


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
    public static ExerciseDetailsDialogFragment newInstanceFromInternal(Exercise exercise) {
        ExerciseDetailsDialogFragment fragment = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("exercise", exercise);
        args.putBoolean("isFromInternal", true);
        fragment.setArguments(args);
        return fragment;
    }

}
