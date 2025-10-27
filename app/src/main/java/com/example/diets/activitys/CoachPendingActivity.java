package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.diets.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CoachPendingActivity extends AppCompatActivity {

    private static final String TAG = "CoachPendingActivity";
    private DrawerLayout drawer;
    private NavigationView navView;
    private DatabaseReference roleRef;
    private ValueEventListener roleListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_pending);

        drawer = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        // Evita NPE si por alguna razón el layout cambia
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
                drawer.closeDrawer(GravityCompat.START);
                return true;
            });
        } else {
            Log.w(TAG, "NavigationView es null. ¿Estás usando el layout correcto?");
        }

        // Escucha en tiempo real si ya te aprobaron como coach
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            // Si no hay sesión, regresa a login
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        String uid = current.getUid();

        roleRef = FirebaseDatabase.getInstance().getReference("users")
                .child(uid).child("role");

        roleListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                Log.d(TAG, "role actual: " + role);
                if ("coach".equalsIgnoreCase(role)) {
                    // Ya eres coach → ir a la lista de usuarios (pantallas del coach)
                    Intent i = new Intent(CoachPendingActivity.this, UsersListActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }
                // Si sigue como "user" o null, se queda esta pantalla con el spinner y el texto
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error leyendo role: " + error.getMessage());
            }
        };
        roleRef.addValueEventListener(roleListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roleRef != null && roleListener != null) {
            roleRef.removeEventListener(roleListener);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
