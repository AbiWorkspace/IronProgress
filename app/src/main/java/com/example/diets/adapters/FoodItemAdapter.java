package com.example.diets.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.dialogs.GramsInputDialog;
import com.example.diets.dialogs.RecipeDialog;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.FoodItemViewHolder> {

    private static final String TAG = "FoodItemAdapter";

    public interface OnFoodItemChangedListener {
        void onFoodItemChanged();
    }

    private final List<FoodItem> items;
    private final Context ctx;
    private final DashboardFragment parent;
    private final OnFoodItemChangedListener listener;
    private final FoodBlock foodBlock;

    public FoodItemAdapter(List<FoodItem> items,
                           Context ctx,
                           DashboardFragment parent,
                           OnFoodItemChangedListener l,
                           FoodBlock foodBlock) {
        Log.d(TAG, "Constructor: Inicializando adaptador con " + items.size() + " items");
        this.items = items;
        this.ctx = ctx;
        this.parent = parent;
        this.listener = l;
        this.foodBlock = foodBlock;
    }

    static class FoodItemViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        EditText name, grams, kcal;

        FoodItemViewHolder(@NonNull View v) {
            super(v);
            Log.d(TAG, "FoodItemViewHolder - Constructor: Asignando vistas del item");
            img = v.findViewById(R.id.foodItemImage);
            name = v.findViewById(R.id.foodItemName);
            grams = v.findViewById(R.id.foodItemGrams);
            kcal = v.findViewById(R.id.foodItemCalories);
        }
    }

    @NonNull
    @Override
    public FoodItemViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vT) {
        Log.d(TAG, "onCreateViewHolder: Inflando layout food_item");
        return new FoodItemViewHolder(LayoutInflater.from(ctx).inflate(R.layout.food_item, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FoodItemViewHolder h, int pos) {
        Log.d(TAG, "onBindViewHolder: Mostrando item en posición → " + pos);

        FoodItem it = items.get(pos);

        h.name.setText(it.getName());
        h.grams.setText(String.format(Locale.getDefault(), "%.0f", it.getGrams()));
        double totK = it.getKcalPer100g() * (it.getGrams() / 100.0);
        h.kcal.setText(String.format(Locale.getDefault(), "%.2f", totK));
        Log.d(TAG, "onBindViewHolder: Calorías calculadas para " + it.getName() + " → " + totK);

        Glide.with(ctx)
                .load(it.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .into(h.img);

        CheckBox cb = h.itemView.findViewById(R.id.foodItemCheck);
        cb.setChecked(it.isChecked());

        cb.setOnCheckedChangeListener((b, c) -> {
            Log.d(TAG, "onBindViewHolder: Check cambiado para " + it.getName() + " → " + c);
            it.setChecked(c);
            notifyParent();
        });

        h.grams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override public void afterTextChanged(Editable e) {
                try {
                    double g = Double.parseDouble(e.toString());
                    it.setGrams(g);
                    double kcalVal = it.getKcalPer100g() * (g / 100.0);
                    h.kcal.setText(String.format(Locale.getDefault(), "%.2f", kcalVal));
                    Log.d(TAG, "afterTextChanged: gramos=" + g + " → kcal=" + kcalVal);
                } catch (NumberFormatException ex) {
                    Log.e(TAG, "afterTextChanged: Valor inválido ingresado. Reestableciendo a 0");
                    it.setGrams(0);
                    h.kcal.setText("0.00");
                }
                notifyParent();
            }
        });

        h.itemView.setOnClickListener(v -> {
            Log.d(TAG, "onBindViewHolder: Click en item → " + it.getName());
            FoodSearchFragment.FoodResult fr = map(it);
            Log.d(TAG, "onBindViewHolder: fr.type = " + fr.type);
            Log.d(TAG, "onBindViewHolder: fr.isRecipe() = " + fr.isRecipe());

            if (fr.ingredients != null && !fr.ingredients.isEmpty()) {

                Log.d(TAG, "onBindViewHolder: Item es receta → abriendo RecipeDialog");

                RecipeDialog dialog = RecipeDialog.newInstance(fr);
                dialog.setOnGramsConfirmedListener(updated -> {
                    it.setGrams(updated.grams);
                    notifyItemChanged(h.getBindingAdapterPosition());
                    notifyParent();
                    Log.d(TAG, "onBindViewHolder: Receta editada → " + it.getName());
                });
                dialog.show(parent.getParentFragmentManager(), "RecipeDialog");

            } else {
                Log.d(TAG, "onBindViewHolder: Item es producto → abriendo GramsInputDialog");

                GramsInputDialog.newInstance(fr, updated -> {
                    it.setGrams(updated.grams);
                    notifyItemChanged(h.getBindingAdapterPosition());
                    notifyParent();
                    Log.d(TAG, "onBindViewHolder: Producto editado → " + it.getName());
                }).show(parent.getParentFragmentManager(), "GramsInputDialog");
            }
        });
    }


    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: Tamaño actual → " + items.size());
        return items.size();
    }

    private void notifyParent() {
        Log.d(TAG, "notifyParent: Notificando al fragmento padre de cambios");
        if (listener != null) listener.onFoodItemChanged();
    }

    private FoodSearchFragment.FoodResult map(FoodItem i) {
        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "map: Creando objeto FoodResult desde FoodItem → " + i.getName());

        FoodSearchFragment.FoodResult r = new FoodSearchFragment.FoodResult();
        r.name         = i.getName();
        r.grams        = i.getGrams();
        r.kcalPer100g  = i.getKcalPer100g();
        r.protein      = i.getProteinPer100g();
        r.carbs        = i.getCarbsPer100g();
        r.fat          = i.getFatPer100g();
        r.imageUrl     = i.getImageUrl();
        r.brand        = "";
        r.portions     = new ArrayList<>();

        // 🔥 Copiar campos importantes para detectar recetas
        r.ingredients  = i.getIngredients();
        r.instructions = i.getInstructions();
        r.type = i.isRecipe() ? "receta" : "producto";

        Log.d(TAG, "map: isRecipe = " + i.isRecipe());
        Log.d(TAG, "map: tipo asignado = " + r.type);
        Log.d(TAG, "map: ingredientes = " +
                (r.ingredients != null ? r.ingredients.size() + " elementos" : "null"));

        if (r.ingredients != null) {
            for (int j = 0; j < r.ingredients.size(); j++) {
                Log.d(TAG, "map: ingrediente[" + j + "] → " + r.ingredients.get(j));
            }
        }

        Log.d(TAG, "map: instrucciones = " +
                (r.instructions != null ? r.instructions : "null"));
        Log.d(TAG, "═══════════════════════════════════════════════");

        return r;
    }


    public void deleteItem(int pos) {
        if (items == null || pos < 0 || pos >= items.size()) {
            Log.e(TAG, "❌ deleteItem: índice inválido → " + pos + " / tamaño: " + items.size());
            notifyDataSetChanged();
            return;
        }

        try {
            FoodItem itemToRemove = items.get(pos);
            Log.d(TAG, "deleteItem: Eliminando → " + itemToRemove.getName());

            items.remove(pos);

            if (foodBlock.getItems() != items) {
                Log.d(TAG, "deleteItem: Eliminando también del modelo FoodBlock");
                foodBlock.removeItem(itemToRemove);
            }

            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, getItemCount());
            notifyParent();

        } catch (Exception e) {
            Log.e(TAG, "💥 deleteItem: Excepción inesperada → " + e.getMessage());
            notifyDataSetChanged();
        }
    }

    public static class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        private final FoodItemAdapter adp;
        private final Drawable icon;
        private final ColorDrawable bg;

        public SwipeToDeleteCallback(Context ctx, FoodItemAdapter a) {
            super(0, ItemTouchHelper.LEFT);
            Log.d(TAG, "SwipeToDeleteCallback - Constructor: Inicializando swipe para eliminación");
            adp = a;
            icon = ContextCompat.getDrawable(ctx, R.drawable.ic_delete);
            bg = new ColorDrawable(Color.parseColor("#D32F2F"));
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            Log.d(TAG, "SwipeToDeleteCallback - clearView: Restableciendo posición del ítem");
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setTranslationX(0);
            recyclerView.invalidate();
        }

        @Override
        public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v,
                              @NonNull RecyclerView.ViewHolder t) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d) {
            int pos = v.getBindingAdapterPosition();
            Log.d(TAG, "SwipeToDeleteCallback - onSwiped: posición → " + pos);

            if (pos == RecyclerView.NO_POSITION || pos >= adp.getItemCount()) {
                Log.e(TAG, "SwipeToDeleteCallback - posición inválida");
                adp.notifyDataSetChanged();
                return;
            }

            FoodItem item = adp.items.get(pos);
            if (item.isRecipe()) {
                Log.w(TAG, "SwipeToDeleteCallback - El ítem es una receta y no se puede eliminar por swipe");
                adp.notifyItemChanged(pos); // Restaurar visualmente
                return;
            }

            adp.deleteItem(pos);
        }



        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView r,
                                @NonNull RecyclerView.ViewHolder v,
                                float dX, float dY, int st, boolean act) {

            View item = v.itemView;
            if (dX == 0f) {
                bg.setBounds(0, 0, 0, 0);
                bg.draw(c);
                super.onChildDraw(c, r, v, dX, dY, st, act);
                return;
            }

            bg.setBounds(item.getRight() + (int) dX, item.getTop(), item.getRight(), item.getBottom());
            bg.draw(c);

            int m = (item.getHeight() - icon.getIntrinsicHeight()) / 2;
            int top = item.getTop() + m;
            int left = item.getRight() - m - icon.getIntrinsicWidth();
            icon.setBounds(left, top, left + icon.getIntrinsicWidth(), top + icon.getIntrinsicHeight());
            icon.draw(c);

            super.onChildDraw(c, r, v, dX, dY, st, act);
        }
    }
}
