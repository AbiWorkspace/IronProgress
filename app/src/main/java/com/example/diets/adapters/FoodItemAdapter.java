package com.example.diets.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.dialogs.GramsInputDialog;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.model.FoodItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.FoodItemViewHolder> {

    private final List<FoodItem> items;
    private final Context context;
    private final DashboardFragment parentFragment;
    private final OnFoodItemChangedListener onChangeListener;

    public FoodItemAdapter(List<FoodItem> items, Context context, DashboardFragment parentFragment, OnFoodItemChangedListener listener) {
        this.items = items;
        this.context = context;
        this.parentFragment = parentFragment;
        this.onChangeListener = listener;
    }

    public static class FoodItemViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        EditText name, grams, kcal;

        public FoodItemViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.foodItemImage);
            name = itemView.findViewById(R.id.foodItemName);
            grams = itemView.findViewById(R.id.foodItemGrams);
            kcal = itemView.findViewById(R.id.foodItemCalories);
        }
    }

    @NonNull
    @Override
    public FoodItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false);
        return new FoodItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodItemViewHolder holder, int position) {
        FoodItem item = items.get(position);

        holder.name.setText(item.getName());
        holder.grams.setText(String.format(Locale.getDefault(), "%.0f", item.getGrams()));

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .into(holder.img);

        double totalKcal = item.getKcalPer100g() * (item.getGrams() / 100.0);
        holder.kcal.setText(String.format(Locale.getDefault(), "%.2f", totalKcal));

        CheckBox checkBox = holder.itemView.findViewById(R.id.foodItemCheck);
        checkBox.setChecked(item.isChecked());

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
            if (onChangeListener != null) onChangeListener.onFoodItemChanged();
        });

        holder.grams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double newGrams = Double.parseDouble(s.toString());
                    item.setGrams(newGrams);
                    double updatedKcal = item.getKcalPer100g() * (newGrams / 100.0);
                    holder.kcal.setText(String.format(Locale.getDefault(), "%.2f", updatedKcal));
                } catch (NumberFormatException e) {
                    item.setGrams(0);
                    holder.kcal.setText("0.00");
                }
                if (onChangeListener != null) onChangeListener.onFoodItemChanged();
            }
        });

        // 🔥 Mostrar diálogo con detalles del alimento
        holder.itemView.setOnClickListener(v -> {
            FoodSearchFragment.FoodResult foodResult = mapToFoodResult(item);
            GramsInputDialog dialog = GramsInputDialog.newInstance(foodResult, updated -> {
                item.setGrams(updated.grams);
                notifyItemChanged(holder.getAdapterPosition());
                if (onChangeListener != null) onChangeListener.onFoodItemChanged();
            });
            dialog.show(parentFragment.getParentFragmentManager(), "GramsInputDialog");
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 🔧 Convierte FoodItem a FoodResult para usar en el diálogo
    private FoodSearchFragment.FoodResult mapToFoodResult(FoodItem item) {
        FoodSearchFragment.FoodResult r = new FoodSearchFragment.FoodResult();
        r.name = item.getName();
        r.grams = item.getGrams();
        r.kcalPer100g = item.getKcalPer100g();
        r.protein = item.getProteinPer100g();
        r.carbs = item.getCarbsPer100g();
        r.fat = item.getFatPer100g();
        r.imageUrl = item.getImageUrl();
        r.brand = "Desconocida";
        r.portions = new ArrayList<>();

        return r;
    }
}