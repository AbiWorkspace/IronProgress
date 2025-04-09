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

    public List<ExerciseSeries> getSeriesList() {
        return this.seriesList;
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

    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        TextView seriesNumber;
        EditText repsInput, weightInput, rpeInput;
        CheckBox checkSeriesCompleted;
        private boolean isUpdating = false;
        TextView previousLoad; // <-- TextView para mostrar la carga anterior

        public SeriesViewHolder(@NonNull View itemView) {
            super(itemView);
            seriesNumber = itemView.findViewById(R.id.seriesNumber);
            repsInput = itemView.findViewById(R.id.repsInput);
            weightInput = itemView.findViewById(R.id.weightInput);
            rpeInput = itemView.findViewById(R.id.rpeInput);
            checkSeriesCompleted = itemView.findViewById(R.id.checkSeriesCompleted);
            previousLoad = itemView.findViewById(R.id.previousLoad);

            Log.d(TAG, "SeriesViewHolder - Constructor: ViewHolder inicializado correctamente.");

            // Aplicar restricciones de entrada
            repsInput.setFilters(new InputFilter[]{new InputFilterMinMax(1, 30)});
            weightInput.setFilters(new InputFilter[]{new InputFilterMinMax(0, 500)});

            // Listener para rpeInput (0 - 10 con .5 permitido)
            rpeInput.addTextChangedListener(new RPETextWatcher(rpeInput));
        }

        public void bind(ExerciseSeries series) {
            isUpdating = true;
            Log.d(TAG, "SeriesViewHolder - bind: Asignando valores a la serie " + series.getSeriesNumber());

            // 🔢 Número de serie
            seriesNumber.setText(String.valueOf(series.getSeriesNumber()));

            // 🔁 Repeticiones
            repsInput.setText(String.valueOf(series.getReps()));

            // ⚖️ Asignar PESO (solo si es mayor a 0)
            Double weightValue = series.getWeight();
            if (weightValue != null && weightValue > 0) {
                weightInput.setText(String.valueOf(weightValue));
            } else {
                weightInput.setText("");
            }

            // 🔧 Asignar RPE
            rpeInput.setText(String.valueOf(series.getRpe()));

            // ✅ Checkbox completado
            checkSeriesCompleted.setChecked(series.isCompleted());

            // 📌 Mostrar valor de "carga anterior"
            Double previousLoadValue = series.getPreviousLoad();
            if (previousLoadValue != null) {
                if (previousLoadValue % 1 == 0) {
                    // Si es entero (por ej. 25.0)
                    previousLoad.setText(String.valueOf(previousLoadValue.intValue()));
                } else {
                    // Si contiene decimales
                    previousLoad.setText(String.valueOf(previousLoadValue));
                }
                Log.d(TAG, "SeriesViewHolder - bind: Carga anterior asignada → " + previousLoadValue);
            } else {
                previousLoad.setText("N/A");
                Log.w(TAG, "SeriesViewHolder - bind: No se encontró valor de carga anterior para la serie " + series.getSeriesNumber());
            }

            checkSeriesCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "CheckBox cambiado: " + isChecked + " para serie " + series.getSeriesNumber());
                series.setCompleted(isChecked);
            });

            // 🔥 Listener para cambios en RPE
            rpeInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdating) return;
                    try {
                        double rpe = Double.parseDouble(s.toString());
                        series.setRpe(rpe);
                        Log.d(TAG, "SeriesViewHolder - afterTextChanged: Nuevo RPE para serie " + series.getSeriesNumber() + " -> " + rpe);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "❌ Error: RPE ingresado no válido para la serie " + series.getSeriesNumber());
                    }
                }
            });

            // 🔥 Listener para cambios en PESO
            weightInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdating) return;
                    try {
                        double weight = Double.parseDouble(s.toString());
                        series.setWeight(weight);
                        Log.d(TAG, "SeriesViewHolder - afterTextChanged: Nuevo PESO para serie " + series.getSeriesNumber() + " -> " + weight);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "❌ Error: Peso ingresado no válido para la serie " + series.getSeriesNumber());
                    }
                }
            });

            isUpdating = false;
        }

        /**
         * 🔥 Clase para restringir el input de RPE a valores entre 0 y 10, permitiendo solo enteros o con .5
         */
        public static class RPETextWatcher implements TextWatcher {
            private EditText editText;
            private boolean isUpdating = false;

            public RPETextWatcher(EditText editText) {
                this.editText = editText;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                String input = s.toString().trim();
                if (input.isEmpty()) {
                    editText.setText("0");
                } else {
                    try {
                        float value = Float.parseFloat(input);
                        // Solo permitir 0-10 y .5
                        if (value < 0 || value > 10 || (value % 1 != 0 && value % 1 != 0.5)) {
                            editText.setText("0");
                        }
                    } catch (NumberFormatException e) {
                        editText.setText("0");
                    }
                }
                Log.d(TAG, "RPETextWatcher - Validación aplicada: " + editText.getText().toString());
                editText.setSelection(editText.getText().length());
                isUpdating = false;
            }
        }

        public static class InputFilterMinMax implements InputFilter {
            private int min, max;

            public InputFilterMinMax(int min, int max) {
                this.min = min;
                this.max = max;
            }

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    int input = Integer.parseInt(dest.toString() + source.toString());
                    if (isInRange(min, max, input)) return null;
                } catch (NumberFormatException ignored) {}
                return "";
            }

            private boolean isInRange(int min, int max, int input) {
                return input >= min && input <= max;
            }
        }
    }
}
