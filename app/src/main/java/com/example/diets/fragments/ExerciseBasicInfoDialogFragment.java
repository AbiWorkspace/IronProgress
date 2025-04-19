package com.example.diets.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.diets.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ExerciseBasicInfoDialogFragment extends DialogFragment {
    private static final String ARG_EXERCISE_ID = "exerciseId";
    private static final String TAG = "ExerciseBasicInfoDialog";

    public static ExerciseBasicInfoDialogFragment newInstance(String exerciseId) {
        ExerciseBasicInfoDialogFragment fragment = new ExerciseBasicInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE_ID, exerciseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_exercise_basic_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String exerciseId = getArguments() != null ? getArguments().getString(ARG_EXERCISE_ID) : null;

        TextView textInfo = view.findViewById(R.id.textInfo);

        if (exerciseId == null) {
            textInfo.setText("Error: ID del ejercicio no válido.");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("userExercises")
                .child(exerciseId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder info = new StringBuilder();

                String name = snapshot.child("name").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String movementType = snapshot.child("movementType").getValue(String.class);
                String equipmentType = snapshot.child("equipmentType").getValue(String.class);
                String exerciseType = snapshot.child("exerciseType").getValue(String.class);

                info.append("📌 Nombre: ").append(name).append("\n");
                info.append("📄 Descripción: ").append(description).append("\n\n");
                info.append("🔀 Movimiento: ").append(movementType).append("\n");
                info.append("🏋️‍♀️ Equipamiento: ").append(equipmentType).append("\n");
                info.append("⚙️ Tipo: ").append(exerciseType).append("\n");

                textInfo.setText(info.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textInfo.setText("❌ Error al cargar datos.");
                Log.e(TAG, "Firebase error", error.toException());
            }
        });
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
}
