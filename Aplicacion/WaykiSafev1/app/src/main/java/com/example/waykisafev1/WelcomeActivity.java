package com.example.waykisafe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        // Obtener nombre del usuario enviado desde Login o Registro
        Intent intent = getIntent();
        String userName = intent.getStringExtra("USERNAME");

        if (userName != null && !userName.isEmpty()) {
            tvWelcome.setText("Bienvenido a WaykiSafe, " + userName + " 👋");
        } else {
            tvWelcome.setText("Bienvenido a WaykiSafe");
        }

        // Botón para regresar al Login
        btnGoToLogin.setOnClickListener(v -> {
            Intent goLogin = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(goLogin);
            finish();
        });
    }
}
