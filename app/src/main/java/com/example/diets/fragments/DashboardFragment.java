package com.example.diets.fragments;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.diets.R;
import com.example.diets.activitys.MainActivity;
import com.example.diets.activitys.WeightActivity;
import com.example.diets.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.Locale;
import android.text.InputFilter;
import android.content.SharedPreferences;
import android.widget.ImageButton;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private OnNavigationItemSelectedListener mListener;

    // UI Elements
    private TextView caloriesNeededTextView, caloriesTodayTextView, proteinTextView, fatTextView, carbTextView;
    private EditText foodTextField;
    private User user;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private NavigationView navigationView;
    private LinearLayout foodContainer;
    private static final int MAX_FOOD_BLOCKS = 10;
    private static final String PREFS_NAME = "FoodPrefs";
    private static final String[] DEFAULT_FOOD_NAMES = {"Desayuno", "Almuerzo", "Comida", "Snack", "Cena"};

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(int position);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigationItemSelectedListener) {
            mListener = (OnNavigationItemSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnNavigationItemSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_food, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializa FirebaseAuth y UI
        mAuth = FirebaseAuth.getInstance();
        initializeUI(view);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Carga los datos del usuario desde Firebase
        if (mAuth.getCurrentUser() != null) {
            loadUserData(mAuth.getCurrentUser().getUid());
        }

        // Configura el NavigationView (menú lateral)
        setupNavigationView(view);

        // Cargar comidas guardadas o predeterminadas
        loadSavedOrDefaultFoodBlocks();
    }

    private void loadUserData(String userId) {
        Log.d(TAG, "📌 Cargando datos del usuario para userId: " + userId);

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "🔥 Datos obtenidos: " + snapshot.getValue());

                    // 🔥 Extraer datos manualmente (EVITANDO `getValue(User.class)`)
                    String gender = snapshot.child("gender").getValue(String.class);
                    int age = snapshot.child("age").getValue(Integer.class) != null ? snapshot.child("age").getValue(Integer.class) : 0;
                    boolean firstTime = snapshot.child("firstTime").getValue(Boolean.class) != null ? snapshot.child("firstTime").getValue(Boolean.class) : false;
                    String activityLevel = snapshot.child("activityLevel").getValue(String.class);
                    String goal = snapshot.child("goal").getValue(String.class);
                    double weight = snapshot.child("weight").getValue(Double.class) != null ? snapshot.child("weight").getValue(Double.class) : 0;
                    double height = snapshot.child("height").getValue(Double.class) != null ? snapshot.child("height").getValue(Double.class) : 0;

                    // 🔥 Crear instancia de User manualmente
                    user = new User();
                    user.setGender(gender);
                    user.setAge(age);
                    user.setFirstTime(firstTime);
                    user.setActivityLevel(activityLevel);
                    user.setGoal(goal);
                    user.setWeight(weight);
                    user.setHeight(height);

                    Log.d(TAG, "✅ Usuario cargado manualmente: " + user.toString());

                    // 🔥 Asegurar que los valores son correctos antes de llamar a `updateNutritionalValues()`
                    Log.d(TAG, "📊 Datos del usuario:");
                    Log.d(TAG, "➡️ Edad: " + user.getAge());
                    Log.d(TAG, "➡️ Género: " + user.getGender());
                    Log.d(TAG, "➡️ Nivel de actividad: " + user.getActivityLevel());
                    Log.d(TAG, "➡️ Objetivo: " + user.getGoal());
                    Log.d(TAG, "➡️ Peso: " + user.getWeight());
                    Log.d(TAG, "➡️ Altura: " + user.getHeight());

                    // 🔥 Actualizar la UI con la información obtenida
                    updateNutritionalValues(user);
                } else {
                    Log.w(TAG, "⚠ No se encontraron datos del usuario en la base de datos.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar los datos del usuario: " + error.getMessage(), error.toException());
            }
        });
    }


    private void initializeUI(View view) {
        foodTextField = view.findViewById(R.id.foodTextField);
        caloriesNeededTextView = view.findViewById(R.id.caloriesNeededTextView);
        caloriesTodayTextView = view.findViewById(R.id.caloriesTodayTextView);
        proteinTextView = view.findViewById(R.id.proteinTextView);
        fatTextView = view.findViewById(R.id.fatTextView);
        carbTextView = view.findViewById(R.id.carbTextView);

        foodContainer = view.findViewById(R.id.foodContainer);
        Button addFoodButton = view.findViewById(R.id.addFoodButton);

        addFoodButton.setOnClickListener(v -> addFoodBlock());

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            Log.d(TAG, "BottomNavigationView encontrado");
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    Log.d(TAG, "Navegando a EntrenamientoFragment");
                    if (mListener != null) {
                        mListener.onNavigationItemSelected(1); // Cambia al primer fragmento del ViewPager
                    }
                    return true;
                } else if (itemId == R.id.navigation_dashboard) {
                    Log.d(TAG, "Navegando a WeightActivity");
                    startActivity(new Intent(getActivity(), WeightActivity.class));
                    return true;
                }
                return false;
            });
        } else {
            Log.e(TAG, "BottomNavigationView no encontrado");
        }

        navigationView = view.findViewById(R.id.navigation_view);
    }

    private void setupNavigationView(View view) {
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                handleLogout();
                return true;
            }
            return false;
        });
    }

    private void handleLogout() {
        mAuth.signOut();
        Toast.makeText(getActivity(), "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show();

        // Redirige a la pantalla de inicio de sesión
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void updateNutritionalValues(User user) {
        double caloriesNeeded = user.calculateCalories(
                user.getWeight(),
                user.getHeight(),
                user.getAge(),
                user.getGender(),
                user.getActivityLevel(),
                user.getGoal()
        );

        double proteinNeeded = user.calculateProtein();
        double fatNeeded = user.calculateFat();
        double carbNeeded = user.calculateCarbs();

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                caloriesTodayTextView.setText(String.format(Locale.getDefault(), "Calorías consumidas: %.2f kcal", user.getCaloriesToday()));
                caloriesNeededTextView.setText(String.format(Locale.getDefault(), "Calorías necesarias: %.2f kcal", caloriesNeeded));
                proteinTextView.setText(String.format(Locale.getDefault(), "Proteínas necesarias: %.2f g", proteinNeeded));
                fatTextView.setText(String.format(Locale.getDefault(), "Grasas necesarias: %.2f g", fatNeeded));
                carbTextView.setText(String.format(Locale.getDefault(), "Carbohidratos necesarios: %.2f g", carbNeeded));
            });
        }
    }

    private void addFoodBlock() {
        if (foodContainer.getChildCount() < MAX_FOOD_BLOCKS) {
            View foodBlock = getLayoutInflater().inflate(R.layout.food_block, null);
            EditText foodTitle = foodBlock.findViewById(R.id.foodTitle);
            foodTitle.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });

            // Añadir margen inferior para separación
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16); // Margen inferior de 16dp
            foodBlock.setLayoutParams(params);

            foodContainer.addView(foodBlock);

            // Guardar el nombre de la comida cuando se modifique
            foodTitle.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    saveFoodNames();
                }
            });

            // Configurar el menú desplegable
            ImageButton menuButton = foodBlock.findViewById(R.id.menuButton);
            menuButton.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(getContext(), menuButton);
                popupMenu.getMenuInflater().inflate(R.menu.food_block_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete) {
                        foodContainer.removeView(foodBlock);
                        saveFoodNames();
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });

            // Añadir lógica para añadir alimentos
            LinearLayout foodItemsContainer = foodBlock.findViewById(R.id.foodItemsContainer);
            ImageButton addFoodItemButton = foodBlock.findViewById(R.id.addFoodItemButton);
            addFoodItemButton.setOnClickListener(v -> addFoodItem(foodItemsContainer));
        } else {
            Toast.makeText(getActivity(), "No puedes añadir más de 10 comidas", Toast.LENGTH_SHORT).show();
        }
    }
    private void addFoodBlockWithName(String name) {
        View foodBlock = getLayoutInflater().inflate(R.layout.food_block, null);
        EditText foodTitle = foodBlock.findViewById(R.id.foodTitle);
        foodTitle.setText(name);
        foodTitle.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });

        // Añadir margen inferior para separación
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16); // Margen inferior de 16dp
        foodBlock.setLayoutParams(params);

        foodContainer.addView(foodBlock);

        // Guardar el nombre de la comida cuando se modifique
        foodTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveFoodNames();
            }
        });

        // Configurar el menú desplegable
        ImageButton menuButton = foodBlock.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getContext(), menuButton);
            popupMenu.getMenuInflater().inflate(R.menu.food_block_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    foodContainer.removeView(foodBlock);
                    saveFoodNames();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        // Añadir lógica para añadir alimentos
        LinearLayout foodItemsContainer = foodBlock.findViewById(R.id.foodItemsContainer);
        ImageButton addFoodItemButton = foodBlock.findViewById(R.id.addFoodItemButton);
        addFoodItemButton.setOnClickListener(v -> addFoodItem(foodItemsContainer));
    }

    private void addFoodItem(LinearLayout foodItemsContainer) {
        View foodItem = getLayoutInflater().inflate(R.layout.food_item, null);

        // Añadir margen inferior para separación
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8); // Margen inferior de 8dp
        foodItem.setLayoutParams(params);

        foodItemsContainer.addView(foodItem);
    }

    private void saveFoodNames() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < foodContainer.getChildCount(); i++) {
            View foodBlock = foodContainer.getChildAt(i);
            EditText foodTitle = foodBlock.findViewById(R.id.foodTitle);
            String foodName = foodTitle.getText().toString();
            editor.putString("foodName" + i, foodName);
        }
        editor.apply();
    }

    private void loadSavedOrDefaultFoodBlocks() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        for (int i = 0; i < MAX_FOOD_BLOCKS; i++) {
            String foodName = prefs.getString("foodName" + i, null);
            if (foodName != null) {
                addFoodBlockWithName(foodName);
            } else if (i < DEFAULT_FOOD_NAMES.length) {
                addFoodBlockWithName(DEFAULT_FOOD_NAMES[i]);
            }
        }
    }
}