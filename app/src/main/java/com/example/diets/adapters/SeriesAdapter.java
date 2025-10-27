package com.example.diets.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.ProgresionDinamica;
import com.example.diets.model.UserExercise;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder> {

    /* =========================================================
     *  Atributos
     * ========================================================= */
    private static final String TAG = "SeriesAdapter";
    private List<ExerciseSeries> prevSession = Collections.emptyList();

    private final List<ExerciseSeries> seriesList;
    private       List<ExerciseSeries> historialSeries = new ArrayList<>();

    /* ----------  cache de la lista original  ---------- */
    private final List<ExerciseSeries> baseSeries = new ArrayList<>();
    private boolean suggestionActive = false;          // bandera


    /* ───────── NUEVO: mejor serie histórica (mayor 1-RM) ───────── */
    private ExerciseSeries bestHistorySerie;
    public  ExerciseSeries getBestHistorySerie(){ return bestHistorySerie; }

    private final Map<String, SeriesAdapter> seriesAdapters = new HashMap<>();
    private final Map<String, UserExercise>  exerciseMap    = new HashMap<>();
    private Map<Integer, ExerciseSeries> prevMap = new HashMap<>();

    /* ───────── NUEVO: última serie histórica ───────── */
    private ExerciseSeries lastHistorySerie;
    public  ExerciseSeries getLastHistorySerie(){ return lastHistorySerie; }

    private Double previousLoad;                 // para “Prev: …”
    private final String                exerciseName;
    private final TrainingDetailAdapter parentAdapter;

    /* =========================================================
     *  Ctor
     * ========================================================= */
    public SeriesAdapter(List<ExerciseSeries> seriesList,
                         String exerciseName,
                         TrainingDetailAdapter parentAdapter) {

        Log.d(TAG,"Constructor → tamaño lista: "+seriesList.size());
        this.seriesList    = seriesList;
        this.exerciseName  = exerciseName;
        this.parentAdapter = parentAdapter;

        /* 🆕  snapshot de la tabla ‘real’  */
        baseSeries.addAll(seriesList);      // ← copia profunda NO necesaria
    }


    /* =========================================================
     *  Getters / setters
     * ========================================================= */

    public Double getPreviousLoad()                 { return previousLoad; }
    public void   setPreviousLoad(Double prevLoad)  { previousLoad = prevLoad; }
    public List<ExerciseSeries> getSeriesList()     { return seriesList; }
    public List<ExerciseSeries> getHistorialSeries(){ return historialSeries; }

    /* =========================================================
     *  RecyclerView
     * ========================================================= */
    @NonNull @Override
    public SeriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                               int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_series, parent, false);
        return new SeriesViewHolder(item, this, exerciseName, parentAdapter);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesViewHolder holder, int position) {
        Log.d(TAG, "SeriesAdapter - onBindViewHolder: Vinculando ViewHolder en posición " + position);

        ExerciseSeries actual = seriesList.get(position);
        holder.bind(actual);


        // ------- LÓGICA DEFINITIVA DE CARGA ANTERIOR --------
        int serieNum = actual.getSeriesNumber();

        Double cargaAnterior = null;

        // 1. ¿Hay una sesión previa real?
        if (prevSession != null && !prevSession.isEmpty()) {
            // Busca por número de serie
            ExerciseSeries match = null;
            for (ExerciseSeries prev : prevSession) {
                if (prev.getSeriesNumber() == serieNum) {
                    match = prev;
                    break;
                }
            }
            if (match != null) {
                cargaAnterior = match.getWeight();
                Log.d(TAG, "SeriesAdapter - onBindViewHolder: Carga anterior encontrada para la serie "
                        + serieNum + " → " + cargaAnterior + " kg");
            } else {
                // Si no hay match, busca el máximo peso de la sesión previa
                double maxPrevWeight = 0;
                for (ExerciseSeries prev : prevSession) {
                    if (prev.getWeight() > maxPrevWeight) maxPrevWeight = prev.getWeight();
                }
                if (maxPrevWeight > 0) {
                    cargaAnterior = maxPrevWeight;
                    Log.d(TAG, "SeriesAdapter - onBindViewHolder: No hay coincidencia, se muestra máximo previo: " + maxPrevWeight);
                }
            }
        }
        // 2. Si NO hay sesión previa, usa el mejor histórico
        if (cargaAnterior == null) {
            double maxHist = 0;
            for (ExerciseSeries s : historialSeries) {
                if (s.getWeight() > maxHist) maxHist = s.getWeight();
            }
            if (maxHist > 0) {
                cargaAnterior = maxHist;
                Log.d(TAG, "SeriesAdapter - onBindViewHolder: No hay sesión previa, se muestra máximo histórico: " + maxHist);
            }
        }
        // 3. Si sigue sin haber nada, muestra N/A
        if (cargaAnterior != null) {
            holder.previousLoad.setText("" + Util.f1(cargaAnterior));
            actual.setPreviousLoad(cargaAnterior);
        } else {
            holder.previousLoad.setText("N/A");
            actual.setPreviousLoad(null);
            Log.d(TAG, "SeriesAdapter - onBindViewHolder: No hay datos históricos para la serie " + serieNum + " → se muestra N/A");
        }

        // ----- check completada -----
        holder.checkSeriesCompleted.setChecked(actual.isCompleted());
        Log.d(TAG, "SeriesAdapter - onBindViewHolder: Serie "
                + (position + 1) + " completada = " + actual.isCompleted());
    }


    @Override public int getItemCount() { return seriesList.size(); }

    /* =========================================================
     *  List utilities
     * ========================================================= */
    public void reindexSeries(){
        for(int i=0;i<seriesList.size();i++)
            seriesList.get(i).setSeriesNumber(i+1);
        notifyDataSetChanged();
    }

    public void setPrevSession(List<ExerciseSeries> prev) {
        this.prevSession = prev != null ? prev : Collections.emptyList();

        // Mapea cada serie previa por su número de serie
        prevMap.clear();
        for (ExerciseSeries s : this.prevSession) {
            prevMap.put(s.getSeriesNumber(), s);
        }
    }

    /** Devuelve las series completadas hoy (o último hist). */
    public List<ExerciseSeries> getUpdatedSeriesList(){

        List<ExerciseSeries> out=new ArrayList<>();
        for(ExerciseSeries s:seriesList)
            if(s.isCompleted()) out.add(s);

        if(out.isEmpty() && lastHistorySerie!=null)
            out.add(lastHistorySerie);

        if(out.isEmpty() && !historialSeries.isEmpty())
            out.addAll(historialSeries);

        /* ---- LOG de lo que enviamos a ProgresionDinamica ---- */
        Log.d(TAG,"getUpdatedSeriesList() → "+out.size()+" serie(s) devueltas:");
        for(ExerciseSeries s:out){
            Log.d(TAG,"   ↳ "+s);
        }
        return out;
    }

    public void clearLastHistorySerie(){ lastHistorySerie=null; }

    public void clearSuggestions() {
        if (!suggestionActive) return;      // nada que hacer

        seriesList.clear();
        seriesList.addAll(baseSeries);      // ← recupera la copia
        suggestionActive = false;

        reindexSeries();                    // redibuja la tabla
    }


    /* =========================================================
     *  Firebase fetch
     * ========================================================= */
    public void cargarUltimasSeriesDesdeFirebase(String exerciseId,
                                                 Context ctx,
                                                 @Nullable Runnable onFinished) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {                            // 0) seguridad
            Log.e(TAG, "❌ Usuario no autenticado.");
            if (onFinished != null) onFinished.run();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("userExercises")
                .child(exerciseId)
                .child("seriesData");

        /* 1) UNA sola lectura → ref.get()  (equivale a addListenerForSingleValueEvent) */
        ref.get().addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {                // 1‑a error de red
                Log.e(TAG, "❌ Error leyendo historial", task.getException());
                if (onFinished != null) onFinished.run();
                return;
            }

            /* 2) limpiamos antes de copiar ⇒ cero duplicados               */
            historialSeries.clear();

            for (DataSnapshot c : task.getResult().getChildren()) {
                ExerciseSeries s = c.getValue(ExerciseSeries.class);
                if (s != null) {
                    s.setCompleted(true);
                    historialSeries.add(s);
                }
            }

            // dentro de onDataChange(), después de haber llenado `historialSeries`
            Map<String, List<ExerciseSeries>> sesiones = new TreeMap<>();
            for (ExerciseSeries s : historialSeries) {
                String dia = DateFormat.format("yyyyMMdd", s.getDate()).toString();
                sesiones.computeIfAbsent(dia, k -> new ArrayList<>()).add(s);
            }

            List<List<ExerciseSeries>> todas = new ArrayList<>(sesiones.values());
            if (todas.size() >= 2) {
                // índice -2 → la sesión inmediatamente anterior
                setPrevSession(todas.get(todas.size() - 2));
            } else {
                setPrevSession(Collections.emptyList());
            }


            Log.d(TAG, "Firebase → " + historialSeries.size() + " series históricas");

            lastHistorySerie = null;
            bestHistorySerie = null;

            if (!historialSeries.isEmpty()) {

                /* 3) última serie y la que da mejor 1 RM                    */
                lastHistorySerie = historialSeries.get(historialSeries.size() - 1);

                bestHistorySerie = Collections.max(
                        historialSeries,
                        Comparator.comparingDouble(se ->
                                ProgresionDinamica.estimar1RM(
                                        (float) se.getWeight(), se.getReps())));

                Log.d(TAG, "   ↳ Última serie = " + lastHistorySerie);
                Log.d(TAG, "   ↳ Mejor 1RM    = " + bestHistorySerie);

                /* 4) propaga el peso previo a las series de la sesión       */
                double prev = lastHistorySerie.getWeight();
                Log.d(TAG, "HIST usado → " + prev + " kg × "
                        + lastHistorySerie.getReps() + " @RPE " + lastHistorySerie.getRpe());

                for (ExerciseSeries s : seriesList) s.setPreviousLoad(prev);
                notifyDataSetChanged();
            }

            /* 5) callback final                                            */
            if (onFinished != null) onFinished.run();
        });
    }

    /* =========================================================
     *  Sube la sesión y deduplica histórico
     * ========================================================= */

    private boolean validateAllRpe() {
        for (int i = 0; i < seriesList.size(); i++) {
            ExerciseSeries s = seriesList.get(i);

            Number rNum = s.getRpe();            // admite Float/Double/Integer
            if (rNum == null) {
                Log.e(TAG, "RPE nulo en serie " + (i + 1));
                return false;
            }

            float r = rNum.floatValue();
            if (Float.isNaN(r) || r < 5f || r > 10f) {
                Log.e(TAG, "RPE fuera de rango (5–10) en serie " + (i + 1) + ": " + r);
                return false;
            }
        }
        return true;
    }


    /* Guarda ‘list’ en Firebase y ejecuta onFinished cuando termine */
    public void saveToFirebase(List<ExerciseSeries> list, Runnable onFinished) {

        // Guardia: RPE debe estar entre 5 y 10 en todas las series
        if (!validateAllRpe()) {
            Log.e(TAG, "RPE fuera de rango (5–10). No se guarda.");
            if (onFinished != null) onFinished.run();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users").child(uid)
                .child("userExercises").child(exerciseName)   // si usas ID cambia aquí
                .child("seriesData");

        ref.setValue(list, (error, _ref) -> {
            if (error != null) Log.e(TAG,"❌ No se pudo subir", error.toException());
            else               Log.d(TAG,"✅ Sesión subida ("+list.size()+")");
            if (onFinished != null) onFinished.run();
        });
    }




    /** Deja la última serie como referencia y elimina solo las antiguas duplicadas. */
    private void dedupHistory() {
        if (historialSeries.isEmpty()) return;

        ExerciseSeries ultima = historialSeries.get(historialSeries.size()-1);
        historialSeries.clear();
        historialSeries.add(ultima);      // <-- mantiene punto de partida del próximo cálculo
        lastHistorySerie = ultima;
        bestHistorySerie = ultima;        // opcional: fuerza enfoque en el último rendimiento
    }
    // ya existe:
    public void commitAllEdits() { commitAllEdits(null); }

    public void commitAllEdits(@Nullable Runnable onFinished) {

        if (seriesAdapters.isEmpty()) {        // nada que guardar
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



    /* =========================================================
     *  Volcar sugerencias en la lista visible
     * ========================================================= */
    public void setFromSuggest(List<ProgresionDinamica.Serie> sug) {

        Log.d(TAG,"setFromSuggest() → "+sug.size()+" serie(s) sugeridas");

        /* 👉 guarda la tabla actual la PRIMERA vez que entras aquí */
        if (!suggestionActive) {
            baseSeries.clear();
            baseSeries.addAll(seriesList);  // copia de seguridad
        }
        suggestionActive = true;

        /* construye la vista con la sugerencia */
        seriesList.clear();
        int n = 1;
        Double prev = previousLoad;

        for (ProgresionDinamica.Serie s : sug) {
            ExerciseSeries e = new ExerciseSeries(
                    n++, s.repeticiones, s.peso, s.rpe);
            e.setCompleted(false);
            if (prev != null) e.setPreviousLoad(prev);
            seriesList.add(e);
        }
        reindexSeries();                    //   notifyDataSetChanged incluido
    }


    /* =========================================================
     *  ViewHolder
     * ========================================================= */
    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        private boolean editingRpe = false;

        private boolean editingReps   = false;
        private boolean editingWeight = false;

        private final TextView  seriesNumber,previousLoad;
        private final EditText  repsInput,weightInput,rpeInput;
        private final CheckBox  checkSeriesCompleted;

        private final SeriesAdapter         adapter;
        private final String                exerciseName;
        private final TrainingDetailAdapter parentAdapter;

        private boolean isUpdating=false;

        private final TextWatcher repsWatcher = new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                // Solo validar cuando el usuario está editando y no durante el pintado
                if (isUpdating || !editingReps) return;

                String txt = s.toString().trim();
                if (txt.isEmpty()) {
                    repsInput.setError("Reps requeridas (1–30)");
                    return;
                }
                try {
                    int v = Integer.parseInt(txt);
                    if (v < 1 || v > 30) {
                        repsInput.setError("Reps fuera de rango (1–30)");
                        return;
                    }
                    repsInput.setError(null);
                    adapter.seriesList.get(getBindingAdapterPosition()).setReps(v);
                } catch (NumberFormatException e) {
                    repsInput.setError("Reps inválidas");
                }
            }
        };

        private final TextWatcher weightWatcher = new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating || !editingWeight) return;

                String txt = s.toString().trim();
                if (txt.isEmpty()) {
                    // permitir vacío mientras escribe; limpia error
                    weightInput.setError(null);
                    return;
                }
                try {
                    double v = Double.parseDouble(txt);
                    // Tu filtro ya limita 0–500 con 1 decimal. Refuerza el rango aquí.
                    if (v < 0 || v > 500) {
                        weightInput.setError("Peso fuera de rango (0–500)");
                        return;
                    }
                    weightInput.setError(null);
                    adapter.seriesList.get(getBindingAdapterPosition()).setWeight(v);
                } catch (NumberFormatException e) {
                    weightInput.setError("Peso inválido");
                }
            }
        };

        private final TextWatcher rpeWatcher = new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                // No validar cuando el adaptador está pintando o si el usuario NO está editando
                if (isUpdating || !editingRpe) return;

                String txt = s.toString().trim();
                if (txt.isEmpty()) {                 // permite que borre para volver a escribir
                    rpeInput.setError(null);
                    return;
                }
                try {
                    float r = Float.parseFloat(txt);

                    // ⚠️ Mostrar error cuando el usuario escribe 0 o fuera de rango
                    if (r == 0f) {
                        rpeInput.setError("RPE no puede ser 0");
                        return;
                    }
                    if (r < 5f || r > 10f) {
                        rpeInput.setError("RPE debe estar entre 5 y 10");
                        return;
                    }

                    // OK
                    rpeInput.setError(null);
                    adapter.seriesList.get(getBindingAdapterPosition()).setRpe(r);
                } catch (NumberFormatException ignore) {
                    rpeInput.setError("RPE inválido");
                }
            }
        };







        SeriesViewHolder(@NonNull View v,SeriesAdapter a,String exName,
                         TrainingDetailAdapter p){
            super(v);
            adapter       = a;
            exerciseName  = exName;
            parentAdapter = p;

            seriesNumber        = v.findViewById(R.id.seriesNumber);
            repsInput           = v.findViewById(R.id.repsInput);
            weightInput         = v.findViewById(R.id.weightInput);
            rpeInput            = v.findViewById(R.id.rpeInput);
            checkSeriesCompleted= v.findViewById(R.id.checkSeriesCompleted);
            previousLoad        = v.findViewById(R.id.previousLoad);

            repsInput.setFilters(new InputFilter[]{new InputFilterMinMax(1,30)});
            weightInput.setFilters(new InputFilter[]{
                    new DecimalDigitsFilter(0,500,1)});

            rpeInput.setFilters(new InputFilter[]{ new DecimalDigitsFilter(0, 10, 1) });

            v.setOnLongClickListener(l->{
                int pos=getBindingAdapterPosition();
                if(pos!=RecyclerView.NO_POSITION){
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Eliminar serie")
                            .setMessage("¿Deseas eliminar esta serie?")
                            .setPositiveButton("Eliminar",(d,w)->{
                                adapter.seriesList.remove(pos);
                                adapter.reindexSeries();
                                parentAdapter.updateSeriesCount(exerciseName,
                                        adapter.seriesList.size());
                            })
                            .setNegativeButton("Cancelar",null)
                            .show();
                }
                return true;
            });
        }

        void bind(ExerciseSeries s){
            isUpdating = true;

            // Quitar listeners y onChecked para evitar rebotes
            repsInput.removeTextChangedListener(repsWatcher);
            weightInput.removeTextChangedListener(weightWatcher);
            rpeInput.removeTextChangedListener(rpeWatcher);
            checkSeriesCompleted.setOnCheckedChangeListener(null);

            // Reiniciar flags de edición para esta fila
            editingReps = false;
            editingWeight = false;
            editingRpe = false;

            // Pintar encabezados/datos
            seriesNumber.setText(String.valueOf(s.getSeriesNumber()));
            Double prev = s.getPreviousLoad();
            previousLoad.setText(prev != null ? Util.f1(prev) : "N/A");

            // Mostrar valores del modelo (sin ocultar números)
            Integer reps = s.getReps();
            repsInput.setText(reps != null ? String.valueOf(reps) : "0");

            Double weight = s.getWeight();
            weightInput.setText(weight != null ? Util.f1(weight) : "0");

            Number rNum = s.getRpe();
            float rpe = (rNum == null) ? 0f : rNum.floatValue();
            rpeInput.setText(Util.f1(rpe)); // si es 0, se ve 0

            // Limpiar errores visibles al pintar
            repsInput.setError(null);
            weightInput.setError(null);
            rpeInput.setError(null);

            // Check completada
            checkSeriesCompleted.setChecked(s.isCompleted());
            checkSeriesCompleted.setOnCheckedChangeListener((btn, checked) -> {
                if (isUpdating) return;
                s.setCompleted(checked);
                Log.d("SeriesAdapter","Serie " + s.getSeriesNumber() + " completada = " + checked);
            });

            // Activar validación SOLO mientras el usuario edita cada campo
            repsInput.setOnFocusChangeListener((v, hasFocus) -> {
                editingReps = hasFocus;
                if (!hasFocus && repsInput.getError() != null) {
                    // opcional: limpiar error al salir
                    // repsInput.setError(null);
                }
            });

            weightInput.setOnFocusChangeListener((v, hasFocus) -> {
                editingWeight = hasFocus;
                if (!hasFocus && weightInput.getError() != null) {
                    // opcional: limpiar error al salir
                    // weightInput.setError(null);
                }
            });

            rpeInput.setOnFocusChangeListener((v, hasFocus) -> {
                editingRpe = hasFocus;
                if (!hasFocus && rpeInput.getError() != null) {
                    // opcional: limpiar error al salir
                    // rpeInput.setError(null);
                }
            });

            // Volver a añadir watchers (una sola vez)
            repsInput.addTextChangedListener(repsWatcher);
            weightInput.addTextChangedListener(weightWatcher);
            rpeInput.addTextChangedListener(rpeWatcher);

            isUpdating = false;
        }




        /* ----------------------------------------------------- */
        private abstract static class SimpleWatcher implements TextWatcher{
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged   (CharSequence s,int a,int b,int c){}
        }

        /* ------------------- Input filters ------------------- */
        public static class InputFilterMinMax implements InputFilter{
            private final int min,max;
            public InputFilterMinMax(int min,int max){
                this.min=min;this.max=max;
            }
            public CharSequence filter(CharSequence src,int s,int e,
                                       Spanned dst,int ds,int de){
                String res = dst.subSequence(0,ds).toString()
                        + src.subSequence(s,e)
                        + dst.subSequence(de,dst.length());
                try{
                    int val=Integer.parseInt(res);
                    return (val>=min && val<=max)?null:"";
                }catch(NumberFormatException ignore){ return ""; }
            }
        }
        public static class DecimalDigitsFilter implements InputFilter{
            private final double min,max;
            private final int decimals;
            private final Pattern pat;
            public DecimalDigitsFilter(double min,double max,int decimals){
                this.min=min; this.max=max; this.decimals=decimals;
                String dec = decimals>0 ? "(\\.\\d{0,"+decimals+"})?" : "";
                pat=Pattern.compile("^\\d{0,3}"+dec+"$");
            }
            public CharSequence filter(CharSequence src,int s,int e,
                                       Spanned dst,int ds,int de){
                String res = dst.subSequence(0,ds).toString()
                        + src.subSequence(s,e)
                        + dst.subSequence(de,dst.length());
                try{
                    if(pat.matcher(res).matches()){
                        double v=res.isEmpty()?0:Double.parseDouble(res);
                        return (v>=min && v<=max)?null:"";
                    }
                }catch(NumberFormatException ignore){}
                return "";
            }
        }
    }
    public void detach() {
        // Si en el futuro vuelves a usar ValueEventListener persistente,
        // elimina aquí el listener de Firebase.
    }

}
