package com.example.diets.adapters;
import static android.content.ContentValues.TAG;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;


import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.DashboardWorkoutFragment;
import com.example.diets.fragments.DashboardFragment;  // ✅ Asegurar que esta clase existe
import com.example.diets.fragments.UserFormFragment;
import com.example.diets.fragments.EntrenamientoFragment;
import com.example.diets.fragments.UserFormFragment;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, boolean trainingFormCompleted, boolean firstTime) {
        super(fragmentActivity);

        Log.d("ViewPagerAdapter", "📌 Constructor llamado");
        Log.d("ViewPagerAdapter", "➡️ trainingFormCompleted: " + trainingFormCompleted);
        Log.d("ViewPagerAdapter", "➡️ firstTime: " + firstTime);

        // 🔥 Manejo del formulario de entrenamiento
        if (trainingFormCompleted) {
            Log.d("ViewPagerAdapter", "✅ Formulario de entrenamiento completado. Agregando DashboardWorkoutFragment.");
            fragmentList.add(new DashboardWorkoutFragment());  // ✅ Muestra entrenamientos
        } else {
            Log.d("ViewPagerAdapter", "❌ Formulario de entrenamiento incompleto. Agregando EntrenamientoFragment.");
            fragmentList.add(new EntrenamientoFragment());  // ✅ Muestra formulario de entrenamiento
        }

        // 🔥 Manejo del `firstTime`
        if (firstTime) {
            Log.d("ViewPagerAdapter", "✅ Es la primera vez. Agregando UserFormFragment.");
            fragmentList.add(new UserFormFragment());  // ✅ Muestra formulario de usuario
        } else {
            Log.d("ViewPagerAdapter", "❌ No es la primera vez. Agregando DashboardFragment.");
            fragmentList.add(new DashboardFragment());  // ✅ Muestra el Dashboard normal
        }

        Log.d("ViewPagerAdapter", "📌 Fragmentos agregados: " + fragmentList.size());
    }


    /**
     * Reemplaza un fragmento en una posición específica.
     * Nota: Para que los cambios surtan efecto, asegúrate de recrear el adaptador o
     * configurar nuevamente el ViewPager2 con el adaptador modificado.
     */
    public void replaceFragment(int position, Fragment fragment) {
        if (position >= 0 && position < fragmentList.size()) {
            fragmentList.set(position, fragment);
        }
    }

    /**
     * Agrega un nuevo fragmento al adaptador.
     */
    public void addFragment(Fragment fragment) {
        fragmentList.add(fragment);
    }

    /**
     * Elimina un fragmento en una posición específica.
     */
    public void removeFragment(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            fragmentList.remove(position);
        }
    }

    /**
     * Obtiene un fragmento en una posición específica.
     */
    public Fragment getFragment(int position) {
        return fragmentList.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }
    @Override
    public long getItemId(int position) {
        return fragmentList.get(position).hashCode(); // Genera un ID único para cada fragmento
    }

    @Override
    public boolean containsItem(long itemId) {
        for (Fragment fragment : fragmentList) {
            if (fragment.hashCode() == itemId) {
                return true;
            }
        }
        return false;
    }

}