package com.example.diets.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
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

/** Adaptador de bloques de comida con optimizaciones de rendimiento. */
public class FoodBlockAdapter extends RecyclerView.Adapter<FoodBlockAdapter.BlockViewHolder> {

    // ───────────────────────────────── Constantes
    private static final String TAG = "FoodBlockAdapter";
    private static final int MAX_TITLE_CHARS = 20;
    private static final RecyclerView.RecycledViewPool POOL_PRODUCTS = new RecyclerView.RecycledViewPool();
    private static final RecyclerView.RecycledViewPool POOL_RECIPES  = new RecyclerView.RecycledViewPool();

    // ───────────────────────────────── Atributos
    private final List<FoodBlock> foodBlocks;
    private final DashboardFragment parentFragment;
    private final LayoutInflater inflater;
    private final OnFoodBlockDeleteRequestedListener deleteListener;
    private final Context ctx;

    // ───────────────────────────────── Constructor
    public FoodBlockAdapter(List<FoodBlock> blocks,
                            Context context,
                            DashboardFragment parentFragment,
                            OnFoodBlockDeleteRequestedListener deleteListener) {
        this.foodBlocks     = (blocks != null) ? blocks : new ArrayList<>();
        this.parentFragment = parentFragment;
        this.inflater       = LayoutInflater.from(context);
        this.deleteListener = deleteListener;
        this.ctx            = context;

        setHasStableIds(true);
        Log.d(TAG, "🟢 Constructor – bloques: " + this.foodBlocks.size());
    }

    // ───────────────────────────────── Interfaces públicas
    public interface OnFoodBlockDeleteRequestedListener {
        void onDeleteRequested(FoodBlock block, int position);
    }

    // ───────────────────────────────── ViewHolder
    static class BlockViewHolder extends RecyclerView.ViewHolder {
        // UI
        final EditText   foodTitle;
        final RecyclerView rvProducts, rvRecipes;
        final ImageButton addFoodItemButton, menuButton;
        final TextView   nutritionKcal, nutritionProtein, nutritionCarbs, nutritionFat;
        final TextView   labelRecetas, labelProductos;

        // Referencias para optimizar
        FoodItemAdapter   productsAdapter;
        ItemTouchHelper   productsHelper;
        RecipeItemAdapter recipesAdapter;
        ItemTouchHelper   recipesHelper;

        BlockViewHolder(@NonNull View v) {
            super(v);
            foodTitle          = v.findViewById(R.id.foodTitle);
            rvProducts         = v.findViewById(R.id.rvProducts);
            rvRecipes          = v.findViewById(R.id.rvRecipes);
            addFoodItemButton  = v.findViewById(R.id.addFoodItemButton);
            menuButton         = v.findViewById(R.id.menuButton);
            nutritionKcal      = v.findViewById(R.id.nutritionKcal);
            nutritionProtein   = v.findViewById(R.id.nutritionProtein);
            nutritionCarbs     = v.findViewById(R.id.nutritionCarbs);
            nutritionFat       = v.findViewById(R.id.nutritionFat);
            labelRecetas       = v.findViewById(R.id.labelRecetas);
            labelProductos     = v.findViewById(R.id.labelProductos);
        }
    }

    // ───────────────────────────────── Métodos base
    @NonNull
    @Override
    public BlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_food_block, parent, false);
        Log.d(TAG, "🟢 onCreateViewHolder → nueva vista inflada");
        return new BlockViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockViewHolder h, int position) {
        Log.d(TAG, "🟢 onBindViewHolder → pos " + position);
        FoodBlock block = foodBlocks.get(position);

        /* ---------- Título ---------- */
        h.foodTitle.setText(block.getTitle());
        h.foodTitle.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(MAX_TITLE_CHARS) });
        h.foodTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                block.setTitle(h.foodTitle.getText().toString());
                Log.d(TAG, "🔄 Título actualizado → " + block.getTitle());
            }
        });

        /* ---------- Separar recetas y productos ---------- */
        List<FoodItem> recetas   = new ArrayList<>();
        List<FoodItem> productos = new ArrayList<>();
        for (FoodItem i : block.getItems()) {
            if (i.isRecipe()) recetas.add(i); else productos.add(i);
        }
        Log.d(TAG, "block '" + block.getTitle() + "' productos=" + productos.size() + ", recetas=" + recetas.size());

        /* ---------- Recycler Productos ---------- */
        if (h.productsAdapter == null) {
            h.productsAdapter = new FoodItemAdapter(productos, ctx, parentFragment,
                    () -> {
                        updateNutritionSummary(h, block);
                        parentFragment.recalculateDayTotals();
                    },
                    block);

            h.rvProducts.setLayoutManager(new LinearLayoutManager(ctx));
            h.rvProducts.setAdapter(h.productsAdapter);
            h.rvProducts.setRecycledViewPool(POOL_PRODUCTS);          // ⬅️ pool exclusivo

            h.productsHelper = new ItemTouchHelper(
                    new FoodItemAdapter.SwipeToDeleteCallback(ctx, h.productsAdapter));
            h.productsHelper.attachToRecyclerView(h.rvProducts);
        } else {
            h.productsAdapter.setItems(productos);
        }

        /* ---------- Recycler Recetas ---------- */
        if (h.recipesAdapter == null) {
            h.recipesAdapter = new RecipeItemAdapter(recetas, ctx, parentFragment,
                    () -> {
                        updateNutritionSummary(h, block);
                        parentFragment.recalculateDayTotals();
                    },
                    block);

            h.rvRecipes.setLayoutManager(new LinearLayoutManager(ctx));
            h.rvRecipes.setAdapter(h.recipesAdapter);
            h.rvRecipes.setRecycledViewPool(POOL_RECIPES);            // ⬅️ pool exclusivo

            h.recipesHelper = new ItemTouchHelper(
                    new RecipeItemAdapter.SwipeToDeleteCallback(ctx, h.recipesAdapter));
            h.recipesHelper.attachToRecyclerView(h.rvRecipes);
        } else {
            h.recipesAdapter.setItems(recetas);
        }

        /* ---------- Visibilidad de etiquetas ---------- */
        h.labelRecetas.setVisibility(recetas.isEmpty()    ? View.GONE : View.VISIBLE);
        h.rvRecipes  .setVisibility(recetas.isEmpty()     ? View.GONE : View.VISIBLE);
        h.labelProductos.setVisibility(productos.isEmpty()? View.GONE : View.VISIBLE);
        h.rvProducts .setVisibility(productos.isEmpty()   ? View.GONE : View.VISIBLE);

        /* ---------- Botón añadir alimento ---------- */
        h.addFoodItemButton.setOnClickListener(v -> {
            FoodMenuDialogFragment dialog = FoodMenuDialogFragment.newInstance();
            dialog.setOnFoodSelected(result -> {
                FoodItem newItem = new FoodItem(
                        result.name, result.grams,
                        result.kcalPer100g, result.protein,
                        result.carbs,       result.fat,
                        result.imageUrl);
                newItem.setChecked(true);
                if (result.ingredients != null && !result.ingredients.isEmpty()) {
                    newItem.setIngredients(result.ingredients);
                    newItem.setInstructions(result.instructions);
                    newItem.setRecipe(true);
                }
                block.addItem(newItem);
                notifyItemChanged(h.getBindingAdapterPosition());
                parentFragment.recalculateDayTotals();
                Log.d(TAG, "Añadido alimento: " + newItem.getName() + " al bloque " + block.getTitle());
            });
            dialog.show(parentFragment.getParentFragmentManager(), "FoodMenuDialogFragment");
        });

        /* ---------- Menú contextual ---------- */
        h.menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), h.menuButton);
            popup.getMenuInflater().inflate(R.menu.food_block_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int pos = h.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                if (item.getItemId() == R.id.action_delete_block) {
                    if (deleteListener != null) deleteListener.onDeleteRequested(block, pos);
                    return true;
                } else if (item.getItemId() == R.id.action_duplicate_block) {
                    duplicateBlock(pos);
                    return true;
                }
                return false;
            });
            popup.show();
        });

        /* ---------- Resumen nutricional ---------- */
        updateNutritionSummary(h, block);
    }


    @Override public int  getItemCount()  { return foodBlocks.size(); }
    @Override public long getItemId(int p){ return foodBlocks.get(p).hashCode(); }

    // ───────────────────────────────── Helpers
    private void duplicateBlock(int pos) {
        FoodBlock original  = foodBlocks.get(pos);
        FoodBlock duplicate = new FoodBlock(original.getTitle() + " (copia)");

        for (FoodItem src : original.getItems()) {
            FoodItem copy = new FoodItem(
                    src.getName(), src.getGrams(),
                    src.getKcalPer100g(), src.getProteinPer100g(),
                    src.getCarbsPer100g(), src.getFatPer100g(),
                    src.getImageUrl());
            copy.setRecipe(src.isRecipe());
            copy.setIngredients(src.getIngredients());
            copy.setInstructions(src.getInstructions());
            duplicate.addItem(copy);
        }

        foodBlocks.add(pos + 1, duplicate);
        notifyItemInserted(pos + 1);
        parentFragment.recalculateDayTotals();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            RecyclerView.ViewHolder vh =
                    parentFragment.recyclerViewFoodBlocks.findViewHolderForAdapterPosition(pos + 1);
            if (vh instanceof BlockViewHolder) {
                BlockViewHolder h = (BlockViewHolder) vh;
                h.foodTitle.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        h.itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(h.foodTitle, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void updateNutritionSummary(BlockViewHolder h, FoodBlock b) {
        double kcal=0,p=0,c=0,g=0;
        for (FoodItem it : b.getItems()) {
            if (!it.isChecked()) continue;
            double factor = it.getGrams() / 100.0;
            kcal += it.getKcalPer100g()   * factor;
            p    += it.getProteinPer100g()* factor;
            c    += it.getCarbsPer100g()  * factor;
            g    += it.getFatPer100g()    * factor;
        }
        h.nutritionKcal   .setText(String.format(Locale.getDefault(), "Kcal: %.0f", kcal));
        h.nutritionProtein.setText(String.format(Locale.getDefault(), "P: %.1fg", p));
        h.nutritionCarbs  .setText(String.format(Locale.getDefault(), "C: %.1fg", c));
        h.nutritionFat    .setText(String.format(Locale.getDefault(), "G: %.1fg", g));
    }
}
