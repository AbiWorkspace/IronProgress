package com.example.diets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.UserExercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

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
    ImageView iconAction;
    ImageView exerciseImage;

    private final List<UserExercise> pendingDeletionExercises = new ArrayList<>();

    public TrainingDetailAdapter(List<UserExercise> exercises, RecyclerView recyclerView, String dayKey) {
        Log.d(TAG, "TrainingDetailAdapter - Constructor: Inicializando adaptador con lista de ejercicios.");
        this.exercises = exercises;
        this.recyclerView = recyclerView;
        this.dayKey = dayKey;
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

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exerciseName);
            recyclerViewSeries = itemView.findViewById(R.id.recyclerViewSeries);
            btnAddSeries = itemView.findViewById(R.id.btnAddSeries);
            iconAction = itemView.findViewById(R.id.iconAction);
            exerciseImage = itemView.findViewById(R.id.exerciseImage);
        }

        /** =================================================================================
         *  Método bind(...)  —  muestra la PRIMERA imagen disponible del ejercicio
         *  (propia o de /exerciseImages) y configura el resto de la tarjeta
         *  ================================================================================= */
        public void bind(UserExercise exercise) {

            /* ------- Nombre del ejercicio ------- */
            exerciseName.setText(exercise.getName());

            /* ------- Construcción de la lista de series ------- */
            seriesList = new ArrayList<>();
            for (int i = 0; i < exercise.getSeries(); i++) {
                int seriesNumber = i + 1;
                ExerciseSeries serie = new ExerciseSeries(seriesNumber,
                        exercise.getReps(), exercise.getLastLoad(), exercise.getRpe());
                serie.setPreviousLoad(exercise.getLastLoad());
                seriesList.add(serie);
            }

            /* ------- Imagen (propia o desde /exerciseImages) ------- */
            boolean imagenMostrada = false;

            // 1) ¿El objeto ya trae image base64?
            String base64Image = exercise.getImage();
            if (base64Image != null && !base64Image.trim().isEmpty()) {
                imagenMostrada = mostrarBase64EnImageView(base64Image, exerciseImage);
            }

            // 2) Si no traía, busca la primera imagen en /exerciseImages por nombre
            if (!imagenMostrada) {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("exerciseImages");

                ref.orderByChild("name").equalTo(exercise.getName())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    DataSnapshot imagesNode = snap.child("images");
                                    String firstImg = null;

                                    // Estructura con image1, image2…
                                    if (imagesNode.child("image1").exists()) {
                                        firstImg = imagesNode.child("image1").getValue(String.class);
                                    }
                                    // Estructura tipo lista 0,1,2…
                                    else if (imagesNode.getChildrenCount() > 0) {
                                        firstImg = imagesNode.getChildren().iterator().next().getValue(String.class);
                                    }

                                    if (firstImg != null) {
                                        mostrarBase64EnImageView(firstImg, exerciseImage);
                                    } else {
                                        exerciseImage.setImageResource(R.drawable.ic_placeholder);
                                    }
                                    break; // solo necesitamos una coincidencia
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {
                                exerciseImage.setImageResource(R.drawable.ic_placeholder);
                                Log.e(TAG, "❌ Error al leer exerciseImages", error.toException());
                            }
                        });
            }

            /* ------- Configurar RecyclerView de series ------- */
            seriesAdapter = new SeriesAdapter(seriesList);
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
                DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("userExercises");

                userExercisesRef.orderByChild("name").equalTo(exercise.getName())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                                    String firebaseKey = exerciseSnapshot.getKey();
                                    ExerciseDetailsDialogFragment dialog =
                                            ExerciseDetailsDialogFragment.newInstance(firebaseKey, dayKey);
                                    FragmentActivity act = (FragmentActivity) itemView.getContext();
                                    dialog.show(act.getSupportFragmentManager(), "ExerciseDetailsDialog");
                                    break;
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "❌ Error al buscar ejercicio en Firebase", error.toException());
                            }
                        });
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
            } catch (Exception ignored) { }
            target.setImageResource(R.drawable.ic_placeholder);
            return false;
        }

    }
}
