package com.example.diets.dialogs;

import android.os.Bundle;
import android.text.Html;
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
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class RecipeDialog extends DialogFragment {

    private static final String ARG_RECIPE = "arg_recipe";
    private FoodSearchFragment.FoodResult recipe;

    private boolean isFav;
    private GramsInputDialog.OnGramsConfirmedListener gramsListener;

    public void setOnGramsConfirmedListener(GramsInputDialog.OnGramsConfirmedListener listener) {
        Log.d("RecipeDialog", "setOnGramsConfirmedListener: listener asignado");
        this.gramsListener = listener;
    }

    public static RecipeDialog newInstance(FoodSearchFragment.FoodResult r) {
        Log.d("RecipeDialog", "newInstance: creando instancia con receta " + r.name);
        Bundle b = new Bundle();
        b.putSerializable(ARG_RECIPE, r);
        RecipeDialog d = new RecipeDialog();
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("RecipeDialog", "onCreateView: inflando vista del diálogo");
        View v = inf.inflate(R.layout.dialog_recipe_view, container, false);

        recipe = (FoodSearchFragment.FoodResult) getArguments().getSerializable(ARG_RECIPE);
        if (recipe == null) {
            Log.e("RecipeDialog", "onCreateView: receta nula, cerrando diálogo");
            dismiss();
            return v;
        }

        Log.d("RecipeDialog", "onCreateView: receta cargada: " + recipe.name);

        ImageView img = v.findViewById(R.id.imgRecipe);
        TextView title = v.findViewById(R.id.txtRecipeName);
        TextView kcal = v.findViewById(R.id.txtRecipeKcal);
        TextView protein = v.findViewById(R.id.txtRecipeProtein);
        TextView carbs   = v.findViewById(R.id.txtRecipeCarbs);
        TextView fat     = v.findViewById(R.id.txtRecipeFat);

        TextView ingList = v.findViewById(R.id.txtRecipeIngList);
        TextView instrText = v.findViewById(R.id.txtRecipeInstructions);
        EditText editGrams = v.findViewById(R.id.editGrams);
        ImageButton favBtn = v.findViewById(R.id.btnFavRecipe);
        Button btnSave = v.findViewById(R.id.btnSaveRecipe);
        Button btnClose = v.findViewById(R.id.btnCloseRecipe);

        Glide.with(this)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(img);

        title.setText(recipe.name);
        kcal.setText(String.format(Locale.getDefault(), "%.0f kcal / 100 g", recipe.kcalPer100g));
        protein.setText(String.format(Locale.getDefault(), "Proteínas: %.1fg / 100g", recipe.protein));
        carbs.setText(String.format(Locale.getDefault(), "Carbohidratos: %.1fg / 100g", recipe.carbs));
        fat.setText(String.format(Locale.getDefault(), "Grasas: %.1fg / 100g", recipe.fat));

        if (recipe.ingredients.isEmpty()) {
            Log.w("RecipeDialog", "onCreateView: ingredientes vacíos");
            ingList.setText("⚠️ Ingredientes no disponibles.");
        } else {
            ingList.setText(bulletize(recipe.ingredients));
            Log.d("RecipeDialog", "onCreateView: ingredientes mostrados");
        }

        if (TextUtils.isEmpty(recipe.instructions)) {
            instrText.setText("⚠️ Instrucciones no disponibles.");
            Log.w("RecipeDialog", "onCreateView: instrucciones vacías");
        } else {
            instrText.setText("Traduciendo…");
            Log.d("RecipeDialog", "onCreateView: iniciando traducción de instrucciones");
            translateInstructionsAsync(recipe.instructions, translated -> {
                recipe.instructions = numberSteps(translated);
                instrText.setText(recipe.instructions);
                Log.d("RecipeDialog", "onCreateView: traducción finalizada");
            });
        }

        isFav = GramsInputDialog.FavoritesManager.isFavorite(requireContext(), recipe.name);
        toggleFavIcon(favBtn, isFav);
        Log.d("RecipeDialog", "onCreateView: receta esFavorita = " + isFav);

        favBtn.setOnClickListener(btn -> {
            isFav = !isFav;
            if (isFav) {
                GramsInputDialog.FavoritesManager.add(requireContext(), recipe);
                Log.d("RecipeDialog", "favBtn: receta añadida a favoritos");
            } else {
                GramsInputDialog.FavoritesManager.remove(requireContext(), recipe.name);
                Log.d("RecipeDialog", "favBtn: receta eliminada de favoritos");
            }
            toggleFavIcon(favBtn, isFav);
        });

        btnSave.setOnClickListener(b -> {
            Log.d("RecipeDialog", "btnSave: clic detectado");

            String gramsStr = editGrams.getText().toString().trim();
            double gramsInput = 100; // valor por defecto

            try {
                if (!gramsStr.isEmpty()) {
                    gramsInput = Double.parseDouble(gramsStr);
                    if (gramsInput <= 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Gramos inválidos", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("RecipeDialog", "btnSave: gramos introducidos = " + gramsInput);

            // Calculamos los valores en base a los gramos
            recipe.grams = gramsInput;
            double factor = gramsInput / 100.0;

            recipe.kcalPer100g = recipe.kcalPer100g; // este no se cambia
            recipe.protein = Math.round(recipe.protein * factor * 10) / 10.0;
            recipe.carbs = Math.round(recipe.carbs * factor * 10) / 10.0;
            recipe.fat = Math.round(recipe.fat * factor * 10) / 10.0;

            Log.d("RecipeDialog", "btnSave: valores ajustados → kcal=" + recipe.kcalPer100g +
                    " P=" + recipe.protein + " C=" + recipe.carbs + " F=" + recipe.fat);

            if (gramsListener != null) {
                gramsListener.onGramsConfirmed(recipe);
            } else {
                Log.e("RecipeDialog", "Listener es null – no se pudo devolver la receta");
                Toast.makeText(getContext(), "Error interno al añadir la receta", Toast.LENGTH_SHORT).show();
            }

            dismiss();
        });
        btnClose.setOnClickListener(btn -> {
            Log.d("RecipeDialog", "btnClose: cerrando diálogo");
            dismiss();
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("RecipeDialog", "onStart: ajustando fondo transparente");
        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private String bulletize(List<String> items) {
        Log.d("RecipeDialog", "bulletize: formateando lista de ingredientes");
        StringBuilder sb = new StringBuilder();
        for (String s : items)
            sb.append("• ").append(s.trim()).append("\n");
        return sb.toString().trim();
    }

    private void translateInstructionsAsync(String src, Consumer<String> onDone) {
        Log.d("RecipeDialog", "translateInstructionsAsync: preparando modelo de traducción");
        TranslatorOptions opts = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build();
        Translator translator = Translation.getClient(opts);

        translator.downloadModelIfNeeded(new DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener(unused -> {
                    Log.d("RecipeDialog", "translateInstructionsAsync: modelo descargado");
                    translator.translate(src)
                            .addOnSuccessListener(translated -> {
                                Log.d("RecipeDialog", "translateInstructionsAsync: traducción exitosa");
                                onDone.accept(translated);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("RecipeDialog", "translateInstructionsAsync: error al traducir", e);
                                onDone.accept(src);
                            })
                            .addOnCompleteListener(t -> translator.close());
                })
                .addOnFailureListener(e -> {
                    Log.e("RecipeDialog", "translateInstructionsAsync: error al descargar modelo", e);
                    onDone.accept(src);
                    translator.close();
                });
    }

    private String numberSteps(String raw) {
        Log.d("RecipeDialog", "numberSteps: formateando pasos numerados");
        if (TextUtils.isEmpty(raw)) return "";

        raw = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        raw = raw.replaceAll("\\s+", " ");
        if (!raw.endsWith(".")) raw += ".";

        String[] split = raw.split("\\.\\s+");
        StringBuilder sb = new StringBuilder();
        int stepNum = 1;
        for (String line : split) {
            line = line.trim();
            if (line.length() < 5) continue;
            sb.append(stepNum++).append(". ")
                    .append(Character.toUpperCase(line.charAt(0)))
                    .append(line.substring(1).toLowerCase())
                    .append(".\n\n");
        }

        return sb.toString().trim();
    }

    private void toggleFavIcon(ImageButton b, boolean fav) {
        Log.d("RecipeDialog", "toggleFavIcon: cambiando ícono de favorito = " + fav);
        b.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_toggle);
        b.setColorFilter(fav ? 0xFFE63946 : 0xFFBBBBBB);
    }
}
