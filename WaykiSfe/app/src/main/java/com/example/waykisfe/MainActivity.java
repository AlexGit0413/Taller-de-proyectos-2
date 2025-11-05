package com.example.waykisfe;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {
    private TextInputEditText editTextEmail, editPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FirebaseApp.initializeApp(this);
    setContentView(R.layout.activity_main);

    editTextEmail = findViewById(R.id.editTextEmail);
    editPassword = findViewById(R.id.editPassword);
    btnLogin = findViewById(R.id.btn_login);

    mAuth = FirebaseAuth.getInstance();

    btnLogin.setOnClickListener(v -> loginUser());
}

// Validación e inicio de sesión con correo/contraseña
private void loginUser() {
    String email = editTextEmail.getText().toString().trim();
    String password = editPassword.getText().toString().trim();

    if (TextUtils.isEmpty(email)) { editTextEmail.setError("Correo electrónico requerido"); return; }
    if (TextUtils.isEmpty(password)) { editPassword.setError("Contraseña requerida"); return; }

    mAuth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(MainActivity.this, "Bienvenido " + user.getEmail(), Toast.LENGTH_SHORT).show();
                // TODO: navegar a tu pantalla post-login (p.ej. MapsActivity/Bienvenido)
            } else {
                Toast.makeText(MainActivity.this,
                    "Error de autenticación: " + task.getException().getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
}
}
