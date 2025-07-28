package com.example.diets.adapters;

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

    // ───────────────────────────────── Listener
    public interface OnFoodItemChangedListener { void onFoodItemChanged(); }

    // ───────────────────────────────── Atributos
    private final List<FoodItem> items;
    private final Context ctx;
    private final DashboardFragment parent;
    private final OnFoodItemChangedListener listener;
    private final FoodBlock foodBlock;

    // ───────────────────────────────── Constructor
    public FoodItemAdapter(List<FoodItem> items,
                           Context ctx,
                           DashboardFragment parent,
                           OnFoodItemChangedListener l,
                           FoodBlock foodBlock) {
        Log.d(TAG, "Constructor: Inicializando adaptador con " + items.size() + " items");
        this.items     = items;  // mantenemos la referencia viva
        this.ctx       = ctx;
        this.parent    = parent;
        this.listener  = l;
        this.foodBlock = foodBlock;
    }

    // ───────────────────────────────── ViewHolder
    static class FoodItemViewHolder extends RecyclerView.ViewHolder {
        final ImageView img;
        final EditText  name, grams, kcal;

        FoodItemViewHolder(@NonNull View v) {
            super(v);
            Log.d(TAG, "FoodItemViewHolder - Constructor");
            img   = v.findViewById(R.id.foodItemImage);
            name  = v.findViewById(R.id.foodItemName);
            grams = v.findViewById(R.id.foodItemGrams);
            kcal  = v.findViewById(R.id.foodItemCalories);
        }
    }

    // ───────────────────────────────── Adapter base
    @NonNull
    @Override
    public FoodItemViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        Log.d(TAG, "onCreateViewHolder: Inflando layout food_item");
        return new FoodItemViewHolder(
                LayoutInflater.from(ctx).inflate(R.layout.food_item, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FoodItemViewHolder h, int pos) {
        Log.d(TAG, "onBindViewHolder: pos → " + pos);
        FoodItem it = items.get(pos);

        h.name .setText(it.getName());
        h.grams.setText(String.format(Locale.getDefault(), "%.0f", it.getGrams()));
        double totK = it.getKcalPer100g() * (it.getGrams() / 100.0);
        h.kcal .setText(String.format(Locale.getDefault(), "%.2f", totK));

        Glide.with(ctx)
                .load(it.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .into(h.img);

        CheckBox cb = h.itemView.findViewById(R.id.foodItemCheck);
        cb.setChecked(it.isChecked());
        cb.setOnCheckedChangeListener((b, c) -> {
            it.setChecked(c);
            notifyParent();
        });

        h.grams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged (CharSequence s,int st,int b,int c){}
            @Override public void afterTextChanged (Editable e){
                try {
                    double g = Double.parseDouble(e.toString());
                    it.setGrams(g);
                    double kcalVal = it.getKcalPer100g() * (g / 100.0);
                    h.kcal.setText(String.format(Locale.getDefault(), "%.2f", kcalVal));
                } catch (NumberFormatException ex) {
                    it.setGrams(0);
                    h.kcal.setText("0.00");
                }
                notifyParent();
            }
        });

        h.itemView.setOnClickListener(v -> {
            FoodSearchFragment.FoodResult fr = map(it);
            if (fr.ingredients != null && !fr.ingredients.isEmpty()) {
                RecipeDialog d = RecipeDialog.newInstance(fr);
                d.setOnGramsConfirmedListener(u -> {
                    it.setGrams(u.grams);
                    notifyItemChanged(h.getBindingAdapterPosition());
                    notifyParent();
                });
                d.show(parent.getParentFragmentManager(), "RecipeDialog");
            } else {
                GramsInputDialog.newInstance(fr, u -> {
                    it.setGrams(u.grams);
                    notifyItemChanged(h.getBindingAdapterPosition());
                    notifyParent();
                }).show(parent.getParentFragmentManager(), "GramsInputDialog");
            }
        });
    }

    @Override public int getItemCount() {
        return items.size();
    }


    /** Permite actualizar la lista sin recrear el adapter. */
    public void setItems(List<FoodItem> newItems) {
        items.clear();          // items es tu lista interna
        items.addAll(newItems); // mantenemos la misma referencia
        notifyDataSetChanged();
    }



    public void deleteItem(int pos){
        if(pos<0 || pos>=items.size()){
            Log.e(TAG,"deleteItem: índice inválido "+pos);
            notifyDataSetChanged(); return;
        }
        FoodItem toRemove = items.get(pos);
        items.remove(pos);
        foodBlock.removeItem(toRemove);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, getItemCount());
        notifyParent();
    }

    // ───────────────────────────────── Utilidades
    private void notifyParent(){ if(listener!=null) listener.onFoodItemChanged(); }

    private FoodSearchFragment.FoodResult map(FoodItem i){
        FoodSearchFragment.FoodResult r = new FoodSearchFragment.FoodResult();
        r.name        = i.getName();
        r.grams       = i.getGrams();
        r.kcalPer100g = i.getKcalPer100g();  r.protein = i.getProteinPer100g();
        r.carbs       = i.getCarbsPer100g(); r.fat     = i.getFatPer100g();
        r.imageUrl    = i.getImageUrl();     r.brand   = "";
        r.portions    = new ArrayList<>();
        r.ingredients = i.getIngredients();  r.instructions = i.getInstructions();
        r.type        = i.isRecipe() ? "receta" : "producto";
        return r;
    }

    // ───────────────────────────────── Swipe
    public static class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback{
        private final FoodItemAdapter adp;
        private final Drawable icon;
        private final ColorDrawable bg;
        public SwipeToDeleteCallback(Context ctx, FoodItemAdapter a){
            super(0, ItemTouchHelper.LEFT);
            adp  = a;
            icon = ContextCompat.getDrawable(ctx, R.drawable.ic_delete);
            bg   = new ColorDrawable(Color.parseColor("#D32F2F"));
        }

        @Override public boolean onMove(@NonNull RecyclerView r,@NonNull RecyclerView.ViewHolder v,@NonNull RecyclerView.ViewHolder t){return false;}

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d){
            int pos = v.getBindingAdapterPosition();
            if(pos==RecyclerView.NO_POSITION || pos<0 || pos>=adp.items.size()){
                adp.notifyDataSetChanged(); return;
            }
            FoodItem item = adp.items.get(pos);
            if(item.isRecipe()){ adp.notifyItemChanged(pos); return; }
            adp.deleteItem(pos);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c,@NonNull RecyclerView r,@NonNull RecyclerView.ViewHolder v,
                                float dX,float dY,int st,boolean act){
            View item = v.itemView;
            if(dX==0f){
                bg.setBounds(0,0,0,0); bg.draw(c);
                super.onChildDraw(c,r,v,dX,dY,st,act); return;
            }
            bg.setBounds(item.getRight()+(int)dX,item.getTop(),item.getRight(),item.getBottom());
            bg.draw(c);
            int m=(item.getHeight()-icon.getIntrinsicHeight())/2;
            int top=item.getTop()+m;
            int left=item.getRight()-m-icon.getIntrinsicWidth();
            icon.setBounds(left,top,left+icon.getIntrinsicWidth(),top+icon.getIntrinsicHeight());
            icon.draw(c);
            super.onChildDraw(c,r,v,dX,dY,st,act);
        }
    }


}
