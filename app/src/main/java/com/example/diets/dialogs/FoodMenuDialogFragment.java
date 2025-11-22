package com.example.diets.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.diets.R;
import com.example.diets.adapters.FoodMenuPagerAdapter;
import com.example.diets.fragments.FoodSearchFragment;

import java.util.function.Consumer;

public class FoodMenuDialogFragment extends DialogFragment {

    private ViewPager2 viewPager;
    private TextView tabSearch, tabFavorites;
    private String userUid; // ✅ CORRECCIÓN 1: DECLARAR el atributo aquí
    private Consumer<FoodSearchFragment.FoodResult> onFoodSelected;

    // Dentro de FoodMenuDialogFragment.java

    public static FoodMenuDialogFragment newInstance(
            Consumer<FoodSearchFragment.FoodResult> onFoodSelected,
            String uid) { // ✅ ACEPTAR UID

        FoodMenuDialogFragment dialog = new FoodMenuDialogFragment();
        dialog.onFoodSelected = onFoodSelected;
        dialog.userUid = uid; // 👈 GUARDAR UID en una nueva variable de instancia
        return dialog;
    }

    public void setOnFoodSelected(Consumer<FoodSearchFragment.FoodResult> listener) {
        this.onFoodSelected = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(R.drawable.background_dialog_food_menu);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_food_menu, container, false);

        // Inicializar referencias
        viewPager = view.findViewById(R.id.viewPagerMenu);
        tabSearch = view.findViewById(R.id.tabSearch);
        tabFavorites = view.findViewById(R.id.tabFavorites);

        viewPager.setAdapter(new FoodMenuPagerAdapter(this, onFoodSelected, userUid));
        // Cambiar tab activo según página
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabHighlight(position);
            }
        });

        // Clic en tab "Buscar"
        tabSearch.setOnClickListener(v -> viewPager.setCurrentItem(0));
        // Clic en tab "Favoritos"
        tabFavorites.setOnClickListener(v -> viewPager.setCurrentItem(1));

        // Inicializar colores
        updateTabHighlight(0);

        return view;
    }


    public void dismissMenu() {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().getDecorView()
                    .animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(this::dismiss)
                    .start();
        } else {
            dismiss();
        }
    }

    private void updateTabHighlight(int position) {
        if (position == 0) {
            tabSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.accentRed));
            tabFavorites.setTextColor(ContextCompat.getColor(requireContext(), R.color.hintColorLight));
        } else {
            tabSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.hintColorLight));
            tabFavorites.setTextColor(ContextCompat.getColor(requireContext(), R.color.accentRed));
        }
    }


}
