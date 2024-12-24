package com.example.diets.activitys;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.example.diets.R;
import com.example.diets.adapters.ViewPagerAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


public class ViewPagerActivity extends AppCompatActivity implements DashboardFragment.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;

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
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
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
                Log.d("ViewPagerActivity", "Página seleccionada: " + position);
                // Habilitar/deshabilitar drawer según el fragment
                if (position == 1) { // DashboardFragment
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    Log.d("ViewPagerActivity", "Drawer desbloqueado para DashboardFragment");
                } else {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    Log.d("ViewPagerActivity", "Drawer bloqueado para EntrenamientoFragment");
                }
            }
        });

        // Configurar NavigationView
        navigationView.setNavigationItemSelectedListener(item -> {
            Log.d("ViewPagerActivity", "Elemento del NavigationView seleccionado: " + item.getTitle());
            // Manejar clicks en el menú
            return true;
        });

        Log.d("ViewPagerActivity", "onCreate completado");
    }

    @Override
    public void onNavigationItemSelected(int position) {
        viewPager.setCurrentItem(position);
    }
}