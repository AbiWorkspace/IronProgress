package com.example.diets.adapters;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.diets.dialogs.FoodMenuDialogFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.fragments.FavoritesFoodFragment;

import java.util.function.Consumer;
//MENU ENCARGADO DE UNIR  FoodSearchFragment y FavoritesFragment

public class FoodMenuPagerAdapter extends FragmentStateAdapter {

    private final Consumer<FoodSearchFragment.FoodResult> onFoodSelected;
    private final FoodMenuDialogFragment parentDialog;
    private final String effectiveUid; // ✅ 1. Atributo para almacenar el UID

    public FoodMenuPagerAdapter(@NonNull FoodMenuDialogFragment parentDialog,
                                Consumer<FoodSearchFragment.FoodResult> onFoodSelected,
                                String effectiveUid) { // ✅ 2. Aceptar el UID en el constructor
        super(parentDialog);
        this.parentDialog = parentDialog;
        this.onFoodSelected = onFoodSelected;
        this.effectiveUid = effectiveUid; // ✅ 3. Guardar el UID
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            // 🛑 CORRECCIÓN DE LA LÍNEA 32: Usar newInstance y el atributo guardado
            FoodSearchFragment searchFoodFragment = FoodSearchFragment.newInstance(this.effectiveUid);

            searchFoodFragment.setOnFoodSelected(result -> {
                onFoodSelected.accept(result);
                Toast.makeText(parentDialog.requireContext(), " Alimento agregado", Toast.LENGTH_SHORT).show();
                parentDialog.dismissMenu();
            });
            return searchFoodFragment;
        } else {
            // NOTA: Si FavoritesFoodFragment también necesita el UID (y probablemente lo necesite para la gestión segura de favoritos),
            // también deberías actualizar su constructor y usar FavoritesFoodFragment.newInstance(this.effectiveUid);
            FavoritesFoodFragment favoritesFragment = FavoritesFoodFragment.newInstance(this.effectiveUid);
            favoritesFragment.setOnFoodSelected(result -> {
                onFoodSelected.accept(result);
                Toast.makeText(parentDialog.requireContext(), "Alimento agregado", Toast.LENGTH_SHORT).show();
                parentDialog.dismissMenu();
            });
            return favoritesFragment;
        }
    }
    @Override
    public int getItemCount() {
        return 2; // Dos páginas: Buscar y Favoritos
    }
}
