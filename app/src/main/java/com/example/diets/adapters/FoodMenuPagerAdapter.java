package com.example.diets.adapters;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.diets.dialogs.FoodMenuDialogFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.fragments.FavoritesFoodFragment;

import java.util.function.Consumer;

public class FoodMenuPagerAdapter extends FragmentStateAdapter {

    private final Consumer<FoodSearchFragment.FoodResult> onFoodSelected;
    private final FoodMenuDialogFragment parentDialog;


    public FoodMenuPagerAdapter(@NonNull FoodMenuDialogFragment parentDialog,
                                Consumer<FoodSearchFragment.FoodResult> onFoodSelected) {
        super(parentDialog);
        this.parentDialog = parentDialog;
        this.onFoodSelected = onFoodSelected;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            FoodSearchFragment searchFoodFragment = new FoodSearchFragment();
            searchFoodFragment.setOnFoodSelected(result -> {
                onFoodSelected.accept(result);
                Toast.makeText(parentDialog.requireContext(), " Alimento agregado", Toast.LENGTH_SHORT).show(); // 🔥 Confirmación
                parentDialog.dismissMenu(); // 🔥 cerrar menú
            });
            return searchFoodFragment;
        } else {
            FavoritesFoodFragment favoritesFragment = new FavoritesFoodFragment();
            favoritesFragment.setOnFoodSelected(result -> {
                onFoodSelected.accept(result);
                Toast.makeText(parentDialog.requireContext(), "Alimento agregado", Toast.LENGTH_SHORT).show(); // 🔥 Confirmación
                parentDialog.dismissMenu(); // 🔥 cerrar menú
            });
            return favoritesFragment;
        }

    }
    @Override
    public int getItemCount() {
        return 2; // Dos páginas: Buscar y Favoritos
    }
}
