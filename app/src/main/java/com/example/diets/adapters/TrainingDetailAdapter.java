package com.example.diets.adapters;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.Utils.Prefs;
import com.example.diets.fragments.ExerciseDetailsDialogFragment;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.ProgresionDinamica;
import com.example.diets.model.UserExercise;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
// imports necesarios
import android.content.Context;
import androidx.appcompat.app.AlertDialog;

public class TrainingDetailAdapter extends RecyclerView.Adapter<TrainingDetailAdapter.ExerciseViewHolder> {

    private static final String TAG = "TrainingDetailAdapter";

    /* ----------------------------- Estado global ----------------------------- */
    private final List<UserExercise>           exercises;
    private final RecyclerView                 recyclerView;
    private final Context                      context;
    private final String                       dayKey;
    private final FragmentManager              fragmentManager;
    private static final int BTN_MIN_HEIGHT_DP = 26;   // antes 40
    private static final int BTN_PAD_H_DP     = 10;   // antes 18
    private static final int BTN_PAD_V_DP     = 4;    // antes 10
    private static final int PILL_RADIUS_DP   = 10;   // antes 24
    private static final int SPACING_BETWEEN  = 20;   // separación entre botones
    private static final int BUTTONS_TOP_M    = 8;    // margen superior fila botones
    private final Map<String, SeriesAdapter> seriesAdapters = new HashMap<>();
    private final Map<String, UserExercise>  exerciseMap    = new HashMap<>();

    private final Map<String, SeriesAdapter> adaptersPorNombre = new HashMap<>();





    private final List<ExerciseViewHolder>     viewHolders              = new ArrayList<>();
    private final Map<String, String>          firebaseKeysPorNombre    = new HashMap<>();
    private final Map<String, Integer>         exerciseSeriesCountMap   = new HashMap<>();
    private final List<UserExercise>           pendingDeletionExercises = new ArrayList<>();

    /* 🔄 NUEVO  ➜  cacheamos las listas completas de imágenes                       */
    private final Map<String, List<String>>    exerciseImagesCache      = new HashMap<>();

    // 1) declara el campo en la cabecera de la clase
    private boolean dynamicEnabled;   // ← nuevo

    private final String effectiveUid; //

    public TrainingDetailAdapter(List<UserExercise> exercises,
                                 RecyclerView rv,
                                 String dayKey,
                                 Context ctx,
                                 FragmentManager fm,
                                 String effectiveUid) {
        this.exercises = exercises;
        this.recyclerView = rv;
        this.dayKey = dayKey;
        this.context = ctx;
        this.fragmentManager = fm;
        this.effectiveUid = effectiveUid;
        this.dynamicEnabled = Prefs.isDynamicEnabled(ctx);
    }



    /* ---------------- Métodos de acceso requeridos por otros componentes --------------- */

    public void updateSeriesCount(String exerciseName, int newCount) {
        for (UserExercise ex : exercises) {
            if (ex.getName().equalsIgnoreCase(exerciseName)) {
                ex.setSeries(newCount);
                Log.d(TAG, "updateSeriesCount → " + exerciseName + " = " + newCount);
                break;
            }
        }
    }

    public void setFirebaseKey(String exerciseName, String key) {
        firebaseKeysPorNombre.put(exerciseName, key);
    }

    /** Compatibilidad: antes quitábamos listeners; ahora es no-op. */
    public void detachAllListeners() {
        for (SeriesAdapter sa : adaptersPorNombre.values()) {
            sa.detach();   // no-op pero segura
        }
    }


    public List<UserExercise> getExercises()                 { return exercises; }
    public List<UserExercise> getPendingDeletionExercises()  { return pendingDeletionExercises; }

    /** Requerido por {@link com.example.diets.fragments.TrainingDetailFragment}. */
    public List<ExerciseSeries> getSeriesListForExercise(String exerciseName) {
        SeriesAdapter sa = getSeriesAdapterPorNombre(exerciseName);
        return sa != null ? sa.getSeriesList() : null;
    }

    public Map<String, List<ExerciseSeries>> getAllCompletedSeriesGroupedByExercise() {
        Map<String, List<ExerciseSeries>> out = new HashMap<>();
        for (ExerciseViewHolder vh : viewHolders) {
            String n = vh.txtName.getText().toString();
            List<ExerciseSeries> ok = new ArrayList<>();
            for (ExerciseSeries s : vh.seriesAdapter.getSeriesList())
                if (s.isCompleted()) ok.add(s);
            out.put(n, ok);
        }
        return out;
    }

    public SeriesAdapter getSeriesAdapterPorNombre(String exerciseName) {
        for (ExerciseViewHolder vh : viewHolders)
            if (vh.txtName.getText().toString().equalsIgnoreCase(exerciseName))
                return vh.seriesAdapter;
        return null;
    }

    public void swapExercises(int from, int to) {
        if (from < 0 || to < 0 || from >= exercises.size() || to >= exercises.size()) return;
        Collections.swap(exercises, from, to);
        notifyItemMoved(from, to);
    }

    public void onItemMove(int from, int to) { swapExercises(from, to); }

    /* ----------------------------- Adaptador RV ----------------------------- */

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contenedor, parent, false);
        return new ExerciseViewHolder(v);
    }

    // --- Soporte para reemplazar el dataset desde el Fragment ---
    public void setExercises(@NonNull List<UserExercise> newList) {
        if (newList == null) {
            this.exercises.clear();
            notifyDataSetChanged();
            return;
        }
        this.exercises.clear();
        this.exercises.addAll(newList);
        notifyDataSetChanged();
    }



    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        UserExercise ue = exercises.get(position);
        holder.bind(ue);
        holder.itemView.setOnClickListener(v -> abrirDetalles(ue));
        viewHolders.add(holder);
    }
    public void setDynamicEnabled(boolean value) {
        this.dynamicEnabled = value;

        for (SeriesAdapter sa : seriesAdapters.values()) {
            if (value) {
                generarSugerencias();        // lo acabas de encender
            } else {
                sa.clearSuggestions();       // lo apagas → limpiamos
                sa.notifyDataSetChanged();
            }
        }
    }


    public void setDynamicEnabledForExercise(String exerciseName, boolean enabled) {
        // Aquí puedes guardar localmente, actualizar, refrescar solo ese SeriesAdapter, etc.
        // Ejemplo:
        SeriesAdapter sa = seriesAdapters.get(exerciseName);
        if (sa != null) {
            if (enabled) {
                // Genera sugerencias solo para ese ejercicio
                UserExercise ex = exerciseMap.get(exerciseName);
                if (ex != null) {
                    final float SALTO_MIN = 2.5f;
                    List<ExerciseSeries> feedback = sa.getUpdatedSeriesList();
                    List<ExerciseSeries> hist = sa.getHistorialSeries();
                    List<ProgresionDinamica.Serie> sugeridas =
                            ProgresionDinamica.sugerirSeries(
                                    context,
                                    ex,
                                    feedback,
                                    hist,
                                    ProgresionDinamica.NivelUsuario.INTERMEDIO,
                                    SALTO_MIN
                            );
                    if (!sugeridas.isEmpty()) sa.setFromSuggest(sugeridas);
                }
            } else {
                sa.clearSuggestions();
                sa.notifyDataSetChanged();
            }
        }
    }



    /* --- Abrir detalles desde el adapter (fuera del ViewHolder) --- */
    private void abrirDetalles(@NonNull UserExercise ue) {
        if (ue.isFromInternal()) {
            ExerciseDetailsDialogFragment dlg =
                    ExerciseDetailsDialogFragment.newInstance(null, dayKey, true, effectiveUid);
            dlg.setInternalExercise(ue);
            dlg.show(fragmentManager, "exerciseDetails");
            return;
        }

        String cachedKey = firebaseKeysPorNombre.get(ue.getName());
        if (cachedKey != null) {
            ExerciseDetailsDialogFragment
                    .newInstance(cachedKey, dayKey, false, effectiveUid)
                    .show(fragmentManager, "exerciseDetails");
            return;
        }

        // 🔧 Obtener la key y abrir el diálogo dentro del callback (NO usar 'k' fuera)
        fetchKeyIfNeeded(ue.getName(), effectiveUid, key -> {
            if (key != null) {
                ExerciseDetailsDialogFragment
                        .newInstance(key, dayKey, false, effectiveUid)
                        .show(fragmentManager, "exerciseDetails");
            } else {
                Log.e(TAG, "abrirDetalles: no se encontró key para " + ue.getName());
            }
        });
    }



    @Override
    public int getItemCount() { return exercises.size(); }

    /* ------------------------------- ViewHolder ------------------------------ */

    public class ExerciseViewHolder extends RecyclerView.ViewHolder {

        private final TextView     txtName;
        private final ImageView    img;
        private final ImageView    icMore;
        private final Button       btnAddSeries;
        private final RecyclerView rvSeries;

        private SeriesAdapter        seriesAdapter;
        private List<ExerciseSeries> seriesList;
        private boolean              pendingOpenDetails = false;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName      = itemView.findViewById(R.id.exerciseName);
            img          = itemView.findViewById(R.id.exerciseImage);
            icMore       = itemView.findViewById(R.id.iconAction);
            btnAddSeries = itemView.findViewById(R.id.btnAddSeries);
            rvSeries     = itemView.findViewById(R.id.recyclerViewSeries);
        }


        public void bind(UserExercise ex) {
            Log.d(TAG, "ExerciseViewHolder - bind: Iniciando vinculación del ejercicio '" + ex.getName() + "'");

            txtName.setText(ex.getName());

            // ─── Reutiliza adapter si ya lo habías creado ───
            SeriesAdapter cached = seriesAdapters.get(ex.getName());
            if (cached != null) {
                Log.d(TAG, "ExerciseViewHolder - bind: Adapter ya existente para '" + ex.getName() + "'");
                seriesAdapter = cached;
                seriesList    = cached.getSeriesList();
            } else {
                Log.d(TAG, "ExerciseViewHolder - bind: Creando adapter y lista de series para '" + ex.getName() + "'");
                seriesList = new ArrayList<>();
                int n = Math.max(1, ex.getSeries());
                for (int i = 0; i < n; i++) {
                    ExerciseSeries serie = new ExerciseSeries(
                            i + 1,
                            ex.getReps(),
                            ex.getWeight(),
                            ex.getRpe()
                    );
                    serie.setCompleted(false); // O el valor real si lo tienes
                    seriesList.add(serie);
                }
                seriesAdapter = new SeriesAdapter(seriesList, ex.getName(), TrainingDetailAdapter.this);
                seriesAdapters.put(ex.getName(), seriesAdapter);
            }

            rvSeries.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rvSeries.setAdapter(seriesAdapter);

            // Referencias rápidas
            seriesAdapters.put(ex.getName(), seriesAdapter);
            exerciseMap.put(ex.getName(), ex);
            exerciseSeriesCountMap.put(ex.getName(), seriesList.size());

            loadFirstPicture(ex, img);

            btnAddSeries.setOnClickListener(v -> {
                int num = seriesList.size() + 1;
                Log.d(TAG, "ExerciseViewHolder - bind: Añadiendo nueva serie (" + num + ") a '" + ex.getName() + "'");
                seriesList.add(new ExerciseSeries(num, 1, 0, 0));
                seriesAdapter.notifyItemInserted(seriesList.size() - 1);
                exerciseSeriesCountMap.put(ex.getName(), seriesList.size());
            });

            icMore.setOnClickListener(v -> showPopupDelete(ex));
            txtName.setOnClickListener(v -> openDetails(ex));

            // --- Refinar series con Firebase y APLICAR PROGRESIÓN DINÁMICA si corresponde ---
            fetchKeyIfNeeded(ex.getName(), key -> {
                if (key == null) {
                    Log.e(TAG, "ExerciseViewHolder - bind: No se encontró clave Firebase para '" + ex.getName() + "'");
                    return;
                }
                Log.d(TAG, "ExerciseViewHolder - bind: Clave Firebase recuperada para '" + ex.getName() + "': " + key);

                seriesAdapter.cargarUltimasSeriesDesdeFirebase(key, context, () -> {
                    Log.d(TAG, "ExerciseViewHolder - bind: Series refinadas desde Firebase para '" + ex.getName() + "'");
                    seriesAdapter.commitAllEdits();

                    // Lee la preferencia real del ejercicio:
                    boolean progDinamicaOn = Prefs.isDynamicEnabledForExercise(itemView.getContext(), ex.getName());
                    Log.d(TAG, "ExerciseViewHolder - bind: Progresión dinámica guardada para '" + ex.getName() + "' = " + progDinamicaOn);

                    if (progDinamicaOn) {
                        // Sugerir series automáticamente al entrar (peso, reps, cantidad, etc.)
                        List<ProgresionDinamica.Serie> sugeridas = ProgresionDinamica.sugerirSeries(
                                context,
                                ex,
                                seriesAdapter.getUpdatedSeriesList(),
                                seriesAdapter.getHistorialSeries(),
                                ProgresionDinamica.NivelUsuario.INTERMEDIO,
                                2.5f
                        );
                        Log.d(TAG, "ExerciseViewHolder - bind: Sugerencia dinámica aplicada (" + sugeridas.size() + " series)");
                        if (!sugeridas.isEmpty()) {
                            seriesAdapter.setFromSuggest(sugeridas);
                        }
                        rvSeries.setAdapter(seriesAdapter);
                    } else {
                        // Si está OFF, deja la lista tal cual (manual/histórica)
                        Log.d(TAG, "ExerciseViewHolder - bind: Dinámica OFF → se conserva la tabla original para '" + ex.getName() + "'");
                        rvSeries.setAdapter(seriesAdapter);
                    }
                });
            });
        }



        public void detachAllListeners() {
            TrainingDetailAdapter.this.detachAllListeners();  // compat, no-op
        }


        /** Muestra el menú contextual (eliminar / progresión dinámica).              */
        private void showPopupDelete(UserExercise ex) {
            PopupMenu pm = new PopupMenu(itemView.getContext(), icMore);

            pm.inflate(R.menu.menu_exercise_actions);





            pm.setOnMenuItemClickListener(menuItem -> {

                if (menuItem.getItemId() == R.id.action_delete) {
                    int p = getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) {
                        showConfirmDialog(
                                itemView.getContext(),
                                "Eliminar ejercicio",
                                "¿Eliminar " + ex.getName() + "?",
                                "Sí, eliminar",
                                () -> {
                                    pendingDeletionExercises.add(exercises.remove(p));
                                    notifyItemRemoved(p);
                                },
                                "Cancelar",
                                null
                        );


                    }
                    return true;
                }





                return false;
            });

            pm.show();
        }





        private void showConfirmDialog(Context ctx,
                                       String title,
                                       String message,
                                       String positiveText,
                                       Runnable onPositive,
                                       String negativeText,
                                       Runnable onNegative) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(ctx, R.style.CustomAlertDialog)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton(negativeText, (d, w) -> { if (onNegative != null) onNegative.run(); })
                    .setPositiveButton(positiveText, (d, w) -> { if (onPositive != null) onPositive.run(); })
                    .create();

            dialog.setOnShowListener(dlg -> {
                Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                styleFilledRedPill(neg, ctx);
                styleFilledRedPill(pos, ctx);

                // margen entre botones para que se vean como en la captura
                LinearLayout parent = (LinearLayout) neg.getParent();
                if (parent != null) {
                    parent.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
                    int m = (int) dp(ctx, 8);
                    LinearLayout.LayoutParams lpNeg = (LinearLayout.LayoutParams) neg.getLayoutParams();
                    LinearLayout.LayoutParams lpPos = (LinearLayout.LayoutParams) pos.getLayoutParams();
                    if (lpNeg != null) lpNeg.setMargins(0, m, m, 0);
                    if (lpPos != null) lpPos.setMargins(0, m, 0, 0);
                    neg.setLayoutParams(lpNeg);
                    pos.setLayoutParams(lpPos);
                }
            });

            dialog.show();
        }
        // Botón "pill" rojo, texto blanco
        private void styleFilledRedPill(Button b, Context ctx) {
            if (b == null) return;
            b.setAllCaps(false);
            b.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
            b.setMinHeight((int) dp(ctx, 40));
            b.setPadding((int) dp(ctx, 18), (int) dp(ctx, 10), (int) dp(ctx, 18), (int) dp(ctx, 10));

            float radius = dp(ctx, 24); // pill
            ShapeAppearanceModel shape = new ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, radius)
                    .build();

            MaterialShapeDrawable bg = new MaterialShapeDrawable(shape);
            bg.setFillColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.brand_red)));
            ViewCompat.setBackground(b, bg);
        }

        private float dp(Context ctx, float v) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
        }


            private void openDetails(@NonNull UserExercise ex) {
            FragmentManager fm = ((FragmentActivity) itemView.getContext()).getSupportFragmentManager();

            if (ex.isFromInternal()) {
                Log.d(TAG, " Es interno → enviando objeto completo a diálogo.");
                ExerciseDetailsDialogFragment dlg =
                        ExerciseDetailsDialogFragment.newInstance(null, dayKey, true, TrainingDetailAdapter.this.effectiveUid);
                dlg.setInternalExercise(ex);
                dlg.show(fm, "ExerciseDetailsDialog");
                return;
            }

            String cachedKey = firebaseKeysPorNombre.get(ex.getName());
            if (cachedKey != null) {
                ExerciseDetailsDialogFragment
                        .newInstance(cachedKey, dayKey, false, TrainingDetailAdapter.this.effectiveUid)
                        .show(fm, "ExerciseDetailsDialog");
                return;
            }

            // Resolver key y abrir diálogo dentro del callback
            fetchKeyIfNeeded(ex.getName(), TrainingDetailAdapter.this.effectiveUid, key -> {
                if (key == null) {
                    Log.e(TAG, "openDetails: No se encontró clave Firebase para '" + ex.getName() + "'");
                    return;
                }

                seriesAdapter.cargarUltimasSeriesDesdeFirebase(key, context, () -> {
                    seriesAdapter.commitAllEdits();
                    rvSeries.setAdapter(seriesAdapter);

                    if (!TrainingDetailAdapter.this.dynamicEnabled) {
                        Log.d(TAG, "Dinámica OFF → se conserva la tabla original");
                    } else {
                        List<ExerciseSeries> comp  = new ArrayList<>();
                        ExerciseSeries best = seriesAdapter.getBestHistorySerie();
                        if (best != null) comp.add(best);
                        comp.addAll(seriesAdapter.getUpdatedSeriesList());

                        List<ProgresionDinamica.Serie> sug =
                                ProgresionDinamica.sugerirSeries(
                                        context,
                                        ex,
                                        comp,
                                        seriesAdapter.getHistorialSeries(),
                                        ProgresionDinamica.NivelUsuario.INTERMEDIO,
                                        2.5f);

                        if (sug.isEmpty()) {
                            float ref = best != null ? (float) best.getWeight() : (float) ex.getWeight();
                            sug = ProgresionDinamica.generarSeriesInicio(
                                    ref,
                                    ProgresionDinamica.TipoEjercicio.MULTIARTICULAR,
                                    3);
                        }
                        if (!sug.isEmpty()) seriesAdapter.setFromSuggest(sug);
                    }

                    //  Abrir el diálogo ya con la key resuelta y el UID correcto
                    ExerciseDetailsDialogFragment
                            .newInstance(key, dayKey, false, TrainingDetailAdapter.this.effectiveUid)
                            .show(fm, "ExerciseDetailsDialog");
                });
            });
        }


        public ProgresionDinamica.Serie calcularSiguienteSerieSugerida(UserExercise ex, List<ExerciseSeries> todasLasSeries) {
            // Puedes parametrizar esto según la UI/usuario:
            ProgresionDinamica.NivelUsuario nivelUsuario = ProgresionDinamica.NivelUsuario.INTERMEDIO; // O desde preferencias/usuario
            float saltoMinimo = 2.5f;        // O el mínimo de tu gym, personalizable
            int volumenRecomendado = ProgresionDinamica.calcularVolumenSemanalObjetivo(
                    ex.getMovementType()!=null && ex.getMovementType().toLowerCase().contains("multi") ?
                            ProgresionDinamica.TipoEjercicio.MULTIARTICULAR :
                            ProgresionDinamica.TipoEjercicio.AISLADO,
                    nivelUsuario
            );
            int repsMeta = ex.getReps()>0 ? ex.getReps() : (ex.getMovementType()!=null && ex.getMovementType().toLowerCase().contains("multi") ? 6 : 10);
            int rpeMeta  = ex.getRpe()>0 ? (int)ex.getRpe() : 8;

            return ProgresionDinamica.sugerenciaProximaSerieAvanzada(
                    ex,
                    todasLasSeries,
                    nivelUsuario,
                    saltoMinimo,
                    volumenRecomendado,
                    repsMeta,
                    rpeMeta
            );
        }


        /** Carga la primera imagen (con caché + trazas) */
        private void loadFirstPicture(UserExercise ex, ImageView target) {

            Log.d(TAG, "▶ loadFirstPicture → " + ex.getName());

            if (ex.getImages() != null && !ex.getImages().isEmpty()) {

                boolean ok = decodeIntoView(ex.getImages().get(0), target);
                Log.d(TAG, "resultado decode = " + ok);

                if (ok) return;          // salimos SOLO si mostramos la imagen
                // si no, continuamos a la caché y al fallback Firebase
            }

            /* ② Ejercicio interno sin imágenes ----------------------------------- */
            if (ex.isFromInternal()) {
                Log.d(TAG, "  interno SIN imágenes → continuamos a caché / Firebase");
                // NO hacemos return: dejamos que pruebe caché y, si es necesario, Firebase
            }

            /* ③ ¿Está en la caché de la sesión? ---------------------------------- */
            List<String> cached = exerciseImagesCache.get(ex.getName());
            if (cached != null && !cached.isEmpty()) {
                Log.d(TAG, "   caché HIT → " + cached.size() + " img(s)");
                decodeIntoView(cached.get(0), target);
                ex.setImages(cached);
                return;
            }
            Log.d(TAG, "   caché MISS");

            /* ④ Fallback → /exerciseImages  (lista completa) ---------------------- */
            Log.d(TAG, "   consultando /exerciseImages en Firebase …");
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("exerciseImages");

            ref.orderByChild("name").equalTo(ex.getName())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {

                            List<String> imgs = new ArrayList<>();

                            for (DataSnapshot match : snap.getChildren()) {
                                DataSnapshot imgsSnap = match.child("images");
                                for (DataSnapshot imgNode : imgsSnap.getChildren()) {
                                    String b64 = imgNode.getValue(String.class);
                                    if (b64 != null && !b64.trim().isEmpty())
                                        imgs.add(b64);
                                }
                                break;   // 1.ª coincidencia basta
                            }

                            Log.d(TAG, "     ↳ Firebase devuelto: " + imgs.size() + " img(s)");

                            if (!imgs.isEmpty()) {
                                exerciseImagesCache.put(ex.getName(), imgs);
                                ex.setImages(imgs);
                                decodeIntoView(imgs.get(0), target);
                            } else {
                                Log.d(TAG, "     ↳ sin imágenes → placeholder");
                                target.setImageResource(R.drawable.ic_placeholder);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                            Log.e(TAG, "     ↳ onCancelled Firebase", e.toException());
                            target.setImageResource(R.drawable.ic_placeholder);
                        }
                    });
        }


        private boolean decodeIntoView(String b64, ImageView target) {
            try {
                // ––––– métricas útiles para el log –––––
                int originalLen = b64.length();
                String head = b64.length() >= 30 ? b64.substring(0, 30) : b64;

                Log.d(TAG, "decodeIntoView:"
                        + " len=" + originalLen
                        + " head=\"" + head + "...\"");

                // ⚠️ por si llegan espacios o saltos de línea
                byte[] data = Base64.decode(b64.replaceAll("\\s", ""), Base64.DEFAULT);
                Bitmap bmp  = BitmapFactory.decodeByteArray(data, 0, data.length);

                if (bmp != null) {
                    Log.d(TAG, "decodeIntoView: bitmap OK  w="
                            + bmp.getWidth() + "  h=" + bmp.getHeight());
                    target.setImageBitmap(bmp);
                    return true;
                } else {
                    Log.e(TAG, "decodeIntoView: BitmapFactory devolvió NULL");
                }

            } catch (Exception e) {
                Log.e(TAG, "decodeIntoView: EXCEPCIÓN al decodificar", e);
            }

            target.setImageResource(R.drawable.ic_placeholder);
            return false;
        }
    }

    /* ---------------------------- util Firebase ----------------------------- */

    private interface KeyCb { void onOk(String key); }

    // Sobrecarga NUEVA: permite pasar el UID del cliente (modo coach).
    private void fetchKeyIfNeeded(@NonNull String exerciseName,
                                  @Nullable String targetUid,
                                  @NonNull KeyCb cb) {
        try {
            if (exerciseName.trim().isEmpty()) {
                cb.onOk(null);
                return;
            }

            // Caché por nombre (compatibilidad con tu código actual)
            if (firebaseKeysPorNombre.containsKey(exerciseName)) {
                cb.onOk(firebaseKeysPorNombre.get(exerciseName));
                return;
            }

            // Determina el UID a usar: el del cliente (coach) o el actual
            String uid = (targetUid != null && !targetUid.trim().isEmpty())
                    ? targetUid.trim()
                    : (FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null);

            if (uid == null) {
                Log.e(TAG, "fetchKeyIfNeeded: UID nulo (no autenticado ni targetUid).");
                cb.onOk(null);
                return;
            }

            final String uidToUse = uid; // efectivamente final para la inner class

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uidToUse)
                    .child("userExercises");

            ref.orderByChild("name").equalTo(exerciseName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s) {
                            for (DataSnapshot c : s.getChildren()) {
                                String k = c.getKey();
                                if (k != null) {
                                    firebaseKeysPorNombre.put(exerciseName, k);
                                    cb.onOk(k);
                                    return;
                                }
                            }
                            cb.onOk(null);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                            Log.e(TAG, "fetchKeyIfNeeded:onCancelled", e.toException());
                            cb.onOk(null);
                        }
                    });
        } catch (Exception ex) {
            Log.e(TAG, "fetchKeyIfNeeded: excepción", ex);
            cb.onOk(null);
        }
    }

    // Versión ORIGINAL preservada para no romper llamadas existentes.
    private void fetchKeyIfNeeded(@NonNull String exerciseName, @NonNull KeyCb cb) {
        fetchKeyIfNeeded(exerciseName, null, cb);
    }


    /* ------------------------ util: borrar internos ------------------------- */

    private void borrarEjerciciosInternos(Context ctx, List<String> nombres) {
        try {
            File f = new File(ctx.getFilesDir(), "exercises.json");
            if (!f.exists()) return;

            BufferedReader br = new BufferedReader(new FileReader(f));
            Gson g = new Gson();
            Type t = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> all = g.fromJson(br, t);
            br.close();

            List<Exercise> keep = new ArrayList<>();
            for (Exercise e : all)
                if (!nombres.contains(e.getName())) keep.add(e);

            FileWriter fw = new FileWriter(f, false);
            g.toJson(keep, fw);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            Log.e(TAG, "borrarEjerciciosInternos", e);
        }
    }

    //    AHORA MISMO ESTÁ AQUÍ
    public void commitAllEdits(@Nullable Runnable onFinished) {
        if (seriesAdapters.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        AtomicInteger restantes = new AtomicInteger(seriesAdapters.size());
        for (SeriesAdapter sa : seriesAdapters.values()) {
            sa.commitAllEdits(() -> {
                if (restantes.decrementAndGet() == 0 && onFinished != null) {
                    onFinished.run();
                }
            });
        }
    }
    public void commitAllEdits() { commitAllEdits(null); }


    /* =========================================================
     *  NUEVO – generarSugerencias: feedback solo de completadas
     * ========================================================= */
    public void generarSugerencias() {
        if (!dynamicEnabled) return;
        if (seriesAdapters.isEmpty()) return;

        final float SALTO_MIN = 2.5f;     // paso de discos

        /* Recorremos los ejercicios visibles (o tu propia lista “ejercicios”) */
        for (Map.Entry<String, SeriesAdapter> e : seriesAdapters.entrySet()) {

            String nombre           = e.getKey();
            SeriesAdapter sa        = e.getValue();
            UserExercise  ejercicio = exerciseMap.get(nombre);
            if (ejercicio == null) continue;

            /* ---------- 1) feedback: solo series completadas hoy ---------- */
            List<ExerciseSeries> feedback = sa.getUpdatedSeriesList();   // ← ya filtra
            if (feedback.isEmpty()) continue;                           // nada que aportar

            /* ---------- 2) historial completo para RM all-time ---------- */
            List<ExerciseSeries> hist = sa.getHistorialSeries();




            /* ---------- 3) pedir sugerencia a ProgresionDinamica ---------- */
            List<ProgresionDinamica.Serie> sugeridas =
                    ProgresionDinamica.sugerirSeries(
                            context,            // ← AGREGA EL CONTEXT AQUÍ
                            ejercicio,          // datos del ejercicio
                            feedback,           //  solo completadas
                            hist,               // historial
                            ProgresionDinamica.NivelUsuario.INTERMEDIO,
                            SALTO_MIN);

            /* ---------- 4) volcar la sugerencia en la UI ---------- */
            if (!sugeridas.isEmpty()) {          //  solo si hay algo que mostrar
                sa.setFromSuggest(sugeridas);    // ya hace notify*
            }   // si está vacía, mantenemos las series actuales

        }

        /* Si algún adapter externo depende del recálculo de sus hijos */
        notifyDataSetChanged();                // opcional: solo si lo necesitas
    }



    /** Ya no mantenemos listeners vivos; método no-op para compatibilidad. */
    public void detach() {
        // Si en el futuro vuelves a usar ValueEventListener persistente,
        // elimina aquí el listener de Firebase.
    }

}



