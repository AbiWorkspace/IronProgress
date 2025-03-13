package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.ExerciseSeries;

import java.util.List;
public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder> {
    private static final String TAG = "SeriesAdapter"; // 🔥 Nombre de la clase para logs
    private List<ExerciseSeries> seriesList;

    public SeriesAdapter(List<ExerciseSeries> seriesList) {
        Log.d(TAG, "SeriesAdapter - Constructor: Recibiendo lista de series.");
        this.seriesList = seriesList;
        Log.d(TAG, "SeriesAdapter - Constructor: Total de series recibidas: " + seriesList.size());
    }

    @NonNull
    @Override
    public SeriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "SeriesAdapter - onCreateViewHolder: Inflando layout para series.");

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_series, parent, false);

        if (view == null) {
            Log.e(TAG, "❌ ERROR: No se pudo inflar item_series.xml. Verifica el archivo XML.");
        } else {
            Log.d(TAG, "✅ Layout inflado correctamente.");
        }

        return new SeriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesViewHolder holder, int position) {
        Log.d(TAG, "SeriesAdapter - onBindViewHolder: Vinculando vista en posición " + position);

        ExerciseSeries series = seriesList.get(position);
        Log.d(TAG, "📌 Serie " + series.getSeriesNumber() + " → Reps: " + series.getReps() + ", Peso: " + series.getWeight() + ", RPE: " + series.getRpe());

        holder.bind(series);
        Log.d(TAG, "✅ Serie vinculada correctamente.");
    }

    @Override
    public int getItemCount() {
        return seriesList.size();
    }

    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "SeriesViewHolder"; // 🔥 Nombre de la clase
        TextView seriesNumber;
        EditText repsInput, weightInput, rpeInput;

        public SeriesViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "SeriesViewHolder - Constructor: Inicializando vistas.");

            seriesNumber = itemView.findViewById(R.id.seriesNumber);
            repsInput = itemView.findViewById(R.id.repsInput);
            weightInput = itemView.findViewById(R.id.weightInput);
            rpeInput = itemView.findViewById(R.id.rpeInput);

            if (seriesNumber == null || repsInput == null || weightInput == null || rpeInput == null) {
                Log.e(TAG, "❌ ERROR: Alguna vista es NULL. Verifica los IDs en item_series.xml.");
            } else {
                Log.d(TAG, "✅ Vistas inicializadas correctamente.");
            }
        }

        public void bind(ExerciseSeries series) {
            Log.d(TAG, "SeriesViewHolder - bind: Configurando datos de la serie.");

            seriesNumber.setText("Serie " + series.getSeriesNumber());
            repsInput.setText(String.valueOf(series.getReps()));
            weightInput.setText(String.valueOf(series.getWeight()));
            rpeInput.setText(String.valueOf(series.getRpe()));

            Log.d(TAG, "✅ Datos de la serie configurados correctamente.");
        }
    }
}
