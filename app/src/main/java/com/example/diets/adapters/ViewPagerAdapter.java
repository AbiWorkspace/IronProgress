package com.example.diets.adapters;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.DashboardWorkoutFragment;
import com.example.diets.fragments.EntrenamientoFragment;
import com.example.diets.fragments.UserFormFragment;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();

    // Flags recibidos desde la Activity
    private final boolean coachMode;
    private final String targetUid;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                            boolean trainingFormCompleted,
                            boolean firstTime,
                            boolean coachMode,
                            @NonNull String targetUid) {
        super(fragmentActivity);

        this.coachMode = coachMode;
        this.targetUid = targetUid;

        Log.d("ViewPagerAdapter - ctor", "trainingFormCompleted=" + trainingFormCompleted
                + " | firstTime=" + firstTime
                + " | coachMode=" + coachMode
                + " | targetUid=" + targetUid);

        // Página 0: Entrenamiento (dashboard de entrenamiento o formulario)
        if (trainingFormCompleted) {
            Log.d("ViewPagerAdapter - ctor", "Agregando DashboardWorkoutFragment (entrenamiento listo)");
            fragmentList.add(withArgs(new DashboardWorkoutFragment()));
        } else {
            Log.d("ViewPagerAdapter - ctor", "Agregando EntrenamientoFragment (formulario entrenamiento)");
            fragmentList.add(withArgs(new EntrenamientoFragment()));
        }

        // Página 1: Alimentación / Dashboard (o primer uso)
        if (firstTime) {
            Log.d("ViewPagerAdapter - ctor", "Agregando UserFormFragment (primera vez)");
            fragmentList.add(withArgs(new UserFormFragment()));
        } else {
            Log.d("ViewPagerAdapter - ctor", "Agregando DashboardFragment");
            fragmentList.add(withArgs(new DashboardFragment()));
        }

        Log.d("ViewPagerAdapter - ctor", "Total fragments=" + fragmentList.size());
    }

    /** Inyecta coachMode y targetUid como argumentos en el fragment. */
    private Fragment withArgs(Fragment fragment) {
        Bundle args = fragment.getArguments() != null
                ? new Bundle(fragment.getArguments())
                : new Bundle();
        args.putBoolean("coachMode", coachMode);
        args.putString("targetUid", targetUid);
        fragment.setArguments(args);
        return fragment;
    }

    /** Reemplaza un fragmento en una posición y notifica el cambio. */
    public void replaceFragment(int position, Fragment fragment) {
        if (position >= 0 && position < fragmentList.size()) {
            fragmentList.set(position, withArgs(fragment)); // asegurar args
            notifyDataSetChanged();
        } else {
            Log.e("ViewPagerAdapter - replaceFragment", "Posición fuera de rango: " + position);
        }
    }

    /** Agrega un fragmento con inyección de argumentos. */
    public void addFragment(Fragment fragment) {
        fragmentList.add(withArgs(fragment));
        notifyDataSetChanged();
    }

    /** Elimina un fragmento en una posición. */
    public void removeFragment(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            fragmentList.remove(position);
            notifyDataSetChanged();
        } else {
            Log.e("ViewPagerAdapter - removeFragment", "Posición fuera de rango: " + position);
        }
    }

    /** Obtiene el fragment en una posición. */
    public Fragment getFragment(int position) {
        return fragmentList.get(position);
    }

    /** Título para TabLayoutMediator. */
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: return "Entrenamiento";
            case 1: return "Alimentación";
            default: return "Página " + position;
        }
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

    // IDs estables basados en hash del fragmento
    @Override
    public long getItemId(int position) {
        return fragmentList.get(position).hashCode();
    }

    @Override
    public boolean containsItem(long itemId) {
        for (Fragment f : fragmentList) {
            if (f.hashCode() == itemId) return true;
        }
        return false;
    }
}
