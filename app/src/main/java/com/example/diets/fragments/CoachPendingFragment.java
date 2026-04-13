package com.example.diets.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.activitys.MainActivity;
import com.example.diets.activitys.UsersListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CoachPendingFragment extends Fragment {

    private DatabaseReference roleRef;
    private ValueEventListener roleListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coach_pending, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Si no hay usuario, vuelve al login
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            Intent i = new Intent(requireContext(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            requireActivity().finish();
            return;
        }

        String uid = fbUser.getUid();
        roleRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("role");

        // Listener que "revisa varias veces" el cambio de role
        roleListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                if ("coach".equalsIgnoreCase(role)) {
                    // Aprobado -> ir a la pantalla de coach (listado de usuarios)
                    Intent i = new Intent(requireContext(), UsersListActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                }
                // Si no es coach, se queda el texto y el círculo de carga 👌
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                // Nada: seguimos mostrando el spinner y el texto.
            }
        };
        roleRef.addValueEventListener(roleListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (roleRef != null && roleListener != null) {
            roleRef.removeEventListener(roleListener);
        }
    }
}