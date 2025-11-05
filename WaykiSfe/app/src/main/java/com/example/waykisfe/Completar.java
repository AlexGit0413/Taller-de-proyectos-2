package com.example.waykisafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Completar extends AppCompatActivity {

    private TextInputEditText editTextCelular, editTextContactoEmergencia;
    private Button btnRegistrar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completar);

        editTextCelular = findViewById(R.id.editTextCelular);
        editTextContactoEmergencia = findViewById(R.id.editTextContactoEmergencia);
        btnRegistrar = findViewById(R.id.btn_registrar_usuario);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegistrar.setOnClickListener(v -> validarYGuardarDatos());
    }

    private void validarYGuardarDatos() {
        String celular = editTextCelular.getText().toString().trim();
        String contacto = editTextContactoEmergencia.getText().toString().trim();

        if (TextUtils.isEmpty(celular) || !celular.matches("\\d{9,}")) {
            editTextCelular.setError("Número inválido");
            editTextCelular.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(contacto) || !contacto.matches("\\d{9,}")) {
            editTextContactoEmergencia.setError("Número inválido");
            editTextContactoEmergencia.requestFocus();
            return;
        }

        if (contacto.equals(celular)) {
            editTextContactoEmergencia.setError("No puede ser igual al número personal");
            editTextContactoEmergencia.requestFocus();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> datos = new HashMap<>();
        datos.put("celular", celular);
        datos.put("contacto_emergencia", contacto);
        datos.put("completo", true); // ✅ marcar como completado

        db.collection("usuarios").document(uid)
                .update(datos)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "✅ Datos completados correctamente", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Completar.this, Bienvenido.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
