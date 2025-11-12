package com.example.waykisfe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Reporte extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private static final String NUMERO_AUTORIDAD = "105"; // 📞 Policía Nacional del Perú

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte);

        // Inicializar servicios
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 🚀 Al iniciar: envía ubicación + datos personales
        enviarUbicacionYDatos();
    }

    // 🔹 6.1.2 Implementar envío automático de ubicación y datos personales
    @SuppressLint("MissingPermission")
    private void enviarUbicacionYDatos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                return;
            }

            double lat = location.getLatitude();
            double lng = location.getLongitude();

            // Guardar ubicación y datos personales en Firebase
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre_usuario", user.getDisplayName() != null ? user.getDisplayName() : "Sin nombre");
            datos.put("correo_usuario", user.getEmail());
            datos.put("usuario_id", user.getUid());
            datos.put("latitud", lat);
            datos.put("longitud", lng);
            datos.put("fecha_envio", new Timestamp(new Date()));

            firestore.collection("ubicaciones_usuarios")
                    .add(datos)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "✅ Ubicación y datos enviados automáticamente", Toast.LENGTH_SHORT).show();

                        // Después de enviar, activar opciones de emergencia
                        realizarLlamadaEmergencia();
                        enviarMensajeEmergencia(lat, lng);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // 🔹 6.1.3 Configurar opción de llamada automática a autoridades
    private void realizarLlamadaEmergencia() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + NUMERO_AUTORIDAD));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 200);
            Toast.makeText(this, "Permiso de llamada requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(intent);
        Toast.makeText(this, "📞 Llamando a las autoridades...", Toast.LENGTH_SHORT).show();
    }

    // 🔹 6.1.3 Envío automático de mensaje SMS a autoridades
    private void enviarMensajeEmergencia(double lat, double lng) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 300);
            Toast.makeText(this, "Permiso de SMS requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        String mensaje = "⚠️ Emergencia detectada.\nUbicación aproximada:\nLat: " + lat + "\nLng: " + lng;

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(NUMERO_AUTORIDAD, null, mensaje, null, null);

        Toast.makeText(this, "📩 Mensaje enviado a autoridades", Toast.LENGTH_SHORT).show();
    }
}
