package com.example.diets.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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
    private static final String ARG_EXERCISE = "exerciseIndex";
    private static final String ARG_DAY     = "dayKey";
    private static final String ARG_INTERNAL= "isFromInternal";
    private static final String TAG         = "ExerciseDetailsDialog";
    private TextView textExerciseName;
    private TextView textDescription;
    private TextView textMusclesWorked;
    private TextView textSynergistMuscles;
    private TextView textRM;
    private TextView textPersonalRecord;
    private LineChart chartExerciseProgress;
    private Spinner spinnerRpeFilter;
    private ViewPager2 imageCarousel;
    private TextView textMovementType;
    private TextView textEquipmentType;
    private TextView textExerciseType;
    private TextView textPatternName;


    private DatabaseReference databaseReference;
    private List<ExerciseSeries> allSeriesData = new ArrayList<>();
    private List<Bitmap>         carouselBitmaps = new ArrayList<>();
    private boolean              isViewDestroyed = false;

    // argumentos
    private String  exerciseIndex;
    private String  dayKey;
    private boolean isFromInternal;
    private Exercise internalExercise;
    private Exercise firebaseExercise;
    /** Busca todas las vistas una vez */
    private void bindViews(View view) {
        textExerciseName       = view.findViewById(R.id.textExerciseName);
        textDescription        = view.findViewById(R.id.textDescription);
        textMusclesWorked      = view.findViewById(R.id.textMusclesWorked);
        textSynergistMuscles   = view.findViewById(R.id.textSynergistMuscles);
        textRM                 = view.findViewById(R.id.textRM);
        textPersonalRecord     = view.findViewById(R.id.textPersonalRecord);
        chartExerciseProgress  = view.findViewById(R.id.chartExerciseProgress);
        spinnerRpeFilter       = view.findViewById(R.id.spinnerRpeFilter);
        imageCarousel          = view.findViewById(R.id.imageCarousel);
        textMovementType       = view.findViewById(R.id.textMovementType);
        textEquipmentType      = view.findViewById(R.id.textEquipmentType);
        textExerciseType       = view.findViewById(R.id.textExerciseType);
        textPatternName        = view.findViewById(R.id.textPatternName);
    }

    public static ExerciseDetailsDialogFragment newInstance(String exerciseIndex, String dayKey, boolean isFromInternal) {
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE, exerciseIndex);
        args.putString(ARG_DAY, dayKey);
        args.putBoolean(ARG_INTERNAL, isFromInternal);
        f.setArguments(args);
        return f;
    }

    public static ExerciseDetailsDialogFragment newInstanceFromInternal(Exercise exercise) {
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("exercise", exercise);
        args.putBoolean(ARG_INTERNAL, true);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (getArguments() != null) {
            exerciseIndex   = getArguments().getString(ARG_EXERCISE);
            dayKey          = getArguments().getString(ARG_DAY);
            isFromInternal  = getArguments().getBoolean(ARG_INTERNAL, false);
            internalExercise= (Exercise) getArguments().getSerializable("exercise");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup  container,
                             @Nullable Bundle     savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_exercise_details, container, false);

        // 1) Referencias a vistas
        bindViews(view);

        // 2) Configuración común
        configureChartLookAndFeel();
        setupSpinner();
        view.findViewById(R.id.btnClose).setOnClickListener(v -> {
            Log.d(TAG, "Cerrar diálogo");
            dismiss();
        });

        // 3) Poblamos la UI según procedencia de datos
        if (isFromInternal && internalExercise != null) {
            populateInternalUI(view, internalExercise);
        } else {
            populateFirebaseUI(view);
        }

        return view;
    }
    /** Población desde ejercicio interno */
    private void populateInternalUI(View view, Exercise ex) {
        // Referencias (si no usas bindViews para todas, cámbialo aquí)
        TextView textExerciseName     = view.findViewById(R.id.textExerciseName);
        TextView textDescription      = view.findViewById(R.id.textDescription);
        TextView textMusclesWorked    = view.findViewById(R.id.textMusclesWorked);
        TextView textSynergistMuscles = view.findViewById(R.id.textSynergistMuscles);
        textMovementType              = view.findViewById(R.id.textMovementType);
        textEquipmentType             = view.findViewById(R.id.textEquipmentType);
        textExerciseType              = view.findViewById(R.id.textExerciseType);

        // Datos estáticos
        textExerciseName.setText(ex.getName());
        textDescription.setText(ex.getDescription());
        textMusclesWorked.setText("Músculos trabajados: " + capitalizeWords(ex.getMusclesWorked()));
        textSynergistMuscles.setText("Músculos sinérgicos: " + TextUtils.join(", ", ex.getSynergistMuscles()));
        textMovementType.setText("Tipo de Movimiento: " + ex.getMovementType());
        textEquipmentType.setText("Equipamiento: " + ex.getEquipmentType());
        textExerciseType.setText("Tipo de Ejercicio: " + ex.getExerciseType());

        // Imágenes (fallback a ex.getImage() si la lista está vacía)
        List<String> base64List = ex.getImages();
        if (base64List == null || base64List.isEmpty()) {
            // si tienes un único campo `image`, úsalo
            String single = ex.getImage();      // método @Deprecated que devuelve la 1ª imagen
            if (single != null && !single.trim().isEmpty()) {
                base64List = new ArrayList<>();
                base64List.add(single);
            }
        }

        if (base64List != null && !base64List.isEmpty()) {
            cargarImagenesDesdeListaInterna(base64List);
        } else {
            Log.w(TAG, "⚠️ No hay imágenes internas para: " + ex.getName());
            // opcionalmente pon un placeholder:
            // imageCarousel.setVisibility(View.GONE);
        }


        // Ocultamos toda la sección de Firebase
        chartExerciseProgress.setVisibility(View.GONE);
        spinnerRpeFilter.setVisibility(View.GONE);
        textPersonalRecord .setVisibility(View.GONE);
        view.findViewById(R.id.textVolume)    .setVisibility(View.GONE);
        view.findViewById(R.id.textBestSeries).setVisibility(View.GONE);
    }

    /** Población desde Firebase */
    private void populateFirebaseUI(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || exerciseIndex == null || exerciseIndex.isEmpty()) {
            Log.e(TAG, "No hay usuario o ID inválido");
            dismiss();
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = databaseReference
                .child("users")
                .child(uid)
                .child("userExercises")
                .child(exerciseIndex);

        Log.d(TAG, "🌐 Cargando ejercicio Firebase: " + exerciseIndex);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (isViewDestroyed || getView() == null) return;

                Exercise ex = snap.getValue(Exercise.class);
                if (ex == null) {
                    Log.e(TAG, "❌ Ejercicio no encontrado en Firebase");
                    dismiss();
                    return;
                }

                // Bind básico
                bindFirebaseExercise(ex);

                // Imágenes
                cargarImagenesFirebase(ex.getName());

                // Series y gráfico
                fetchSeriesData(exerciseIndex, textPersonalRecord);

                // Patrón de movimiento
                loadMovementPattern(view, uid);

                // RM estimado
                double rm = computeEstimatedRm(snap);
                ((TextView)view.findViewById(R.id.textRM))
                        .setText("RM Estimado: " + String.format("%.2f", rm) + " kg");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error Firebase", error.toException());
                dismiss();
            }
        });
    }

    /** Extrae y asigna los campos básicos de Firebase a las vistas */
    private void bindFirebaseExercise(Exercise ex) {
        ((TextView)getView().findViewById(R.id.textExerciseName))
                .setText(ex.getName());
        ((TextView)getView().findViewById(R.id.textDescription))
                .setText(ex.getDescription());
        ((TextView)getView().findViewById(R.id.textMusclesWorked))
                .setText("Músculos trabajados: " + capitalizeWords(ex.getMusclesWorked()));
        ((TextView)getView().findViewById(R.id.textSynergistMuscles))
                .setText("Músculos sinérgicos: " + TextUtils.join(", ", ex.getSynergistMuscles()));
        textMovementType.setText("Tipo de Movimiento: " + ex.getMovementType());
        textEquipmentType.setText("Equipamiento: " + ex.getEquipmentType());
        textExerciseType.setText("Tipo de Ejercicio: " + ex.getExerciseType());
    }

    /** Carga el nombre del patrón si hay dayKey */
    private void loadMovementPattern(View view, String uid) {
        if (dayKey == null || dayKey.isEmpty()) return;
        databaseReference.child("users")
                .child(uid)
                .child("userMovementPatterns")
                .child(dayKey)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        if (isViewDestroyed || getView() == null) return;
                        String nm = ds.getValue(String.class);
                        if (nm != null) {
                            textPatternName.setText("Patrón: " + nm);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e){}
                });
    }

    /** Calcula el RM estimado a partir del DataSnapshot */
    private double computeEstimatedRm(DataSnapshot snap) {
        Integer currentLoad = snap.child("currentLoad").getValue(Integer.class);
        Integer maxReps     = snap.child("maxReps")    .getValue(Integer.class);
        if (currentLoad == null || currentLoad <= 0) currentLoad = 1;
        if (maxReps     == null || maxReps     <= 0) maxReps     = 1;
        return currentLoad * (1 + (maxReps / 30.0));
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewDestroyed = true;
    }
    private void setupSpinner() {
        spinnerRpeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                Log.d(TAG + " - SpinnerRpe", "Se seleccionó RPE: " + selected);
                try {
                    float rpeValue = Float.parseFloat(selected);
                    filterAndDrawChartByRpe(rpeValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG + " - SpinnerRpe", "Error parseando RPE: " + e.getMessage());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG + " - SpinnerRpe", "No se seleccionó RPE.");
            }
        });
    }

    /**
     * Decodifica una lista de Base64 y la muestra en el ViewPager2 (carrusel).
     */
    private void cargarImagenesDesdeListaInterna(List<String> base64Images) {
        if (isViewDestroyed) {
            Log.w(TAG, "⚠️ cargarImagenesDesdeListaInterna cancelado: vista destruida.");
            return;
        }
        carouselBitmaps.clear();
        for (String base64 : base64Images) {
            if (base64 == null || base64.trim().isEmpty()) continue;
            try {
                byte[] decoded = Base64.decode(base64.replaceAll("\\s",""), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) carouselBitmaps.add(bmp);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error decodificando imagen interna", e);
            }
        }
        if (!carouselBitmaps.isEmpty() && imageCarousel != null) {
            ImagePagerAdapter adapter = new ImagePagerAdapter();
            adapter.submit(carouselBitmaps);
            imageCarousel.setAdapter(adapter);
            Log.d(TAG, "✅ Imágenes internas cargadas: " + carouselBitmaps.size());
        } else {
            Log.w(TAG, "⚠️ No hay imágenes internas válidas o carrusel no inicializado.");
        }
    }
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

        // Validar lista antes de limpiar
        if (allSeriesData == null) {
            allSeriesData = new ArrayList<>();
        } else {
            allSeriesData.clear();
        }

        seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isViewDestroyed || getView() == null) {
                    Log.w(TAG, "⚠️ Callback de Firebase ignorado porque el fragmento ya fue destruido.");
                    return;
                }

                if (getView() == null) {
                    Log.w(TAG, "⚠️ El fragmento ya no está en vista (getView() es null). Cancelando actualización.");
                    return;
                }

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
                textPersonalRecord.setTextColor(Color.parseColor("#BBBBBB"));

                // Volumen Total
                TextView textVolume = getView().findViewById(R.id.textVolume);
                if (textVolume != null) {
                    double totalVolume = 0.0;
                    for (ExerciseSeries s : allSeriesData) {
                        totalVolume += s.getWeight() * s.getReps();
                    }
                    textVolume.setText("Volumen Total: " + String.format("%.2f", totalVolume) + " kg");
                    textVolume.setTextColor(Color.parseColor("#BBBBBB"));
                }

                // Mejor Serie
                TextView textBestSeries = getView().findViewById(R.id.textBestSeries);
                if (textBestSeries != null) {
                    if (bestSeries != null) {
                        String resumen = "Mejor Serie: #" + bestSeries.getSeriesNumber()
                                + " | " + bestSeries.getWeight() + " kg x " + bestSeries.getReps()
                                + " reps (1RM: " + String.format("%.2f", bestRm) + " kg)";
                        textBestSeries.setText(resumen);
                        textBestSeries.setTextColor(Color.parseColor("#BBBBBB"));
                        Log.d(TAG, "🏆 Mejor Serie detectada: " + resumen);
                    } else {
                        textBestSeries.setText("Mejor Serie: No disponible");
                        textBestSeries.setTextColor(Color.parseColor("#BBBBBB"));
                        Log.w(TAG, "⚠️ No se detectó mejor serie.");
                    }
                }

                // Dibujar gráfica
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

    private void cargarImagenesFirebase(String nombreEjercicio) {
        Log.d(TAG, "📥 Iniciando carga de imágenes para: " + nombreEjercicio);

        DatabaseReference imagesRef = FirebaseDatabase.getInstance()
                .getReference("exerciseImages");

        imagesRef.orderByChild("name").equalTo(nombreEjercicio)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isViewDestroyed || getView() == null) {
                            Log.w(TAG, "⚠️ Callback de Firebase ignorado porque el fragmento ya fue destruido.");
                            return;
                        }

                        for (DataSnapshot data : snapshot.getChildren()) {
                            DataSnapshot imagesNode = data.child("images");

                            List<String> base64List = new ArrayList<>();
                            for (DataSnapshot imageSnap : imagesNode.getChildren()) {
                                String base64 = imageSnap.getValue(String.class);
                                if (base64 != null && !base64.trim().isEmpty()) {
                                    base64List.add(base64);
                                }
                            }

                            if (!base64List.isEmpty()) {
                                Log.d(TAG, "🖼 Se encontraron " + base64List.size() + " imágenes en Firebase.");
                                cargarImagenesDesdeListaInterna(base64List); // Usa tu método ya existente
                            } else {
                                Log.w(TAG, "⚠️ No se encontraron imágenes válidas para " + nombreEjercicio);
                            }

                            break; // solo se usa el primer match
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error al cargar imágenes desde Firebase", error.toException());
                    }
                });
    }


}
