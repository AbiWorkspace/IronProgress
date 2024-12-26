package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.diets.R;

import com.example.diets.activitys.MainActivity;

import com.example.diets.activitys.WeightActivity;
import com.example.diets.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Locale;
import java.util.Map;

public class FoodActivity extends AppCompatActivity {
    private static final String TAG = "FoodActivity";

    // UI Elements
    private TextView caloriesNeededTextView, caloriesTodayTextView, proteinTextView, fatTextView, carbTextView;
    private TextView breakfastDetails, lunchDetails, dinnerDetails, snacksDetails;
    private EditText foodTextField;
    private User user;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);

        // Inicializa FirebaseAuth y UI
        mAuth = FirebaseAuth.getInstance();
        initializeUI();

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



        // Configura el menú inferior
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            Log.d("BottomNav", "BottomNavigationView encontrado");
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                Log.d("BottomNav", "Item seleccionado: " + item.getItemId());
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    Log.d("BottomNav", "Navegando a FoodActivity");
                    startActivity(new Intent(FoodActivity.this, FoodActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_dashboard) {
                    Log.d("BottomNav", "Navegando a WeightActivity");
                    startActivity(new Intent(FoodActivity.this, WeightActivity.class));
                    return true;
                }
                return false;
            });
        } else {
            Log.e("BottomNav", "BottomNavigationView no encontrado");
        }

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
        Toast.makeText(FoodActivity.this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show();

        // Redirige a la pantalla de inicio de sesión
        Intent intent = new Intent(FoodActivity.this, MainActivity.class);
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