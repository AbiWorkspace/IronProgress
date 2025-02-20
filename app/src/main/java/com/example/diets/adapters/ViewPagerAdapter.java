package com.example.diets.adapters;
import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.DashboardWorkoutFragment;
import com.example.diets.fragments.EntrenamientoFragment;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, boolean trainingFormCompleted) {
        super(fragmentActivity);

        if (trainingFormCompleted) {
            fragmentList.add(new DashboardWorkoutFragment());
        } else {
            Log.d(TAG,"Entrenamiento"+"-Valor:"+trainingFormCompleted);
            fragmentList.add(new EntrenamientoFragment());
        }
        fragmentList.add(new DashboardFragment());
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
}