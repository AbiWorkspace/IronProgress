package com.example.diets.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.model.WeightRecord;
import com.example.diets.adapters.WeightAdapter;
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

    private EditText weightInput;
    private TextView selectedDateText;

    /* NUEVO —— indicadores semanales */
    private TextView avgCalText, avgWeightText, trendText;

    private Button selectDateButton, saveWeightButton;
    private RecyclerView weightRecyclerView;
    private LineChart weightChart;

    private String selectedDate;          // "dd/MM/yyyy"
    private final List<WeightRecord> weightRecords = new ArrayList<>();
    private WeightAdapter weightAdapter;

    private DatabaseReference databaseReference;
    private String userId;

    /* ---- SharedPreferences para las Kcal/día ---- */
    private static final String PREFS_NAME = "FoodPrefs";
    private static final DateTimeFormatter SP_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    public WeightFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View v = inflater.inflate(R.layout.fragment_weight, container, false);

        // Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("weightRecords")
                .child(userId);

        // UI
        weightInput        = v.findViewById(R.id.weightInput);
        selectedDateText   = v.findViewById(R.id.selectedDateText);
        selectDateButton   = v.findViewById(R.id.selectDateButton);
        saveWeightButton   = v.findViewById(R.id.saveWeightButton);
        weightRecyclerView = v.findViewById(R.id.weightRecyclerView);
        weightChart        = v.findViewById(R.id.weightChart);

        /* NUEVOS TextView (añádelos al layout, ver más abajo) */
        avgCalText   = v.findViewById(R.id.avgCalText);
        avgWeightText= v.findViewById(R.id.avgWeightText);
        trendText    = v.findViewById(R.id.trendText);

        // Recycler
        weightAdapter = new WeightAdapter(weightRecords);
        weightRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        weightRecyclerView.setAdapter(weightAdapter);

        // Eventos
        selectDateButton.setOnClickListener(vw -> showDatePickerDialog());
        saveWeightButton  .setOnClickListener(vw -> saveWeightRecord());

        loadWeightRecords();
        setupBottomNav(v);
        return v;
    }

    /* -------------------- Date picker -------------------- */
    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(y, m, d, 0, 0, 0);
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    selectedDate = df.format(picked.getTime());
                    selectedDateText.setText("Fecha seleccionada: " + selectedDate);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        /* Limitar a hoy o fechas pasadas */
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    /* -------------------- Guardar peso -------------------- */
    private void saveWeightRecord() {
        String wStr = weightInput.getText().toString().trim();
        if (wStr.isEmpty() || selectedDate == null) {
            Toast.makeText(getContext(), "Ingresa peso y fecha", Toast.LENGTH_SHORT).show();
            return;
        }
        double weight = Double.parseDouble(wStr);
        WeightRecord rec = new WeightRecord(weight, selectedDate);

        String id = databaseReference.push().getKey();
        if (id == null) return;

        databaseReference.child(id).setValue(rec).addOnSuccessListener(unused -> {
            weightRecords.add(rec);
            weightAdapter.notifyItemInserted(weightRecords.size()-1);
            updateChart();
            computeWeeklyStats();      // ⬅️ NUEVO
            Toast.makeText(getContext(), "Registro guardado", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());
    }

    /* -------------------- Cargar registros -------------------- */
    private void loadWeightRecords() {
        databaseReference.get().addOnSuccessListener(ds -> {
            weightRecords.clear();
            for (DataSnapshot sn : ds.getChildren()) {
                WeightRecord r = sn.getValue(WeightRecord.class);
                if (r != null) weightRecords.add(r);
            }
            weightAdapter.notifyDataSetChanged();
            updateChart();
            computeWeeklyStats();   // ⬅️ inicial
        });
    }

    /* -------------------- Graficar -------------------- */
    private void updateChart() {
        List<Entry> e = new ArrayList<>();
        for (int i=0;i<weightRecords.size();i++)
            e.add(new Entry(i, (float) weightRecords.get(i).getWeight()));

        LineDataSet set = new LineDataSet(e, "Peso (kg)");
        set.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        set.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        weightChart.setData(new LineData(set));
        weightChart.invalidate();
    }

    /* -----------------------------------------------------------
       ✨ NUEVO: cálculo de medias y diagnóstico de tendencia
       ----------------------------------------------------------- */
    private void computeWeeklyStats() {
        if (weightRecords.isEmpty()) return;

        /* 1. Ordenar por fecha (dd/MM/yyyy) */
        weightRecords.sort(Comparator.comparing(r -> {
            try {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(r.getDate());
            } catch (ParseException e) { return new Date(); }
        }));

        /* 2. Tomar los últimos 7 registros de PESO */
        int n = weightRecords.size();
        int from = Math.max(0, n - 7);
        List<WeightRecord> last7 = weightRecords.subList(from, n);
        double avgWeight = last7.stream().mapToDouble(WeightRecord::getWeight).average().orElse(0);

        /* 3. Media calórica de los mismos 7 días */
        SharedPreferences prefs = requireContext().getSharedPreferences("FoodPrefs", Context.MODE_PRIVATE);
        double sumKcal = 0; int daysWithKcal = 0;

        for (WeightRecord wr : last7) {
            // convertir dd/MM/yyyy → yyyy-MM-dd
            try {
                Date d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(wr.getDate());
                LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                float kcal = prefs.getFloat("totKcal_" + ld.format(SP_DATE), -1f);
                if (kcal >= 0) { sumKcal += kcal; daysWithKcal++; }
            } catch (ParseException ignored) {}
        }
        double avgKcal = daysWithKcal == 0 ? 0 : sumKcal / daysWithKcal;

        /* 4. Mostrar en pantalla */
        avgWeightText.setText(String.format(Locale.getDefault(),"Peso medio 7d: %.1f kg", avgWeight));
        avgCalText.setText(String.format(Locale.getDefault(),"Calorías medias 7d: %.0f kcal", avgKcal));

        /* 5. Determinar tendencia (comparar contra semana previa si existe) */
        String trend = "—";
        if (n >= 8) {
            int prevFrom = Math.max(0, n - 14);
            List<WeightRecord> prev7 = weightRecords.subList(prevFrom, from);
            double prevAvg = prev7.stream().mapToDouble(WeightRecord::getWeight).average().orElse(avgWeight);

            double diff = avgWeight - prevAvg;
            if (diff > 0.2)      trend = "Subiendo peso 📈";
            else if (diff < -0.2)trend = "Bajando peso 📉";
            else                 trend = "Manteniendo ⚖️";
        }
        trendText.setText(trend);
    }

    /* -------------------- Bottom nav helper -------------------- */
    private void setupBottomNav(View v) {
        BottomNavigationView bottom = v.findViewById(R.id.bottom_navigation);
        bottom.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                if (getActivity() instanceof ViewPagerActivity)
                    ((ViewPagerActivity) getActivity()).switchToDashboardFragment();
                return true;
            }
            return item.getItemId() == R.id.navigation_dashboard;
        });
    }
}
