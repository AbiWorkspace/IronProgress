package com.example.diets.activitys;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.example.diets.R;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.adapters.ViewPagerAdapter;
import com.example.diets.fragments.ExerciseDistributionFragment;
import com.example.diets.fragments.MenuPopupFragment;
import com.example.diets.model.Exercise;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.diets.fragments.SelectExercisesFragment;
public class ViewPagerActivity extends AppCompatActivity implements DashboardFragment.OnNavigationItemSelectedListener, MenuPopupFragment.OnExerciseSelectedListener, ExercisesAdapter.OnExerciseClickListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        // Inicializar vistas
        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Configurar ViewPager
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Configurar TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Entrenamiento");
            } else if (position == 1) {
                tab.setText("Alimentación");
            }
        }).attach();

        // Manejar cambios de página
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Habilitar/deshabilitar drawer según el fragment
                if (position == 1) { // DashboardFragment
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                } else {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
            }
        });

        // Configurar NavigationView
        navigationView.setNavigationItemSelectedListener(item -> {
            // Manejar clics en el menú
            return true;
        });
    }

    @Override
    public void onNavigationItemSelected(int position) {
        viewPager.setCurrentItem(position);
    }

    public void switchToExerciseDistributionFragment(Bundle data) {
        // Crea el nuevo fragmento y actualiza el adaptador
        ExerciseDistributionFragment fragment = new ExerciseDistributionFragment();
        fragment.setArguments(data);

        // Reemplazar el fragmento en la lista del adaptador
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter); // Vuelve a configurar el adaptador si es necesario
    }

    public void switchToSelectExercisesFragment(Bundle data) {
        // Crea el nuevo fragmento y actualiza el adaptador
        SelectExercisesFragment fragment = new SelectExercisesFragment();
        fragment.setArguments(data);

        // Reemplazar el fragmento en la lista del adaptador
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter); // Vuelve a configurar el adaptador si es necesario
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        // Encuentra el fragmento de selección de ejercicios y actualiza la lista de ejercicios seleccionados
        ViewPagerAdapter adapter = (ViewPagerAdapter) viewPager.getAdapter();
        if (adapter != null) {
            SelectExercisesFragment fragment = (SelectExercisesFragment) adapter.getFragment(0); // Suponiendo que SelectExercisesFragment está en la posición 0
            if (fragment != null) {
                fragment.addSelectedExercise(exercise);
            }
        }
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        // Implementación del método de la interfaz OnExerciseClickListener
        // Aquí puedes manejar el evento de clic en el ejercicio
    }
}