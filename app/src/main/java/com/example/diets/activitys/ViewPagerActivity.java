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
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.adapters.ViewPagerAdapter;
import com.example.diets.fragments.AssignLoadFragment;
import com.example.diets.fragments.BaseFragment;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.DashboardWorkoutFragment;
import com.example.diets.fragments.ExerciseDistributionFragment;
import com.example.diets.fragments.MenuPopupFragment;
import com.example.diets.fragments.SelectExercisesFragment;
import com.example.diets.fragments.TrainingDetailFragment;
import com.example.diets.fragments.WeightFragment;
import com.example.diets.model.Exercise;
import com.example.diets.model.UserScope;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ViewPagerActivity extends AppCompatActivity
        implements DashboardFragment.OnNavigationItemSelectedListener,
        MenuPopupFragment.OnExerciseSelectedListener,
        ExercisesAdapter.OnExerciseClickListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private OnBackPressedCallback onBackPressedCallback;

    // ⬅️ NUEVO: banderas de impersonación
    private boolean coachMode = false;
    private String activeUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        // Inicializar vistas
        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // ⬅️ NUEVO: leer extras e iniciar el UserScope
        coachMode = getIntent().getBooleanExtra("coachMode", false);
        String targetUid = getIntent().getStringExtra("targetUid");
        UserScope.start(targetUid, coachMode);

        // ⬅️ NUEVO: usar SIEMPRE el UID activo del scope
        activeUid = UserScope.getActiveUid();
        if (activeUid == null) {
            // Sin sesión válida
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(activeUid);

        // 🔥 Consultar isFirstTime y trainingFormCompleted juntos
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean firstTimeValue = snapshot.child("isFirstTime").getValue(Boolean.class);
                boolean firstTime = firstTimeValue != null && firstTimeValue;

                boolean trainingFormCompleted = snapshot.child("trainingFormCompleted").getValue(Boolean.class) != null
                        && snapshot.child("trainingFormCompleted").getValue(Boolean.class);

                Log.d("ViewPagerActivity",
                        "✅ UID=" + activeUid +
                                " isFirstTime=" + firstTime +
                                " trainingFormCompleted=" + trainingFormCompleted +
                                " coachMode=" + coachMode);

                // ✅ Configurar el ViewPager con valores reales
                adapter = new ViewPagerAdapter(ViewPagerActivity.this, trainingFormCompleted, firstTime);
                viewPager.setAdapter(adapter);

                // **Agregar nombres a las pestañas del TabLayout**
                new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Entrenamiento");
                            break;
                        case 1:
                            tab.setText("Alimentación");
                            break;
                        default:
                            tab.setText("Sección " + (position + 1));
                            break;
                    }
                }).attach();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ViewPagerActivity", "❌ Error al leer datos del usuario", error.toException());
            }
        });

        // ✅ Listener para manejar el botón "Cerrar sesión"
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                Log.d("ViewPagerActivity", "🔓 Cerrar sesión presionado");
                FirebaseAuth.getInstance().signOut();
                UserScope.clear(); // ⬅️ limpiar scope al salir

                Intent intent = new Intent(ViewPagerActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        // Manejo del botón "Back"
        setupOnBackPressedHandler();
    }

    private void setupOnBackPressedHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d("ViewPagerActivity", "🔙 Botón Back del sistema presionado");

                Fragment currentFragment = getCurrentFragment();
                if (currentFragment instanceof BaseFragment) {
                    if (((BaseFragment) currentFragment).handleBackPressed()) {
                        return;
                    }
                }

                if (viewPager.getCurrentItem() > 0) {
                    int previousItem = viewPager.getCurrentItem() - 1;
                    Log.d("ViewPagerActivity", "⬅️ Moviendo a fragmento anterior en ViewPager: " + previousItem);
                    viewPager.setCurrentItem(previousItem, true);
                } else {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    int backStackCount = fragmentManager.getBackStackEntryCount();

                    if (backStackCount > 0) {
                        Log.d("ViewPagerActivity", "↩️ Regresando al fragmento anterior en BackStack");
                        fragmentManager.popBackStack();
                    } else {
                        Log.d("ViewPagerActivity", "⚠️ No hay más fragmentos. Mostrando diálogo de salida.");
                        mostrarDialogoSalida();
                    }
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private Fragment getCurrentFragment() {
        if (adapter != null) {
            return adapter.getFragment(viewPager.getCurrentItem());
        }
        return null;
    }

    private void mostrarDialogoSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de la aplicación")
                .setMessage("¿Estás seguro de que quieres salir?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Log.d("ViewPagerActivity", "✅ Usuario confirmó salida.");
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ViewPagerActivity", "🏠 Aplicación minimizada o enviada a segundo plano.");
    }

    public void switchToTrainingDetailFragment(Bundle data) {
        TrainingDetailFragment fragment = new TrainingDetailFragment();
        fragment.setArguments(data);
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToSelectExercisesFragment(Bundle data) {
        SelectExercisesFragment fragment = new SelectExercisesFragment();
        fragment.setArguments(data);
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToAssignLoadFragment(Bundle data) {
        AssignLoadFragment fragment = new AssignLoadFragment();
        fragment.setArguments(data);
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToDashboardFragment() {
        DashboardFragment dashboardFragment = new DashboardFragment();
        replaceFragmentInViewPager(1, dashboardFragment); // pestaña de alimentación
    }

    public void switchToExerciseDistributionFragment(Bundle data) {
        ExerciseDistributionFragment fragment = new ExerciseDistributionFragment();
        fragment.setArguments(data);
        replaceFragmentInViewPager(0, fragment);
    }

    public void switchToDashboardWorkoutFragment(Bundle data) {
        DashboardWorkoutFragment fragment = new DashboardWorkoutFragment();
        fragment.setArguments(data);
        replaceFragmentInViewPager(0, fragment);
    }

    private void replaceFragmentInViewPager(int position, Fragment fragment) {
        if (adapter != null) {
            adapter.replaceFragment(position, fragment);
            adapter.notifyDataSetChanged();
            viewPager.setCurrentItem(position, true);
        } else {
            Log.e("ViewPagerActivity", "❌ Adaptador es null en replaceFragmentInViewPager");
        }
    }

    @Override
    public void onNavigationItemSelected(int position) {
        viewPager.setCurrentItem(position);
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        ViewPagerAdapter adapter = (ViewPagerAdapter) viewPager.getAdapter();
        if (adapter != null) {
            SelectExercisesFragment fragment = (SelectExercisesFragment) adapter.getFragment(0);
            if (fragment != null) {
                // fragment.addSelectedExercise(exercise);
            }
        }
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        // Implementación del método de la interfaz OnExerciseClickListener
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }

    public void handleActivityBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            mostrarDialogoSalida();
        }
    }

    public void switchToWeightFragment() {
        WeightFragment weightFragment = new WeightFragment();
        replaceFragmentInViewPager(1, weightFragment); // <- va en la pestaña de Alimentación
    }

    @Override
    public void onBackPressed() {
        Log.d("ViewPagerActivity", "🔙 Botón Back presionado");

        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof BaseFragment) {
            if (((BaseFragment) currentFragment).handleBackPressed()) {
                return;
            }
        }

        if (viewPager.getCurrentItem() > 0) {
            int previousItem = viewPager.getCurrentItem() - 1;
            Log.d("ViewPagerActivity", "⬅️ Moviendo a fragmento anterior en ViewPager: " + previousItem);
            viewPager.setCurrentItem(previousItem, true);
        } else {
            Fragment firstFragment = adapter.getFragment(0);
            if (firstFragment instanceof DashboardWorkoutFragment) {
                Log.d("ViewPagerActivity", "🏠 Primer fragmento es DashboardWorkoutFragment. No cerramos la app todavía.");
                return;
            }

            FragmentManager fragmentManager = getSupportFragmentManager();
            int backStackCount = fragmentManager.getBackStackEntryCount();

            if (backStackCount > 0) {
                Log.d("ViewPagerActivity", "↩️ Regresando al fragmento anterior en BackStack");
                fragmentManager.popBackStack();
            } else {
                Log.d("ViewPagerActivity", "⚠️ No hay más fragmentos. Mostrando diálogo de salida.");
                mostrarDialogoSalida();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ⬅️ NUEVO: limpiar scope si veníamos en modo coach
        if (UserScope.isCoachMode()) {
            UserScope.clear();
        }
    }
}