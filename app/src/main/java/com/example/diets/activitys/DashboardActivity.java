package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import com.example.diets.R;
import com.example.diets.api.FoodService;
import com.example.diets.model.User;
import com.example.diets.record.RecipeResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";

    // UI Elements
    private TextView caloriesNeededTextView, caloriesTodayTextView, proteinTextView, fatTextView, carbTextView;
    private TextView breakfastDetails, lunchDetails, dinnerDetails, snacksDetails;
    private EditText foodTextField;
    private User user;
    private FoodService foodService;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private final Map<String, JsonArray> mealNutritionMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Inicializa FirebaseAuth y UI
        mAuth = FirebaseAuth.getInstance();
        initializeUI();

        foodService = new FoodService();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Carga los datos del usuario desde Firebase
        loadUserData(mAuth.getCurrentUser().getUid());

        // Configura el NavigationView (menú lateral)
        setupNavigationView();
    }

    private void loadUserData(String userId) {
        Log.d(TAG, "Loading user data for userId: " + userId);
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User.class);
                    if (user != null) {
                        Log.d(TAG, "User data loaded successfully: " + user.toString());
                        updateNutritionalValues(user);
                    }
                } else {
                    Log.w(TAG, "No user data found for userId: " + userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user data: " + error.getMessage(), error.toException());
            }
        });
    }

    private void initializeUI() {
        foodTextField = findViewById(R.id.foodTextField);
        caloriesNeededTextView = findViewById(R.id.caloriesNeededTextView);
        caloriesTodayTextView = findViewById(R.id.caloriesTodayTextView);
        proteinTextView = findViewById(R.id.proteinTextView);
        fatTextView = findViewById(R.id.fatTextView);
        carbTextView = findViewById(R.id.carbTextView);

        breakfastDetails = findViewById(R.id.breakfastDetails);
        lunchDetails = findViewById(R.id.lunchDetails);
        dinnerDetails = findViewById(R.id.dinnerDetails);
        snacksDetails = findViewById(R.id.snacksDetails);

        // Configura el menú inferior
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                startActivity(new Intent(this, WeightActivity.class));
                return true;
            } else if (item.getItemId() == R.id.navigation_video) {
                startActivity(new Intent(this, VideoActivity.class));
                return true;
            } else {
                return false;
            }

        });

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
    }

    private void setupNavigationView() {
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
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show();

        // Redirige a la pantalla de inicio de sesión
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Cierra el DrawerLayout si está abierto
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        }
    }

    private void modifyMeal(String mealType) {
        String newFood = foodTextField.getText().toString().trim();
        if (newFood.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un alimento para modificar.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonArray currentNutrients = mealNutritionMap.get(mealType);
        if (currentNutrients != null) {
            adjustUserNutrition(currentNutrients, false);
            mealNutritionMap.remove(mealType);
        }
        fetchAndAddRecipe(mealType);
    }

    private void fetchAndAddRecipe(String mealType) {
        String newFood = foodTextField.getText().toString().trim();
        if (newFood.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un alimento.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                List<RecipeResponse.Result> recipes = foodService.getFoodRecipes(newFood);
                if (recipes != null && !recipes.isEmpty()) {
                    runOnUiThread(() -> showRecipeSelectionDialog(recipes, mealType));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se encontraron recetas.", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error al obtener recetas: " + e.getMessage(), e);
            }
        }).start();
    }

    private void showRecipeSelectionDialog(List<RecipeResponse.Result> recipes, String mealType) {
        String[] recipeTitles = recipes.stream().map(RecipeResponse.Result::title).toArray(String[]::new);
        new AlertDialog.Builder(this)
                .setTitle("Selecciona una receta para " + mealType)
                .setItems(recipeTitles, (dialog, which) -> {
                    RecipeResponse.Result selectedRecipe = recipes.get(which);
                    fetchAndAddNutritionData(selectedRecipe, mealType);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void fetchAndAddNutritionData(RecipeResponse.Result selectedRecipe, String mealType) {
        new Thread(() -> {
            try {
                JsonObject nutrition = foodService.getRecipeNutrition(selectedRecipe.id());
                if (nutrition != null) {
                    JsonArray nutrients = nutrition.getAsJsonArray("nutrients");
                    mealNutritionMap.put(mealType, nutrients);
                    adjustUserNutrition(nutrients, true);
                    updateMealDetails(mealType, selectedRecipe.title());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error al obtener la nutrición: " + e.getMessage(), e);
            }
        }).start();
    }

    private void adjustUserNutrition(JsonArray nutrients, boolean isAdding) {
        double caloriesChange = 0.0, proteinChange = 0.0, fatChange = 0.0, carbChange = 0.0;

        for (int i = 0; i < nutrients.size(); i++) {
            JsonObject nutrient = nutrients.get(i).getAsJsonObject();
            String name = nutrient.get("name").getAsString();
            double amount = nutrient.get("amount").getAsDouble();

            switch (name.toLowerCase(Locale.ROOT)) {
                case "calories": caloriesChange = amount; break;
                case "protein": proteinChange = amount; break;
                case "fat": fatChange = amount; break;
                case "carbohydrates": carbChange = amount; break;
            }
        }

        if (isAdding) {
            user.addCaloriesToday(caloriesChange);
            user.addProteinToday(proteinChange);
            user.addFatToday(fatChange);
            user.addCarbToday(carbChange);
        } else {
            user.subtractCaloriesToday(caloriesChange);
            user.subtractProteinToday(proteinChange);
            user.subtractFatToday(fatChange);
            user.subtractCarbToday(carbChange);
        }

        runOnUiThread(() -> updateNutritionalValues(user));
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

        runOnUiThread(() -> {
            caloriesTodayTextView.setText(String.format(Locale.getDefault(), "Calorías de hoy: %.2f kcal", user.getCaloriesToday()));
            caloriesNeededTextView.setText(String.format(Locale.getDefault(), "Calorías necesarias: %.2f kcal", caloriesNeeded));
            proteinTextView.setText(String.format(Locale.getDefault(), "Proteínas: %.2f g", user.getProteinToday()));
            fatTextView.setText(String.format(Locale.getDefault(), "Grasas: %.2f g", user.getFatToday()));
            carbTextView.setText(String.format(Locale.getDefault(), "Carbohidratos: %.2f g", user.getCarbToday()));
        });
    }

    private void updateMealDetails(String mealType, String recipeTitle) {
        Map<String, TextView> mealDetailsMap = Map.of(
                "Desayuno", breakfastDetails,
                "Almuerzo", lunchDetails,
                "Cena", dinnerDetails,
                "Snacks", snacksDetails
        );

        TextView mealDetailView = mealDetailsMap.get(mealType);
        if (mealDetailView != null) {
            runOnUiThread(() -> mealDetailView.setText(recipeTitle));
        }
    }
}
