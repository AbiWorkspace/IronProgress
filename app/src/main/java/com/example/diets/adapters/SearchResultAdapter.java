package com.example.diets.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.fragments.FoodSearchFragment;

import java.util.List;
import java.util.Locale;

public class SearchResultAdapter extends ArrayAdapter<FoodSearchFragment.FoodResult> {

    private final LayoutInflater inflater;

    public SearchResultAdapter(@NonNull Context ctx, @NonNull List<FoodSearchFragment.FoodResult> data) {
        super(ctx, 0, data);
        inflater = LayoutInflater.from(ctx);
    }

    @NonNull @Override
    public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView == null
                ? inflater.inflate(R.layout.item_search_result, parent, false)
                : convertView;

        FoodSearchFragment.FoodResult f = getItem(pos);

        ImageView img      = v.findViewById(R.id.imgThumb);
        TextView nameTxt  = v.findViewById(R.id.txtName);
        TextView  subTxt   = v.findViewById(R.id.txtSubtitle);
        TextView  typeTxt  = v.findViewById(R.id.txtType);

        // foto o placeholder
        Glide.with(inflater.getContext())
                .load(f.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(img);

        nameTxt.setText(f.name);
        subTxt.setText(String.format(Locale.getDefault(),
                "%.0f kcal/100g  ·  %s", f.kcalPer100g, f.brand));

        boolean recipe = f.isRecipe();
        typeTxt.setText(recipe ? "Receta" : "Producto");
        typeTxt.setBackgroundResource(
                recipe ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);

        return v;
    }
}
