package com.example.diets.adapters;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.ExerciseSeries;

import java.util.ArrayList;
import java.util.List;

public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder> {
    private static final String TAG = "SeriesAdapter";
    private List<ExerciseSeries> seriesList;
    private Double previousLoad;

    public Double getPreviousLoad() {
        return previousLoad;
    }

    public void setPreviousLoad(Double previousLoad) {
        this.previousLoad = previousLoad;
    }

    public SeriesAdapter(List<ExerciseSeries> seriesList) {
        Log.d(TAG, "SeriesAdapter - Constructor: Recibiendo lista de series con tamaño " + seriesList.size());
        this.seriesList = seriesList;
    }

    @NonNull
    @Override
    public SeriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "SeriesAdapter - onCreateViewHolder: Creando vista para un nuevo elemento de la lista.");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_series, parent, false);
        return new SeriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesViewHolder holder, int position) {
        ExerciseSeries series = seriesList.get(position);
        Log.d(TAG, "SeriesAdapter - onBindViewHolder: Asignando datos a la serie en posición " + position);
        holder.bind(series);
    }

    @Override
    public int getItemCount() {
        return seriesList.size();
    }

    public List<ExerciseSeries> getSeriesList() {
        return this.seriesList;
    }

    public List<ExerciseSeries> getUpdatedSeriesList() {
        List<ExerciseSeries> completedSeries = new ArrayList<>();
        for (ExerciseSeries series : seriesList) {
            if (series.isCompleted()) {
                completedSeries.add(series);
                Log.d(TAG, "SeriesAdapter - getUpdatedSeriesList: Agregando serie completada - " + series.toString());
            }
        }
        Log.d(TAG, "SeriesAdapter - getUpdatedSeriesList: Total de series completadas a guardar: " + completedSeries.size());
        return completedSeries;
    }

    public void commitAllEdits() {
        Log.d("SeriesAdapter", "commitAllEdits: Guardando cambios en todas las series.");
        for (ExerciseSeries series : seriesList) {
            Log.d("SeriesAdapter", "commitAllEdits: Revisando serie " + series.getSeriesNumber());
        }
    }

    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        TextView seriesNumber;
        EditText repsInput, weightInput, rpeInput;
        CheckBox checkSeriesCompleted;
        TextView previousLoad;
        private boolean isUpdating = false;

        public SeriesViewHolder(@NonNull View itemView) {
            super(itemView);
            seriesNumber = itemView.findViewById(R.id.seriesNumber);
            repsInput = itemView.findViewById(R.id.repsInput);
            weightInput = itemView.findViewById(R.id.weightInput);
            rpeInput = itemView.findViewById(R.id.rpeInput);
            checkSeriesCompleted = itemView.findViewById(R.id.checkSeriesCompleted);
            previousLoad = itemView.findViewById(R.id.previousLoad);

            Log.d(TAG, "SeriesViewHolder - Constructor: ViewHolder inicializado correctamente.");

            repsInput.setFilters(new InputFilter[]{new InputFilterMinMax(1, 30)});
            weightInput.setFilters(new InputFilter[]{new InputFilterMinMax(0, 500)});
        }

        public void bind(ExerciseSeries series) {
            isUpdating = true;

            seriesNumber.setText(String.valueOf(series.getSeriesNumber()));
            previousLoad.setText(series.getPreviousLoad() != null
                    ? (series.getPreviousLoad() % 1 == 0
                    ? String.valueOf(series.getPreviousLoad().intValue())
                    : String.valueOf(series.getPreviousLoad()))
                    : "N/A");

            // Reps
            String currentReps = String.valueOf(series.getReps());
            if (!repsInput.getText().toString().equals(currentReps)) {
                repsInput.setText(currentReps);
            }

            double weight = series.getWeight();
            String currentWeight = (weight > 0) ? String.valueOf(weight) : "";

            if (!weightInput.getText().toString().equals(currentWeight)) {
                weightInput.setText(currentWeight);
            }

            // RPE
            String currentRpe = String.valueOf(series.getRpe());
            if (!rpeInput.getText().toString().equals(currentRpe)) {
                rpeInput.setText(currentRpe);
            }

            checkSeriesCompleted.setChecked(series.isCompleted());

            repsInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isUpdating) return;
                    try {
                        int reps = Integer.parseInt(s.toString());
                        series.setReps(reps);
                        Log.d(TAG, "SeriesViewHolder - afterTextChanged: Nuevo REPS -> " + reps);
                    } catch (NumberFormatException e) {
                        repsInput.setError("Reps inválido");
                        Log.e(TAG, "❌ Error: Reps ingresado no válido");
                    }
                }
            });

            weightInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isUpdating) return;
                    try {
                        double weight = Double.parseDouble(s.toString());
                        series.setWeight(weight);
                        Log.d(TAG, "SeriesViewHolder - afterTextChanged: Nuevo PESO -> " + weight);
                    } catch (NumberFormatException e) {
                        weightInput.setError("Peso inválido");
                        Log.e(TAG, "❌ Error: Peso ingresado no válido");
                    }
                }
            });

            rpeInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (isUpdating) return;
                    try {
                        float rpe = Float.parseFloat(s.toString());
                        if (rpe < 0 || rpe > 10 || (rpe % 1 != 0 && rpe % 1 != 0.5)) {
                            rpeInput.setError("RPE inválido. Usa .5 o entero entre 0–10");
                        } else {
                            series.setRpe(rpe);
                            Log.d(TAG, "SeriesViewHolder - afterTextChanged: Nuevo RPE -> " + rpe);
                        }
                    } catch (NumberFormatException e) {
                        rpeInput.setError("RPE inválido");
                        Log.e(TAG, "❌ Error: RPE ingresado no válido");
                    }
                }
            });

            checkSeriesCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                series.setCompleted(isChecked);
                Log.d(TAG, "CheckBox cambiado: " + isChecked + " para serie " + series.getSeriesNumber());
            });

            isUpdating = false;
        }

        public static class InputFilterMinMax implements InputFilter {
            private final int min, max;

            public InputFilterMinMax(int min, int max) {
                this.min = min;
                this.max = max;
            }

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    int input = Integer.parseInt(dest.toString() + source.toString());
                    if (input >= min && input <= max) return null;
                } catch (NumberFormatException ignored) {}
                return "";
            }
        }
    }
}
