package com.example.diets.activitys;
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

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                caloriesTodayTextView.setText(String.format(Locale.getDefault(), "Calorías de hoy: %.2f kcal", user.getCaloriesToday()));
                caloriesNeededTextView.setText(String.format(Locale.getDefault(), "Calorías necesarias: %.2f kcal", caloriesNeeded));
                proteinTextView.setText(String.format(Locale.getDefault(), "Proteínas: %.2f g", user.getProteinToday()));
                fatTextView.setText(String.format(Locale.getDefault(), "Grasas: %.2f g", user.getFatToday()));
                carbTextView.setText(String.format(Locale.getDefault(), "Carbohidratos: %.2f g", user.getCarbToday()));
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
        } else {
            Toast.makeText(getActivity(), "No puedes añadir más de 10 comidas", Toast.LENGTH_SHORT).show();
        }
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


}