package com.example.diets.adapters;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.diets.fragments.DashboardFragment;
import com.example.diets.fragments.EntrenamientoFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "ViewPagerAdapter";

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        Log.d(TAG, "ViewPagerAdapter: Constructor called");
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "createFragment: Position " + position);
        if (position == 0) {
            Log.d(TAG, "createFragment: Creating EntrenamientoFragment");
            return new EntrenamientoFragment();
        } else if (position == 1) {
            Log.d(TAG, "createFragment: Creating DashboardFragment");
            return new DashboardFragment();
        } else {
            Log.d(TAG, "createFragment: Creating default EntrenamientoFragment");
            return new EntrenamientoFragment();
        }
    }

    @Override
    public int getItemCount() {
        int itemCount = 2;
        Log.d(TAG, "getItemCount: " + itemCount);
        return itemCount;
    }
}