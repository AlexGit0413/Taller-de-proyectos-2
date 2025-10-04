package com.example.waykisafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegisterEmail, etRegisterPassword;
    private Button btnRegisterUser, btnGoToLogin;
    private ProgressBar loading;

    // TextViews de instrucciones
    private TextView tvLengthRule, tvUpperRule, tvLowerRule, tvNumberRule, tvSymbolRule;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vistas
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        btnRegisterUser = findViewById(R.id.btnRegisterUser);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        loading = findViewById(R.id.loading);

        // TextViews de reglas
        tvLengthRule = findViewById(R.id.tvLengthRule);
        tvUpperRule = findViewById(R.id.tvUpperRule);
        tvLowerRule = findViewById(R.id.tvLowerRule);
        tvNumberRule = findViewById(R.id.tvNumberRule);
        tvSymbolRule = findViewById(R.id.tvSymbolRule);

        // Botón de registro
        btnRegisterUser.setOnClickListener(v -> validateAndRegister());

        // Ir al login
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    // Validar campos
    private void validateAndRegister() {
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etRegisterEmail.setError("El correo es obligatorio");
            etRegisterEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etRegisterPassword.setError("La contraseña es obligatoria");
            etRegisterPassword.requestFocus();
            return;
        }

        boolean valid = true;

        // Validar longitud
        if (password.length() >= 6) {
            tvLengthRule.setText("✔ Al menos 6 caracteres");
            tvLengthRule.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvLengthRule.setText("✘ Al menos 6 caracteres");
            tvLengthRule.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            valid = false;
        }

        // Validar mayúscula
        if (password.matches(".*[A-Z].*")) {
            tvUpperRule.setText("✔ Una mayúscula");
            tvUpperRule.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvUpperRule.setText("✘ Una mayúscula");
            tvUpperRule.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            valid = false;
        }

        // Validar minúscula
        if (password.matches(".*[a-z].*")) {
            tvLowerRule.setText("✔ Una minúscula");
            tvLowerRule.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvLowerRule.setText("✘ Una minúscula");
            tvLowerRule.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            valid = false;
        }

        // Validar número
        if (password.matches(".*[0-9].*")) {
            tvNumberRule.setText("✔ Un número");
            tvNumberRule.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvNumberRule.setText("✘ Un número");
            tvNumberRule.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            valid = false;
        }

        // Validar símbolo
        if (password.matches(".*[@#$%^&+=!].*")) {
            tvSymbolRule.setText("✔ Un símbolo (@#$%^&+=!)");
            tvSymbolRule.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvSymbolRule.setText("✘ Un símbolo (@#$%^&+=!)");
            tvSymbolRule.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            valid = false;
        }

        if (!valid) {
            Toast.makeText(this, "La contraseña no cumple todos los requisitos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registrar
        registerUser(email, password);
    }

    private void registerUser(String email, String password) {
        loading.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    loading.setVisibility(View.GONE);

                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        // Guardar datos en Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(uid)
                                .set(userMap)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "Usuario guardado en Firestore ✅", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error al guardar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );

                        Toast.makeText(this, "Registro exitoso 🎉", Toast.LENGTH_SHORT).show();

                        // 👉 Enviar al LoginActivity
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();

                    } else {
                        String errorMsg = (task.getException() != null) ?
                                task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
