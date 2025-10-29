package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.diets.R;
import com.example.diets.adapters.ViewPagerAdapter;
import com.example.diets.fragments.AssignLoadFragment;
import com.example.diets.fragments.BaseFragment;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.DashboardWorkoutFragment;
import com.example.diets.fragments.ExerciseDistributionFragment;
import com.example.diets.fragments.SelectExercisesFragment;
import com.example.diets.fragments.TrainingDetailFragment;
import com.example.diets.fragments.WeightFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ViewPagerActivity extends AppCompatActivity
        implements DashboardFragment.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private TabLayout tabLayout;
    private OnBackPressedCallback onBackPressedCallback;

    // Flags de impersonación y UID efectivo
    private boolean coachMode = false;
    private String activeUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        // Inicializar vistas
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Leer extras y determinar el UID efectivo
        coachMode = getIntent().getBooleanExtra("coachMode", false);
        String targetUidFromIntent = getIntent().getStringExtra("targetUid");

        if (!coachMode || targetUidFromIntent == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e("ViewPagerActivity - onCreate", "No hay usuario logueado ni targetUid válido.");
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
            activeUid = currentUser.getUid();
            coachMode = false;
        } else {
            activeUid = targetUidFromIntent;
        }
        Log.i("ViewPagerActivity - onCreate",
                (coachMode ? "MODO COACH" : "MODO USUARIO") + " | UID activo=" + activeUid);

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(activeUid);

        // Consultar flags iniciales y crear el adapter pasando coachMode/activeUid
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean firstTimeValue = snapshot.child("isFirstTime").getValue(Boolean.class);
                boolean firstTime = firstTimeValue != null && firstTimeValue;

                Boolean tfcValue = snapshot.child("trainingFormCompleted").getValue(Boolean.class);
                boolean trainingFormCompleted = tfcValue != null && tfcValue;

                Log.d("ViewPagerActivity - onDataChange",
                        "UID=" + activeUid +
                                " | isFirstTime=" + firstTime +
                                " | trainingFormCompleted=" + trainingFormCompleted +
                                " | coachMode=" + coachMode);

                // PASAR coachMode y activeUid al constructor del adapter
                adapter = new ViewPagerAdapter(
                        ViewPagerActivity.this,
                        trainingFormCompleted,
                        firstTime,
                        coachMode,
                        activeUid
                );
                viewPager.setAdapter(adapter);

                // TabLayoutMediator obtiene títulos desde el adapter
                new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                        tab.setText(adapter.getPageTitle(position))
                ).attach();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ViewPagerActivity - onCancelled", "Error al leer datos del usuario", error.toException());
            }
        });

        // Logout
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                Log.d("ViewPagerActivity - logout", "Cerrar sesión");
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ViewPagerActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        // Back
        setupOnBackPressedHandler();
    }

    private void setupOnBackPressedHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Log.d("ViewPagerActivity - handleOnBackPressed", "Back presionado");

                Fragment currentFragment = getCurrentFragment();
                if (currentFragment instanceof BaseFragment &&
                        ((BaseFragment) currentFragment).handleBackPressed()) {
                    Log.d("ViewPagerActivity - handleOnBackPressed",
                            "Back manejado por " + currentFragment.getClass().getSimpleName());
                    return;
                }

                if (viewPager.getCurrentItem() > 0) {
                    int previousItem = viewPager.getCurrentItem() - 1;
                    Log.d("ViewPagerActivity - handleOnBackPressed",
                            "Moviendo a página previa: " + previousItem);
                    viewPager.setCurrentItem(previousItem, true);
                } else {
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        Log.d("ViewPagerActivity - handleOnBackPressed", "Pop de back stack");
                        fm.popBackStack();
                    } else {
                        Log.d("ViewPagerActivity - handleOnBackPressed", "Mostrando diálogo de salida");
                        mostrarDialogoSalida();
                    }
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private Fragment getCurrentFragment() {
        if (adapter != null && viewPager != null) {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem >= 0 && currentItem < adapter.getItemCount()) {
                return adapter.getFragment(currentItem);
            } else {
                Log.e("ViewPagerActivity - getCurrentFragment",
                        "Índice fuera de rango: " + currentItem);
            }
        } else {
            Log.e("ViewPagerActivity - getCurrentFragment",
                    "Adapter o ViewPager es null");
        }
        return null;
    }

    private void mostrarDialogoSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de la aplicación")
                .setMessage("¿Estás seguro de que quieres salir?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Log.d("ViewPagerActivity - mostrarDialogoSalida", "Usuario confirmó salida");
                    finishAffinity();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override protected void onStop() {
        super.onStop();
        Log.d("ViewPagerActivity - onStop", "App en segundo plano");
    }

    // Helper: adjunta coachMode y targetUid a cualquier fragment
    private Bundle prepareFragmentArgs(Bundle data) {
        if (data == null) data = new Bundle();
        data.putBoolean("coachMode", coachMode);
        data.putString("targetUid", activeUid); // clave consistente
        return data;
    }

    // ===========================
    // Navegaciones (con y sin Bundle), siempre pasando args
    // ===========================

    public void switchToTrainingDetailFragment(Bundle data) {
        TrainingDetailFragment fragment = new TrainingDetailFragment();
        fragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToSelectExercisesFragment(Bundle data) {
        SelectExercisesFragment fragment = new SelectExercisesFragment();
        fragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToAssignLoadFragment(Bundle data) {
        AssignLoadFragment fragment = new AssignLoadFragment();
        fragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(0, fragment);
    }

    // --- Dashboard (sin y con Bundle) ---
    public void switchToDashboardFragment() {
        DashboardFragment fragment = new DashboardFragment();
        fragment.setArguments(prepareFragmentArgs(null));
        replaceFragmentInViewPager(1, fragment);
    }

    public void switchToDashboardFragment(Bundle data) {
        DashboardFragment fragment = new DashboardFragment();
        fragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(1, fragment);
    }

    // --- DashboardWorkout (sin y con Bundle) ---
    public void switchToDashboardWorkoutFragment() {
        DashboardWorkoutFragment fragment = new DashboardWorkoutFragment();
        fragment.setArguments(prepareFragmentArgs(null));
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToDashboardWorkoutFragment(Bundle data) {
        DashboardWorkoutFragment fragment = new DashboardWorkoutFragment();
        fragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToExerciseDistributionFragment(Bundle data) {
        ExerciseDistributionFragment fragment = new ExerciseDistributionFragment();
        fragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(0, fragment);
    }

    // --- Weight (sin y con Bundle) ---
    public void switchToWeightFragment() {
        WeightFragment weightFragment = new WeightFragment();
        weightFragment.setArguments(prepareFragmentArgs(null));
        replaceFragmentInViewPager(1, weightFragment);
    }

    public void switchToWeightFragment(Bundle data) {
        WeightFragment weightFragment = new WeightFragment();
        weightFragment.setArguments(prepareFragmentArgs(data));
        replaceFragmentInViewPager(1, weightFragment);
    }

    // Reemplazo en ViewPager asegurando args (coachMode/activeUid)
    private void replaceFragmentInViewPager(int position, Fragment fragment) {
        if (adapter != null) {
            if (fragment.getArguments() == null) {
                fragment.setArguments(prepareFragmentArgs(null));
            } else {
                fragment.getArguments().putBoolean("coachMode", coachMode);
                fragment.getArguments().putString("targetUid", activeUid);
            }
            adapter.replaceFragment(position, fragment);
            viewPager.setCurrentItem(position, false);
        } else {
            Log.e("ViewPagerActivity - replaceFragmentInViewPager", "Adapter es null");
        }
    }

    @Override
    public void onNavigationItemSelected(int position) {
        if (viewPager != null) viewPager.setCurrentItem(position);
    }
}
