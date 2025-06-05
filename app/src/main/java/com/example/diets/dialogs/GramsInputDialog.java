package com.example.diets.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.diets.fragments.FavoritesFoodFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.example.diets.fragments.FoodSearchFragment.FoodResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GramsInputDialog extends DialogFragment {

    /* ---------- constantes ---------- */
    private static final String TAG = "GramsInputDialog";
    private static final String ARG_FOOD = "arg_food";
    private double selectedPortionGrams = 0;
    private static final Gson gson = new Gson(); // 🔥 Gson para serializar
    private FavoritesFoodFragment.OnFavoriteStatusChangedListener favoriteStatusListener;

    /* ---------- listener ---------- */
    public interface OnGramsConfirmedListener {
        void onGramsConfirmed(FoodSearchFragment.FoodResult result);
    }
    private OnGramsConfirmedListener listener;

    /* ---------- estado ---------- */
    private boolean isFavoriteNow;

    /* ---------- creación ---------- */
    public static GramsInputDialog newInstance(FoodSearchFragment.FoodResult food,
                                               OnGramsConfirmedListener listener) {
        Log.d(TAG + " - newInstance", "📥 Creando nueva instancia de GramsInputDialog.");
        Bundle args = new Bundle();
        args.putSerializable(ARG_FOOD, food);
        GramsInputDialog d = new GramsInputDialog();
        d.setArguments(args);
        d.listener = listener;
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Esto es lo ÚNICO que importa: fondo transparente global
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }
    }



    public void setOnFavoriteStatusChangedListener(FavoritesFoodFragment.OnFavoriteStatusChangedListener listener) {
        this.favoriteStatusListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d(TAG + " - onCreateView", "📥 Creando vista del diálogo.");
        View v = inf.inflate(R.layout.dialog_input_grams, container, false);

        /* ----- referencias UI ----- */
        ImageButton btnFav = v.findViewById(R.id.btnFavToggle);
        ImageView img = v.findViewById(R.id.foodImage);
        TextView brand = v.findViewById(R.id.foodBrand);
        TextView det = v.findViewById(R.id.foodDetails);
        TextView title = v.findViewById(R.id.dialogTitle);
        EditText inGrams = v.findViewById(R.id.inputGrams);
        Spinner spnPort = v.findViewById(R.id.spinnerPortions);
        Button cancel = v.findViewById(R.id.btnCancel);
        Button ok = v.findViewById(R.id.btnAccept);

        /* ----- alimento recibido ----- */
        FoodSearchFragment.FoodResult food =
                (FoodSearchFragment.FoodResult) getArguments().getSerializable(ARG_FOOD);
        if (food == null) { Log.e(TAG, "❌ FoodResult es null"); dismiss(); return v; }

        title.setText("¿Cuánto de " + food.name + "?");
        brand.setText("Marca: " + food.brand);
        det.setText(String.format(Locale.getDefault(),
                "Calorías: %.0f kcal/100g\nProteínas: %.1fg\nCarbohidratos: %.1fg\nGrasas: %.1fg",
                food.kcalPer100g, food.protein, food.carbs, food.fat));

        Glide.with(requireContext())
                .load(food.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(img);

        /* ---------- Spinner: gramos manual o porción ---------- */
        List<String> entries = new ArrayList<>();
        entries.add("Ingresar gramos manualmente");
        for (FoodSearchFragment.FoodResult.Portion p : food.portions) {
            entries.add(p.label + " (" + p.grams + " g)");
        }
        ArrayAdapter<String> spnAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, entries);
        spnPort.setAdapter(spnAdapter);

        spnPort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    selectedPortionGrams = 0;
                    inGrams.setHint("Cantidad en gramos");
                    inGrams.setText("");
                    inGrams.setEnabled(true);
                } else {
                    selectedPortionGrams = food.portions.get(pos - 1).grams;
                    inGrams.setHint("N.º porciones (ej. 1.25)");
                    inGrams.setText("1");
                    inGrams.setEnabled(true);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        /* ---------- favoritos ---------- */
        isFavoriteNow = FavoritesManager.isFavorite(requireContext(), food.name);
        updateFavIcon(btnFav, isFavoriteNow);
        btnFav.setOnClickListener(vw -> {
            isFavoriteNow = !isFavoriteNow;
            if (isFavoriteNow) {
                FavoritesManager.add(requireContext(), food);
            } else {
                FavoritesManager.remove(requireContext(), food.name);
                if (favoriteStatusListener != null) {
                    favoriteStatusListener.onFavoriteStatusChanged(); // 🔥 Avisar que se eliminó
                }
            }
            updateFavIcon(btnFav, isFavoriteNow);
        });


        /* ---------- botones ---------- */
        cancel.setOnClickListener(vw -> dismiss());

        ok.setOnClickListener(vw -> {
            String s = inGrams.getText().toString().trim();
            double g;
            try {
                if (TextUtils.isEmpty(s)) throw new NumberFormatException("vacío");
                double val = Double.parseDouble(s);

                g = (selectedPortionGrams == 0)
                        ? val
                        : val * selectedPortionGrams;
            } catch (NumberFormatException e) {
                g = 0;
            }

            if (g <= 0) {
                Toast.makeText(getContext(), "Ingresa un número válido", Toast.LENGTH_SHORT).show();
                return;
            }

            food.grams = g;
            if (listener != null) listener.onGramsConfirmed(food);
            dismiss();
        });

        return v;
    }

    private void updateFavIcon(ImageButton btn, boolean fav) {
        btn.setSelected(fav);
        btn.setColorFilter(fav ? 0xFFE63946 : 0xFFBBBBBB);
    }

    /* --------------------------------------------------------------------- */
    /* ----------------------- FAVORITES MANAGER  -------------------------- */
    /* --------------------------------------------------------------------- */
    public static final class FavoritesManager {

        private static final String PREF = "favorites_pref";
        private static final String KEY = "fav_set";

        private static Set<String> getSet(Context c) {
            SharedPreferences p = c.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            return new HashSet<>(p.getStringSet(KEY, new HashSet<>()));
        }

        private static void saveSet(Context c, Set<String> set) {
            c.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(KEY, set)
                    .apply();
        }

        public static boolean isFavorite(Context c, String name) {
            for (String json : getSet(c)) {
                FoodSearchFragment.FoodResult f = gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                if (f.name.equals(name)) return true;
            }
            return false;
        }

        public static void add(Context c, FoodSearchFragment.FoodResult food) {
            Set<String> s = getSet(c);
            s.add(gson.toJson(food));
            saveSet(c, s);
        }

        public static void remove(Context c, String name) {
            Set<String> s = getSet(c);
            s.removeIf(json -> {
                FoodSearchFragment.FoodResult f = gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                return f.name.equals(name);
            });
            saveSet(c, s);
        }

        public static Set<String> getAllFavorites(Context c) {
            return getSet(c);
        }

        private FavoritesManager() {}
    }
}
