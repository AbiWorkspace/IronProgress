package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
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
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                    return true;
                }
                // Otros ítems (si los hay)…
                drawer.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // ---- Lista de usuarios ----
        recycler = findViewById(R.id.recyclerUsers);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter();
        recycler.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserRow> rows = new ArrayList<>();
                for (DataSnapshot u : snapshot.getChildren()) {
                    String uid   = u.getKey();
                    String first = u.child("firstName").getValue(String.class);
                    String last  = u.child("lastName").getValue(String.class);
                    String email = u.child("email").getValue(String.class);

                    Boolean train = u.child("trainingFormCompleted").getValue(Boolean.class);
                    Boolean food  = u.child("foodFormCompleted").getValue(Boolean.class);

                    boolean trainingDone = train != null && train;
                    boolean foodDone     = food  != null && food;

                    rows.add(new UserRow(uid,
                            (first == null ? "" : first) + " " + (last == null ? "" : last),
                            email == null ? "" : email,
                            trainingDone, foodDone));
                }
                adapter.submit(rows);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
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
            h.name.setText(r.name);
            h.email.setText(r.email);
            h.trainStatus.setText(r.trainingDone ? "Entrenamiento: ✔" : "Entrenamiento: ✖");
            h.foodStatus.setText(r.foodDone ? "Alimentación: ✔" : "Alimentación: ✖");

            h.open.setEnabled(r.ready());
            h.open.setAlpha(r.ready() ? 1f : 0.4f);
            h.open.setOnClickListener(v -> {
                if (!r.ready()) return;
                // Abre el dashboard del usuario (usa tu Activity real)
                Intent i = new Intent(UsersListActivity.this, ViewPagerActivity.class);
                i.putExtra("coachMode", true);
                i.putExtra("userId", r.uid);
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
