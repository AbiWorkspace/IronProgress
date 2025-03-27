package com.example.diets.activitys;

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

import com.example.diets.fragments.TrainingDetailFragment;
import com.example.diets.model.Exercise;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import com.example.diets.fragments.SelectExercisesFragment;
import com.google.android.material.tabs.TabLayoutMediator;

public class ViewPagerActivity extends AppCompatActivity
        implements DashboardFragment.OnNavigationItemSelectedListener,
        MenuPopupFragment.OnExerciseSelectedListener,
        ExercisesAdapter.OnExerciseClickListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        // Inicializar vistas
        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        boolean trainingFormCompleted = getIntent().getBooleanExtra("trainingFormCompleted", false);
        boolean firstTime = getIntent().getBooleanExtra("firstTime", false);

        // Configurar el ViewPager con `firstTime`
        adapter = new ViewPagerAdapter(this, trainingFormCompleted, firstTime);
        viewPager.setAdapter(adapter);

        // **Agregar nombres a las pestañas del TabLayout**
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Entrenamiento");  // 🔥 Nombre de la primera pestaña
                    break;
                case 1:
                    tab.setText("Alimentación");  // 🔥 Nombre de la segunda pestaña
                    break;
                default:
                    tab.setText("Sección " + (position + 1));  // Opcional: para más secciones dinámicas
                    break;
            }
        }).attach();

        // Manejo del botón "Back" con OnBackPressedDispatcher
        setupOnBackPressedHandler();
    }


    /**
     * Configura el manejo del botón "Back" para asegurarse de que los fragmentos lo manejen correctamente.
     */
    private void setupOnBackPressedHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d("ViewPagerActivity", "🔙 Botón Back del sistema presionado");

                Fragment currentFragment = getCurrentFragment();
                if (currentFragment instanceof BaseFragment) {
                    if (((BaseFragment) currentFragment).handleBackPressed()) {
                        return; // El fragmento maneja "Back", no hacemos nada más
                    }
                }

                if (viewPager.getCurrentItem() > 0) {
                    // Retrocede al fragmento anterior en ViewPager2
                    int previousItem = viewPager.getCurrentItem() - 1;
                    Log.d("ViewPagerActivity", "⬅️ Moviendo a fragmento anterior en ViewPager: " + previousItem);
                    viewPager.setCurrentItem(previousItem, true);
                } else {
                    // Si estamos en el primer fragmento, verificamos el BackStack
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    int backStackCount = fragmentManager.getBackStackEntryCount();

                    if (backStackCount > 0) {
                        Log.d("ViewPagerActivity", "↩️ Regresando al fragmento anterior en BackStack");
                        fragmentManager.popBackStack();
                    } else {
                        // Si no hay más fragmentos en la pila, mostrar diálogo antes de salir
                        Log.d("ViewPagerActivity", "⚠️ No hay más fragmentos. Mostrando diálogo de salida.");
                        mostrarDialogoSalida();
                    }
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    /**
     * Obtiene el fragmento actualmente visible en ViewPager2.
     */
    private Fragment getCurrentFragment() {
        if (adapter != null) {
            return adapter.getFragment(viewPager.getCurrentItem());
        }
        return null;
    }


    /**
     * Muestra un diálogo de confirmación antes de cerrar la aplicación.
     */
    private void mostrarDialogoSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de la aplicación")
                .setMessage("¿Estás seguro de que quieres salir?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Log.d("ViewPagerActivity", "✅ Usuario confirmó salida.");
                    finish(); // Cierra la actividad correctamente
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

    /**
     * Método reutilizable para reemplazar un fragmento en la lista del ViewPagerAdapter.
     */
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
    /**
     * Permite que los fragmentos accedan al ViewPager2
     */
    public ViewPager2 getViewPager() {
        return viewPager;
    }

    /**
     * Maneja el evento "Back" cuando el usuario está en el primer fragmento
     */
    public void handleActivityBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            mostrarDialogoSalida();
        }
    }
    @Override
    public void onBackPressed() {
        Log.d("ViewPagerActivity", "🔙 Botón Back presionado");

        // Verificar el fragmento actual
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof BaseFragment) {
            if (((BaseFragment) currentFragment).handleBackPressed()) {
                return; // Si el fragmento maneja "Back", no hacemos nada más
            }
        }

        // Verificar si hay más fragmentos en ViewPager2 antes de cerrar la app
        if (viewPager.getCurrentItem() > 0) {
            int previousItem = viewPager.getCurrentItem() - 1;
            Log.d("ViewPagerActivity", "⬅️ Moviendo a fragmento anterior en ViewPager: " + previousItem);
            viewPager.setCurrentItem(previousItem, true);
        } else {
            // Verificar si el primer fragmento realmente es `DashboardWorkoutFragment`
            Fragment firstFragment = adapter.getFragment(0);
            if (firstFragment instanceof DashboardWorkoutFragment) {
                Log.d("ViewPagerActivity", "🏠 Primer fragmento es DashboardWorkoutFragment. No cerramos la app todavía.");
                return; // Evita cerrar la app si el primer fragmento es DashboardWorkoutFragment
            }

            // Si no hay más fragmentos en la pila, mostrar diálogo antes de salir
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

}
