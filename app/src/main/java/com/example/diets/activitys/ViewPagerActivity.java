package com.example.diets.activitys;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.diets.R;
import com.example.diets.adapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ViewPagerActivity extends AppCompatActivity {
    private static final String TAG = "ViewPagerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        // Encuentra el ViewPager y el TabLayout por su ID
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // Verifica si los elementos fueron encontrados
        if (viewPager == null) {
            Log.d(TAG, "ViewPager no encontrado");
        } else {
            Log.d(TAG, "ViewPager encontrado");
        }

        if (tabLayout == null) {
            Log.d(TAG, "TabLayout no encontrado");
        } else {
            Log.d(TAG, "TabLayout encontrado");
        }

        // Configura el adaptador para el ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        Log.d(TAG, "Adapter configurado");

        // Configura el TabLayoutMediator para enlazar el TabLayout con el ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            Log.d(TAG, "Configurando tab en posición: " + position);
            if (position == 0) {
                tab.setText("Entrenamiento");
                Log.d(TAG, "Tab 'Entrenamiento' configurado");
            } else if (position == 1) {
                tab.setText("Alimentación");
                Log.d(TAG, "Tab 'Alimentación' configurado");
            } else {
                tab.setText("Tab " + (position + 1));
                Log.d(TAG, "Tab " + (position + 1) + " configurado");
            }
        }).attach();
        Log.d(TAG, "TabLayoutMediator adjuntado");
    }
}