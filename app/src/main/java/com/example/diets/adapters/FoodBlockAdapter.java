package com.example.diets.adapters;


import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.dialogs.FoodMenuDialogFragment;

import com.example.diets.fragments.DashboardFragment;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodBlockAdapter extends RecyclerView.Adapter<FoodBlockAdapter.BlockViewHolder> {

    private static final String TAG = "FoodBlockAdapter";

    private final List<FoodBlock> foodBlocks;
    private final Context context;
    private final DashboardFragment parentFragment;

    public FoodBlockAdapter(List<FoodBlock> foodBlocks, Context context, DashboardFragment parentFragment) {
        Log.d(TAG, "FoodBlockAdapter - Constructor: Inicializando adaptador con " + foodBlocks.size() + " bloques");
        this.foodBlocks = foodBlocks;
        this.context = context;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public BlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "FoodBlockAdapter - onCreateViewHolder: Inflando vista para bloque de comida");
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_block, parent, false);
        return new BlockViewHolder(view);
    }
    public interface OnFoodChangedListener {
        void onFoodChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull BlockViewHolder holder, int position) {
        Log.d(TAG, "FoodBlockAdapter - onBindViewHolder: Renderizando bloque en posición → " + position);

        FoodBlock block = foodBlocks.get(position);
        holder.foodTitle.setText(block.getTitle());
        holder.foodTitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        // Separar recetas y productos
        List<FoodItem> recetas = new ArrayList<>();
        List<FoodItem> productos = new ArrayList<>();
        for (FoodItem item : block.getItems()) {
            (item.isRecipe() ? recetas : productos).add(item);
        }

        // Adaptador de productos
        FoodItemAdapter productosAdapter = new FoodItemAdapter(
                productos, context, parentFragment, () -> {
            updateNutritionSummary(holder, block);
            parentFragment.recalculateDayTotals();
        }, block
        );
        holder.rvProducts.setLayoutManager(new LinearLayoutManager(context));
        holder.rvProducts.setAdapter(productosAdapter);

        ItemTouchHelper swipe = new ItemTouchHelper(
                new FoodItemAdapter.SwipeToDeleteCallback(context, productosAdapter));
        swipe.attachToRecyclerView(holder.rvProducts);

        // Adaptador de recetas
        RecipeItemAdapter recetasAdapter = new RecipeItemAdapter(
                recetas, context, parentFragment, () -> {
            updateNutritionSummary(holder, block);
            parentFragment.recalculateDayTotals();
        }, block
        );
        holder.rvRecipes.setLayoutManager(new LinearLayoutManager(context));
        holder.rvRecipes.setAdapter(recetasAdapter);
        ItemTouchHelper swipeRecetas = new ItemTouchHelper(
                new RecipeItemAdapter.SwipeToDeleteCallback(context, recetasAdapter));
        swipeRecetas.attachToRecyclerView(holder.rvRecipes);



        // Mostrar etiquetas
        holder.labelRecetas.setVisibility(recetas.isEmpty() ? View.GONE : View.VISIBLE);
        holder.rvRecipes.setVisibility(recetas.isEmpty() ? View.GONE : View.VISIBLE);
        holder.labelProductos.setVisibility(productos.isEmpty() ? View.GONE : View.VISIBLE);
        holder.rvProducts.setVisibility(productos.isEmpty() ? View.GONE : View.VISIBLE);

        // Actualizar título
        holder.foodTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                block.setTitle(holder.foodTitle.getText().toString());
                Log.d(TAG, "Título actualizado a → " + block.getTitle());
            }
        });

        // Añadir alimento
        holder.addFoodItemButton.setOnClickListener(v -> {
            FoodMenuDialogFragment dialog = FoodMenuDialogFragment.newInstance();
            dialog.setOnFoodSelected(result -> {
                FoodItem newItem = new FoodItem(
                        result.name,
                        result.grams,
                        result.kcalPer100g,
                        result.protein,
                        result.carbs,
                        result.fat,
                        result.imageUrl
                );

                if (result.ingredients != null && !result.ingredients.isEmpty()) {
                    newItem.setIngredients(result.ingredients);
                    newItem.setInstructions(result.instructions);
                    newItem.setRecipe(true);
                }

                block.addItem(newItem);
                notifyItemChanged(holder.getBindingAdapterPosition());
            });
            dialog.show(parentFragment.getParentFragmentManager(), "FoodMenuDialogFragment");
        });

        // Menú duplicar / eliminar bloque
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuButton);
            popup.getMenuInflater().inflate(R.menu.food_block_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;

                if (item.getItemId() == R.id.action_delete_block) {
                    foodBlocks.remove(pos);
                    notifyItemRemoved(pos);
                    parentFragment.recalculateDayTotals();
                    return true;

                } else if (item.getItemId() == R.id.action_duplicate_block) {
                    FoodBlock original = foodBlocks.get(pos);
                    FoodBlock duplicate = new FoodBlock(original.getTitle() + " (copia)");

                    for (FoodItem itemCopy : original.getItems()) {
                        duplicate.addItem(new FoodItem(
                                itemCopy.getName(),
                                itemCopy.getGrams(),
                                itemCopy.getKcalPer100g(),
                                itemCopy.getProteinPer100g(),
                                itemCopy.getCarbsPer100g(),
                                itemCopy.getFatPer100g(),
                                itemCopy.getImageUrl()
                        ));
                    }

                    foodBlocks.add(pos + 1, duplicate);
                    notifyItemInserted(pos + 1);
                    parentFragment.recalculateDayTotals();

                    new android.os.Handler().postDelayed(() -> {
                        RecyclerView.ViewHolder vh =
                                parentFragment.recyclerViewFoodBlocks.findViewHolderForAdapterPosition(pos + 1);
                        if (vh instanceof BlockViewHolder) {
                            BlockViewHolder newHolder = (BlockViewHolder) vh;
                            newHolder.foodTitle.requestFocus();
                            InputMethodManager imm = (InputMethodManager)
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null)
                                imm.showSoftInput(newHolder.foodTitle, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 200);
                    return true;
                }

                return false;
            });

            popup.show();
        });

        updateNutritionSummary(holder, block);
    }

    private void updateNutritionSummary(BlockViewHolder h, FoodBlock b) {
        double totalKcal = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0;
        for (FoodItem it : b.getItems()) {
            if (!it.isChecked()) continue;
            double factor = it.getGrams() / 100.0;
            totalKcal += it.getKcalPer100g() * factor;
            totalProtein += it.getProteinPer100g() * factor;
            totalCarbs += it.getCarbsPer100g() * factor;
            totalFat += it.getFatPer100g() * factor;
        }

        h.nutritionKcal.setText(String.format(Locale.getDefault(), "Kcal: %.0f", totalKcal));
        h.nutritionProtein.setText(String.format(Locale.getDefault(), "P: %.1fg", totalProtein));
        h.nutritionCarbs.setText(String.format(Locale.getDefault(), "C: %.1fg", totalCarbs));
        h.nutritionFat.setText(String.format(Locale.getDefault(), "G: %.1fg", totalFat));
    }

    @Override
    public int getItemCount() {
        return foodBlocks.size();
    }

    public static class BlockViewHolder extends RecyclerView.ViewHolder {
        EditText foodTitle;
        RecyclerView rvProducts, rvRecipes;
        ImageButton addFoodItemButton, menuButton;
        TextView nutritionKcal, nutritionProtein, nutritionCarbs, nutritionFat;
        TextView labelRecetas, labelProductos;

        public BlockViewHolder(@NonNull View itemView) {
            super(itemView);
            foodTitle = itemView.findViewById(R.id.foodTitle);
            rvProducts = itemView.findViewById(R.id.rvProducts);
            rvRecipes = itemView.findViewById(R.id.rvRecipes);
            addFoodItemButton = itemView.findViewById(R.id.addFoodItemButton);
            menuButton = itemView.findViewById(R.id.menuButton);
            nutritionKcal = itemView.findViewById(R.id.nutritionKcal);
            nutritionProtein = itemView.findViewById(R.id.nutritionProtein);
            nutritionCarbs = itemView.findViewById(R.id.nutritionCarbs);
            nutritionFat = itemView.findViewById(R.id.nutritionFat);
            labelRecetas = itemView.findViewById(R.id.labelRecetas);
            labelProductos = itemView.findViewById(R.id.labelProductos);
        }
    }
}
