package com.example.diets.activitys;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.example.diets.R;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.adapters.ViewPagerAdapter;
import com.example.diets.fragments.AssignLoadFragment;
import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.DashboardWorkoutFragment;
import com.example.diets.fragments.ExerciseDetailsLoadFragment;
import com.example.diets.fragments.ExerciseDistributionFragment;
import com.example.diets.fragments.MenuPopupFragment;
import com.example.diets.fragments.TrainingDayDetailFragment2;
import com.example.diets.fragments.TrainingDetailFragment;
import com.example.diets.model.Exercise;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.User;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.diets.fragments.SelectExercisesFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        // Cargar el valor de trainingFormCompleted del usuario
        loadTrainingFormCompleted();
    }

    private void loadTrainingFormCompleted() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        boolean trainingFormCompleted = user.isTrainingFormCompleted();
                        setupViewPager(trainingFormCompleted);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ViewPagerActivity", "Error al cargar el estado de trainingFormCompleted", error.toException());
                }
            });
        }
    }

    private void setupViewPager(boolean trainingFormCompleted) {
        // Configurar ViewPager
        adapter = new ViewPagerAdapter(this, trainingFormCompleted);
        viewPager.setAdapter(adapter);

        // Configurar TabLayout
        TabLayout tabLayout = findViewById(R.id.tabLayout);
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
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            // Manejar clics en el menú
            return true;
        });
    }

    // Método para cambiar al fragmento de detalles de entrenamiento
    public void switchToTrainingDetailFragment(Bundle data) {
        TrainingDetailFragment fragment = new TrainingDetailFragment();
        fragment.setArguments(data);
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, true); // Cambiar a la posición del nuevo fragmento
    }


    @Override
    public void onNavigationItemSelected(int position) {
        viewPager.setCurrentItem(position);
    }

    public void switchToSelectExercisesFragment(Bundle data) {
        // Crea el nuevo fragmento y actualiza el adaptador
        SelectExercisesFragment fragment = new SelectExercisesFragment();
        fragment.setArguments(data);

        // Reemplazar el fragmento en la lista del adaptador
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter); // Vuelve a configurar el adaptador si es necesario
    }

    public void switchToExerciseDetailsLoadFragment(Bundle data) {
        ExerciseDetailsLoadFragment fragment = new ExerciseDetailsLoadFragment();
        fragment.setArguments(data);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
    public void switchToAssignLoadFragment(Bundle data) {
        // Crear el nuevo fragmento y asignar los datos
        AssignLoadFragment fragment = new AssignLoadFragment();
        fragment.setArguments(data);

        // 🔥 Asegurarse de que el adaptador no sea null
        if (adapter != null) {
            adapter.replaceFragment(0, fragment); // Reemplaza el fragmento en la posición correcta
            adapter.notifyDataSetChanged(); // 🔥 Notifica que hay cambios en el adaptador
        } else {
            Log.e("ViewPagerActivity", "❌ Adaptador es null en switchToAssignLoadFragment");
        }

        // 🔥 Mover el `ViewPager2` a la posición del fragmento de carga de entrenamiento
        viewPager.setCurrentItem(0, true);
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        // Encuentra el fragmento de selección de ejercicios y actualiza la lista de ejercicios seleccionados
        ViewPagerAdapter adapter = (ViewPagerAdapter) viewPager.getAdapter();
        if (adapter != null) {
            SelectExercisesFragment fragment = (SelectExercisesFragment) adapter.getFragment(0); // Suponiendo que SelectExercisesFragment está en la posición 0
            if (fragment != null) {
                // fragment.addSelectedExercise(exercise);
            }
        }
    }

    public void switchToExerciseDistributionFragment(Bundle data) {
        // Obtener el userId del usuario autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            data.putString("userId", userId); // Pasar el userId al Bundle
        }

        // Crea el nuevo fragmento y actualiza el adaptador
        ExerciseDistributionFragment fragment = new ExerciseDistributionFragment();
        fragment.setArguments(data);

        // Reemplazar el fragmento en la lista del adaptador
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter); // Vuelve a configurar el adaptador si es necesario
    }



    public void switchToDashboardWorkoutFragment(Bundle data) {
        DashboardWorkoutFragment fragment = new DashboardWorkoutFragment();
        fragment.setArguments(data);

        // Reemplazar el fragmento en la lista del adaptador
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter); // Vuelve a configurar el adaptador si es necesario
    }

    public void switchToTrainingDayDetailFragment(TrainingDay trainingDay) {
        TrainingDayDetailFragment2 fragment = TrainingDayDetailFragment2.newInstance(trainingDay);
        adapter.replaceFragment(0, fragment);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, true); // Mantiene el ViewPager en la posición de entrenamiento
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        // Implementación del método de la interfaz OnExerciseClickListener
        // Aquí puedes manejar el evento de clic en el ejercicio
    }
}