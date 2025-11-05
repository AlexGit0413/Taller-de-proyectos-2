package com.example.waykisfe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Bienvenido extends AppCompatActivity {

    private TextView txtWelcome;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CardView btnNext;

    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bienvenido);

        // --- Inicialización de vistas ---
        txtWelcome = findViewById(R.id.txtWelcome);
        btnNext = findViewById(R.id.btnInfo);
        viewPager = findViewById(R.id.viewPager);

        // Inicialmente ocultamos el carrusel
        viewPager.setVisibility(View.GONE);

        // --- Firebase ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Adapter del carrusel ---
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // --- Animación entre fragmentos ---
        viewPager.setPageTransformer(new DepthPageTransformer());

        // --- Saludo al usuario ---
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();

            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String apellido = documentSnapshot.getString("apellido");

                            if (nombre == null) nombre = "";
                            if (apellido == null) apellido = "";

                            String saludo = "¡Hola, " + nombre + " " + apellido + "!";
                            txtWelcome.setText(saludo);
                        } else {
                            txtWelcome.setText("¡Hola, Usuario!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Bienvenido", "Error al obtener datos", e);
                        txtWelcome.setText("¡Hola, Usuario!");
                    });
        } else {
            txtWelcome.setText("¡Hola, Usuario!");
        }

        // --- Botón de flecha ---
        btnNext.setOnClickListener(v -> {
            if (viewPager.getVisibility() == View.GONE) {
                // Mostrar el carrusel por primera vez
                viewPager.setVisibility(View.VISIBLE);
                return; // no avanzar todavía
            }

            if (viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                // Avanza al siguiente fragmento con animación
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            } else {
                // Último fragmento, abrir MapsActivity con animación
                Intent intent = new Intent(Bienvenido.this, MapsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        });
    }
}
