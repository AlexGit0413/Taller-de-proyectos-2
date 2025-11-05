package com.example.waykisfe;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waykisfe.MainActivity;
import com.example.waykisfe.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
public class Inicio extends AppCompatActivity {
    private FirebaseAuth mAuth;


    Button btnSignIn, btnSignUp;
    Animation bounce, slideUpFade;
    LinearLayout containerBienvenido;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        // ✅ Inicializar Firebase
FirebaseApp.initializeApp(this);
mAuth = FirebaseAuth.getInstance();


        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        containerBienvenido = findViewById(R.id.containerBienvenido);

        bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        slideUpFade = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);

        containerBienvenido.startAnimation(slideUpFade);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Usuario ya logueado → ir directo a Bienvenido
            startActivity(new Intent(Inicio.this, Bienvenido.class));
            finish();
        }

        btnSignIn.setOnClickListener(v -> {
            v.startAnimation(bounce);
            startActivity(new Intent(Inicio.this, MainActivity.class));
        });

        btnSignUp.setOnClickListener(v -> {
            v.startAnimation(bounce);
            startActivity(new Intent(Inicio.this, Registro.class));
        });
        @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_inicio);

    // Inicializar Firebase Authentication
    FirebaseApp.initializeApp(this);
    mAuth = FirebaseAuth.getInstance();

    // Referencias a botones y contenedor
    btnSignIn = findViewById(R.id.btnSignIn);
    btnSignUp = findViewById(R.id.btnSignUp);
    containerBienvenido = findViewById(R.id.containerBienvenido);

    // Animaciones
    bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
    slideUpFade = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
    containerBienvenido.startAnimation(slideUpFade);

    // Verificar si el usuario ya está logueado
    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser != null) {
        startActivity(new Intent(Inicio.this, Bienvenido.class));
        finish();
    }

    // Botones
    btnSignIn.setOnClickListener(v -> {
        v.startAnimation(bounce);
        startActivity(new Intent(Inicio.this, MainActivity.class));
    });

    btnSignUp.setOnClickListener(v -> {
        v.startAnimation(bounce);
        startActivity(new Intent(Inicio.this, Registro.class));
    });
}

    }

