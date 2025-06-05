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

import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class RecipeItemAdapter extends RecyclerView.Adapter<RecipeItemAdapter.RecipeViewHolder> {

    private static final String TAG = "RecipeItemAdapter";

    private final List<FoodItem> items;
    private final Context context;
    private final DashboardFragment parent;
    private final FoodItemAdapter.OnFoodItemChangedListener listener;
    private final FoodBlock foodBlock;

    public RecipeItemAdapter(List<FoodItem> items, Context context, DashboardFragment parent,
                             FoodItemAdapter.OnFoodItemChangedListener listener, FoodBlock foodBlock) {
        this.items = items;
        this.context = context;
        this.parent = parent;
        this.listener = listener;
        this.foodBlock = foodBlock;
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name;
        TextView kcal;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.foodItemImage);
            name = itemView.findViewById(R.id.foodItemName);
            kcal = itemView.findViewById(R.id.foodItemCalories);
        }
    }

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
                .into(holder.img);

        // --- CHECKBOX CONTROL PARA RECETAS ---
        CheckBox cb = holder.itemView.findViewById(R.id.foodItemCheck);
        cb.setChecked(item.isChecked());
        cb.setOnCheckedChangeListener((b, c) -> {
            Log.d(TAG, "onBindViewHolder: Check cambiado para receta " + item.getName() + " → " + c);
            item.setChecked(c);
            if (listener != null) listener.onFoodItemChanged();
        });


        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Mostrando RecipeDialog para → " + item.getName());

            FoodSearchFragment.FoodResult result = new FoodSearchFragment.FoodResult();
            result.name = item.getName();
            result.grams = item.getGrams();
            result.kcalPer100g = item.getKcalPer100g();
            result.protein = item.getProteinPer100g();
            result.carbs = item.getCarbsPer100g();
            result.fat = item.getFatPer100g();
            result.imageUrl = item.getImageUrl();
            result.ingredients = item.getIngredients();
            result.instructions = item.getInstructions();
            result.type = "receta";

            RecipeDialog dialog = RecipeDialog.newInstance(result);
            dialog.setOnGramsConfirmedListener(updated -> {
                item.setGrams(updated.grams);
                notifyItemChanged(holder.getBindingAdapterPosition());
                if (listener != null) listener.onFoodItemChanged();
            });
            dialog.show(parent.getParentFragmentManager(), "RecipeDialog");
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }
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

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                adapter.deleteItem(position);
            } else {
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();

            // Fondo
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
            background.draw(c);

            // Ícono
            int iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            icon.setTint(Color.WHITE);
            icon.draw(c);

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
    public void deleteItem(int position) {
        if (position < 0 || position >= items.size()) return;

        FoodItem removed = items.remove(position);
        if (foodBlock.getItems() != items) {
            foodBlock.removeItem(removed);
        }

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
        if (listener != null) listener.onFoodItemChanged();
    }

}
