package com.example.waykisfe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private DatabaseReference realtimeDatabase;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        realtimeDatabase = FirebaseDatabase.getInstance().getReference("reportes_automaticos");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "No se pudo cargar el mapa", Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.fab_sos).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, Alerta.class);
            startActivity(intent);
        });

        // Escuchar cambios en Realtime Database y actualizar el mapa en tiempo real
        realtimeDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) return;

                mMap.clear();
                cargarZonasPeligrosas(); // Mantener zonas de usuarios
                for (DataSnapshot data : snapshot.getChildren()) {
                    Map<String, Object> reporte = (Map<String, Object>) data.getValue();
                    if (reporte == null) continue;

                    Double lat = (Double) reporte.get("latitud");
                    Double lng = (Double) reporte.get("longitud");
                    String nivel = (String) reporte.get("nivel_peligro");
                    if (lat == null || lng == null || nivel == null) continue;

                    LatLng ubicacion = new LatLng(lat, lng);
                    int colorStroke = nivel.equalsIgnoreCase("Naranja") ? Color.rgb(255, 165, 0) : Color.RED;
                    int colorFill = nivel.equalsIgnoreCase("Naranja") ? 0x44FFA500 : 0x44FF0000;

                    mMap.addCircle(new CircleOptions()
                            .center(ubicacion)
                            .radius(8)
                            .strokeColor(colorStroke)
                            .fillColor(colorFill));

                    mMap.addMarker(new MarkerOptions()
                            .position(ubicacion)
                            .title("[Reporte Automático]")
                            .snippet("Nivel: " + nivel)
                            .icon(BitmapDescriptorFactory.fromBitmap(
                                    Bitmap.createScaledBitmap(
                                            BitmapFactory.decodeResource(getResources(), R.drawable.ia),
                                            50, 50, false))));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, "Error al cargar reportes en tiempo real", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        float zoomLevel = 19.0f;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));

                        mMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("Tu ubicación actual")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ubi)));

                        mMap.addCircle(new CircleOptions()
                                .center(userLocation)
                                .radius(8)
                                .strokeColor(Color.GRAY)
                                .fillColor(Color.argb(50, 128, 128, 128)));

                        verificarProximidadYRegistrar(userLocation);
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                });

        cargarZonasPeligrosas();
    }

    private void verificarProximidadYRegistrar(LatLng userLocation) {
        final double RADIUS = 5; // metros

        firestore.collection("reportes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("latitud");
                        Double lng = doc.getDouble("longitud");
                        String nivel = doc.getString("nivel_peligro");

                        if (lat == null || lng == null || nivel == null) continue;

                        Location userLoc = new Location("user");
                        userLoc.setLatitude(userLocation.latitude);
                        userLoc.setLongitude(userLocation.longitude);

                        Location zoneLoc = new Location("zone");
                        zoneLoc.setLatitude(lat);
                        zoneLoc.setLongitude(lng);

                        float distance = userLoc.distanceTo(zoneLoc);

                        if (distance < RADIUS) {
                            Toast.makeText(MapsActivity.this, "¡Estás ingresando a una zona peligrosa! Se registrará automáticamente el incidente.", Toast.LENGTH_LONG).show();

                            Map<String, Object> incidente = new HashMap<>();
                            incidente.put("latitud", userLocation.latitude);
                            incidente.put("longitud", userLocation.longitude);
                            incidente.put("nivel_peligro", nivel);
                            incidente.put("timestamp", System.currentTimeMillis());
                            incidente.put("tipo", "Automático");

                            // Guardar en Firestore
                            firestore.collection("reportes_automaticos")
                                    .add(incidente)
                                    .addOnSuccessListener(docRef -> Toast.makeText(this, "Incidente registrado automáticamente en Firestore", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Error al registrar incidente: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                            // Guardar en Realtime Database
                            realtimeDatabase.push().setValue(incidente)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Incidente guardado en Realtime Database", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar en Realtime Database: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
    }

    private void cargarZonasPeligrosas() {
        final HashMap<String, Boolean> zonasOcupadas = new HashMap<>();

        firestore.collection("reportes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("latitud");
                        Double lng = doc.getDouble("longitud");
                        String nivel = doc.getString("nivel_peligro");

                        if (lat == null || lng == null || nivel == null) continue;

                        LatLng ubicacion = new LatLng(lat, lng);
                        int colorStroke = nivel.equalsIgnoreCase("Naranja") ? Color.rgb(255, 165, 0) : Color.RED;
                        int colorFill = nivel.equalsIgnoreCase("Naranja") ? 0x44FFA500 : 0x44FF0000;

                        mMap.addCircle(new CircleOptions()
                                .center(ubicacion)
                                .radius(8)
                                .strokeColor(colorStroke)
                                .fillColor(colorFill));

                        mMap.addMarker(new MarkerOptions()
                                .position(ubicacion)
                                .title("[Usuario] Zona reportada")
                                .snippet("Nivel: " + nivel)
                                .icon(BitmapDescriptorFactory.fromBitmap(
                                        Bitmap.createScaledBitmap(
                                                BitmapFactory.decodeResource(getResources(), R.drawable.human),
                                                50, 50, false))));

                        String key = Math.round(lat * 1000) + "," + Math.round(lng * 1000);
                        zonasOcupadas.put(key, true);
                    }

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(location -> {
                                    if (location != null) {
                                        double latCentro = location.getLatitude();
                                        double lonCentro = location.getLongitude();

                                        for (int latOffset = -3; latOffset <= 3; latOffset++) {
                                            for (int lonOffset = -3; lonOffset <= 3; lonOffset++) {
                                                double lat = latCentro + (latOffset * 0.0005);
                                                double lon = lonCentro + (lonOffset * 0.0005);
                                                String key = Math.round(lat * 1000) + "," + Math.round(lon * 1000);

                                                if (!zonasOcupadas.containsKey(key)) {
                                                    LatLng ubicacion = new LatLng(lat, lon);
                                                    mMap.addGroundOverlay(new GroundOverlayOptions()
                                                            .image(BitmapDescriptorFactory.fromResource(R.drawable.verde))
                                                            .position(ubicacion, 60f)
                                                            .transparency(0.4f));
                                                }
                                            }
                                        }
                                    }
                                });
                    }
                });
    }
}
