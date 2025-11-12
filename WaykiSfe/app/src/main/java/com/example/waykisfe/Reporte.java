package com.example.waykisfe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

public class Reporte extends AppCompatActivity implements SensorEventListener {

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private SensorManager sensorManager;
    private long lastShakeTime = 0;
    private static final String NUMERO_AUTORIDAD = "105"; // Policía Nacional del Perú
    private String contactoEmergencia = null;

    private ActivityResultLauncher<Intent> pickContactLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Selección de contacto de emergencia
        pickContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        Uri contactData = result.getData().getData();
                        Cursor cursor = getContentResolver().query(contactData, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            contactoEmergencia = cursor.getString(phoneIndex);
                            cursor.close();
                            Toast.makeText(this, "📱 Contacto guardado: " + contactoEmergencia, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        Button btnEnviar = findViewById(R.id.btnEnviar);
        btnEnviar.setOnClickListener(v -> mostrarConfirmacionEnvio());

        // 🔹 Mantener presionado para elegir contacto de emergencia
        btnEnviar.setOnLongClickListener(v -> {
            seleccionarContactoEmergencia();
            return true;
        });
    }

    // ✅ Pantalla de confirmación antes del envío
    private void mostrarConfirmacionEnvio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar envío de alerta 🚨");
        builder.setMessage("¿Deseas enviar tu ubicación y datos personales a las autoridades y a tu contacto de emergencia?");
        builder.setCancelable(false);

        builder.setPositiveButton("Sí, enviar", (dialog, which) -> enviarUbicacionYDatos());
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            Toast.makeText(this, "❎ Envío cancelado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // ✅ Seleccionar contacto
    private void seleccionarContactoEmergencia() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        pickContactLauncher.launch(intent);
    }

    // ✅ Enviar ubicación y datos personales
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
                        Toast.makeText(this, "✅ Datos enviados", Toast.LENGTH_SHORT).show();
                        realizarLlamadaEmergencia();
                        enviarMensajeEmergencia(lat, lng);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // 📞 Llamada automática
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
        Toast.makeText(this, "📞 Llamando a autoridades...", Toast.LENGTH_SHORT).show();
    }

    // 💬 Enviar SMS automático a autoridades o contacto
    private void enviarMensajeEmergencia(double lat, double lng) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 300);
            Toast.makeText(this, "Permiso de SMS requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        String mensaje = "⚠️ Emergencia detectada.\nUbicación:\nLat: " + lat + "\nLng: " + lng;

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(NUMERO_AUTORIDAD, null, mensaje, null, null);
        if (contactoEmergencia != null) {
            smsManager.sendTextMessage(contactoEmergencia, null, mensaje, null, null);
        }

        Toast.makeText(this, "📩 Mensaje enviado a autoridades y contacto", Toast.LENGTH_SHORT).show();
    }

    // 🚨 Activación por agitar el dispositivo
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();

            if ((currentTime - lastShakeTime) > 1000) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

                if (acceleration > 15) {
                    lastShakeTime = currentTime;
                    Toast.makeText(this, "🚨 Dispositivo agitado - Enviando alerta", Toast.LENGTH_SHORT).show();
                    mostrarConfirmacionEnvio();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
