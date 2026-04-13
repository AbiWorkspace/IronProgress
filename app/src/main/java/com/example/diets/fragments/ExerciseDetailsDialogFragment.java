package com.example.diets.fragments;

import android.app.Dialog;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.diets.R;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ExerciseDetailsDialogFragment — versión robusta
 * - Flujo LOCAL (almacenamiento interno) si trae objeto Exercise.
 * - Flujo REMOTO (Firebase) si trae exerciseIndex.
 * - Si faltan ambos, se registra error y se cierra.
 */
public class ExerciseDetailsDialogFragment extends DialogFragment {

    private static final String TAG = "ExerciseDetailsDialog";

    /* ====== ARG KEYS ====== */
    private static final String ARG_EXERCISE_INDEX = "exerciseIndex";   // key de userExercises
    private static final String ARG_DAY            = "dayKey";
    private static final String ARG_INTERNAL       = "isFromInternal";
    private static final String ARG_UID            = "effectiveUid";
    private static final String ARG_EXERCISE_OBJ   = "exercise";         // Serializable Exercise

    /* ====== Parámetros de auditoría 6.4 ====== */
    private static final int  IMAGE_MAX_DIM          = 1920;               // ≈1080p
    private static final int  BASE64_THRESHOLD_BYTES = 1_500_000;          // 1.5 MB
    private static final int  LRU_CACHE_MAX_BYTES    = 12 * 1024 * 1024;   // ~12 MB

    // Cache LRU por proceso (≈12MB)
    private static final LruCache<String, Bitmap> sBitmapCache =
            new LruCache<String, Bitmap>(LRU_CACHE_MAX_BYTES) {
                @Override protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                    return value.getByteCount();
                }
            };

    /* ─────────── Vistas ─────────── */
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

    /* ─────────── Estado ─────────── */
    private DatabaseReference          databaseReference;
    private final List<ExerciseSeries> allSeriesData    = new ArrayList<>();
    private final List<Bitmap>         carouselBitmaps  = new ArrayList<>();
    private final Set<String>          imageKeys        = new HashSet<>();
    private boolean                    isViewDestroyed  = false;

    private ImagePagerAdapter imageAdapter;

    /* ─────────── Argumentos ─────────── */
    private String  exerciseIndex;          // remoto
    private String  dayKey;
    private boolean isFromInternal;         // indicador original (se recalcula)
    private String  uidForQueries;
    private Exercise internalExercise;      // local

    /* ===================== Factories recomendados ===================== */

    /** REMOTO (Firebase) */
    public static ExerciseDetailsDialogFragment newRemote(@NonNull String exerciseIndex,
                                                          @Nullable String dayKey,
                                                          @Nullable String effectiveUid) {
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE_INDEX, exerciseIndex);
        if (!TextUtils.isEmpty(dayKey))       args.putString(ARG_DAY, dayKey);
        if (!TextUtils.isEmpty(effectiveUid)) args.putString(ARG_UID, effectiveUid);
        args.putBoolean(ARG_INTERNAL, false);
        f.setArguments(args);
        return f;
    }

    /** LOCAL (almacenamiento interno) */
    public static ExerciseDetailsDialogFragment newLocal(@NonNull Exercise exercise,
                                                         @Nullable String dayKey,
                                                         @Nullable String effectiveUid) {
        ExerciseDetailsDialogFragment f = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EXERCISE_OBJ, exercise);
        if (!TextUtils.isEmpty(dayKey))       args.putString(ARG_DAY, dayKey);
        if (!TextUtils.isEmpty(effectiveUid)) args.putString(ARG_UID, effectiveUid);
        args.putBoolean(ARG_INTERNAL, true);
        f.setArguments(args);
        return f;
    }

    /* ====== Factories legacy (compat) ====== */
    /** Remoto (legacy) */
    public static ExerciseDetailsDialogFragment newInstance(String exerciseIndex,
                                                            String dayKey,
                                                            boolean isFromInternal) {
        // Si alguien pasa isFromInternal=true aquí, forzamos remoto (evita confusión).
        return newRemote(exerciseIndex, dayKey, null);
    }

    /** Remoto (legacy con UID) */
    public static ExerciseDetailsDialogFragment newInstance(String exerciseIndex,
                                                            String dayKey,
                                                            boolean isFromInternal,
                                                            @Nullable String effectiveUid) {
        return newRemote(exerciseIndex, dayKey, effectiveUid);
    }

    /** Local (legacy) */
    public static ExerciseDetailsDialogFragment newInstanceFromInternal(Exercise exercise,
                                                                        @Nullable String dayKey,
                                                                        @Nullable String effectiveUid) {
        return newLocal(exercise, dayKey, effectiveUid);
    }

    /** Local (legacy simple) */
    public static ExerciseDetailsDialogFragment newInstanceFromInternal(Exercise exercise) {
        return newLocal(exercise, null, null);
    }

    public void setInternalExercise(Exercise ex) { this.internalExercise = ex; }

    /* ===================== Ciclo de vida ===================== */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        Bundle args = getArguments();
        if (args != null) {
            exerciseIndex  = args.getString(ARG_EXERCISE_INDEX);
            dayKey         = args.getString(ARG_DAY);
            isFromInternal = args.getBoolean(ARG_INTERNAL, false);

            // 🔧 IMPORTANTE:
            // Solo sobrescribir internalExercise si REALMENTE viene en el Bundle.
            if (args.containsKey(ARG_EXERCISE_OBJ)) {
                Exercise exFromArgs = (Exercise) args.getSerializable(ARG_EXERCISE_OBJ);
                if (exFromArgs != null) {
                    internalExercise = exFromArgs;
                }
            }

            String argUid = args.getString(ARG_UID, null);

            if (!TextUtils.isEmpty(argUid)) {
                uidForQueries = argUid;
            } else {
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                uidForQueries = (u != null) ? u.getUid() : null;
            }

            // Recalcular flujo: si hay objeto → LOCAL; si no y hay index → REMOTO
            if (internalExercise != null) {
                isFromInternal = true;
            } else if (!TextUtils.isEmpty(exerciseIndex)) {
                isFromInternal = false;
            }

            Log.d(TAG, "onCreate: args → exerciseIndex=" + exerciseIndex
                    + ", dayKey=" + dayKey
                    + ", isFromInternal=" + isFromInternal
                    + ", uidForQueries=" + uidForQueries
                    + ", internalExercise=" + (internalExercise != null));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_exercise_details, container, false);
        isViewDestroyed = false;

        bindViews(view);
        configureChartLookAndFeel();
        setupSpinner();
        setupCarouselAdapterIfNeeded();

        View btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        boolean handled = false;

        // 1) Flujo LOCAL si tenemos objeto
        if (internalExercise != null) {
            Log.d(TAG, "onCreateView: FLUJO LOCAL (internalExercise)");
            populateInternalUI(view, internalExercise);
            handled = true;
        }

        // 2) Si no, flujo REMOTO si hay index
        if (!handled && !TextUtils.isEmpty(exerciseIndex)) {
            Log.d(TAG, "onCreateView: FLUJO REMOTO Firebase (exerciseIndex=" + exerciseIndex + ")");
            populateFirebaseUI(view);
            handled = true;
        }

        // 3) Si nada, error claro y salir
        if (!handled) {
            Log.e(TAG, "Invocación inválida: faltan «exercise» (local) y «exerciseIndex» (remoto). "
                    + "Usa newLocal(exercise, ...) para locales o newRemote(index, ...) para Firebase.");
            dismiss();
            return view;
        }

        // Patrón (si tenemos uid y dayKey)
        if (!TextUtils.isEmpty(uidForQueries) && !TextUtils.isEmpty(dayKey)) {
            loadMovementPattern(view, uidForQueries);
        } else {
            Log.d(TAG, "Patrón omitido (uidForQueries o dayKey vacío)");
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewDestroyed = true;
    }

    /* ===================== Binding ===================== */
    private void bindViews(View view) {
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

    /* ===================== UI: LOCAL ===================== */
    private void populateInternalUI(View view, Exercise ex) {
        String name          = ex.getName() != null ? ex.getName() : "";
        String description   = ex.getDescription() != null ? ex.getDescription() : "Sin descripción";
        String musclesWorked = ex.getMusclesWorked() != null ? ex.getMusclesWorked() : "";
        List<String> synList = ex.getSynergistMuscles();
        String movementType  = ex.getMovementType() != null ? ex.getMovementType() : "No especificado";
        String equipmentType = ex.getEquipmentType() != null ? ex.getEquipmentType() : "No especificado";
        String exerciseType  = ex.getExerciseType() != null ? ex.getExerciseType() : "No especificado";

        textExerciseName     .setText(name);
        textDescription      .setText(description);
        textMusclesWorked    .setText("Músculos trabajados: " + capitalizeWords(musclesWorked));
        textSynergistMuscles .setText("Músculos sinérgicos: " +
                ((synList != null && !synList.isEmpty())
                        ? TextUtils.join(", ", synList)
                        : "No especificados"));
        textMovementType     .setText("Tipo de Movimiento: " + movementType);
        textEquipmentType    .setText("Equipamiento: " + equipmentType);
        textExerciseType     .setText("Tipo de Ejercicio: " + exerciseType);

        // 1) Imágenes locales (si existen)
        List<String> base64List = ex.getImages();
        if ((base64List == null || base64List.isEmpty()) && !TextUtils.isEmpty(ex.getImage())) {
            base64List = new ArrayList<>();
            base64List.add(ex.getImage());
        }
        if (base64List != null && !base64List.isEmpty()) {
            cargarImagenesDesdeListaInterna(base64List, /*append=*/false);
        }

        // 2) Merge con Firebase por nombre (agrega sin borrar las locales)
        if (!TextUtils.isEmpty(ex.getName())) {
            cargarImagenesFirebase(ex.getName(), /*append=*/true);
            // Series por nombre
            fetchSeriesDataPorNombre(ex.getName(), textPersonalRecord);
        }
    }

    /* ===================== UI: REMOTO (Firebase) ===================== */
    private void populateFirebaseUI(View view) {
        if (TextUtils.isEmpty(uidForQueries) || TextUtils.isEmpty(exerciseIndex)) {
            Log.e(TAG, "populateFirebaseUI: uidForQueries o exerciseIndex inválido.");
            dismiss();
            return;
        }

        DatabaseReference ref = databaseReference
                .child("users")
                .child(uidForQueries)
                .child("userExercises")
                .child(exerciseIndex);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (isViewDestroyed || getView() == null) return;

                Exercise ex = snap.getValue(Exercise.class);
                if (ex == null) {
                    Log.e(TAG, "populateFirebaseUI: ejercicio nulo en Firebase");
                    dismiss();
                    return;
                }

                bindFirebaseExercise(ex);

                // Imágenes por nombre
                if (!TextUtils.isEmpty(ex.getName())) {
                    cargarImagenesFirebase(ex.getName(), /*append=*/false);
                }

                // Series por key
                fetchSeriesData(exerciseIndex, textPersonalRecord);

                // RM estimado (del nodo ejercicio)
                double rm = computeEstimatedRm(snap);
                textRM.setText("RM Estimado: " + String.format("%.2f", rm) + " kg");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "populateFirebaseUI:onCancelled", error.toException());
                dismiss();
            }
        });
    }

    private void bindFirebaseExercise(Exercise ex) {
        String name          = ex.getName() != null ? ex.getName() : "";
        String description   = ex.getDescription() != null ? ex.getDescription() : "Sin descripción";
        String musclesWorked = ex.getMusclesWorked() != null ? ex.getMusclesWorked() : "";
        List<String> synList = ex.getSynergistMuscles();
        String movementType  = ex.getMovementType() != null ? ex.getMovementType() : "No especificado";
        String equipmentType = ex.getEquipmentType() != null ? ex.getEquipmentType() : "No especificado";
        String exerciseType  = ex.getExerciseType() != null ? ex.getExerciseType() : "No especificado";

        textExerciseName     .setText(name);
        textDescription      .setText(description);
        textMusclesWorked    .setText("Músculos trabajados: " + capitalizeWords(musclesWorked));
        textSynergistMuscles .setText("Músculos sinérgicos: " +
                ((synList != null && !synList.isEmpty())
                        ? TextUtils.join(", ", synList)
                        : "No especificados"));
        textMovementType     .setText("Tipo de Movimiento: " + movementType);
        textEquipmentType    .setText("Equipamiento: " + equipmentType);
        textExerciseType     .setText("Tipo de Ejercicio: " + exerciseType);
    }

    /* ===================== Patrón de movimiento ===================== */
    private void loadMovementPattern(View view, String uid) {
        if (TextUtils.isEmpty(dayKey) || TextUtils.isEmpty(uid)) return;
        databaseReference.child("users")
                .child(uid)
                .child("userMovementPatterns")
                .child(dayKey)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        if (isViewDestroyed || getView() == null) return;
                        String nm = ds.getValue(String.class);
                        if (nm != null) textPatternName.setText("Patrón: " + nm);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "loadMovementPattern:onCancelled", e.toException());
                    }
                });
    }

    /* ===================== RM estimado ===================== */
    private double computeEstimatedRm(@NonNull DataSnapshot snap) {
        Integer currentLoad = snap.child("currentLoad").getValue(Integer.class);
        Integer maxReps     = snap.child("maxReps")    .getValue(Integer.class);
        if (currentLoad == null || currentLoad <= 0) currentLoad = 1;
        if (maxReps     == null || maxReps     <= 0) maxReps     = 1;
        return currentLoad * (1 + (maxReps / 30.0));
    }

    /* ===================== Spinner RPE ===================== */
    private void setupSpinner() {
        if (spinnerRpeFilter == null) return;
        spinnerRpeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selected = String.valueOf(parent.getItemAtPosition(position));
                try {
                    float rpeValue = Float.parseFloat(selected);
                    filterAndDrawChartByRpe(rpeValue);
                } catch (NumberFormatException ignore) {
                    // "Todos" u otro texto → todo
                    drawProgressChart(allSeriesData);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    /* ===================== Imágenes (decode+cache) ===================== */

    private static int computeInSample(int w, int h, int maxDim) {
        int inSample = 1;
        while ((w / inSample) > maxDim || (h / inSample) > maxDim) inSample <<= 1;
        return Math.max(1, inSample);
    }

    private static String keyOf(String base64NoWs) {
        return "b64#" + (base64NoWs != null ? base64NoWs.hashCode() : 0);
    }

    private static int approxBytes(String base64NoWs) {
        int len = (base64NoWs != null ? base64NoWs.length() : 0);
        return (int) ((len * 3L) / 4L);
    }

    /** Limpia espacios y remueve prefijos tipo data URI si existen. */
    @Nullable
    private static String cleanBase64(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) return null;
        String s = raw.replaceAll("\\s", "");
        int idx = s.indexOf("base64,");
        if (idx >= 0) s = s.substring(idx + "base64,".length());
        return s;
    }

    /** Decodifica con trazas ImageDecode (base64_len, out=WxH, inSample, t_ms). */
    private static Bitmap decodeWithAudit(String base64NoWs, String origin, int maxDim) {
        long t0 = System.currentTimeMillis();
        int base64Len = (base64NoWs != null ? base64NoWs.length() : 0);
        String outWH = "null";
        int inSample = 1;
        try {
            byte[] bytes = Base64.decode(base64NoWs, Base64.DEFAULT);

            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, probe);

            inSample = computeInSample(probe.outWidth, probe.outHeight, maxDim);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = inSample;
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

            long dt = System.currentTimeMillis() - t0;
            if (bmp != null) outWH = bmp.getWidth() + "x" + bmp.getHeight();

            Log.d("ImageDecode", "origin=" + origin +
                    " | base64_len=" + base64Len +
                    " | out=" + outWH +
                    " | inSample=" + inSample +
                    " | t_ms=" + dt);

            return bmp;
        } catch (Throwable t) {
            long dt = System.currentTimeMillis() - t0;
            Log.e("ImageDecode", "origin=" + origin +
                    " | fail | base64_len=" + base64Len +
                    " | t_ms=" + dt, t);
            return null;
        }
    }

    /** Garantiza que el adapter del carrusel exista y tenga listener de clic. */
    private void setupCarouselAdapterIfNeeded() {
        if (imageCarousel == null) return;
        if (imageCarousel.getAdapter() == null) {
            imageAdapter = new ImagePagerAdapter();
            imageAdapter.setOnImageClickListener((pos, bmp) -> showBitmapFullscreen(bmp));
            imageCarousel.setAdapter(imageAdapter);
        } else if (imageCarousel.getAdapter() instanceof ImagePagerAdapter) {
            imageAdapter = (ImagePagerAdapter) imageCarousel.getAdapter();
        }
    }

    /** Mostrar imagen a pantalla completa (tap para cerrar). */
    private void showBitmapFullscreen(@NonNull Bitmap bmp) {
        if (!isAdded()) return;
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(requireContext());
        iv.setBackgroundColor(Color.BLACK);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageBitmap(bmp);
        iv.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(iv);
        dialog.show();
    }

    /** Carga lista de base64 y agrega al carrusel; si append=false, limpia primero. */
    private void cargarImagenesDesdeListaInterna(List<String> base64Images, boolean append) {
        setupCarouselAdapterIfNeeded();
        if (imageAdapter == null) return;

        if (!append) { carouselBitmaps.clear(); imageKeys.clear(); }
        imageAdapter.submit(new ArrayList<>(carouselBitmaps)); // publicar estado inicial

        new Thread(() -> {
            for (String raw : base64Images) {
                String b64 = cleanBase64(raw);
                if (TextUtils.isEmpty(b64)) continue;

                String key = keyOf(b64);
                if (imageKeys.contains(key)) continue;

                Bitmap cached = sBitmapCache.get(key);
                Bitmap bmp;
                if (cached != null) {
                    bmp = cached;
                } else {
                    int bytes = approxBytes(b64);
                    if (bytes > BASE64_THRESHOLD_BYTES) {
                        Log.d("ImageDecode", "origin=Carousel | heavy=true | approxBytes=" + bytes);
                    }
                    bmp = decodeWithAudit(b64, "ExerciseDetailsDialog/Carousel", IMAGE_MAX_DIM);
                    if (bmp != null) sBitmapCache.put(key, bmp);
                }

                if (bmp != null) {
                    imageKeys.add(key);
                    synchronized (carouselBitmaps) { carouselBitmaps.add(bmp); }
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!isViewDestroyed && imageAdapter != null) {
                                imageAdapter.submit(new ArrayList<>(carouselBitmaps));
                            }
                        });
                    }
                }
            }
        }).start();
    }

    /** Wrapper compatibilidad (limpia y carga) */
    private void cargarImagenesDesdeListaInterna(List<String> base64Images) {
        cargarImagenesDesdeListaInterna(base64Images, /*append=*/false);
    }

    /** Firebase: obtiene base64 por nombre y agrega (append controla si limpia o suma). */
    private void cargarImagenesFirebase(String nombreEjercicio, boolean append) {
        if (TextUtils.isEmpty(nombreEjercicio)) return;
        Log.d(TAG, "cargarImagenesFirebase: iniciando para " + nombreEjercicio + " (append=" + append + ")");
        DatabaseReference imagesRef = FirebaseDatabase.getInstance().getReference("exerciseImages");
        imagesRef.orderByChild("name").equalTo(nombreEjercicio)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isViewDestroyed || getView() == null) return;

                        List<String> base64List = new ArrayList<>();
                        for (DataSnapshot n : snapshot.getChildren()) {
                            DataSnapshot imgs = n.child("images");
                            for (DataSnapshot img : imgs.getChildren()) {
                                String b64 = img.getValue(String.class);
                                if (!TextUtils.isEmpty(b64)) base64List.add(b64);
                            }
                            break; // primer match por nombre
                        }

                        if (!base64List.isEmpty()) {
                            Log.d(TAG, "cargarImagenesFirebase: encontradas " + base64List.size() + " imágenes.");
                            cargarImagenesDesdeListaInterna(base64List, /*append=*/append);
                        } else {
                            Log.w(TAG, "cargarImagenesFirebase: sin imágenes válidas.");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "cargarImagenesFirebase:onCancelled", error.toException());
                    }
                });
    }

    /** Wrapper compatibilidad (limpia y carga) */
    private void cargarImagenesFirebase(String nombreEjercicio) {
        cargarImagenesFirebase(nombreEjercicio, /*append=*/false);
    }

    /* ===================== Series / Chart ===================== */
    private void fetchSeriesData(String exerciseId, TextView textPersonalRecord) {
        Log.d(TAG, "fetchSeriesData: Iniciando descarga de series para: " + exerciseId);
        if (TextUtils.isEmpty(uidForQueries)) {
            Log.e(TAG, "fetchSeriesData: uidForQueries vacío.");
            return;
        }

        DatabaseReference seriesRef = databaseReference
                .child("users")
                .child(uidForQueries)
                .child("userExercises")
                .child(exerciseId)
                .child("seriesData");

        allSeriesData.clear();

        seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isViewDestroyed || getView() == null) return;

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
                    }
                }

                Collections.sort(allSeriesData, Comparator.comparingInt(ExerciseSeries::getSeriesNumber));

                textPersonalRecord.setText("Personal Record: " + String.format("%.2f", personalRecord) + " kg");
                textPersonalRecord.setTextColor(Color.parseColor("#BBBBBB"));

                TextView textVolume = getView().findViewById(R.id.textVolume);
                if (textVolume != null) {
                    double totalVolume = 0;
                    for (ExerciseSeries s : allSeriesData) totalVolume += s.getWeight() * s.getReps();
                    textVolume.setText("Volumen Total: " + String.format("%.2f", totalVolume) + " kg");
                    textVolume.setTextColor(Color.parseColor("#BBBBBB"));
                }

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

    private void fetchSeriesDataPorNombre(String nombreEjercicio, TextView textPersonalRecord) {
        if (TextUtils.isEmpty(uidForQueries)) {
            Log.e(TAG, "fetchSeriesDataPorNombre: uidForQueries vacío.");
            return;
        }
        DatabaseReference ref = databaseReference
                .child("users")
                .child(uidForQueries)
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
                        Log.w(TAG, "No se encontraron series para «" + nombreEjercicio + "»");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "fetchSeriesDataPorNombre:onCancelled", e.toException());
                    }
                });
    }

    private void filterAndDrawChartByRpe(float rpeFilter) {
        if (allSeriesData.isEmpty()) {
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }
        List<ExerciseSeries> filtered = new ArrayList<>();
        for (ExerciseSeries s : allSeriesData)
            if ((float) s.getRpe() == rpeFilter) filtered.add(s);

        drawProgressChart(filtered.isEmpty() ? allSeriesData : filtered);
    }

    private void drawProgressChart(List<ExerciseSeries> seriesData) {
        if (chartExerciseProgress == null) return;

        if (seriesData == null || seriesData.isEmpty()) {
            chartExerciseProgress.setNoDataText("No hay datos suficientes");
            chartExerciseProgress.setNoDataTextColor(Color.YELLOW);
            chartExerciseProgress.clear();
            chartExerciseProgress.invalidate();
            return;
        }

        List<ExerciseSeries> ordered = new ArrayList<>(seriesData);
        Collections.sort(ordered, Comparator.comparingInt(ExerciseSeries::getSeriesNumber));

        List<Entry> entries = new ArrayList<>();
        float bestRM = 0;
        Entry bestEntry = null;

        for (ExerciseSeries s : ordered) {
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

    private void configureChartLookAndFeel() {
        if (chartExerciseProgress == null) return;

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

    /* ===================== Aux ===================== */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] w = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String s : w) {
            if (s.length() > 1) {
                sb.append(Character.toUpperCase(s.charAt(0)))
                        .append(s.substring(1).toLowerCase())
                        .append(" ");
            } else {
                sb.append(s.toUpperCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /* ===================== Adaptador carrusel ===================== */
    private static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {
        private final List<Bitmap> data = new ArrayList<>();

        interface OnImageClickListener { void onClick(int position, Bitmap bmp); }
        private OnImageClickListener listener;

        void setOnImageClickListener(OnImageClickListener l) { this.listener = l; }

        void submit(List<Bitmap> newList) {
            data.clear();
            if (newList != null) data.addAll(newList);
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setBackgroundColor(Color.BLACK);
            return new VH(iv);
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            Bitmap bmp = data.get(position);
            holder.iv.setImageBitmap(bmp);
            holder.iv.setOnClickListener(v -> {
                if (listener != null) listener.onClick(holder.getAdapterPosition(), bmp);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView iv;
            VH(@NonNull View itemView) {
                super(itemView);
                iv = (ImageView) itemView;
            }
        }
    }

}
