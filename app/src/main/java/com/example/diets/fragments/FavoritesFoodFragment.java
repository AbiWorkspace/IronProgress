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

    // ✅ ATRIBUTO PARA EL UID
    private String effectiveUid;

    private final List<FoodSearchFragment.FoodResult> favoritesList = new ArrayList<>();

    private Consumer<FoodSearchFragment.FoodResult> onFoodSelected;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    // ==================== NEW INSTANCE / CICLO DE VIDA ======================

    // ✅ NEW INSTANCE MODIFICADO PARA RECIBIR UID
    public static FavoritesFoodFragment newInstance(String uid) {
        FavoritesFoodFragment fragment = new FavoritesFoodFragment();
        Bundle args = new Bundle();
        args.putString("uid", uid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            // ✅ RECUPERAR EL UID
            effectiveUid = args.getString("uid");
            Log.d(TAG, "Fragment iniciado con effectiveUid: " + effectiveUid);
        } else {
            Log.e(TAG, "ADVERTENCIA: FavoritesFoodFragment iniciado sin UID.");
        }
    }

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

        // 🛑 ANTES (EL ERROR):
        // adapter = new SearchResultAdapter(requireContext(), favoritesList);
        // Esto vinculaba el adaptador directamente a tu lista maestra. Al limpiar uno, se borraba el otro.

        // ✅ AHORA (LA SOLUCIÓN):
        // Inicializamos con una ArrayList NUEVA y VACÍA.
        adapter = new SearchResultAdapter(requireContext(), new ArrayList<>());
        favoritesListView.setAdapter(adapter);


        favoritesListView.setOnItemClickListener((parent, v, position, id) -> {
            if (position < adapter.getCount()) {
                FoodSearchFragment.FoodResult selectedFood = favoritesList.get(position);

                if (effectiveUid == null) {
                    Log.e(TAG, "UID nulo. No se puede abrir diálogo.");
                    return;
                }

                if (selectedFood.isRecipe()) {
                    // 🛑 CORRECCIÓN AQUÍ: Pasar effectiveUid
                    RecipeDialog dialog = RecipeDialog.newInstance(selectedFood, effectiveUid);

                    dialog.setOnGramsConfirmedListener(result -> {
                        if (onFoodSelected != null) {
                            onFoodSelected.accept(result);
                            if (getParentFragment() instanceof DialogFragment) {
                                ((DialogFragment) getParentFragment()).dismiss();
                            }
                        }
                    });
                    dialog.show(getParentFragmentManager(), "RecipeDialog");
                } else {
                    // ✅ Ya estaba correcto, pasa effectiveUid
                    GramsInputDialog dialog = GramsInputDialog.newInstance(
                            selectedFood,
                            gramsResult -> {
                                if (onFoodSelected != null) {
                                    onFoodSelected.accept(gramsResult);
                                    if (getParentFragment() instanceof DialogFragment) {
                                        ((DialogFragment) getParentFragment()).dismiss();
                                    }
                                }
                            },
                            effectiveUid
                    );
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
        String queryLower = query.toLowerCase();

        // Filtra SIEMPRE la lista maestra (favoritesList)
        for (FoodSearchFragment.FoodResult food : favoritesList) {
            if (food.name.toLowerCase().contains(queryLower)) {
                filteredFoods.add(food);
            }
        }

        // Actualiza el adaptador con los resultados filtrados.
        adapter.clear();
        adapter.addAll(filteredFoods);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Filtro aplicado para '" + query + "'. Mostrando " + filteredFoods.size() + " resultados.");
    }


    private void loadFavorites() {
        // Validación de seguridad
        if (effectiveUid == null) {
            Log.e(TAG, "No se puede cargar favoritos: effectiveUid es nulo.");
            favoritesList.clear();
            if (adapter != null) {
                adapter.clear();
                adapter.notifyDataSetChanged();
            }
            return;
        }

        Log.d(TAG, "Iniciando carga de favoritos desde FavoritesManager para UID: " + effectiveUid);

        // 1. Obtener el Set de Strings (JSON) de los favoritos persistidos.
        Set<String> jsonSet = GramsInputDialog.FavoritesManager.getAllFavorites(requireContext(), effectiveUid);

        List<FoodSearchFragment.FoodResult> allFoods = new ArrayList<>();

        // 2. Iterar sobre cada JSON para deserializarlo.
        for (String json : jsonSet) {
            try {
                if (json != null && json.trim().startsWith("{")) {
                    FoodSearchFragment.FoodResult food = gson.fromJson(json, FoodSearchFragment.FoodResult.class);
                    allFoods.add(food);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al deserializar favorito: " + e.getMessage());
            }
        }

        // 3. Actualizar la lista local de favoritos (favoritesList).
        favoritesList.clear();
        favoritesList.addAll(allFoods);

        Log.d(TAG, "Lista local 'favoritesList' actualizada con " + allFoods.size() + " alimentos.");

        // 4. 🔥 Llamar al refresco de la UI para aplicar el filtro actual.
        refreshFavoritesUI();
        Log.d(TAG, "Carga de favoritos completada.");
    }

    private void refreshFavoritesUI() {
        String query = searchFavoritesEditText.getText().toString().trim();
        Log.d(TAG, "Refrescando UI. Query: '" + query + "'");

        if (query.isEmpty()) {
            // Si no hay búsqueda, mostramos la lista completa (favoritesList).
            adapter.clear();
            adapter.addAll(favoritesList);
            adapter.notifyDataSetChanged();
            Log.d(TAG, "UI refrescada: Mostrando " + favoritesList.size() + " elementos sin filtrar.");
        } else {
            // Si hay búsqueda, delegamos la tarea a filterFavorites.
            filterFavorites(query);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }
}