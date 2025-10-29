package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends AppCompatActivity {

    private static final String TAG = "UsersListActivity";

    private RecyclerView recycler;
    private UsersAdapter adapter;
    private DatabaseReference usersRef;

    // Drawer
    private DrawerLayout drawer;
    private NavigationView navView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        // ---- Drawer (menú lateral por gesto) ----
        drawer  = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_logout) {
                    Log.d(TAG, "Cerrando sesión...");
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                    return true;
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // ---- Lista de usuarios ----
        recycler = findViewById(R.id.recyclerUsers);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter();
        recycler.setAdapter(adapter);

        final String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        Log.d(TAG, "onCreate: Coach/Admin UID: " + (myUid != null ? myUid : "¡¡ERROR: NULL!!"));

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: Se encontraron " + snapshot.getChildrenCount() + " nodos de usuario en total.");

                List<UserRow> rows = new ArrayList<>();
                for (DataSnapshot u : snapshot.getChildren()) {
                    String uid   = u.getKey();
                    Log.v(TAG, "Procesando UID: " + uid);

                    // ⛔ Omitirme a mí mismo (opcional, recomendado)
                    if (myUid != null && myUid.equals(uid)) {
                        Log.w(TAG, "-> Omitiendo usuario " + uid + " (Soy yo, el coach/admin).");
                        continue;
                    }

                    // ⛔ Omitir coaches / admins
                    String role = u.child("role").getValue(String.class);
                    Log.v(TAG, "-> UID: " + uid + ", Role: " + (role != null ? role : "null"));

                    if (role != null) {
                        String r = role.trim().toLowerCase();
                        if ("coach".equals(r) || "admin".equals(r)) {
                            Log.w(TAG, "-> Omitiendo usuario " + uid + " (es " + r + ").");
                            continue;
                        }
                    }
                    // (Si role es null, lo tratamos como "user" y sí se lista)

                    String first = u.child("firstName").getValue(String.class);
                    String last  = u.child("lastName").getValue(String.class);
                    String email = u.child("email").getValue(String.class);

                    Boolean train = u.child("trainingFormCompleted").getValue(Boolean.class);

                    // --- INICIO DE LA MODIFICACIÓN ---
                    // Se cambia la lógica: ya no se usa 'foodFormCompleted'.
                    // Ahora se lee 'isFirstTime'.
                    Boolean isFirstTime  = u.child("isFirstTime").getValue(Boolean.class);

                    boolean trainingDone = train != null && train;

                    // El formulario de comida se considera "hecho" (true) SÓLO SI 'isFirstTime' es 'false'.
                    // Si 'isFirstTime' es 'true' o es 'null' (no existe),
                    // entonces 'foodDone' será 'false' (no completo).
                    boolean foodDone = (isFirstTime != null && !isFirstTime);
                    // --- FIN DE LA MODIFICACIÓN ---


                    // Log actualizado para depuración
                    Log.i(TAG, "==> AÑADIENDO usuario: " + first + " " + last + " (UID: " + uid + ")");
                    Log.d(TAG, "    -> trainDone: " + trainingDone + ", isFirstTime: " + isFirstTime + ", foodDone (calculado): " + foodDone);


                    rows.add(new UserRow(uid,
                            (first == null ? "" : first) + " " + (last == null ? "" : last),
                            email == null ? "" : email,
                            trainingDone, foodDone)); // Se pasa el nuevo valor de foodDone
                }
                Log.i(TAG, "onDataChange: Finalizado. Enviando " + rows.size() + " usuarios al adaptador.");
                adapter.submit(rows);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "¡¡ERROR AL LEER USUARIOS!!: ", error.toException());
                Log.e(TAG, "MENSAJE: " + error.getMessage());
                Log.e(TAG, "DETALLES: " + error.getDetails());
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // --- modelo simple ---
    static class UserRow {
        final String uid, name, email;
        final boolean trainingDone, foodDone;
        UserRow(String uid, String name, String email, boolean trainingDone, boolean foodDone) {
            this.uid = uid; this.name = name; this.email = email;
            this.trainingDone = trainingDone; this.foodDone = foodDone;
        }
        // Esta función 'ready()' ahora usará la nueva lógica de 'foodDone' automáticamente
        boolean ready() { return trainingDone && foodDone; }
    }

    // --- adapter ---
    class UsersAdapter extends RecyclerView.Adapter<UserVH> {
        private List<UserRow> data = new ArrayList<>();
        void submit(List<UserRow> d) { data = d; notifyDataSetChanged(); }

        @NonNull @Override public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_row, parent, false);
            return new UserVH(v);
        }

        @Override public void onBindViewHolder(@NonNull UserVH h, int pos) {
            UserRow r = data.get(pos);
            h.name.setText(r.name.trim());
            h.email.setText(r.email);
            h.trainStatus.setText(r.trainingDone ? "Entrenamiento: ✔" : "Entrenamiento: ✖");

            // Esta línea ahora muestra el estado basado en 'isFirstTime'
            h.foodStatus.setText(r.foodDone ? "Alimentación: ✔" : "Alimentación: ✖");

            // Esta lógica también se actualiza automáticamente
            h.open.setEnabled(r.ready());
            h.open.setAlpha(r.ready() ? 1f : 0.4f);
            h.open.setOnClickListener(v -> {
                if (!r.ready()) return;  // solo si completó ambos formularios
                Log.d(TAG, "Abriendo dashboard para UID: " + r.uid);

                Intent i = new Intent(UsersListActivity.this, ViewPagerActivity.class);
                i.putExtra("coachMode", true);
                i.putExtra("targetUid", r.uid);   // <-- UID del cliente a editar
                startActivity(i);
            });
        }

        @Override public int getItemCount() { return data.size(); }
    }

    static class UserVH extends RecyclerView.ViewHolder {
        final TextView name, email, trainStatus, foodStatus;
        final Button open;
        UserVH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.txtName);
            email = v.findViewById(R.id.txtEmail);
            trainStatus = v.findViewById(R.id.txtTrain);
            foodStatus  = v.findViewById(R.id.txtFood);
            open = v.findViewById(R.id.btnOpenDashboard);
        }
    }
}
