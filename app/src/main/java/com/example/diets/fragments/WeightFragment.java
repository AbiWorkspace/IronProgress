package com.example.diets.fragments;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.model.WeightRecord;
import com.example.diets.adapters.WeightAdapter;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class WeightFragment extends Fragment {

    /* -------------------------------------------------- */
    /* ------------------- CONSTANTES ------------------- */
    /* -------------------------------------------------- */
    private static final String KEY_WAITING_PROGRESS = "waiting_progress";
    private static final String KEY_WAITING_DATE     = "waiting_date"; // epochDay del último ajuste
    private static final int WAIT_DAYS_AFTER_ADJUST  = 7;

    private static final String LOG_PREFIX = "WeightFragment";
    private static final String PREF_STATS = "WeightStats";
    private static final String KEY_MISMATCH_WEEKS = "mismatchWeeks";
    private static final String KEY_LAST_ADJ_DATE  = "lastAdjustmentDate";

    private static final String PREFS_NAME = "FoodPrefs"; // donde guardas los totales diarios

    private static final DateTimeFormatter SP_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    private static final DateTimeFormatter DF_IN   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static long daysBetween(LocalDate a, LocalDate b){
        return java.time.temporal.ChronoUnit.DAYS.between(a, b);
    }
    private static LocalDate parseDDMMYYYY(String d){
        return LocalDate.parse(d, DF_IN);
    }

    /* -------------------------------------------------- */
    /* ------------- MODO COACH / UID EFECTIVO ---------- */
    /* -------------------------------------------------- */
    private boolean isCoachMode = false;
    private String effectiveUid = null;

    /* -------------------------------------------------- */
    /* ----------------- ATRIBUTOS UI ------------------- */
    /* -------------------------------------------------- */
    private EditText  weightInput;
    private TextView  selectedDateText, avgCalText, avgWeightText,
            trendText, avgCalPrevText, predictedText;
    private Button    selectDateButton, saveWeightButton;
    private RecyclerView weightRecyclerView;
    private LineChart weightChart;

    /* -------------------------------------------------- */
    /* ----------------- DATOS & FIREBASE --------------- */
    /* -------------------------------------------------- */
    private final List<WeightRecord> weightRecords = new ArrayList<>();
    private WeightAdapter weightAdapter;
    private DatabaseReference databaseReference; // -> /weightRecords/{effectiveUid}
    private String userGoal = "";          // perder / ganar / mantener
    private String selectedDate = null;    // dd/MM/yyyy

    /* ===================== CICLO DE VIDA ===================== */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Determinar UID efectivo desde argumentos (modo coach) o usuario actual
        Bundle args = getArguments();
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();

        if (args != null) {
            isCoachMode = args.getBoolean("coachMode", false);
            if (isCoachMode) {
                effectiveUid = args.getString("targetUid");
                Log.i(LOG_PREFIX, "MODO COACH. effectiveUid=" + effectiveUid);
            } else {
                effectiveUid = (current != null) ? current.getUid() : null;
                Log.i(LOG_PREFIX, "MODO USUARIO (args). effectiveUid=" + effectiveUid);
            }
        } else {
            isCoachMode = false;
            effectiveUid = (current != null) ? current.getUid() : null;
            Log.i(LOG_PREFIX, "MODO USUARIO (sin args). effectiveUid=" + effectiveUid);
        }

        if (effectiveUid == null) {
            Log.e(LOG_PREFIX, "ERROR: effectiveUid es null. No se podrán cargar/guardar datos.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_PREFIX, "[onCreateView] ► inicio");

        View v = inflater.inflate(R.layout.fragment_weight, container, false);

        /* ---------- Referencias UI ---------- */
        bindViews(v);

        /* ---------- Listeners ---------- */
        selectDateButton.setOnClickListener(btn -> showDatePickerDialog());
        saveWeightButton.setOnClickListener(btn -> saveWeightRecord());

        /* ---------- Recycler ---------- */
        weightAdapter = new WeightAdapter(weightRecords,
                (rec, pos) -> showEditDialog(rec, pos));

        LinearLayoutManager lm = new LinearLayoutManager(
                getContext(), LinearLayoutManager.VERTICAL, true); // reverseLayout = true
        lm.setStackFromEnd(true);

        weightRecyclerView.setLayoutManager(lm);
        weightRecyclerView.setAdapter(weightAdapter);
        weightRecyclerView.setNestedScrollingEnabled(false);

        setupBottomNav(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        if (effectiveUid == null) {
            if (isAdded()) Toast.makeText(getContext(), "No se pudo identificar al usuario.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inicializa referencia Firebase con effectiveUid
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("weightRecords")
                .child(effectiveUid);

        // Habilita swipe delete (usa databaseReference)
        attachSwipeToDelete();

        // Descarga objetivo y luego registros
        FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid).child("goal")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    userGoal = snap.exists() ? snap.getValue(String.class) : "";
                    loadWeightRecords();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(LOG_PREFIX, "Error leyendo goal", e);
                    loadWeightRecords();
                });
    }

    /**
     * Habilita swipe-to-delete a la DERECHA con fondo rojo e ícono 🗑️
     */
    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT) {

            @Override public boolean onMove(@NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            @NonNull RecyclerView.ViewHolder tgt) {
                return false;   // no drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                // 1️⃣ eliminar de Firebase (si existe id y tenemos referencia)
                WeightRecord rec = weightRecords.get(pos);
                if (databaseReference != null && rec.getId() != null) {
                    databaseReference.child(rec.getId()).removeValue();
                }

                // 2️⃣ eliminar de lista + UI
                weightRecords.remove(pos);
                weightAdapter.notifyItemRemoved(pos);

                // 3️⃣ refrescar gráfica y estadísticas
                if (isAdded() && getContext() != null) updateChart();
                computeWeeklyStats();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int actionState, boolean isActive) {

                View item = vh.itemView;
                Paint paint = new Paint();
                paint.setColor(ContextCompat.getColor(requireContext(), R.color.red_600));

                // fondo
                RectF bg = new RectF(item.getLeft(), item.getTop(),
                        item.getLeft() + dX, item.getBottom());
                c.drawRect(bg, paint);

                // ícono 🗑️ centrado
                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
                int margin = (item.getHeight() - icon.getIntrinsicHeight()) / 2;
                int top    = item.getTop() + margin;
                int bottom = top + icon.getIntrinsicHeight();
                int left   = item.getLeft() + margin;
                int right  = left + icon.getIntrinsicWidth();
                icon.setBounds(left, top, right, bottom);
                icon.setTint(Color.WHITE);
                icon.draw(c);

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
            }
        };

        new ItemTouchHelper(cb).attachToRecyclerView(weightRecyclerView);
    }

    private void bindViews(View v) {
        weightInput        = v.findViewById(R.id.weightInput);
        selectedDateText   = v.findViewById(R.id.selectedDateText);
        selectDateButton   = v.findViewById(R.id.selectDateButton);
        saveWeightButton   = v.findViewById(R.id.saveWeightButton);
        weightRecyclerView = v.findViewById(R.id.weightRecyclerView);
        weightChart        = v.findViewById(R.id.weightChart);

        avgCalPrevText = v.findViewById(R.id.avgCalPrevText);
        predictedText  = v.findViewById(R.id.predictedText);
        avgCalText     = v.findViewById(R.id.avgCalText);
        avgWeightText  = v.findViewById(R.id.avgWeightText);
        trendText      = v.findViewById(R.id.trendText);
    }

    /* ---------------------- DATE PICKER ---------------------- */
    private void showDatePickerDialog() {
        Log.d(LOG_PREFIX, "[showDatePickerDialog] ► abierto");
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(y, m, d, 0, 0, 0);
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    selectedDate = df.format(picked.getTime());
                    selectedDateText.setText("Fecha seleccionada: " + selectedDate);
                    Log.d(LOG_PREFIX, "[showDatePickerDialog] fecha=" + selectedDate);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /* ----------------- GUARDAR / EDITAR REGISTROS ----------------- */
    private void showEditDialog(WeightRecord record, int position) {
        Log.d(LOG_PREFIX, "[showEditDialog] id=" + record.getId());
        Dialog d = new Dialog(requireContext());
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_edit_weight);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText inpW = d.findViewById(R.id.editWeightInput);
        EditText inpC = d.findViewById(R.id.editCaloriesInput);
        Button   save = d.findViewById(R.id.saveButton);
        Button   cancel = d.findViewById(R.id.cancelButton);

        inpW.setText(String.valueOf(record.getWeight()));
        inpC.setText(String.valueOf(record.getCalories()));

        cancel.setOnClickListener(v -> { Log.d(LOG_PREFIX, "[showEditDialog] cancel");
            d.dismiss(); });

        save.setOnClickListener(v -> {
            try {
                double newW = Double.parseDouble(inpW.getText().toString());
                double newC = Double.parseDouble(inpC.getText().toString());
                record.setWeight(newW);
                record.setCalories(newC);
                weightRecords.set(position, record);
                weightAdapter.notifyItemChanged(position);
                if (databaseReference != null && record.getId()!=null)
                    databaseReference.child(record.getId()).setValue(record);
                Log.d(LOG_PREFIX, "[showEditDialog] guardado id=" + record.getId());
                if (isAdded() && getContext() != null) updateChart();
                computeWeeklyStats();
                d.dismiss();
            } catch (Exception ex) {
                Log.e(LOG_PREFIX, "[showEditDialog] ERROR parsing values", ex);
                Toast.makeText(getContext(),"Valores inválidos",Toast.LENGTH_SHORT).show();
            }
        });
        d.show();
    }

    private static double slopeKgPerDay(List<WeightRecord> list){
        int n=list.size();
        if(n<2) return 0;
        double sumX=0,sumY=0,sumXY=0,sumXX=0;
        for(int i=0;i<n;i++){
            double x=i, y=list.get(i).getWeight();
            sumX += x;        sumY += y;
            sumXY+= x*y;      sumXX+= x*x;
        }
        double num = n*sumXY - sumX*sumY;
        double den = n*sumXX - sumX*sumX;
        return den==0? 0 : num/den;
    }

    private void saveWeightRecord() {
        Log.d(LOG_PREFIX, "[saveWeightRecord] ► inicio");

        if (databaseReference == null || effectiveUid == null) {
            if (isAdded()) Toast.makeText(getContext(),"No se ha inicializado el usuario.",Toast.LENGTH_SHORT).show();
            return;
        }

        String wStr = weightInput.getText().toString().trim();
        if (wStr.isEmpty() || selectedDate == null) {
            Log.w(LOG_PREFIX, "[saveWeightRecord] peso o fecha vacío");
            Toast.makeText(getContext(),"Ingresa peso y fecha",Toast.LENGTH_SHORT).show();
            return;
        }
        if (hasRecordForDate(selectedDate)) {
            Log.w(LOG_PREFIX, "[saveWeightRecord] registro duplicado " + selectedDate);
            Toast.makeText(getContext(),"Ya existe un registro para esa fecha",Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(wStr);
        if(weight <= 20 || weight > 300) {
            Toast.makeText(getContext(), "Peso fuera de rango", Toast.LENGTH_SHORT).show();
            return;
        }
        LocalDate date = LocalDate.parse(selectedDate, DF_IN);
        if(date.isAfter(LocalDate.now())) {
            Toast.makeText(getContext(), "No puedes registrar fechas futuras", Toast.LENGTH_SHORT).show();
            return;
        }
        float kcal = fetchStoredCalories(selectedDate);
        String id = databaseReference.push().getKey();
        WeightRecord rec = new WeightRecord(id, weight, kcal, selectedDate);

        databaseReference.child(id).setValue(rec)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Log.d(LOG_PREFIX, "[saveWeightRecord] Firebase OK id=" + id);
                    weightRecords.add(rec);
                    sortByDateDesc();
                    weightAdapter.notifyDataSetChanged();
                    updateChart();
                    computeWeeklyStats();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(LOG_PREFIX, "[saveWeightRecord] Firebase ERROR", e);
                    Toast.makeText(getContext(),"Error al guardar",Toast.LENGTH_SHORT).show();
                });

        Log.d(LOG_PREFIX, "Calorías obtenidas para " + selectedDate + ": " + kcal);
    }

    /**
     * Busca en SharedPreferences (FoodPrefs) las kcal del día (clave compatible con DashboardFragment):
     * Formato: "totK_{effectiveUid}_{yyyy-MM-dd}"
     */
    private float fetchStoredCalories(String dateDDMMYYYY) {
        if (!isAdded() || getContext() == null || effectiveUid == null) return 0f;

        try {
            Date d  = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateDDMMYYYY);
            LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String key = "totK_" + effectiveUid + "_" + ld.format(SP_DATE);

            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (prefs.contains(key)) {
                float kcal = prefs.getFloat(key, 0f);
                Log.d(LOG_PREFIX, "[fetchStoredCalories] key=" + key + " kcal=" + kcal);
                return kcal;
            }
            Log.d(LOG_PREFIX, "[fetchStoredCalories] key=" + key + " sin datos");
        } catch (Exception ex) {
            Log.e(LOG_PREFIX, "[fetchStoredCalories] parse FAIL", ex);
        }
        return 0f;
    }

    /* --------------------- CARGA DE REGISTROS --------------------- */
    private void loadWeightRecords() {
        if (databaseReference == null) return;

        Log.d(LOG_PREFIX, "[loadWeightRecords] ► inicio");
        databaseReference.get().addOnSuccessListener(ds -> {
            if (!isAdded()) return;

            weightRecords.clear();
            for (DataSnapshot sn : ds.getChildren()) {
                WeightRecord r = sn.getValue(WeightRecord.class);
                if (r != null) weightRecords.add(r);
            }
            Log.d(LOG_PREFIX, "[loadWeightRecords] total=" + weightRecords.size());
            sortByDateDesc();
            weightAdapter.notifyDataSetChanged();
            updateChart();
            computeWeeklyStats();
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Log.e(LOG_PREFIX, "Error al cargar weightRecords", e);
        });
    }

    private void sortByDateDesc() {
        Log.d(LOG_PREFIX, "[sortByDateDesc] ordenando");
        weightRecords.sort((a,b)->{
            try {
                Date da = new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).parse(a.getDate());
                Date db = new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).parse(b.getDate());
                return db.compareTo(da);
            } catch (Exception e){ return 0; }
        });
    }

    private void updateChart() {
        if (!isAdded()) return;

        Log.d(LOG_PREFIX,"[updateChart] puntos=" + weightRecords.size());

        // 1️⃣ Ordenar por fecha ASC: más antiguo a la izquierda
        weightRecords.sort(Comparator.comparing(r -> {
            try {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(r.getDate());
            } catch (Exception e) {
                return new Date();
            }
        }));

        // 2️⃣ Construir entradas en orden normal
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weightRecords.size(); i++) {
            entries.add(new Entry(i, (float) weightRecords.get(i).getWeight()));
        }

        LineDataSet set = new LineDataSet(entries, "Peso (kg)");
        set.setLineWidth(2.5f);
        set.setCircleRadius(4f);
        set.setCircleHoleRadius(2f);
        set.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        set.setCircleColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        Drawable fill = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fill);
        set.setFillDrawable(fill);

        // 3️⃣ Etiquetas del eje X: día/mes
        weightChart.getXAxis().setGranularity(1f);
        weightChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx < 0 || idx >= weightRecords.size()) return "";
                String[] parts = weightRecords.get(idx).getDate().split("/");
                return parts[0] + "/" + parts[1]; // día/mes
            }
        });

        weightChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        weightChart.getXAxis().setTextColor(Color.WHITE);

        // 4️⃣ Apariencia general
        weightChart.getAxisLeft().setTextColor(Color.WHITE);
        weightChart.getAxisRight().setEnabled(false);
        weightChart.getLegend().setTextColor(Color.WHITE);
        weightChart.getDescription().setEnabled(false);

        // 5️⃣ Flechas: primer valor = inicio | último valor = actual
        if (!entries.isEmpty()) {
            LimitLine inicio = new LimitLine(entries.get(0).getY(), "▲ inicio");
            inicio.setLineColor(Color.TRANSPARENT);
            inicio.setTextColor(Color.WHITE);
            inicio.setTextSize(10f);

            LimitLine actual = new LimitLine(entries.get(entries.size() - 1).getY(), "▼ actual");
            actual.setLineColor(Color.TRANSPARENT);
            actual.setTextColor(Color.WHITE);
            actual.setTextSize(10f);

            YAxis left = weightChart.getAxisLeft();
            left.removeAllLimitLines();
            left.addLimitLine(inicio);
            left.addLimitLine(actual);
        }

        // 6️⃣ Mostrar gráfica
        weightChart.setData(new LineData(set));
        weightChart.animateX(700);
        weightChart.invalidate();
    }

    private void computeWeeklyStats() {
        if (!isAdded() || getContext() == null) return;

        Log.d(LOG_PREFIX, "[computeWeeklyStats] ► inicio regs=" + weightRecords.size());

        // --- MODULAR: Bloque de espera tras ajuste (per-user con prefKey) ---
        SharedPreferences p = requireContext().getSharedPreferences(PREF_STATS, Context.MODE_PRIVATE);
        boolean waiting = p.getBoolean(prefKey(KEY_WAITING_PROGRESS), false);
        long lastAdjustDay = p.getLong(prefKey(KEY_WAITING_DATE), 0);
        long today = LocalDate.now().toEpochDay();
        int daysLeft = 0;

        if (waiting) {
            long daysSince = today - lastAdjustDay;
            daysLeft = (int) (WAIT_DAYS_AFTER_ADJUST - daysSince);
            if (daysLeft > 0) {
                Log.d(LOG_PREFIX, "⏳ esperando " + daysLeft + " d tras ajuste");
            } else {
                Log.d(LOG_PREFIX, "🟢 Fin del periodo de espera. Se reanuda el análisis.");
                waiting = false;
                p.edit().putBoolean(prefKey(KEY_WAITING_PROGRESS), false).apply();
            }
        }

        // --- Checar pesajes suficientes en los últimos 14 días ---
        long last14 = weightRecords.stream()
                .filter(r -> daysBetween(parseDDMMYYYY(r.getDate()), LocalDate.now()) < 14)
                .count();
        Log.d(LOG_PREFIX, "[computeWeeklyStats] Pesajes últimos 14 días: " + last14);
        if (last14 < 6) {
            avgWeightText.setText("Peso medio 7 d: —");
            avgCalText.setText("Calorías medias 7 d: —");
            avgCalPrevText.setText("Calorías semana previa: —");
            predictedText.setText("Peso esperado: —");
            trendText.setText("—");
            return;
        }

        // --- Cálculo de medias semanales ---
        List<Double> avgs = computeWeeklyAverages(); // hasta 3 medias
        Log.d(LOG_PREFIX, "[computeWeeklyStats] medias disponibles: " + avgs.size());
        if (avgs.size() < 3) {
            avgWeightText.setText("Peso medio 7 d: —");
            avgCalText.setText("Calorías medias 7 d: —");
            avgCalPrevText.setText("Calorías semana previa: —");
            predictedText.setText("Peso esperado: —");
            trendText.setText("—");
            return;
        }

        double avgW3 = avgs.get(2);   // más reciente
        double avgW2 = avgs.get(1);
        double avgW1 = avgs.get(0);

        // --- Tendencia semanal (peso) ---
        double kgPerWeek = avgW3 - avgW2;
        double trendKg7  = kgPerWeek;

        // --- Calorías medias ---
        double avgK3 = kcalAverageForRange(-7);
        double avgK2 = kcalAverageForRange(-14);
        double avgK1 = kcalAverageForRange(-21);
        double diffKcal   = (avgK3 - avgK2) * 7;
        double diffKgPred = diffKcal / 7700.0;

        // --- UI: SIEMPRE actualizar (aunque esté esperando) ---
        avgWeightText.setText(String.format(Locale.getDefault(),
                "Peso medio 7 d: %.1f kg", avgW3));
        avgCalText.setText(String.format(Locale.getDefault(),
                "Calorías medias 7 d: %.0f kcal", avgK3));
        avgCalPrevText.setText(String.format(Locale.getDefault(),
                "Calorías semana previa: %.0f kcal", avgK2));

        if (diffKgPred == 0){
            predictedText.setText("Sin cambio calórico entre semanas — predicción no disponible");
        } else {
            predictedText.setText(String.format(Locale.getDefault(),
                    "Cambio estimado por calorías: %.2f kg\nCambio real observado: %.2f kg",
                    diffKgPred, trendKg7));
        }

        trendText.setText(trendKg7 > 0.2 ? "Subiendo Peso" :
                trendKg7 < -0.2 ? "Perdiendo Peso " : "Peso Estable ");

        // --- Comparar con meta ---
        boolean goalDown = userGoal.toLowerCase().contains("perder");
        boolean goalUp   = userGoal.toLowerCase().contains("ganar");
        boolean goalKeep = !(goalDown || goalUp);

        boolean weightOk = (goalDown && kgPerWeek <= -0.5 * avgW3 * 0.01) ||   // -0.5 %/sem
                (goalUp   && kgPerWeek >= 0.25) ||                              // ≥250 g/sem
                (goalKeep && Math.abs(kgPerWeek) < 0.15);                        // ±150 g/sem

        boolean kcalOk   = (goalDown && diffKcal < -350) ||
                (goalUp   && diffKcal > 350)  ||
                (goalKeep && Math.abs(diffKcal) < 350);

        // --- Mismatch/espera y ajuste ---
        int mismatch = p.getInt(prefKey(KEY_MISMATCH_WEEKS), 0);

        if (weightOk && kcalOk) {
            mismatch = 0;
        } else {
            mismatch++;
            Log.d(LOG_PREFIX, String.format(Locale.getDefault(), "✖ desajuste (mismatch=%d/2) [pesoOK=%b kcalOK=%b]", mismatch, weightOk, kcalOk));
        }

        // --- Ajuste si corresponde y no estamos en espera ---
        if (!waiting && mismatch >= 2) {
            ajustarCaloriasSegunMeta(trendKg7, goalDown, goalUp);
            waiting = true;
            mismatch = 0;
            long adjToday = LocalDate.now().toEpochDay();
            Log.d(LOG_PREFIX, "🔄 Ajuste de calorías realizado. Esperando " + WAIT_DAYS_AFTER_ADJUST + " días.");
            p.edit()
                    .putBoolean(prefKey(KEY_WAITING_PROGRESS), true)
                    .putLong(prefKey(KEY_WAITING_DATE), adjToday)
                    .apply();
        }

        // Guardar estado por usuario
        p.edit()
                .putInt(prefKey(KEY_MISMATCH_WEEKS), mismatch)
                .putBoolean(prefKey(KEY_WAITING_PROGRESS), waiting)
                .apply();

        Log.d(LOG_PREFIX, "[computeWeeklyStats] ► fin");
    }

    /**
     * Ajusta la meta calórica diaria en Firebase según la tendencia de peso.
     *
     * @param trendKg7  pendiente de la media semanal (kg / 7 d)
     * @param goalDown  true → meta “perder peso”
     * @param goalUp    true → meta “ganar músculo”
     */
    private void ajustarCaloriasSegunMeta(double trendKg7,
                                          boolean goalDown,
                                          boolean goalUp) {

        if (effectiveUid == null) return;

        DatabaseReference kcalRef = FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid).child("caloriesToday");

        kcalRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists() || !isAdded()) return;

            double curr = snap.getValue(Double.class);
            double upd  = curr; // por defecto sin cambio

            if (goalDown) {                              // perder peso
                if (trendKg7 > -0.15) upd = curr * 0.95;
            } else if (goalUp) {                         // ganar músculo
                if (trendKg7 <  0.15) upd = curr * 1.05;
            } else {                                     // mantener peso
                if (trendKg7 >  0.15) upd = curr * 0.95;
                else if (trendKg7 < -0.15) upd = curr * 1.05;
            }

            if (upd != curr) {
                kcalRef.setValue(upd);
                Log.d(LOG_PREFIX, String.format(Locale.getDefault(),
                        "[ajustarCalorias] kcal %,.0f → %,.0f", curr, upd));

                // registra fecha del ajuste (per-user)
                SharedPreferences ps = requireContext()
                        .getSharedPreferences(PREF_STATS, Context.MODE_PRIVATE);
                ps.edit()
                        .putString(prefKey(KEY_LAST_ADJ_DATE), LocalDate.now().format(SP_DATE))
                        .apply();
            } else {
                Log.d(LOG_PREFIX, "[ajustarCalorias] kcal sin cambio");
            }
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Log.e(LOG_PREFIX, "Error leyendo caloriesToday", e);
        });
    }

    /** Calcula kcal promedio en los 7 días terminando en offset (-7,-14,-21). */
    private double kcalAverageForRange(int offsetFromToday){
        LocalDate to   = LocalDate.now().plusDays(offsetFromToday); // inclusive
        LocalDate from = to.minusDays(7);
        return weightRecords.stream()
                .filter(r -> {
                    LocalDate d = parseDDMMYYYY(r.getDate());
                    return !d.isBefore(from) && d.isBefore(to);
                })
                .mapToDouble(WeightRecord::getCalories)
                .average()
                .orElse(0);
    }

    /**
     * Devuelve las medias de peso de hasta las 3 últimas semanas.
     * Cada media se calcula solo si hay ≥3 pesajes en esos 7 días.
     * La lista resultante queda ordenada: [sem-3, sem-2, sem-1]
     */
    private List<Double> computeWeeklyAverages() {
        List<Double> avgs = new ArrayList<>();
        int n = weightRecords.size();
        if (n == 0) return avgs;

        // ordenar ASC (antiguo→reciente)
        weightRecords.sort(Comparator.comparing(r -> parseDDMMYYYY(r.getDate())));

        for (int start = n - 21; start < n; start += 7) {
            if (start < 0) continue;
            int end = Math.min(start + 7, n);
            List<WeightRecord> slice = weightRecords.subList(start, end);
            if (slice.size() >= 3) {
                double avg = slice.stream()
                        .mapToDouble(WeightRecord::getWeight)
                        .average()
                        .orElse(0);
                avgs.add(avg);
            }
        }
        return avgs; // 0-3 medias
    }

    /* -------------------------- HELPERS -------------------------- */
    private boolean hasRecordForDate(String d){
        for (WeightRecord r:weightRecords) if (d.equals(r.getDate())) return true;
        return false;
    }

    private void setupBottomNav(View v){
        BottomNavigationView bottom=v.findViewById(R.id.bottom_navigation);
        bottom.setOnNavigationItemSelectedListener(item->{
            Log.d(LOG_PREFIX,"[setupBottomNav] click id="+item.getItemId());
            if(item.getItemId()==R.id.navigation_home){
                if(getActivity() instanceof ViewPagerActivity)
                    ((ViewPagerActivity)getActivity()).switchToDashboardFragment();
                return true;
            }
            return item.getItemId()==R.id.navigation_dashboard;
        });
    }

    /** Prefijo por usuario para claves en SharedPreferences */
    private String prefKey(String base) {
        return (effectiveUid == null) ? base : (effectiveUid + "_" + base);
    }
}