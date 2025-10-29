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
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.diets.R;
import com.example.diets.adapters.ImagePagerAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.UserExercise;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Muestra un diálogo con todos los detalles de un ejercicio y una gráfica de progreso
 * basada en las series guardadas en Firebase.
 */
public class ExerciseDetailsDialogFragment extends DialogFragment {

    private static final String ARG_EXERCISE = "exerciseIndex";
    private static final String ARG_DAY      = "dayKey";
    private static final String ARG_INTERNAL = "isFromInternal";
    private static final String ARG_UID      = "effectiveUid";   // ✅ nuevo
    private static final String TAG          = "ExerciseDetailsDialogFragment";

    /* ──────────────────────────── vistas ──────────────────────────── */
    private TextView  textExerciseName;
    private TextView  textDescription;
    private TextView  textMusclesWorked;
    private TextView  textSynergistMuscles;
    private TextView  textRM;
    private TextView  textPersonalRecord;
    private LineChart chartExerciseProgress;
    private Spinner   spinnerRpeFilter;
    private ViewPager2 imageCarousel;
    private TextView  textMovementType;
    private TextView  textEquipmentType;
    private TextView  textExerciseType;
    private TextView  textPatternName;

    /* ───────────────────────── estado ───────────────────────── */
    private DatabaseReference           databaseReference;
    private List<ExerciseSeries>        allSeriesData   = new ArrayList<>();
    private List<Bitmap>                carouselBitmaps = new ArrayList<>();
    private boolean                     isViewDestroyed = false;
    private UserExercise                internalExercise;

    /* argumentos */
    private String  exerciseIndex;
    private String  dayKey;
    private boolean isFromInternal;
    private String  uidForQueries; // ✅ UID efectivo (cliente en modo coach, o usuario actual)

    /* ────────────────────────── binding de vistas ────────────────────────── */
    private void bindViews(View view) {
        Log.d(TAG, "bindViews: buscando y referenciando vistas.");
        textExerciseName      = view.findViewById(R.id.textExerciseName);
        textDescription       = view.findViewById(R.id.textDescription);
        textMusclesWorked     = view.findViewById(R.id.textMusclesWorked);
        textSynergistMuscles  = view.findViewById(R.id.textSynergistMuscles);
        textRM                = view.findViewById(R.id.textRM);
        textPersonalRecord    = view.findViewById(R.id.textPersonalRecord);
        chartExerciseProgress = view.findViewById(R.id.chartExerciseProgress);
        spinnerRpeFilter      = view.findViewById(R.id.spinnerRpeFilter);
        imageCarousel         = view.findViewById(R.id.imageCarousel);
        textMovementType      = view.findViewById(R.id.textMovementType);
        textEquipmentType     = view.findViewById(R.id.textEquipmentType);
        textExerciseType      = view.findViewById(R.id.textExerciseType);
        textPatternName       = view.findViewById(R.id.textPatternName);
    }

    /* ───────────────────────── factory methods ───────────────────────── */
    /** Versión legacy (sin UID explícito). Mantiene compatibilidad. */
    public static ExerciseDetailsDialogFragment newInstance(String exerciseIndex,
                                                            String dayKey,
                                                            boolean isFromInternal) {
        Log.d(TAG, "newInstance: creando fragmento con argumentos.");
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE, exerciseIndex);
        args.putString(ARG_DAY, dayKey);
        args.putBoolean(ARG_INTERNAL, isFromInternal);
        // sin ARG_UID → usará el currentUser
        f.setArguments(args);
        return f;
    }

    /** Nueva versión para **modo Coach**: pasa el UID del cliente. */
    public static ExerciseDetailsDialogFragment newInstance(String exerciseIndex,
                                                            String dayKey,
                                                            boolean isFromInternal,
                                                            @Nullable String effectiveUid) {
        Log.d(TAG, "newInstance(+uid): creando fragmento con argumentos (coach compatible).");
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE, exerciseIndex);
        args.putString(ARG_DAY, dayKey);
        args.putBoolean(ARG_INTERNAL, isFromInternal);
        if (!TextUtils.isEmpty(effectiveUid)) {
            args.putString(ARG_UID, effectiveUid); // ✅
        }
        f.setArguments(args);
        return f;
    }

    /** Legacy: interno. Se mantiene igual. */
    public static ExerciseDetailsDialogFragment newInstanceFromInternal(Exercise exercise) {
        Log.d(TAG, "newInstanceFromInternal: creando fragmento para ejercicio interno.");
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("exercise", exercise);
        args.putBoolean(ARG_INTERNAL, true);
        f.setArguments(args);
        return f;
    }

    /* ───────────────────────── ciclo de vida ───────────────────────── */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: inicializando fragmento.");
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (getArguments() != null) {
            exerciseIndex    = getArguments().getString(ARG_EXERCISE);
            dayKey           = getArguments().getString(ARG_DAY);
            isFromInternal   = getArguments().getBoolean(ARG_INTERNAL, false);
            internalExercise = (UserExercise) getArguments().getSerializable("exercise");
            String argUid    = getArguments().getString(ARG_UID, null);

            // ✅ Resolver UID efectivo: primero el pasado por argumentos; si no, el usuario actual
            if (!TextUtils.isEmpty(argUid)) {
                uidForQueries = argUid;
            } else {
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                uidForQueries = (u != null) ? u.getUid() : null;
            }

            Log.d(TAG, "onCreate: args → exerciseIndex=" + exerciseIndex
                    + ", dayKey=" + dayKey
                    + ", isFromInternal=" + isFromInternal
                    + ", uidForQueries=" + uidForQueries);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView: inflando layout de diálogo.");
        View view = inflater.inflate(R.layout.dialog_exercise_details, container, false);

        bindViews(view);
        configureChartLookAndFeel();
        setupSpinner();

        /* botón cerrar */
        view.findViewById(R.id.btnClose).setOnClickListener(v -> {
            Log.d(TAG, "onCreateView: Cerrar diálogo.");
            dismiss();
        });

        if (isFromInternal && internalExercise != null) {
            Log.d(TAG, "onCreateView: Cargando datos desde ejercicio interno.");
            populateInternalUI(view, internalExercise);
        } else {
            Log.d(TAG, "onCreateView: Cargando datos desde Firebase.");
            populateFirebaseUI(view);
        }

        if (!TextUtils.isEmpty(uidForQueries) && !TextUtils.isEmpty(dayKey)) {
            loadMovementPattern(view, uidForQueries);   // ✅ siempre con UID efectivo
        } else {
            Log.d(TAG, "Patrón de movimiento omitido (uidForQueries o dayKey nulo)");
        }

        return view;
    }

    public void setInternalExercise(UserExercise ue) { this.internalExercise = ue; }

    /* ───────────────────────── población UI: ejercicio interno ───────────────────────── */
    private void populateInternalUI(View view, Exercise ex) {

        Log.d(TAG, "populateInternalUI: poblando UI desde ejercicio interno.");

        /* textos básicos */
        textExerciseName    .setText(ex.getName());
        textDescription     .setText(ex.getDescription());
        textMusclesWorked   .setText("Músculos trabajados: " + capitalizeWords(ex.getMusclesWorked()));
        textSynergistMuscles.setText("Músculos sinérgicos: " + TextUtils.join(", ", ex.getSynergistMuscles()));
        textMovementType    .setText("Tipo de Movimiento: " + ex.getMovementType());
        textEquipmentType   .setText("Equipamiento: " + ex.getEquipmentType());
        textExerciseType    .setText("Tipo de Ejercicio: " + ex.getExerciseType());

        Log.d(TAG, "[INT] Datos recibidos → "
                + "name=" + ex.getName()
                + " | movementType="  + ex.getMovementType()
                + " | equip="         + ex.getEquipmentType()
                + " | exerciseType="  + ex.getExerciseType()
                + " | muscles="       + ex.getMusclesWorked()
                + " | synergists="    + ex.getSynergistMuscles()
                + " | images#="       + (ex.getImages() != null ? ex.getImages().size() : 0));

        /* imágenes */
        List<String> base64List = ex.getImages();
        if (base64List == null || base64List.isEmpty()) {
            String single = ex.getImage();
            if (single != null && !single.trim().isEmpty()) {
                base64List = new ArrayList<>();
                base64List.add(single);
            }
        }
        if (base64List != null && !base64List.isEmpty()) {
            Log.d(TAG, "populateInternalUI: cargando imágenes internas.");
            cargarImagenesDesdeListaInterna(base64List);
        } else {
            Log.w(TAG, "populateInternalUI: ⚠️ No hay imágenes internas para: " + ex.getName());
        }

        fetchSeriesDataPorNombre(ex.getName(), textPersonalRecord);
    }

    /* ───────────────────────── población UI: Firebase ───────────────────────── */
    private void populateFirebaseUI(View view) {

        Log.d(TAG, "populateFirebaseUI: poblando UI desde Firebase.");
        if (TextUtils.isEmpty(uidForQueries) || TextUtils.isEmpty(exerciseIndex)) {
            Log.e(TAG, "populateFirebaseUI: uidForQueries o exerciseIndex inválido.");
            dismiss();
            return;
        }

        DatabaseReference ref = databaseReference
                .child("users")
                .child(uidForQueries)                  // ✅ UID efectivo
                .child("userExercises")
                .child(exerciseIndex);

        Log.d(TAG, "populateFirebaseUI: 🌐 Cargando ejercicio Firebase: " + exerciseIndex);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {

                Log.d(TAG, "populateFirebaseUI:onDataChange: datos recibidos de Firebase.");
                if (isViewDestroyed || getView() == null) return;

                Exercise ex = snap.getValue(Exercise.class);
                if (ex == null) {
                    Log.e(TAG, "populateFirebaseUI:onDataChange: ❌ Ejercicio no encontrado en Firebase");
                    dismiss();
                    return;
                }

                bindFirebaseExercise(ex);
                cargarImagenesFirebase(ex.getName());
                fetchSeriesData(exerciseIndex, textPersonalRecord);
                loadMovementPattern(view, uidForQueries);

                double rm = computeEstimatedRm(snap);
                textRM.setText("RM Estimado: " + String.format("%.2f", rm) + " kg");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "populateFirebaseUI:onCancelled: ❌ Error Firebase", error.toException());
                dismiss();
            }
        });
    }

    /* ───────────────────────── asigna campos básicos Firebase ───────────────────────── */
    private void bindFirebaseExercise(Exercise ex) {

        Log.d(TAG, "bindFirebaseExercise: asignando datos básicos a vistas.");
        textExerciseName.setText(ex.getName());
        textDescription .setText(ex.getDescription());
        textMusclesWorked.setText("Músculos trabajados: " + capitalizeWords(ex.getMusclesWorked()));
        textSynergistMuscles.setText("Músculos sinérgicos: " + TextUtils.join(", ", ex.getSynergistMuscles()));
        textMovementType .setText("Tipo de Movimiento: " + ex.getMovementType());
        textEquipmentType.setText("Equipamiento: " + ex.getEquipmentType());
        textExerciseType .setText("Tipo de Ejercicio: " + ex.getExerciseType());

        Log.d(TAG, "[FB]  Datos recibidos → "
                + "name=" + ex.getName()
                + " | movementType="  + ex.getMovementType()
                + " | equip="         + ex.getEquipmentType()
                + " | exerciseType="  + ex.getExerciseType()
                + " | muscles="       + ex.getMusclesWorked()
                + " | synergists="    + ex.getSynergistMuscles());
    }

    /* ───────────────────────── patrón de movimiento ───────────────────────── */
    private void loadMovementPattern(View view, String uid) {

        Log.d(TAG, "loadMovementPattern: buscando patrón de movimiento para dayKey=" + dayKey);
        if (dayKey == null || dayKey.isEmpty() || TextUtils.isEmpty(uid)) return;

        databaseReference.child("users")
                .child(uid)                             // ✅ UID efectivo
                .child("userMovementPatterns")
                .child(dayKey)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        if (isViewDestroyed || getView() == null) return;
                        String nm = ds.getValue(String.class);
                        if (nm != null) {
                            textPatternName.setText("Patrón: " + nm);
                            Log.d(TAG, "loadMovementPattern:onDataChange: patrón encontrado: " + nm);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "loadMovementPattern:onCancelled", e.toException());
                    }
                });
    }

    /* ───────────────────────── RM estimado ───────────────────────── */
    private double computeEstimatedRm(DataSnapshot snap) {

        Log.d(TAG, "computeEstimatedRm: calculando RM estimado.");
        Integer currentLoad = snap.child("currentLoad").getValue(Integer.class);
        Integer maxReps     = snap.child("maxReps")    .getValue(Integer.class);
        if (currentLoad == null || currentLoad <= 0) currentLoad = 1;
        if (maxReps     == null || maxReps     <= 0) maxReps     = 1;
        double rm = currentLoad * (1 + (maxReps / 30.0));
        Log.d(TAG, "computeEstimatedRm: RM=" + rm
                + " (carga=" + currentLoad + ", reps=" + maxReps + ")");
        return rm;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: liberando recursos y marcando vista destruida.");
        super.onDestroyView();
        isViewDestroyed = true;
    }

    /* ───────────────────────── spinner RPE ───────────────────────── */
    private void setupSpinner() {

        Log.d(TAG, "setupSpinner: configurando spinner de RPE.");
        spinnerRpeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent,
                                                 View v, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "setupSpinner:onItemSelected: Se seleccionó RPE: " + selected);
                try {
                    float rpeValue = Float.parseFloat(selected);
                    filterAndDrawChartByRpe(rpeValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "setupSpinner:onItemSelected: Error parseando RPE", e);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "setupSpinner:onNothingSelected: No se seleccionó RPE.");
            }
        });
    }

    /* ───────────────────────── imágenes internas ───────────────────────── */
    private void cargarImagenesDesdeListaInterna(List<String> base64Images) {

        Log.d(TAG, "cargarImagenesDesdeListaInterna: decodificando y mostrando imágenes internas.");
        if (isViewDestroyed) {
            Log.w(TAG, "cargarImagenesDesdeListaInterna: cancelado, vista destruida.");
            return;
        }
        carouselBitmaps.clear();

        for (String base64 : base64Images) {
            if (base64 == null || base64.trim().isEmpty()) continue;
            try {
                byte[] decoded = Base64.decode(base64.replaceAll("\\s", ""), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) carouselBitmaps.add(bmp);
            } catch (Exception e) {
                Log.e(TAG, "cargarImagenesDesdeListaInterna: error decodificando imagen", e);
            }
        }

        if (!carouselBitmaps.isEmpty() && imageCarousel != null) {
            ImagePagerAdapter adapter = new ImagePagerAdapter();
            adapter.submit(carouselBitmaps);
            imageCarousel.setAdapter(adapter);
            Log.d(TAG, "cargarImagenesDesdeListaInterna: imágenes internas cargadas: "
                    + carouselBitmaps.size());
        } else {
            Log.w(TAG, "cargarImagenesDesdeListaInterna: no hay imágenes válidas.");
        }
    }

    /* ───────────────────────── descarga series (por key) ───────────────────────── */
    private void fetchSeriesData(String exerciseId, TextView textPersonalRecord) {

        Log.d(TAG, "fetchSeriesData: Iniciando descarga de series para: " + exerciseId);
        if (TextUtils.isEmpty(uidForQueries)) {
            Log.e(TAG, "fetchSeriesData: uidForQueries vacío.");
            return;
        }

        DatabaseReference seriesRef = databaseReference
                .child("users")
                .child(uidForQueries)                 // ✅ UID efectivo
                .child("userExercises")
                .child(exerciseId)
                .child("seriesData");

        allSeriesData.clear();

        seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Log.d(TAG, "fetchSeriesData:onDataChange: Series descargadas.");
                if (isViewDestroyed || getView() == null) {
                    Log.w(TAG, "fetchSeriesData:onDataChange: vista destruida, se ignora.");
                    return;
                }

                double personalRecord = 0;
                double bestRm = 0;
                ExerciseSeries bestSeries = null;

                for (DataSnapshot seriesSnapshot : dataSnapshot.getChildren()) {

                    Integer seriesNumber = seriesSnapshot.child("seriesNumber").getValue(Integer.class);
                    Integer reps        = seriesSnapshot.child("reps").getValue(Integer.class);
                    Double  weight      = seriesSnapshot.child("weight").getValue(Double.class);
                    Double  rpe         = seriesSnapshot.child("rpe").getValue(Double.class);

                    if (seriesNumber != null && reps != null && weight != null && rpe != null) {
                        ExerciseSeries newSeries = new ExerciseSeries(seriesNumber, reps, weight, rpe);
                        allSeriesData.add(newSeries);
                        personalRecord = Math.max(personalRecord, weight);

                        double rm = weight * (1 + (reps / 30.0));
                        if (rm > bestRm) {
                            bestRm = rm;
                            bestSeries = newSeries;
                        }

                        Log.d(TAG, "Serie cargada: #" + seriesNumber + " | "
                                + weight + " kg x " + reps + " reps | RPE=" + rpe
                                + " | 1RM=" + String.format("%.2f", rm));
                    }
                }

                /* texto PR */
                textPersonalRecord.setText("Personal Record: "
                        + String.format("%.2f", personalRecord) + " kg");
                textPersonalRecord.setTextColor(Color.parseColor("#BBBBBB"));

                /* volumen total */
                TextView textVolume = getView().findViewById(R.id.textVolume);
                if (textVolume != null) {
                    double totalVolume = 0;
                    for (ExerciseSeries s : allSeriesData)
                        totalVolume += s.getWeight() * s.getReps();
                    textVolume.setText("Volumen Total: "
                            + String.format("%.2f", totalVolume) + " kg");
                    textVolume.setTextColor(Color.parseColor("#BBBBBB"));
                }

                /* mejor serie */
                TextView textBestSeries = getView().findViewById(R.id.textBestSeries);
                if (textBestSeries != null) {
                    if (bestSeries != null) {
                        String resumen = "Mejor Serie: #" + bestSeries.getSeriesNumber()
                                + " | " + bestSeries.getWeight()
                                + " kg x " + bestSeries.getReps()
                                + " reps (1RM: " + String.format("%.2f", bestRm) + " kg)";
                        textBestSeries.setText(resumen);
                    } else {
                        textBestSeries.setText("Mejor Serie: No disponible");
                    }
                    textBestSeries.setTextColor(Color.parseColor("#BBBBBB"));
                }

                if (textRM != null) {
                    textRM.setText("RM Estimado: " + String.format("%.2f", bestRm) + " kg");
                }

                drawProgressChart(allSeriesData);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "fetchSeriesData:onCancelled", error.toException());
            }
        });
    }

    /* ───────────────────────── descarga series (por nombre) ───────────────────────── */
    private void fetchSeriesDataPorNombre(String nombreEjercicio,
                                          TextView textPersonalRecord) {

        if (TextUtils.isEmpty(uidForQueries)) {
            Log.e(TAG, "fetchSeriesDataPorNombre: uidForQueries vacío.");
            return;
        }

        DatabaseReference ref = databaseReference
                .child("users")
                .child(uidForQueries)                 // ✅ UID efectivo
                .child("userExercises");

        ref.orderByChild("name").equalTo(nombreEjercicio)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        for (DataSnapshot match : ds.getChildren()) {
                            String key = match.getKey();
                            if (key != null) {
                                fetchSeriesData(key, textPersonalRecord);
                                return;
                            }
                        }
                        Log.w(TAG, "❌ No se encontraron series para «" + nombreEjercicio + "»");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "fetchSeriesDataPorNombre:onCancelled", e.toException());
                    }
                });
    }

    /* ───────────────────────── filtrar y dibujar gráfica ───────────────────────── */
    private void filterAndDrawChartByRpe(float rpeFilter) {

        Log.d(TAG, "filterAndDrawChartByRpe: filtrando por RPE " + rpeFilter);
        if (allSeriesData.isEmpty()) {
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }

        List<ExerciseSeries> filtered = new ArrayList<>();
        for (ExerciseSeries s : allSeriesData)
            if ((float) s.getRpe() == rpeFilter) filtered.add(s);

        drawProgressChart(filtered);
    }

    private void drawProgressChart(List<ExerciseSeries> seriesData) {

        Log.d(TAG, "drawProgressChart: dibujando gráfica (" + seriesData.size() + " series)");
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

        for (ExerciseSeries s : seriesData) {
            float x = s.getSeriesNumber();
            float y = (float) (s.getWeight() * (1 + (s.getReps() / 30.0)));
            Entry e = new Entry(x, y);
            entries.add(e);
            if (y > bestRM) { bestRM = y; bestEntry = e; }
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
            List<Entry> bestList = new ArrayList<>();
            bestList.add(bestEntry);
            LineDataSet highlight = new LineDataSet(bestList, "Mejor Serie");
            highlight.setColor(Color.RED);
            highlight.setCircleColor(Color.RED);
            highlight.setCircleRadius(6f);
            highlight.setDrawValues(false);
            chartExerciseProgress.setData(new LineData(dataSet, highlight));
        } else {
            chartExerciseProgress.setData(new LineData(dataSet));
        }
        chartExerciseProgress.invalidate();
    }

    /* ───────────────────────── look & feel LineChart ───────────────────────── */
    private void configureChartLookAndFeel() {

        Log.d(TAG, "configureChartLookAndFeel: aplicando estilo.");
        Description d = new Description();
        d.setText("Progreso de Cargas (RM Estimado)");
        d.setTextColor(Color.WHITE);
        d.setTextSize(12f);
        chartExerciseProgress.setDescription(d);

        chartExerciseProgress.setDrawGridBackground(false);
        chartExerciseProgress.setBackgroundColor(Color.TRANSPARENT);
        chartExerciseProgress.setDragEnabled(true);
        chartExerciseProgress.setScaleEnabled(true);
        chartExerciseProgress.setPinchZoom(true);

        Legend legend = chartExerciseProgress.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setForm(Legend.LegendForm.CIRCLE);

        XAxis xAxis = chartExerciseProgress.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return "Serie " + (int) v; }
        });

        YAxis left = chartExerciseProgress.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setDrawGridLines(true);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return String.format("%.0f kg", v); }
        });
        chartExerciseProgress.getAxisRight().setEnabled(false);
    }

    /* ───────────────────────── aux ───────────────────────── */
    private String capitalizeWords(String text) {

        if (text == null || text.isEmpty()) return "";
        String[] w = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String s : w) {
            if (s.length() > 1)
                sb.append(Character.toUpperCase(s.charAt(0)))
                        .append(s.substring(1).toLowerCase()).append(" ");
            else
                sb.append(s.toUpperCase()).append(" ");
        }
        return sb.toString().trim();
    }

    /* ───────────────────────── imágenes Firebase ───────────────────────── */
    private void cargarImagenesFirebase(String nombreEjercicio) {

        Log.d(TAG, "cargarImagenesFirebase: iniciando para " + nombreEjercicio);
        DatabaseReference imagesRef = FirebaseDatabase.getInstance()
                .getReference("exerciseImages");

        imagesRef.orderByChild("name").equalTo(nombreEjercicio)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isViewDestroyed || getView() == null) return;

                        for (DataSnapshot data : snapshot.getChildren()) {
                            DataSnapshot imagesNode = data.child("images");

                            List<String> base64List = new ArrayList<>();
                            for (DataSnapshot imgSnap : imagesNode.getChildren()) {
                                String b64 = imgSnap.getValue(String.class);
                                if (b64 != null && !b64.trim().isEmpty())
                                    base64List.add(b64);
                            }

                            if (!base64List.isEmpty()) {
                                Log.d(TAG, "cargarImagenesFirebase: encontradas "
                                        + base64List.size() + " imágenes.");
                                cargarImagenesDesdeListaInterna(base64List);
                            } else {
                                Log.w(TAG, "cargarImagenesFirebase: sin imágenes válidas.");
                            }
                            break;
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "cargarImagenesFirebase:onCancelled", error.toException());
                    }
                });
    }
}
