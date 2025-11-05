package com.example.waykisafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.WindowManager;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.fab_sos).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, Alerta.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 19.0f));

                        mMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("Tu ubicación actual")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ubi)));

                        mMap.addCircle(new CircleOptions()
                                .center(userLocation)
                                .radius(8)
                                .strokeColor(Color.GRAY)
                                .fillColor(Color.argb(50, 128, 128, 128)));

                        verificarProximidad(userLocation);
                    }
                });

        cargarZonasPeligrosas();
    }


    private void verificarProximidad(LatLng userLocation) {
        final double RADIUS = 5;

        firestore.collection("reportes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("latitud");
                        Double lng = doc.getDouble("longitud");
                        String nivel = doc.getString("nivel_peligro");

                        if (lat == null || lng == null || !"Rojo".equalsIgnoreCase(nivel)) continue;

                        Location userLoc = new Location("user");
                        userLoc.setLatitude(userLocation.latitude);
                        userLoc.setLongitude(userLocation.longitude);

                        Location zoneLoc = new Location("zone");
                        zoneLoc.setLatitude(lat);
                        zoneLoc.setLongitude(lng);

                        float distance = userLoc.distanceTo(zoneLoc);

                        if (distance < RADIUS) {
                            Toast.makeText(MapsActivity.this, "¡Estás ingresando a una zona peligrosa!", Toast.LENGTH_LONG).show();
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
                        String key = Math.round(lat * 1000) + "," + Math.round(lng * 1000);
                        zonasOcupadas.put(key, true);

                        int colorStroke = Color.RED;
                        int colorFill = 0x44FF0000;

                        if ("Naranja".equalsIgnoreCase(nivel)) {
                            colorStroke = Color.rgb(255, 165, 0);
                            colorFill = 0x44FFA500;
                        }

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
                    }

                    firestore.collection("reportes_ia")
                            .get()
                            .addOnSuccessListener(queryIA -> {

                                for (QueryDocumentSnapshot doc : queryIA) {

                                    Double lat = doc.getDouble("lat");
                                    Double lng = doc.getDouble("lon");
                                    String nivel = doc.getString("nivel_peligro");
                                    String texto = doc.getString("texto");

                                    if (lat == null || lng == null || nivel == null) continue;

                                    LatLng ubicacion = new LatLng(lat, lng);
                                    String key = Math.round(lat * 1000) + "," + Math.round(lng * 1000);
                                    zonasOcupadas.put(key, true);

                                    int colorStroke = Color.RED;
                                    int colorFill = 0x44FF0000;

                                    if ("Naranja".equalsIgnoreCase(nivel)) {
                                        colorStroke = Color.rgb(255, 165, 0);
                                        colorFill = 0x44FFA500;
                                    }

                                    mMap.addCircle(new CircleOptions()
                                            .center(ubicacion)
                                            .radius(8)
                                            .strokeColor(colorStroke)
                                            .fillColor(colorFill));

                                    mMap.addMarker(new MarkerOptions()
                                            .position(ubicacion)
                                            .title("[IA] " + (texto != null ? texto : "Zona IA"))
                                            .snippet("Nivel: " + nivel)
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    Bitmap.createScaledBitmap(
                                                            BitmapFactory.decodeResource(getResources(), R.drawable.ia),
                                                            50, 50, false))));
                                }

                                // ✅ Pintar ZONAS VERDES SOLO donde NO hay reportes
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
                            });
                });
    }
}
