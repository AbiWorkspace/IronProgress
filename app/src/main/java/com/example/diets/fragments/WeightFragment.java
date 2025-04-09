package com.example.diets.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeightFragment extends Fragment {

    private EditText weightInput;
    private TextView selectedDateText;
    private Button selectDateButton, saveWeightButton;
    private RecyclerView weightRecyclerView;
    private LineChart weightChart;

    private String selectedDate;
    private List<WeightRecord> weightRecords;
    private WeightAdapter weightAdapter;

    private DatabaseReference databaseReference;
    private String userId;

    public WeightFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weight, container, false);

        // Inicializar Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("weightRecords").child(userId);

        // Inicializar UI
        weightInput = view.findViewById(R.id.weightInput);
        selectedDateText = view.findViewById(R.id.selectedDateText);
        selectDateButton = view.findViewById(R.id.selectDateButton);
        saveWeightButton = view.findViewById(R.id.saveWeightButton);
        weightRecyclerView = view.findViewById(R.id.weightRecyclerView);
        weightChart = view.findViewById(R.id.weightChart);

        // RecyclerView
        weightRecords = new ArrayList<>();
        weightAdapter = new WeightAdapter(weightRecords);
        weightRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        weightRecyclerView.setAdapter(weightAdapter);

        // Eventos
        selectDateButton.setOnClickListener(v -> showDatePickerDialog());
        saveWeightButton.setOnClickListener(v -> saveWeightRecord());

        loadWeightRecords();

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                Log.d("WeightFragment", "🏋️ Navegar a DashboardFragment");

                if (getActivity() instanceof ViewPagerActivity) {
                    ((ViewPagerActivity) getActivity()).switchToDashboardFragment();
                }
                return true;
            }



            if (itemId == R.id.navigation_dashboard) {
                Log.d("WeightFragment", "⚖️ Ya estás en el fragmento de peso");
                return true; // No hacer nada porque ya estás aquí
            }



            return false;
        });
        return view;
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Context context = requireContext();
        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (view, year1, month1, dayOfMonth) -> {
                    selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    selectedDateText.setText("Fecha seleccionada: " + selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveWeightRecord() {
        String weightStr = weightInput.getText().toString().trim();
        if (weightStr.isEmpty() || selectedDate == null) {
            Toast.makeText(getContext(), "Ingresa el peso y selecciona una fecha.", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightStr);
        WeightRecord record = new WeightRecord(weight, selectedDate);

        String recordId = databaseReference.push().getKey();
        if (recordId != null) {
            databaseReference.child(recordId).setValue(record).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    weightRecords.add(record);
                    weightAdapter.notifyDataSetChanged();
                    updateChart();
                    Toast.makeText(getContext(), "Registro guardado.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al guardar el registro.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadWeightRecords() {
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    WeightRecord record = snapshot.getValue(WeightRecord.class);
                    if (record != null) {
                        weightRecords.add(record);
                    }
                }
                weightAdapter.notifyDataSetChanged();
                updateChart();
            }
        });
    }

    private void updateChart() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weightRecords.size(); i++) {
            entries.add(new Entry(i, (float) weightRecords.get(i).getWeight()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Peso (kg)");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        LineData lineData = new LineData(dataSet);
        weightChart.setData(lineData);
        weightChart.invalidate();
    }
}
