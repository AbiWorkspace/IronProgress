package com.example.diets.dialogs;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.fragments.FoodSearchFragment;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translation;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Diálogo que muestra detalles completos de una receta o producto seleccionado (estético). */
public class RecipeDialog extends DialogFragment {

    /* ---------- CONSTANTES ---------- */
    private static final String TAG        = "RecipeDialog";
    private static final String ARG_RECIPE = "arg_recipe";

    /* ---------- MODELO ---------- */
    private FoodSearchFragment.FoodResult recipe;

    /* ---------- FLAGS ---------- */
    private boolean isFav;

    /* ---------- LISTENER ---------- */
    private GramsInputDialog.OnGramsConfirmedListener gramsListener;

    public void setOnGramsConfirmedListener(GramsInputDialog.OnGramsConfirmedListener listener) {
        Log.d(TAG, "setOnGramsConfirmedListener: listener asignado");
        this.gramsListener = listener;
    }

    public static RecipeDialog newInstance(FoodSearchFragment.FoodResult r) {
        Log.d(TAG, "newInstance: creando instancia con receta " + r.name);
        Bundle b = new Bundle();
        b.putSerializable(ARG_RECIPE, r);
        RecipeDialog d = new RecipeDialog();
        d.setArguments(b);
        return d;
    }

    /* ================= CICLO DE VIDA ================= */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView: inflando vista del diálogo");
        View v = inf.inflate(R.layout.dialog_recipe_view, container, false);
        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000)); // fondo transparente

        recipe = (FoodSearchFragment.FoodResult) getArguments().getSerializable(ARG_RECIPE);
        if (recipe == null) {
            Log.e(TAG, "onCreateView: receta nula – cerrando diálogo.");
            dismiss();
            return v;
        }
        Log.d(TAG, "onCreateView: receta recibida – idMeal=" + recipe.idMeal +
                " | name=" + recipe.name +
                " | instrLen=" + (recipe.instructions == null ? "null" : recipe.instructions.length()));

        /* ---------- REFERENCIAS UI ---------- */
        ImageView  img        = v.findViewById(R.id.imgRecipe);
        TextView   title      = v.findViewById(R.id.txtRecipeName);
        TextView   kcalTxt    = v.findViewById(R.id.txtRecipeKcal);
        TextView   proteinTxt = v.findViewById(R.id.txtRecipeProtein);
        TextView   carbsTxt   = v.findViewById(R.id.txtRecipeCarbs);
        TextView   fatTxt     = v.findViewById(R.id.txtRecipeFat);
        TextView   ingList    = v.findViewById(R.id.txtRecipeIngList);
        TextView   instrTxt   = v.findViewById(R.id.txtRecipeInstructions);
        EditText   gramsEdit  = v.findViewById(R.id.editGrams);
        ImageButton favBtn    = v.findViewById(R.id.btnFavRecipe);
        Button     btnSave    = v.findViewById(R.id.btnSaveRecipe);
        Button     btnClose   = v.findViewById(R.id.btnCloseRecipe);

        /* ---------- IMAGEN ---------- */
        Glide.with(this)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(img);
        Log.d(TAG, "onCreateView: imagen cargada → " + recipe.imageUrl);

        img.setOnClickListener(view -> {
            Log.d(TAG, "imgRecipe: clic – abriendo en fullscreen.");
            if (recipe.imageUrl != null && !recipe.imageUrl.isEmpty()) {
                ImageFullscreenDialog d = ImageFullscreenDialog.newInstance(recipe.imageUrl);
                d.show(getParentFragmentManager(), "ImageFullscreenDialog");
            } else {
                Log.w(TAG, "imgRecipe: imagen vacía, no se abre diálogo.");
            }
        });

        /* ---------- MACROS BASE ---------- */
        final double baseKcal    = recipe.kcalPer100g;
        final double baseProtein = recipe.protein;
        final double baseCarbs   = recipe.carbs;
        final double baseFat     = recipe.fat;

        /* ---------- INFO BÁSICA ---------- */
        title.setText(recipe.name);
        kcalTxt.setText   (String.format(Locale.getDefault(), "%.0f kcal / 100 g", baseKcal));
        proteinTxt.setText(String.format(Locale.getDefault(), "Proteínas: %.1fg / 100 g", baseProtein));
        carbsTxt.setText  (String.format(Locale.getDefault(), "Carbohidratos: %.1fg / 100 g", baseCarbs));
        fatTxt.setText    (String.format(Locale.getDefault(), "Grasas: %.1fg / 100 g", baseFat));

        /* ---------- INGREDIENTES ---------- */
        if (recipe.ingredients == null || recipe.ingredients.isEmpty()) {
            Log.w(TAG, "onCreateView: ingredientes vacíos.");
            ingList.setText("⚠️ Ingredientes no disponibles.");
        } else {
            ingList.setText(htmlBulletize(recipe.ingredients));
            Log.d(TAG, "onCreateView: ingredientes mostrados. total=" + recipe.ingredients.size());
        }

        /* ---------- INSTRUCCIONES ---------- */
        if (TextUtils.isEmpty(recipe.instructions)) {
            Log.w(TAG, "onCreateView: recipe.instructions está vacío – mostrando placeholder.");
            instrTxt.setText("⚠️ Instrucciones no disponibles.");
        } else {
            Log.d(TAG, "onCreateView: instrucciones originales (len=" + recipe.instructions.length() + ") → " +
                    preview(recipe.instructions));
            instrTxt.setText("Traduciendo…");
            translateInstructionsAsync(recipe.instructions, translated -> {
                String formatted = numberStepsHtml(translated);   // HTML estilizado
                recipe.instructions = formatted;
                Spanned sp = Html.fromHtml(formatted, Html.FROM_HTML_MODE_LEGACY);
                instrTxt.setText(sp);
                Log.d(TAG, "onCreateView: instrucciones formateadas len=" + formatted.length());
            });
        }

        /* ---------- FAVORITOS ---------- */
        isFav = GramsInputDialog.FavoritesManager.isFavorite(requireContext(), recipe.name);
        Log.d(TAG, "onCreateView: receta esFavorita = " + isFav);
        toggleFavIcon(favBtn, isFav);
        favBtn.setOnClickListener(btn -> {
            isFav = !isFav;
            if (isFav) {
                GramsInputDialog.FavoritesManager.add(requireContext(), recipe);
                Log.d(TAG, "favBtn: añadido a favoritos.");
            } else {
                GramsInputDialog.FavoritesManager.remove(requireContext(), recipe.name);
                Log.d(TAG, "favBtn: eliminado de favoritos.");
            }
            toggleFavIcon(favBtn, isFav);
        });

        /* ---------- CAMBIO DINÁMICO DE GRAMOS ---------- */
        gramsEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                double grams = 100;
                try {
                    String g = s.toString().trim();
                    if (!g.isEmpty()) grams = Double.parseDouble(g);
                    if (grams <= 0) grams = 100;
                } catch (NumberFormatException e) {
                    grams = 100;
                }
                double f = grams / 100.0;
                kcalTxt.setText   (String.format(Locale.getDefault(), "%.0f kcal / %.0fg", baseKcal * f, grams));
                proteinTxt.setText(String.format(Locale.getDefault(), "Proteínas: %.1fg / %.0fg", baseProtein * f, grams));
                carbsTxt.setText  (String.format(Locale.getDefault(), "Carbohidratos: %.1fg / %.0fg", baseCarbs * f, grams));
                fatTxt.setText    (String.format(Locale.getDefault(), "Grasas: %.1fg / %.0fg", baseFat * f, grams));
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        /* ---------- BOTÓN GUARDAR ---------- */
        btnSave.setOnClickListener(b -> {
            double grams = 100;
            try {
                String g   = gramsEdit.getText().toString().trim();
                if (!g.isEmpty()) grams = Double.parseDouble(g);
                if (grams <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Gramos inválidos", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "btnSave: gramos inválidos introducidos.");
                return;
            }

            double factor = grams / 100.0;
            recipe.grams       = grams;
            recipe.kcalPer100g = Math.round(baseKcal * factor);
            recipe.protein     = Math.round(baseProtein * factor * 10) / 10.0;
            recipe.carbs       = Math.round(baseCarbs * factor * 10) / 10.0;
            recipe.fat         = Math.round(baseFat * factor * 10) / 10.0;

            Log.d(TAG, "btnSave: valores ajustados → grams=" + grams +
                    " | kcal=" + recipe.kcalPer100g +
                    " | P=" + recipe.protein +
                    " | C=" + recipe.carbs +
                    " | F=" + recipe.fat);

            if (gramsListener != null) {
                gramsListener.onGramsConfirmed(recipe);
            } else {
                Log.e(TAG, "btnSave: listener nulo – no se devolvió receta.");
            }
            dismiss();
        });

        /* ---------- BOTÓN CERRAR ---------- */
        btnClose.setOnClickListener(vw -> {
            Log.d(TAG, "btnClose: diálogo cerrado por usuario.");
            dismiss();
        });

        return v;
    }

    /* ================= MÉTODOS AUXILIARES ================= */

    /** Devuelve ingredientes como lista HTML elegante. */
    private Spanned htmlBulletize(List<String> items) {
        StringBuilder sb = new StringBuilder("<ul>");
        for (String s : items) {
            sb.append("<li>").append(Html.escapeHtml(s.trim())).append("</li>");
        }
        sb.append("</ul>");
        return Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY);
    }

    private void translateInstructionsAsync(String src, Consumer<String> onDone) {
        Log.d(TAG, "translateInstructionsAsync: texto origen len=" + src.length());
        TranslatorOptions opts = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build();
        Translator translator = Translation.getClient(opts);

        translator.downloadModelIfNeeded(new DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "translateInstructionsAsync: modelo descargado.");
                    translator.translate(src)
                            .addOnSuccessListener(translated -> {
                                Log.d(TAG, "translateInstructionsAsync: traducción recibida len=" + translated.length());
                                onDone.accept(translated);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "translateInstructionsAsync: error al traducir.", e);
                                onDone.accept(src);
                            })
                            .addOnCompleteListener(t -> translator.close());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "translateInstructionsAsync: error al descargar modelo.", e);
                    onDone.accept(src);
                    translator.close();
                });
    }

    /** Convierte instrucciones en HTML numerado, con párrafos espaciados. */
    private String numberStepsHtml(String raw) {
        Log.d(TAG, "numberStepsHtml: rawLen=" + raw.length());
        if (TextUtils.isEmpty(raw)) return "";

        raw = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        raw = raw.replaceAll("\\s+", " ");
        if (!raw.endsWith(".")) raw += ".";

        String[] split = raw.split("\\.\\s+");
        StringBuilder sb = new StringBuilder();
        int step = 1;
        for (String line : split) {
            line = line.trim();
            if (line.length() < 3) continue;
            sb.append("<b>").append(step++).append(".</b> ")
                    .append(Character.toUpperCase(line.charAt(0)))
                    .append(line.substring(1))
                    .append(".<br><br>");
        }
        String res = sb.toString().trim();
        Log.d(TAG, "numberStepsHtml: resultLen=" + res.length());
        return res;
    }

    private void toggleFavIcon(ImageButton btn, boolean fav) {
        btn.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_toggle);
        btn.setColorFilter(fav ? 0xFFE63946 : 0xFFBBBBBB);
    }

    private static String preview(String s) {
        if (s == null) return "null";
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }
}
