package com.example.diets.adapters;


import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.dialogs.FoodMenuDialogFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;

import java.util.List;
import java.util.Locale;

public class FoodBlockAdapter extends RecyclerView.Adapter<FoodBlockAdapter.BlockViewHolder> {
    public interface OnFoodChangedListener {
        void onFoodChanged();
    }
    private static final String TAG = "FoodBlockAdapter";

    private final List<FoodBlock> foodBlocks;
    private final Context context;
    private final DashboardFragment parentFragment;
    TextView nutritionSummary; // NUEVO

    public FoodBlockAdapter(List<FoodBlock> foodBlocks, Context context, DashboardFragment parentFragment) {
        Log.d(TAG, "Constructor: Inicializando adaptador con " + foodBlocks.size() + " bloques");
        this.foodBlocks = foodBlocks;
        this.context = context;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public BlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: Inflando vista para bloque de comida");
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_block, parent, false);
        return new BlockViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull BlockViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: Renderizando bloque en posición → " + position);

        /* ======  D A T O S   D E L   B L O Q U E  ====== */
        FoodBlock block = foodBlocks.get(position);
        holder.foodTitle.setText(block.getTitle());
        holder.foodTitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        holder.foodTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                block.setTitle(holder.foodTitle.getText().toString());
                Log.d(TAG, "onBindViewHolder: Título actualizado → " + block.getTitle());
            }
        });

        /* ======  B O T Ó N   “ A Ñ A D I R   A L I M E N T O ”  ====== */
        holder.addFoodItemButton.setOnClickListener(v -> {
            FoodMenuDialogFragment dialog = FoodMenuDialogFragment.newInstance();

            dialog.setOnFoodSelected(result -> {
                Log.d(TAG, "Añadiendo alimento → " + result.name +
                        " | " + result.kcalPer100g + " kcal/100g | "
                        + result.grams + " g seleccionados");

                FoodItem newItem = new FoodItem(
                        result.name,
                        result.grams,
                        result.kcalPer100g,
                        result.protein,
                        result.carbs,
                        result.fat,
                        result.imageUrl // 🔥 ¡Aquí le pasas también la URL de imagen!
                );


                block.addItem(newItem);

                holder.recyclerFoodItems.setAdapter(new FoodItemAdapter(
                        block.getItems(),
                        context,
                        parentFragment,
                        () -> {
                            Log.d(TAG, "🔁 onFoodItemChanged: Cambio detectado → actualizando resumen y totales");
                            updateNutritionSummary(holder, block);
                            parentFragment.recalculateDayTotals();
                        }
                ));


                // 3️⃣ Recalcular y mostrar el resumen nutricional
                updateNutritionSummary(holder, block);

                // 4️⃣ Actualizar totales del día
                parentFragment.recalculateDayTotals();

            });

            dialog.show(parentFragment.getParentFragmentManager(), "FoodMenuDialogFragment");
        });


        /* ======  R E C Y C L E R   I N T E R N O  ====== */
        if (holder.recyclerFoodItems.getLayoutManager() == null) {
            holder.recyclerFoodItems.setLayoutManager(new LinearLayoutManager(context));
        }
        holder.recyclerFoodItems.setAdapter(new FoodItemAdapter(
                block.getItems(),
                context,
                parentFragment,
                () -> {
                    Log.d(TAG, "🔁 onFoodItemChanged: Cambio detectado → actualizando resumen y guardando");
                    updateNutritionSummary(holder, block);
                    parentFragment.recalculateDayTotals();
                    parentFragment.guardarComidasDelDia(parentFragment.getCurrentDate());
                }
        ));


        /* ======  R E S U M E N   I N I C I A L  ====== */
        updateNutritionSummary(holder, block);
    }

    private void updateNutritionSummary(BlockViewHolder h, FoodBlock b) {
        Log.d(TAG, "🧮 updateNutritionSummary: Iniciando resumen nutricional para bloque: " + b.getTitle());

        double totalKcal = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        List<FoodItem> items = b.getItems();
        Log.d(TAG, "🔍 updateNutritionSummary: Total de alimentos en bloque: " + items.size());

        for (FoodItem it : items) {
            Log.d(TAG, "📦 Revisión → " + it.getName() +
                    " | Checked: " + it.isChecked() +
                    " | Gramos: " + it.getGrams());

            if (!it.isChecked()) {
                Log.d(TAG, "⏭️ Alimento omitido por estar desactivado: " + it.getName());
                continue;
            }

            double factor = it.getGrams() / 100.0;
            double itemKcal = it.getKcalPer100g() * factor;
            double itemProtein = it.getProteinPer100g() * factor;
            double itemCarbs = it.getCarbsPer100g() * factor;
            double itemFat = it.getFatPer100g() * factor;

            Log.d(TAG, "📊 Aportación de " + it.getName() + " → " +
                    "Kcal: " + itemKcal + ", P: " + itemProtein +
                    ", C: " + itemCarbs + ", G: " + itemFat);

            totalKcal += itemKcal;
            totalProtein += itemProtein;
            totalCarbs += itemCarbs;
            totalFat += itemFat;
        }

        Log.d(TAG, "✅ Resultado Total → Kcal: " + totalKcal +
                ", Proteínas: " + totalProtein +
                ", Carbs: " + totalCarbs +
                ", Grasas: " + totalFat);

        h.nutritionKcal.setText(String.format(Locale.getDefault(), "Kcal: %.0f", totalKcal));
        h.nutritionProtein.setText(String.format(Locale.getDefault(), "P: %.1fg", totalProtein));
        h.nutritionCarbs.setText(String.format(Locale.getDefault(), "C: %.1fg", totalCarbs));
        h.nutritionFat.setText(String.format(Locale.getDefault(), "G: %.1fg", totalFat));

        Log.d(TAG, "🟢 TextViews actualizados visualmente.");
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: Tamaño total de bloques → " + foodBlocks.size());
        return foodBlocks.size();
    }

    public static class BlockViewHolder extends RecyclerView.ViewHolder {
        EditText foodTitle;
        RecyclerView recyclerFoodItems;

        ImageButton addFoodItemButton;
        TextView nutritionKcal, nutritionProtein, nutritionCarbs, nutritionFat; // NUEVO

        public BlockViewHolder(@NonNull View itemView) {
            super(itemView);
            foodTitle = itemView.findViewById(R.id.foodTitle);
            recyclerFoodItems = itemView.findViewById(R.id.recyclerFoodItems);

            addFoodItemButton = itemView.findViewById(R.id.addFoodItemButton);

            // Asignar los nuevos TextView individuales
            nutritionKcal = itemView.findViewById(R.id.nutritionKcal);
            nutritionProtein = itemView.findViewById(R.id.nutritionProtein);
            nutritionCarbs = itemView.findViewById(R.id.nutritionCarbs);
            nutritionFat = itemView.findViewById(R.id.nutritionFat);

            Log.d(TAG, "BlockViewHolder: ViewHolder inicializado");
        }
    }


}
