package com.example.diets.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.dialogs.RecipeDialog;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;

import java.util.List;
import java.util.Locale;

public class RecipeItemAdapter extends RecyclerView.Adapter<RecipeItemAdapter.RecipeViewHolder> {

    private static final String TAG = "RecipeItemAdapter";

    private final List<FoodItem> items;
    private final Context context; // ⚠️ Nota: se llama 'context'
    private final DashboardFragment parent;
    private final FoodItemAdapter.OnFoodItemChangedListener listener;
    private final FoodBlock foodBlock;
    private final String userUid; // ✅ 1. NUEVO ATRIBUTO


    public RecipeItemAdapter(List<FoodItem> items,
                             Context ctx,
                             DashboardFragment parent,
                             // 🛑 CORRECCIÓN 1: Usar el tipo correcto de listener
                             FoodItemAdapter.OnFoodItemChangedListener listener,
                             FoodBlock foodBlock,
                             String userUid) {
        this.items = items;
        this.context = ctx; // 🛑 CORRECCIÓN 2: Asignar a 'this.context', no 'this.ctx'
        this.parent = parent;
        this.listener = listener;
        this.foodBlock = foodBlock;
        this.userUid = userUid;
    }

    // ───────────────────────────────── ViewHolder
    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView  name, kcal;
        CheckBox  chk;
        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            img  = itemView.findViewById(R.id.foodItemImage);
            name = itemView.findViewById(R.id.foodItemName);
            kcal = itemView.findViewById(R.id.foodItemCalories);
            chk  = itemView.findViewById(R.id.foodItemCheck);
        }
    }

    // ───────────────────────────────── Adapter base
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false);
        return new RecipeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        FoodItem item = items.get(position);
        Log.d(TAG, "onBindViewHolder: Mostrando receta → " + item.getName());

        holder.name.setText(item.getName());
        holder.kcal.setText(String.format(Locale.getDefault(), "%.0f kcal", item.getKcalPer100g()));

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .into(holder.img);

        // --- CHECKBOX ---
        holder.chk.setChecked(item.isChecked());
        holder.chk.setOnCheckedChangeListener((b, c) -> {
            item.setChecked(c);
            if (listener != null) listener.onFoodItemChanged();
        });

        // --- Click para editar gramos ---
        holder.itemView.setOnClickListener(v -> {
            FoodSearchFragment.FoodResult result = new FoodSearchFragment.FoodResult();
            result.name         = item.getName();
            result.grams        = item.getGrams();
            result.kcalPer100g  = item.getKcalPer100g();
            result.protein      = item.getProteinPer100g();
            result.carbs        = item.getCarbsPer100g();
            result.fat          = item.getFatPer100g();
            result.imageUrl     = item.getImageUrl();
            result.ingredients  = item.getIngredients();
            result.instructions = item.getInstructions();
            result.type         = "receta";

            RecipeDialog dialog = RecipeDialog.newInstance(result, userUid);
            dialog.setOnGramsConfirmedListener(updated -> {
                item.setGrams(updated.grams);
                notifyItemChanged(holder.getBindingAdapterPosition());
                if (listener != null) listener.onFoodItemChanged();
            });
            dialog.show(parent.getParentFragmentManager(), "RecipeDialog");
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ───────────────────────────────── API pública
    public void setItems(List<FoodItem> newItems) {
        Log.d("RecipeItemAdapter#setItems(rec)",
                "size=" + newItems.size());
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void deleteItem(int position) {
        if (position < 0 || position >= items.size()) return;

        FoodItem removed = items.remove(position);
        foodBlock.removeItem(removed);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
        if (listener != null) listener.onFoodItemChanged();
    }

    // ───────────────────────────────── Swipe
    public static class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        private final RecipeItemAdapter adapter;
        private final Drawable icon;
        private final ColorDrawable background;

        public SwipeToDeleteCallback(Context context, RecipeItemAdapter adapter) {
            super(0, ItemTouchHelper.LEFT);
            this.adapter = adapter;
            this.icon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
            this.background = new ColorDrawable(Color.parseColor("#D32F2F"));
        }

        @Override public boolean onMove(@NonNull RecyclerView r,
                                        @NonNull RecyclerView.ViewHolder v,
                                        @NonNull RecyclerView.ViewHolder t) { return false; }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) adapter.deleteItem(position);
            else adapter.notifyDataSetChanged();
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView r,
                                @NonNull RecyclerView.ViewHolder v,
                                float dX, float dY, int state, boolean active) {

            View itemView = v.itemView;

            // Fondo rojo
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
            background.draw(c);

            // Ícono
            int margin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int top    = itemView.getTop() + margin;
            int left   = itemView.getRight() - margin - icon.getIntrinsicWidth();
            icon.setBounds(left, top,
                    left + icon.getIntrinsicWidth(),
                    top  + icon.getIntrinsicHeight());
            icon.setTint(Color.WHITE);
            icon.draw(c);

            super.onChildDraw(c, r, v, dX, dY, state, active);
        }
    }
}
