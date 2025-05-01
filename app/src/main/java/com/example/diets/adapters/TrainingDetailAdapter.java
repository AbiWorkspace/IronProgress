package com.example.diets.adapters;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.fragments.ExerciseDetailsDialogFragment;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.UserExercise;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class TrainingDetailAdapter extends RecyclerView.Adapter<TrainingDetailAdapter.ExerciseViewHolder> {
    private static final String TAG = "TrainingDetailAdapter";
    private List<UserExercise> exercises;
    private boolean hasUnsavedChanges = false;
    private RecyclerView recyclerView;
    private final List<ExerciseViewHolder> viewHolders = new ArrayList<>();
    private final Map<String, Integer> exerciseSeriesCountMap = new HashMap<>();
    private boolean seriesSaved = false;
    private String dayKey;
    private final Map<String, String> firebaseKeysPorNombre = new HashMap<>();

    private final List<UserExercise> pendingDeletionExercises = new ArrayList<>();
    private final Context context;
    public TrainingDetailAdapter(List<UserExercise> exercises, RecyclerView recyclerView, String dayKey, Context context) {
        Log.d(TAG, "TrainingDetailAdapter - Constructor: Inicializando adaptador con lista de ejercicios.");
        this.exercises = exercises;
        this.recyclerView = recyclerView;
        this.dayKey = dayKey;
        this.context = context; // ✅ esto es lo que estaba faltando
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contenedor, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        UserExercise exercise = exercises.get(position);
        holder.bind(exercise);

        holder.itemView.setOnClickListener(v -> hasUnsavedChanges = true);
        viewHolders.add(holder);

        String exerciseName = exercise.getName();
        String firebaseKey = firebaseKeysPorNombre.get(exerciseName);

        if (firebaseKey != null && holder.seriesAdapter != null) {
            Log.d(TAG, "onBindViewHolder: Cargando sugerencias para " + exerciseName + " (key=" + firebaseKey + ")");
            holder.seriesAdapter.cargarUltimaSerieDesdeFirebase(firebaseKey, context);
        } else {
            Log.w(TAG, "onBindViewHolder: No se encontró adapter o clave Firebase para " + exerciseName);
        }
    }

    public void setFirebaseKey(String nombre, String key) {
        firebaseKeysPorNombre.put(nombre, key);
    }
    @Override
    public int getItemCount() {
        return exercises.size();
    }


    public List<ExerciseSeries> getSeriesListForExercise(String exerciseName) {
        for (ExerciseViewHolder holder : viewHolders) {
            if (holder.exerciseName.getText().toString().equals(exerciseName)) {
                return holder.seriesAdapter.getSeriesList();
            }
        }
        return null;
    }



    public List<UserExercise> getExercises() {
        return exercises;
    }

    public List<UserExercise> getPendingDeletionExercises() {
        return pendingDeletionExercises;
    }

    public Map<String, List<ExerciseSeries>> getAllCompletedSeriesGroupedByExercise() {
        Map<String, List<ExerciseSeries>> seriesMap = new HashMap<>();
        for (ExerciseViewHolder holder : viewHolders) {
            String name = holder.exerciseName.getText().toString();
            List<ExerciseSeries> completadas = new ArrayList<>();
            for (ExerciseSeries series : holder.seriesAdapter.getSeriesList()) {
                if (series.isCompleted()) {
                    completadas.add(series);
                }
            }
            seriesMap.put(name, completadas);
        }
        return seriesMap;
    }


    public class ExerciseViewHolder extends RecyclerView.ViewHolder {
        public TextView exerciseName;
        public RecyclerView recyclerViewSeries;
        public Button btnAddSeries;
        public SeriesAdapter seriesAdapter;
        public List<ExerciseSeries> seriesList;
        public ImageView iconAction;
        public ImageView exerciseImage;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exerciseName);
            recyclerViewSeries = itemView.findViewById(R.id.recyclerViewSeries);
            btnAddSeries = itemView.findViewById(R.id.btnAddSeries);
            iconAction = itemView.findViewById(R.id.iconAction);
            exerciseImage = itemView.findViewById(R.id.exerciseImage);
            this.iconAction = itemView.findViewById(R.id.iconAction);
            this.exerciseImage = itemView.findViewById(R.id.exerciseImage);
        }

        /** =================================================================================
         *  Método bind(...)  —  muestra la PRIMERA imagen disponible del ejercicio
         *  (propia o de /exerciseImages) y configura el resto de la tarjeta
         *  ================================================================================= */
        public void bind(UserExercise exercise) {
            /* ------- Nombre del ejercicio ------- */
            exerciseName.setText(exercise.getName());
            Log.d(TAG, "Tamaño de getSeries: " +exercise.getSeries()) ;
            /* ------- Construcción de la lista de series ------- */
            seriesList = new ArrayList<>();
            for (int i = 0; i < exercise.getSeries(); i++) {
                int seriesNumber = i + 1;
                ExerciseSeries serie = new ExerciseSeries(seriesNumber,
                        exercise.getReps(), exercise.getLastLoad(), exercise.getRpe());
                serie.setPreviousLoad(exercise.getLastLoad());
                seriesList.add(serie);
            }

            /* ------- Imagen del ejercicio ------- */
            if (exercise.isFromInternal()) {
                // 🔒 Almacenamiento interno → tomar la primera imagen disponible
                Log.d(TAG, "🖼️ Imagen interna - ejercicio: " + exercise.getName());
                if (exercise.getImages() != null && !exercise.getImages().isEmpty()) {
                    String base64Primera = exercise.getImages().get(0);
                    boolean cargada = mostrarBase64EnImageView(base64Primera, exerciseImage);
                    if (!cargada) exerciseImage.setImageResource(R.drawable.ic_placeholder);
                } else {
                    exerciseImage.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                // 🔄 Firebase: buscar por nombre en exerciseImages
                Log.d(TAG, "🌐 Buscando imagen para ejercicio Firebase: " + exercise.getName());
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("exerciseImages");

                ref.orderByChild("name").equalTo(exercise.getName())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    String nombre = snap.child("name").getValue(String.class);
                                    if (nombre == null || !nombre.equalsIgnoreCase(exercise.getName())) continue;

                                    DataSnapshot imagesNode = snap.child("images");
                                    String firstImg = null;

                                    if (imagesNode.child("image1").exists()) {
                                        firstImg = imagesNode.child("image1").getValue(String.class);
                                    } else if (imagesNode.getChildrenCount() > 0) {
                                        for (DataSnapshot child : imagesNode.getChildren()) {
                                            firstImg = child.getValue(String.class);
                                            if (firstImg != null && !firstImg.trim().isEmpty()) break;
                                        }
                                    }

                                    if (firstImg != null && !firstImg.trim().isEmpty()) {
                                        boolean cargada = mostrarBase64EnImageView(firstImg, exerciseImage);
                                        if (cargada)
                                            Log.d(TAG, "✅ Imagen cargada: " + exercise.getName());
                                        else
                                            exerciseImage.setImageResource(R.drawable.ic_placeholder);
                                    } else {
                                        Log.w(TAG, "⚠️ Imagen no válida para: " + exercise.getName());
                                        exerciseImage.setImageResource(R.drawable.ic_placeholder);
                                    }
                                    break;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "❌ Error Firebase al cargar imagen", error.toException());
                                exerciseImage.setImageResource(R.drawable.ic_placeholder);
                            }
                        });
            }

            /* ------- Configurar RecyclerView de series ------- */
            seriesAdapter = new SeriesAdapter(seriesList, exercise.getName(), TrainingDetailAdapter.this);
            recyclerViewSeries.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerViewSeries.setAdapter(seriesAdapter);
            exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());

            /* ------- Botón “Añadir Serie” ------- */
            btnAddSeries.setOnClickListener(v -> {
                int newSeriesNumber = seriesList.size() + 1;
                seriesList.add(new ExerciseSeries(newSeriesNumber, 1, 0.0, 0.0));
                seriesAdapter.notifyItemInserted(seriesList.size() - 1);
                exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());
            });

            /* ------- Menú de acciones (iconAction) ------- */
            iconAction.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(itemView.getContext(), iconAction);
                popup.inflate(R.menu.menu_exercise_actions);
                popup.setOnMenuItemClickListener(item -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && item.getItemId() == R.id.action_delete) {
                        new AlertDialog.Builder(itemView.getContext())
                                .setTitle("Eliminar ejercicio")
                                .setMessage("¿Estás seguro de que deseas eliminar " + exercise.getName() + "?")
                                .setPositiveButton("Eliminar", (dialog, which) -> {
                                    UserExercise exerciseToDelete = exercises.get(position);
                                    exercises.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, exercises.size());
                                    pendingDeletionExercises.add(exerciseToDelete);







                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });


            /* ------- Click sobre el nombre ⇒ diálogo de detalles ------- */
            exerciseName.setOnClickListener(v -> {
                Log.d("TrainingDetailAdapter", "📌 isFromInternal(): " + exercise.isFromInternal());

                if (exercise.isFromInternal()) {
                    Log.d(TAG, "📦 Ejercicio es interno. Mostrando detalles desde almacenamiento local.");

                    Exercise base = new Exercise();
                    base.setName(exercise.getName());
                    base.setDescription(exercise.getDescription());
                    base.setEquipmentType(exercise.getEquipmentType());
                    base.setExerciseType(exercise.getExerciseType());
                    base.setMusclesWorked(exercise.getMusclesWorked());
                    base.setMovementType(exercise.getMovementType());
                    base.setImages(exercise.getImages());

                    Bundle args = new Bundle();
                    args.putSerializable("exercise", base);
                    args.putString("dayKey", dayKey);
                    args.putBoolean("isFromInternal", true);

                    ExerciseDetailsDialogFragment dialog = new ExerciseDetailsDialogFragment();
                    dialog.setArguments(args);
                    dialog.show(((FragmentActivity) itemView.getContext()).getSupportFragmentManager(), "ExerciseDetailsDialog");
                } else {
                    Log.d(TAG, "🌐 Ejercicio viene de Firebase. Mostrando con claves desde Firebase.");

                    DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("userExercises");

                    userExercisesRef.orderByChild("name").equalTo(exercise.getName())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                                        String firebaseKey = exerciseSnapshot.getKey();
                                        if (firebaseKey != null && !firebaseKey.trim().isEmpty()) {
                                            Bundle args = new Bundle();
                                            args.putString("exerciseIndex", firebaseKey);
                                            args.putString("dayKey", dayKey);
                                            args.putBoolean("isFromInternal", false);

                                            ExerciseDetailsDialogFragment dialog = new ExerciseDetailsDialogFragment();
                                            dialog.setArguments(args);
                                            dialog.show(((FragmentActivity) itemView.getContext()).getSupportFragmentManager(), "ExerciseDetailsDialog");
                                        } else {
                                            Log.e(TAG, "❌ Clave Firebase vacía o nula, no se puede abrir el diálogo");
                                        }

                                        Bundle args = new Bundle();
                                        args.putString("exerciseIndex", firebaseKey);
                                        args.putString("dayKey", dayKey);
                                        args.putBoolean("isFromInternal", false);

                                        ExerciseDetailsDialogFragment dialog = new ExerciseDetailsDialogFragment();
                                        dialog.setArguments(args);
                                        dialog.show(((FragmentActivity) itemView.getContext()).getSupportFragmentManager(), "ExerciseDetailsDialog");
                                        break;
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "❌ Error al buscar ejercicio en Firebase", error.toException());
                                }
                            });
                }
            });



        }

        /* =============================================================================
         *  Helper: decodifica Base64 en el ImageView. Devuelve true si tuvo éxito
         *  ============================================================================= */
        private boolean mostrarBase64EnImageView(String base64, ImageView target) {
            try {
                base64 = base64.replaceAll("\\s", "");
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    target.setImageBitmap(bmp);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error al decodificar imagen base64", e);
            }
            target.setImageResource(R.drawable.ic_placeholder);
            return false;
        }



    }
    public void updateSeriesCount(String exerciseName, int newCount) {
        for (UserExercise ex : exercises) {
            if (ex.getName().equalsIgnoreCase(exerciseName)) {
                ex.setSeries(newCount); // 👈 actualiza internamente el objeto
                Log.d(TAG, "🔁 updateSeriesCount: actualizadas series para " + exerciseName + " → " + newCount);
                break;
            }
        }
    }
    private void borrarEjerciciosInternos(Context context, List<String> nombresAEliminar) {
        try {
            File file = new File(context.getFilesDir(), "exercises.json");
            if (!file.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> ejercicios = gson.fromJson(reader, listType);
            reader.close();

            List<Exercise> filtrados = new ArrayList<>();
            for (Exercise ex : ejercicios) {
                if (!nombresAEliminar.contains(ex.getName())) {
                    filtrados.add(ex);
                }
            }

            // Reescribir el archivo con los ejercicios restantes
            FileWriter writer = new FileWriter(file, false);
            gson.toJson(filtrados, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al borrar ejercicios internos del archivo", e);
        }
    }


    public void swapExercises(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= exercises.size() || toPosition >= exercises.size())
            return;

        Collections.swap(exercises, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        Log.d(TAG, "🔀 swapExercises: movido de " + fromPosition + " a " + toPosition);
    }
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= exercises.size() || toPosition >= exercises.size()) {
            Log.w(TAG, "❌ Posiciones inválidas en onItemMove: " + fromPosition + " → " + toPosition);
            return;
        }

        Collections.swap(exercises, fromPosition, toPosition); // 🔄 Reordenar en la lista real
        notifyItemMoved(fromPosition, toPosition);             // 🔔 Notificar al RecyclerView

        Log.d(TAG, "🔀 onItemMove: " + fromPosition + " → " + toPosition);
    }

    public SeriesAdapter getSeriesAdapterPorNombre(String exerciseName) {
        for (ExerciseViewHolder holder : viewHolders) {
            if (holder.exerciseName.getText().toString().equalsIgnoreCase(exerciseName)) {
                return holder.seriesAdapter;
            }
        }
        return null;
    }

}
