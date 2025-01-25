package com.example.diets.fragments;

import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.fragments.MenuPopupFragment;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseDistribution;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.diets.model.TrainingDay;

import java.util.ArrayList;
import java.util.List;


public class SelectExercisesFragment extends Fragment {
    private static final String TAG = "SelectExercisesFragment";
    private List<String> selectedExercises = new ArrayList<>();
    private LinearLayout containerLayout;
    private DatabaseReference mDatabase;
    private List<TrainingDay> trainingDays;
    private String userGoal; // Meta del usuario

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerLayout = view.findViewById(R.id.containerLayout);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            String selectedDistribution = data.getString("selectedDistribution");
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            userGoal = data.getString("userGoal"); // Obtener la meta del usuario
            createExerciseContainers(trainingDays);
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Ejercicios seleccionados: " + selectedExercises);
            // Aquí puedes agregar la lógica para guardar los ejercicios seleccionados
        });
    }

    private void createExerciseContainers(List<TrainingDay> trainingDays) {
        for (int i = 0; i < trainingDays.size(); i++) {
            TrainingDay trainingDay = trainingDays.get(i);
            LinearLayout dayContainer = new LinearLayout(getContext());
            dayContainer.setOrientation(LinearLayout.VERTICAL);
            dayContainer.setPadding(16, 16, 16, 16);
            dayContainer.setBackgroundColor(Color.parseColor("#2F2F2F"));

            // Agregar margen inferior y borde para separar los contenedores
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            dayContainer.setLayoutParams(params);
            dayContainer.setBackgroundResource(R.drawable.container_background);

            // Crear un RelativeLayout para el título y el botón de arrastre
            RelativeLayout titleLayout = new RelativeLayout(getContext());
            titleLayout.setPadding(0, 0, 0, 16);

            // Crear un LinearLayout horizontal para el día y el nombre del patrón
            LinearLayout textLayout = new LinearLayout(getContext());
            textLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView dayTitle = new TextView(getContext());
            dayTitle.setText("Día " + (i + 1));
            dayTitle.setTextColor(Color.WHITE);
            dayTitle.setTextSize(20);
            dayTitle.setTypeface(null, Typeface.BOLD);
            textLayout.addView(dayTitle);

            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                TextView patternTitle = new TextView(getContext());
                patternTitle.setText(" - " + pattern.getName());
                patternTitle.setTextColor(Color.WHITE);
                patternTitle.setTextSize(18);
                patternTitle.setTypeface(null, Typeface.BOLD);
                textLayout.addView(patternTitle);
            }

            RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            textLayout.setLayoutParams(textLayoutParams);
            titleLayout.addView(textLayout);

            // Añadir el ImageView para arrastrar
            ImageView dragHandle = new ImageView(getContext());
            dragHandle.setImageResource(R.drawable.ic_drag_handle); // Asegúrate de tener un icono de arrastre en tus recursos
            dragHandle.setColorFilter(Color.WHITE); // Cambiar el color a blanco
            RelativeLayout.LayoutParams dragHandleParams = new RelativeLayout.LayoutParams(
                    100, // Ancho del botón de arrastre
                    100  // Alto del botón de arrastre
            );
            dragHandleParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            dragHandleParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            dragHandle.setLayoutParams(dragHandleParams);
            dragHandle.setOnLongClickListener(v -> {
                Log.d(TAG, "Drag handle long click - initiating drag");
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(dayContainer);
                dayContainer.startDragAndDrop(data, shadowBuilder, dayContainer, 0);
                return true;
            });
            titleLayout.addView(dragHandle);

            dayContainer.addView(titleLayout);

            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                RecyclerView recyclerView = new RecyclerView(getContext());
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                loadExercises(recyclerView, pattern);
                dayContainer.addView(recyclerView);
            }

            Button addButton = new Button(getContext());
            addButton.setText("Agregar Ejercicios");
            addButton.setTextColor(Color.WHITE);
            addButton.setBackgroundColor(Color.parseColor("#E63946"));
            addButton.setOnClickListener(v -> {
                // Lógica para agregar ejercicios
                showExerciseSelectionDialog(trainingDay, userGoal);
            });
            dayContainer.addView(addButton);

            containerLayout.addView(dayContainer);
        }

        containerLayout.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d(TAG, "Drag started");
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d(TAG, "Drag entered");
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    Log.d(TAG, "Drag location: " + event.getY());
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d(TAG, "Drag exited");
                    return true;
                case DragEvent.ACTION_DROP:
                    Log.d(TAG, "Drop event");
                    View draggedView = (View) event.getLocalState();
                    draggedView.setVisibility(View.VISIBLE);
                    containerLayout.removeView(draggedView);
                    int dropPosition = (int) event.getY();
                    int insertIndex = 0;
                    for (int i = 0; i < containerLayout.getChildCount(); i++) {
                        View child = containerLayout.getChildAt(i);
                        if (dropPosition < child.getBottom()) {
                            insertIndex = i;
                            break;
                        }
                    }
                    Log.d(TAG, "Inserting at index: " + insertIndex);
                    containerLayout.addView(draggedView, insertIndex);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d(TAG, "Drag ended");
                    View view = (View) event.getLocalState();
                    view.setVisibility(View.VISIBLE);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showExerciseSelectionDialog(TrainingDay trainingDay, String userGoal) {
        MenuPopupFragment dialogFragment = new MenuPopupFragment();
        Bundle args = new Bundle();
        args.putSerializable("trainingDay", trainingDay);
        args.putString("userGoal", userGoal); // Pasar la meta del usuario
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "ExerciseSelectionDialog");
    }

    private void loadExercises(RecyclerView recyclerView, MovementPattern pattern) {
        DatabaseReference exercisesRef = mDatabase.child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Exercise> exercises = new ArrayList<>();
                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Exercise exercise = exerciseSnapshot.getValue(Exercise.class);
                    if (exercise != null && exercise.getGoals().contains(userGoal)) {
                        if (exercise.getSynergistMuscles().stream().anyMatch(muscle -> pattern.getSynergistMuscles().contains(muscle))) {
                            exercises.add(exercise);
                        }
                    }
                }
                ExercisesAdapter adapter = new ExercisesAdapter(exercises, selectedExercises);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "Error de base de datos: " + error.getMessage());
            }
        });
    }

    private static class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
        private final List<Exercise> exercises;
        private final List<String> selectedExercises;

        ExercisesAdapter(List<Exercise> exercises, List<String> selectedExercises) {
            this.exercises = exercises;
            this.selectedExercises = selectedExercises;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Exercise exercise = exercises.get(position);
            holder.exerciseName.setText(exercise.getName());
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedExercises.contains(exercise.getName()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedExercises.add(exercise.getName());
                } else {
                    selectedExercises.remove(exercise.getName());
                }
            });
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView exerciseName;
            CheckBox checkBox;

            ViewHolder(View itemView) {
                super(itemView);
                exerciseName = itemView.findViewById(R.id.exerciseName);
                checkBox = itemView.findViewById(R.id.checkBox);
            }
        }
    }
    public void addSelectedExercise(Exercise exercise) {
        selectedExercises.add(exercise.getName());
        // Actualiza la interfaz de usuario para reflejar el nuevo ejercicio seleccionado
        updateSelectedExercisesUI();
    }

    private void updateSelectedExercisesUI() {
        // Implementa la lógica para actualizar la interfaz de usuario
        // Por ejemplo, notificar al adaptador del RecyclerView
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout dayContainer = (LinearLayout) child;
                for (int j = 0; j < dayContainer.getChildCount(); j++) {
                    View dayChild = dayContainer.getChildAt(j);
                    if (dayChild instanceof RecyclerView) {
                        RecyclerView recyclerView = (RecyclerView) dayChild;
                        RecyclerView.Adapter adapter = recyclerView.getAdapter();
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    }
}