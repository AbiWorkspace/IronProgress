package com.example.diets.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.fragments.FavoritesFoodFragment;
import com.example.diets.fragments.FoodSearchFragment;
import com.google.gson.Gson;

import java.util.*;

public class GramsInputDialog extends DialogFragment {

    private static final String TAG      = "GramsInputDialog";
    private static final String ARG_FOOD = "arg_food";
    private static final Gson   gson     = new Gson();

    private double  selectedPortionGrams = 0;
    private boolean isFavoriteNow;

    public interface OnGramsConfirmedListener {
        void onGramsConfirmed(FoodSearchFragment.FoodResult result);
    }
    private OnGramsConfirmedListener listener;
    private FavoritesFoodFragment.OnFavoriteStatusChangedListener favoriteStatusListener;

    public static GramsInputDialog newInstance(FoodSearchFragment.FoodResult food,
                                               OnGramsConfirmedListener listener) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_FOOD, food);
        GramsInputDialog d = new GramsInputDialog();
        d.setArguments(args);
        d.listener = listener;
        return d;
    }

    @Override public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow()
                    .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
            p.width  = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            p.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(p);
        }
    }

    public void setOnFavoriteStatusChangedListener(
            FavoritesFoodFragment.OnFavoriteStatusChangedListener l) {
        this.favoriteStatusListener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inf.inflate(R.layout.dialog_input_grams, container, false);

        ImageButton btnFav   = v.findViewById(R.id.btnFavToggle);
        ImageView   img      = v.findViewById(R.id.foodImage);
        TextView    brand    = v.findViewById(R.id.foodBrand);
        TextView    det      = v.findViewById(R.id.foodDetails);
        TextView    title    = v.findViewById(R.id.dialogTitle);
        EditText    inGrams  = v.findViewById(R.id.inputGrams);
        Spinner     spnPort  = v.findViewById(R.id.spinnerPortions);
        Button      cancel   = v.findViewById(R.id.btnCancel);
        Button      ok       = v.findViewById(R.id.btnAccept);

        // ----- Alimento recibido -----
        FoodSearchFragment.FoodResult food =
                (FoodSearchFragment.FoodResult) getArguments().getSerializable(ARG_FOOD);
        if (food == null) { Log.e(TAG,"FoodResult null"); dismiss(); return v; }

        title.setText("¿Cuánto de " + food.name + "?");
        brand.setText("Marca: " + food.brand);

        // --------- Preview inicial con 100g ----------
        updateDetails(det, food, 100);

        Glide.with(requireContext())
                .load(food.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(img);

        img.setOnClickListener(view -> {
            if (food.imageUrl != null && !food.imageUrl.isEmpty()) {
                ImageFullscreenDialog d = ImageFullscreenDialog.newInstance(food.imageUrl);
                d.show(getParentFragmentManager(), "ImageFullscreenDialog");
            }
        });


        // ---------- Filtros de entrada ----------
        inGrams.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(6),
                (src, s, e, dest, ds, de) -> {
                    for (int i = s; i < e; i++) {
                        char c = src.charAt(i);
                        if (!Character.isDigit(c) && c != '.') return "";
                    }
                    return null;
                }
        });

        // ---------- Label dinámico kcal/macros encima del EditText ----------



        ViewGroup parent = (ViewGroup) inGrams.getParent();
        int posBeforeEdit = parent.indexOfChild(inGrams);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(4);


        // ---------- Spinner de porciones ----------
        List<String> entries = new ArrayList<>();
        entries.add("Ingresar gramos manualmente");
        for (FoodSearchFragment.FoodResult.Portion p : food.portions) {
            entries.add(p.label + " (" + p.grams + " g)");
        }
        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, entries);
        spnPort.setAdapter(spAdapter);

        spnPort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    selectedPortionGrams = 0;
                    inGrams.setHint("Cantidad en gramos");
                    inGrams.setText("");
                } else {
                    selectedPortionGrams = food.portions.get(pos - 1).grams;
                    inGrams.setHint("N.º porciones (ej. 1.25)");
                    inGrams.setText("1");
                }
                // Actualizar preview al cambiar porción
                Double val = parseSafeDecimal(inGrams.getText().toString());
                double grams = (val == null)
                        ? (selectedPortionGrams == 0 ? 100 : selectedPortionGrams)
                        : (selectedPortionGrams == 0 ? val : val * selectedPortionGrams);
                updateDetails(det, food, grams);

            }
        });

        // ---------- TextWatcher kcal/macros instantáneos ----------
        inGrams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c) {
                Double val = parseSafeDecimal(s.toString());
                double grams = (val == null)
                        ? (selectedPortionGrams == 0 ? 100 : selectedPortionGrams)
                        : (selectedPortionGrams == 0 ? val : val * selectedPortionGrams);
                updateDetails(det, food, grams);

            }
        });

        // ---------- Favoritos ----------
        isFavoriteNow = FavoritesManager.isFavorite(requireContext(), food.name);
        updateFavIcon(btnFav, isFavoriteNow);
        btnFav.setOnClickListener(vw -> {
            isFavoriteNow = !isFavoriteNow;
            if (isFavoriteNow) {
                FavoritesManager.add(requireContext(), food);
            } else {
                FavoritesManager.remove(requireContext(), food.name);
                if (favoriteStatusListener != null)
                    favoriteStatusListener.onFavoriteStatusChanged();
            }
            updateFavIcon(btnFav, isFavoriteNow);
        });

        // ---------- Botones ----------
        cancel.setOnClickListener(vw -> dismiss());

        ok.setOnClickListener(vw -> {
            Double val = parseSafeDecimal(inGrams.getText()
                    .toString().trim());
            double grams = (val == null)
                    ? (selectedPortionGrams == 0 ? 100 : selectedPortionGrams)
                    : (selectedPortionGrams == 0 ? val : val * selectedPortionGrams);
            if (grams <= 0) { showError(); return; }
            food.grams = grams;
            if (listener != null) listener.onGramsConfirmed(food);
            dismiss();
        });

        return v;
    }

    // ---- Actualiza el TextView det con el preview tipo etiqueta (igual que la captura) ----
    private void updateDetails(TextView det, FoodSearchFragment.FoodResult food, double grams) {
        double kcal = kcalFromGrams(grams, food.kcalPer100g);
        double prot = food.protein * grams / 100.0;
        double carb = food.carbs   * grams / 100.0;
        double fat  = food.fat     * grams / 100.0;
        det.setText(String.format(Locale.getDefault(),
                "%.0f kcal / %.0fg\nProteínas: %.1fg / %.0fg\nCarbohidratos: %.1fg / %.0fg\nGrasas: %.1fg / %.0fg",
                kcal, grams, prot, grams, carb, grams, fat, grams
        ));
    }

    // ---- Actualiza el TextView kcalLive (encima del input) ----
    private void updateKcalLive(TextView kcalLive, FoodSearchFragment.FoodResult food, double grams) {
        if (grams == 0) {
            kcalLive.setText("");
        } else {
            double kcal = kcalFromGrams(grams, food.kcalPer100g);
            double prot = food.protein * grams / 100.0;
            double carb = food.carbs   * grams / 100.0;
            double fat  = food.fat     * grams / 100.0;
            kcalLive.setText(String.format(Locale.getDefault(),
                    "≈ %.0f kcal • P %.1fg • C %.1fg • F %.1fg",
                    kcal, prot, carb, fat));
        }
    }

    private void showError() {
        Toast.makeText(getContext(), "Ingresa un número válido", Toast.LENGTH_SHORT).show();
    }
    private int dp(int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val,
                getResources().getDisplayMetrics());
    }
    private static Double parseSafeDecimal(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }
    private static double kcalFromGrams(double g, double kcal100) {
        return g * kcal100 / 100.0;
    }
    private void updateFavIcon(ImageButton btn, boolean fav) {
        btn.setSelected(fav);
        btn.setColorFilter(fav ? 0xFFE63946 : 0xFFBBBBBB);
    }

    public static final class FavoritesManager {
        private static final String PREF = "favorites_pref";
        private static final String KEY  = "fav_set";

        private static Set<String> getSet(Context c) {
            SharedPreferences p = c.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            return new HashSet<>(p.getStringSet(KEY, new HashSet<>()));
        }
        private static void saveSet(Context c, Set<String> s) {
            c.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putStringSet(KEY, s).apply();
        }

        public static boolean isFavorite(Context c, String name) {
            for (String json : getSet(c)) {
                FoodSearchFragment.FoodResult f =
                        gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                if (f.name.equals(name)) return true;
            }
            return false;
        }
        public static void add(Context c, FoodSearchFragment.FoodResult f) {
            Set<String> s = getSet(c);
            s.add(gson.toJson(f)); saveSet(c, s);
        }
        public static void remove(Context c, String name) {
            Set<String> s = getSet(c);
            s.removeIf(j -> gson.fromJson(j, FoodSearchFragment.FoodResult.class)
                    .name.equals(name));
            saveSet(c, s);
        }
        public static Set<String> getAllFavorites(Context c) { return getSet(c); }
        private FavoritesManager() {}
    }
}
