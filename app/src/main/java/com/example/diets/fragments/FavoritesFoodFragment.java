package com.example.diets.fragments;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.adapters.SearchResultAdapter;
import com.example.diets.dialogs.GramsInputDialog;
import com.example.diets.dialogs.RecipeDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FavoritesFoodFragment extends Fragment {
    private final String TAG = "FavoritesFoodFragment";
    private ListView favoritesListView;
    private EditText searchFavoritesEditText;
    private ImageButton searchFavoritesButton;
    private SearchResultAdapter adapter;


    private final List<FoodSearchFragment.FoodResult> favoritesList = new ArrayList<>();

    private Consumer<FoodSearchFragment.FoodResult> onFoodSelected;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();
    public void setOnFoodSelected(Consumer<FoodSearchFragment.FoodResult> listener) {
        this.onFoodSelected = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites_food, container, false);

        favoritesListView = view.findViewById(R.id.favoritesListView);
        searchFavoritesEditText = view.findViewById(R.id.searchFavoritesEditText);
        searchFavoritesButton = view.findViewById(R.id.searchFavoritesButton);

        adapter = new SearchResultAdapter(requireContext(), favoritesList);
        favoritesListView.setAdapter(adapter);


        favoritesListView.setOnItemClickListener((parent, v, position, id) -> {
            if (position < adapter.getCount()) {
                FoodSearchFragment.FoodResult selectedFood = favoritesList.get(position);
                if (selectedFood.isRecipe()) {
                    RecipeDialog dialog = RecipeDialog.newInstance(selectedFood);
                    dialog.setOnGramsConfirmedListener(result -> {
                        if (onFoodSelected != null) {
                            onFoodSelected.accept(result);
                            // Si quieres cerrar el fragmento padre:
                            if (getParentFragment() instanceof DialogFragment) {
                                ((DialogFragment) getParentFragment()).dismiss();
                            }
                        }
                    });
                    dialog.show(getParentFragmentManager(), "RecipeDialog");
                } else {
                    GramsInputDialog dialog = GramsInputDialog.newInstance(selectedFood, gramsResult -> {
                        if (onFoodSelected != null) {
                            onFoodSelected.accept(gramsResult);
                            if (getParentFragment() instanceof DialogFragment) {
                                ((DialogFragment) getParentFragment()).dismiss();
                            }
                        }
                    });
                    dialog.setOnFavoriteStatusChangedListener(() -> loadFavorites());
                    dialog.show(getParentFragmentManager(), "GramsInputDialog");
                }

            }
        });

        view.setBackgroundColor(Color.TRANSPARENT);

        setupSearchListener();
        loadFavorites();

        return view;
    }
    public interface OnFavoriteStatusChangedListener {
        void onFavoriteStatusChanged();
    }


    private void setupSearchListener() {
        searchFavoritesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFavorites(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        searchFavoritesButton.setOnClickListener(v -> {
            String query = searchFavoritesEditText.getText().toString().trim();
            filterFavorites(query);
        });
    }

    private void filterFavorites(String query) {
        List<FoodSearchFragment.FoodResult> filteredFoods = new ArrayList<>();

        for (FoodSearchFragment.FoodResult food : favoritesList) {
            if (food.name.toLowerCase().contains(query.toLowerCase())) {
                filteredFoods.add(food);
            }
        }

        adapter.clear();
        adapter.addAll(filteredFoods);
        adapter.notifyDataSetChanged();
    }


    private void loadFavorites() {
        // 1. Obtener el Set de Strings (JSON) de los favoritos persistidos.
        Log.d(TAG, "Iniciando carga de favoritos desde FavoritesManager.");
        Set<String> jsonSet = GramsInputDialog.FavoritesManager.getAllFavorites(requireContext());
        Log.d(TAG, "Obtenidos " + jsonSet.size() + " JSONs de favoritos.");

        List<FoodSearchFragment.FoodResult> allFoods = new ArrayList<>();

        // 2. Iterar sobre cada JSON para deserializarlo a un objeto FoodResult.
        for (String json : jsonSet) {
            try {
                // Verificación básica de formato antes de intentar la deserialización
                if (json != null && json.trim().startsWith("{")) {
                    FoodSearchFragment.FoodResult food = gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                    allFoods.add(food);
                    Log.d(TAG, "Deserializado y agregado: " + food.name);
                } else {
                    Log.w(TAG, "JSON de favorito es nulo o inválido, omitiendo.");
                }
            } catch (Exception e) {
                // Manejo de errores de deserialización (ej. JSON malformado o cambio de estructura de la clase)
                Log.e(TAG, "Error al deserializar favorito (JSON: " + json + "): " + e.getMessage());
            }
        }

        // 3. Actualizar la lista local de favoritos del fragmento.
        favoritesList.clear();
        favoritesList.addAll(allFoods);
        Log.d(TAG, "Lista local 'favoritesList' actualizada con " + allFoods.size() + " alimentos.");

        // 4. Actualizar el adaptador de la interfaz de usuario.
        adapter.clear();
        adapter.addAll(allFoods);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adaptador de la UI notificado del cambio de datos. Carga completada.");
    }


    @Override
    public void onResume() {
        super.onResume();
        loadFavorites(); // 🔥 Cada vez que se vea el fragmento, actualizamos favoritos
    }

}
