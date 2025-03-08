package com.example.diets.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.adapters.TrainingDetailAdapter;
import com.example.diets.model.UserExercise;

import java.util.ArrayList;
import java.util.List;
public class TrainingDetailFragment extends Fragment {
    private RecyclerView recyclerView;
    private TrainingDetailAdapter adapter;
    private List<UserExercise> exercises;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training_detail, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewExercises);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Obtener los ejercicios del bundle
        if (getArguments() != null) {
            exercises = (List<UserExercise>) getArguments().getSerializable("exercises");
        }

        // Asegurar que la lista de ejercicios no sea nula
        if (exercises == null) {
            exercises = new ArrayList<>();
        }

        adapter = new TrainingDetailAdapter(exercises);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
