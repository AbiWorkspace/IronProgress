package com.example.diets.fragments;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {
    /**
     * Este método puede ser sobrescrito en cada fragmento.
     * Si devuelve `true`, significa que el fragmento ya manejó el evento.
     */
    public boolean handleBackPressed() {
        return false; // Por defecto, no maneja el evento.
    }
}
