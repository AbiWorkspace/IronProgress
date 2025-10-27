package com.example.diets.adapters;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

public class TrainingDetailAdapter extends RecyclerView.Adapter<TrainingDetailAdapter.ExerciseViewHolder> {

    private static final String TAG = "TrainingDetailAdapter";

    /* ----------------------------- Estado global ----------------------------- */
    private final List<UserExercise>           exercises;
    private final RecyclerView                 recyclerView;
    private final Context                      context;
    private final String                       dayKey;
    private final FragmentManager              fragmentManager;

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

    // 2) ajusta TU MISMO constructor
    public TrainingDetailAdapter(List<UserExercise> exercises,
                                 RecyclerView       recyclerView,
                                 String             dayKey,
                                 Context            context,
                                 FragmentManager    fragmentManager) {

        Log.d(TAG, "Constructor: ejercicios=" + exercises.size());

        this.exercises       = exercises;
        this.recyclerView    = recyclerView;
        this.dayKey          = dayKey;
        this.context         = context;
        this.fragmentManager = fragmentManager;

        // ⬇️  NUEVA línea: leemos la preferencia
        this.dynamicEnabled  = Prefs.isDynamicEnabled(context);
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



    private void abrirDetalles(UserExercise ue) {
        ExerciseDetailsDialogFragment dlg =
                ExerciseDetailsDialogFragment.newInstance(
                        null, dayKey, ue.isFromInternal());
        dlg.setInternalExercise(ue);
        dlg.show(fragmentManager, "exerciseDetails");
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

            MenuItem dynItem = pm.getMenu().findItem(R.id.action_toggle_dynamic);
            if (dynItem != null) {
                boolean estadoGuardado = Prefs.isDynamicEnabledForExercise(itemView.getContext(), ex.getName());
                dynItem.setChecked(estadoGuardado);

                Log.d(TAG, "showPopupDelete: Check 'Progresión dinámica' cargado para " + ex.getName() + " = " + estadoGuardado);
            }


            pm.setOnMenuItemClickListener(menuItem -> {

                if (menuItem.getItemId() == R.id.action_delete) {
                    int p = getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) {
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle("Eliminar ejercicio")
                                .setMessage("¿Eliminar " + ex.getName() + "?")
                                .setPositiveButton("Eliminar", (d, w) -> {
                                    pendingDeletionExercises.add(exercises.remove(p));
                                    notifyItemRemoved(p);
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                    return true;
                }

                if (menuItem.getItemId() == R.id.action_toggle_dynamic) {
                    boolean newVal = !menuItem.isChecked();
                    menuItem.setChecked(newVal);

                    Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                            "El usuario ha " + (newVal ? "activado" : "desactivado") +
                            " la Progresión Dinámica para el ejercicio '" + ex.getName() + "'.");

                    // Guarda preferencia del usuario para este ejercicio
                    Prefs.setDynamicEnabledForExercise(itemView.getContext(), ex.getName(), newVal);
                    Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                            "Se ha guardado la preferencia Progresión Dinámica = " + newVal +
                            " para el ejercicio '" + ex.getName() + "'.");

                    if (newVal) {
                        // El usuario quiere sugerencias automáticas
                        Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                                "Solicitando sugerencia dinámica de series para '" + ex.getName() + "'.");
                        List<ProgresionDinamica.Serie> sugeridas = ProgresionDinamica.sugerirSeries(
                                context,
                                ex,
                                seriesAdapter.getUpdatedSeriesList(),
                                seriesAdapter.getHistorialSeries(),
                                ProgresionDinamica.NivelUsuario.INTERMEDIO,
                                2.5f
                        );
                        Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                                "Sugerencias recibidas: " + sugeridas.size() + " series.");
                        seriesAdapter.setFromSuggest(sugeridas);
                        Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                                "La tabla de series ha sido reemplazada por la sugerencia dinámica.");
                    } else {
                        // El usuario desactivó la sugerencia, restaurar lista original
                        Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                                "Restaurando la lista original de series para '" + ex.getName() + "'.");
                        seriesAdapter.clearSuggestions();
                        Log.d(TAG, "ExerciseViewHolder - showPopupDelete: " +
                                "La tabla de series ha sido restaurada a los valores de Firebase.");
                    }

                    return true;
                }



                return false;
            });

            pm.show();
        }





        private void openDetails(UserExercise ex) {
            FragmentManager fm = ((FragmentActivity) itemView.getContext()).getSupportFragmentManager();

            if (ex.isFromInternal()) {
                Log.d(TAG, "📦 Es interno → enviando objeto completo a diálogo.");
                Bundle args = new Bundle();
                args.putSerializable("exercise", ex);
                args.putString("dayKey", dayKey);
                args.putBoolean("isFromInternal", true);
                ExerciseDetailsDialogFragment dialog = new ExerciseDetailsDialogFragment();
                dialog.setArguments(args);
                dialog.show(fm, "ExerciseDetailsDialog");
                return;
            }

            String cachedKey = firebaseKeysPorNombre.get(ex.getName());
            if (cachedKey != null) {
                ExerciseDetailsDialogFragment
                        .newInstance(cachedKey, dayKey, false)
                        .show(fm, "ExerciseDetailsDialog");
            } else {
                pendingOpenDetails = true;
                fetchKeyIfNeeded(ex.getName(), key -> {
                    if (key != null) {
                        seriesAdapter.cargarUltimasSeriesDesdeFirebase(key, context, () -> {

                            seriesAdapter.commitAllEdits();            // refresca cache-UI
                            rvSeries.setAdapter(seriesAdapter);        // siempre

                            if (!TrainingDetailAdapter.this.dynamicEnabled) {
                                Log.d(TAG, "Dinámica OFF → se conserva la tabla original");
                                return;                           // ⬅️  ¡nada de sugerencias!
                            }

                            /* ---------- feedback + historial ---------- */
                            List<ExerciseSeries> comp  = new ArrayList<>();
                            ExerciseSeries best = seriesAdapter.getBestHistorySerie();
                            if (best != null) comp.add(best);
                            comp.addAll(seriesAdapter.getUpdatedSeriesList());

                            Log.d(TAG,
                                    "PD-IN  ➜ best="
                                            + (best==null? "null":best.toSimple())
                                            + "  comp=" + comp.size()
                                            + "  hist=" + seriesAdapter.getHistorialSeries().size());

                            List<ProgresionDinamica.Serie> sug =
                                    ProgresionDinamica.sugerirSeries(
                                            context,
                                            ex,
                                            comp,
                                            seriesAdapter.getHistorialSeries(),
                                            ProgresionDinamica.NivelUsuario.INTERMEDIO,
                                            2.5f);

                            /* fallback inicial */
                            if (sug.isEmpty()) {
                                float ref = best != null ? (float) best.getWeight()
                                        : (float) ex.getWeight();
                                sug = ProgresionDinamica.generarSeriesInicio(
                                        ref,
                                        ProgresionDinamica.TipoEjercicio.MULTIARTICULAR,
                                        3);
                            }

                            /* 🚫 NO sustituimos la lista si no hay sugerencias válidas */
                            if (!sug.isEmpty()) {
                                seriesAdapter.setFromSuggest(sug);
                            }
                        });




                    }

                });

            }
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

                if (ok) return;          // ✅ salimos SOLO si mostramos la imagen
                // si no, continuamos a la caché y al fallback Firebase
            }

            /* ② Ejercicio interno sin imágenes ----------------------------------- */
            if (ex.isFromInternal()) {
                Log.d(TAG, "  ② interno SIN imágenes → continuamos a caché / Firebase");
                // NO hacemos return: dejamos que pruebe caché y, si es necesario, Firebase
            }

            /* ③ ¿Está en la caché de la sesión? ---------------------------------- */
            List<String> cached = exerciseImagesCache.get(ex.getName());
            if (cached != null && !cached.isEmpty()) {
                Log.d(TAG, "  ③ caché HIT → " + cached.size() + " img(s)");
                decodeIntoView(cached.get(0), target);
                ex.setImages(cached);
                return;
            }
            Log.d(TAG, "  ③ caché MISS");

            /* ④ Fallback → /exerciseImages  (lista completa) ---------------------- */
            Log.d(TAG, "  ④ consultando /exerciseImages en Firebase …");
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

    private void fetchKeyIfNeeded(String exerciseName, KeyCb cb) {
        if (firebaseKeysPorNombre.containsKey(exerciseName)) {
            cb.onOk(firebaseKeysPorNombre.get(exerciseName));
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
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

    //  ⛔  AHORA MISMO ESTÁ AQUÍ  ⛔
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
                            feedback,           // ✅ solo completadas
                            hist,               // historial
                            ProgresionDinamica.NivelUsuario.INTERMEDIO,
                            SALTO_MIN);

            /* ---------- 4) volcar la sugerencia en la UI ---------- */
            if (!sugeridas.isEmpty()) {          // ✅ solo si hay algo que mostrar
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



