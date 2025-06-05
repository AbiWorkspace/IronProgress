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
    private static final String LOG_PREFIX = "WeigntFragment";           // para filtrar en Logcat
    private static final String PREF_STATS = "WeightStats";
    private static final String KEY_MISMATCH_WEEKS   = "mismatchWeeks";
    private static final String KEY_WAITING_PROGRESS = "waitingProgress";
    private static final String PREFS_NAME = "FoodPrefs";
    private static final DateTimeFormatter SP_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

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
    /* ----------------- DATOS & FIREBASE --------------- */
    /* -------------------------------------------------- */
    private final List<WeightRecord> weightRecords = new ArrayList<>();
    private WeightAdapter weightAdapter;
    private DatabaseReference databaseReference;
    private String userId;
    private String userGoal = "";          // perder / ganar / mantener
    private String selectedDate = null;    // dd/MM/yyyy

    /* -------------------------------------------------- */
    /* ---------------- CICLO DE VIDA ------------------- */
    /* -------------------------------------------------- */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_PREFIX, "[onCreateView] ► inicio");

        View v = inflater.inflate(R.layout.fragment_weight, container, false);

        /* ---------- Firebase ---------- */
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("weightRecords")
                .child(userId);
        Log.d(LOG_PREFIX, "[onCreateView] userId=" + userId);

        /* ---------- Referencias UI ---------- */
        bindViews(v);

        /* ---------- Listeners ---------- */
        selectDateButton.setOnClickListener(btn -> showDatePickerDialog());
        saveWeightButton.setOnClickListener(btn -> saveWeightRecord());

        /* ---------- Recycler ---------- */
        weightAdapter = new WeightAdapter(weightRecords,
                (rec, pos) -> showEditDialog(rec, pos));
        weightRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        weightRecyclerView.setAdapter(weightAdapter);
        attachSwipeToDelete();                 // 👈 NUEVO

        /* ---------- Descarga metas y registros ---------- */
        FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("goal")
                .get()
                .addOnSuccessListener(snap -> {
                    userGoal = snap.exists() ? snap.getValue(String.class) : "";
                    loadWeightRecords();
                });

        setupBottomNav(v);
        return v;
    }

    /**
     * Habilita swipe‑to‑delete a la DERECHA con fondo rojo e ícono 🗑️
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

                // 1️⃣ eliminar de Firebase (si existe id)
                WeightRecord rec = weightRecords.get(pos);
                if (rec.getId() != null)
                    databaseReference.child(rec.getId()).removeValue();

                // 2️⃣ eliminar de lista + UI
                weightRecords.remove(pos);
                weightAdapter.notifyItemRemoved(pos);

                // 3️⃣ refrescar gráfica y estadísticas
                // 3️⃣ refrescar gráfica y estadísticas
                if (isAdded() && getContext() != null) updateChart();

                computeWeeklyStats();
            }

            // — dibuja fondo e ícono mientras se desliza — //
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

    /* -------------------------------------------------- */
    /* ---------------- GENERAR DATE PICKER ------------- */
    /* -------------------------------------------------- */
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

    /* -------------------------------------------------- */
    /* ------- GUARDAR / EDITAR REGISTROS --------------- */
    /* -------------------------------------------------- */
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
                if (record.getId()!=null)
                    databaseReference.child(record.getId()).setValue(record);
                Log.d(LOG_PREFIX, "[showEditDialog] guardado id=" + record.getId());
// 3️⃣ refrescar gráfica y estadísticas
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
        return den==0? 0 : num/den;           // kg / índice‑día
    }
    private void saveWeightRecord() {
        Log.d(LOG_PREFIX, "[saveWeightRecord] ► inicio");
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
        float kcal = fetchStoredCalories(selectedDate);
        String id = databaseReference.push().getKey();
        WeightRecord rec = new WeightRecord(id, weight, kcal, selectedDate);

        databaseReference.child(id).setValue(rec)
                .addOnSuccessListener(v -> {
                    Log.d(LOG_PREFIX, "[saveWeightRecord] Firebase OK id=" + id);
                    weightRecords.add(rec);
                    sortByDateDesc();
                    weightAdapter.notifyDataSetChanged();
                    // 3️⃣ refrescar gráfica y estadísticas
                    if (isAdded() && getContext() != null) updateChart();

                    computeWeeklyStats();
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_PREFIX, "[saveWeightRecord] Firebase ERROR", e);
                    Toast.makeText(getContext(),"Error al guardar",Toast.LENGTH_SHORT).show();
                });
    }

    private float fetchStoredCalories(String dateDDMMYYYY) {
        try {
            Date d  = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateDDMMYYYY);
            LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String key = "totKcal_" + ld.format(SP_DATE);
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

    /* -------------------------------------------------- */
    /* ----------- CARGA DE REGISTROS ------------------- */
    /* -------------------------------------------------- */
    private void loadWeightRecords() {
        Log.d(LOG_PREFIX, "[loadWeightRecords] ► inicio");
        databaseReference.get().addOnSuccessListener(ds -> {
            weightRecords.clear();
            for (DataSnapshot sn : ds.getChildren()) {
                WeightRecord r = sn.getValue(WeightRecord.class);
                if (r != null) weightRecords.add(r);
            }
            Log.d(LOG_PREFIX, "[loadWeightRecords] total=" + weightRecords.size());
            sortByDateDesc();
            weightAdapter.notifyDataSetChanged();
            // 3️⃣ refrescar gráfica y estadísticas
            if (isAdded() && getContext() != null) updateChart();

            computeWeeklyStats();
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




    /* -------------------------------------------------- */
    /* --------------- CHART & ESTADÍSTICAS ------------- */
    /* -------------------------------------------------- */
    private void computeWeeklyStats() {
        Log.d(LOG_PREFIX,"[computeWeeklyStats] ► inicio regs="+weightRecords.size());

        /* ‑‑‑ Se necesita al menos 14 días para dos semanas completas ‑‑‑ */
        if(weightRecords.size()<14){
            Log.d(LOG_PREFIX,"[computeWeeklyStats] ⏸ <14 registros");
            return;
        }

        /* 1️⃣ Orden cronológico ascendente ------------------------------------------------------ */
        weightRecords.sort(Comparator.comparing(r->{
            try{return new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).parse(r.getDate());}
            catch(ParseException e){return new Date();}
        }));

        /* 2️⃣ Sub‑listas de 7 días y estadísticos ------------------------------------------------ */
        int n = weightRecords.size();
        List<WeightRecord> week2 = weightRecords.subList(n-7,n);       // semana más reciente
        List<WeightRecord> week1 = weightRecords.subList(n-14,n-7);    // semana previa

        double avgW2 = week2.stream().mapToDouble(WeightRecord::getWeight).average().orElse(0);
        double avgW1 = week1.stream().mapToDouble(WeightRecord::getWeight).average().orElse(avgW2);
        double avgK2 = week2.stream().mapToDouble(WeightRecord::getCalories).average().orElse(0);
        double avgK1 = week1.stream().mapToDouble(WeightRecord::getCalories).average().orElse(avgK2);

        /* 3️⃣ Tendencia REAL – pendiente de regresión 14 d -------------------------------------- */
        double kgPerDay   = slopeKgPerDay(weightRecords.subList(n-14,n));
        double trendKg7   = kgPerDay * 7;                // kg/sem según regresión
        double diffKcal   = (avgK2 - avgK1) * 7;         // superávit / déficit semanal
        double diffKgPred = diffKcal / 7700.0;           // kg esperados por kcal

        Log.d(LOG_PREFIX,String.format(Locale.getDefault(),
                "[computeWeeklyStats] slope=%.4f kg/d  → %.2f kg/sem",kgPerDay,trendKg7));
        Log.d(LOG_PREFIX,String.format(Locale.getDefault(),
                "[computeWeeklyStats] kcalΔ=%.0f  kgPred=%.2f",diffKcal,diffKgPred));

        /* 4️⃣ UI ↴ */
        avgWeightText .setText(String.format(Locale.getDefault(),
                "Peso medio 7 d: %.1f kg",avgW2));
        avgCalText    .setText(String.format(Locale.getDefault(),
                "Calorías medias 7 d: %.0f kcal",avgK2));
        avgCalPrevText.setText(String.format(Locale.getDefault(),
                "Calorías medias semana previa: %.0f kcal",avgK1));
        predictedText .setText(String.format(Locale.getDefault(),
                "Δkg esperado (↓/↑): %.2f kg",diffKgPred));

        String tr = trendKg7> 0.2? "Subiendo 📈" :
                trendKg7<-0.2? "Bajando 📉" : "Estable ⚖️";
        trendText.setText(tr);

        /* 5️⃣ Comparar con la META -------------------------------------------------------------- */
        boolean goalDown = userGoal.toLowerCase().contains("perder");
        boolean goalUp   = userGoal.toLowerCase().contains("ganar");
        boolean goalKeep = !(goalDown||goalUp);

        boolean weightOk = (goalDown && trendKg7<-0.15) ||
                (goalUp   && trendKg7> 0.15) ||
                (goalKeep && Math.abs(trendKg7)<0.15);

        boolean kcalOk   = (goalDown && diffKcal<-350) ||
                (goalUp   && diffKcal> 350) ||
                (goalKeep && Math.abs(diffKcal)<350);

        Log.d(LOG_PREFIX,"[computeWeeklyStats] weightOk="+weightOk+" kcalOk="+kcalOk);

        /* 6️⃣ Contador de “desajustes” y lógicas de espera -------------------------------------- */
        SharedPreferences p = requireContext().getSharedPreferences(PREF_STATS, Context.MODE_PRIVATE);
        int     mismatch = p.getInt   (KEY_MISMATCH_WEEKS ,0);
        boolean waiting  = p.getBoolean(KEY_WAITING_PROGRESS,false);

        if(weightOk && kcalOk){
            mismatch = 0; waiting = false;
            Log.d(LOG_PREFIX,"[computeWeeklyStats] ✔ progreso alineado → reset");
        }else{
            mismatch++;
            Log.d(LOG_PREFIX,"[computeWeeklyStats] ✖ desajuste ("+mismatch+"/2)");
        }

        /* 7️⃣ Ajuste de caloriesToday ----------------------------------------------------------- */
        if(!waiting && mismatch>=2){
            Log.d(LOG_PREFIX,"[computeWeeklyStats] ► evaluando ajuste kcal");

            DatabaseReference kcalRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(userId).child("caloriesToday");

            kcalRef.get().addOnSuccessListener(snap->{
                if(!snap.exists()) return;
                double curr = snap.getValue(Double.class);
                double upd  = curr;                             // por defecto sin cambio

                if(goalDown){                                   // perder
                    if(trendKg7>-0.15) upd = curr*0.95;
                }else if(goalUp){                               // ganar
                    if(trendKg7< 0.15) upd = curr*1.05;
                }else{                                          // mantener
                    if(trendKg7> 0.15) upd = curr*0.95;
                    else if(trendKg7<-0.15) upd = curr*1.05;
                }

                if(upd!=curr){
                    kcalRef.setValue(upd);
                    Log.d(LOG_PREFIX,String.format(Locale.getDefault(),
                            "[computeWeeklyStats] kcal %,.0f → %,.0f",curr,upd));
                }else
                    Log.d(LOG_PREFIX,"[computeWeeklyStats] kcal sin cambio");
            });

            waiting  = true;    // espera hasta ver progreso
            mismatch = 0;       // reinicio
        }

        /* 8️⃣ Persistir estado ------------------------------------------------------------------ */
        p.edit()
                .putInt   (KEY_MISMATCH_WEEKS , mismatch)
                .putBoolean(KEY_WAITING_PROGRESS, waiting)
                .apply();
        Log.d(LOG_PREFIX,"[computeWeeklyStats] estado guardado mismatch="+mismatch+" waiting="+waiting);
    }


    /* -------------------------------------------------- */
    /* -------------- HELPERS VARIOS -------------------- */
    /* -------------------------------------------------- */
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
}
