package com.example.mapaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class VisualizarMapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location ultimaLocalizacao;
    private List<LatLng> pathPoints = new ArrayList<>();

    private Marker marcadorAtual;
    private Circle circuloPrecisao;
    private Polyline polyline;

    private String tipoMapa;
    private String navegacao;
    private String iconeMarcador;
    private boolean exibirTrafego;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int MAX_PATH_POINTS = 200;

    private Handler animationHandler = new Handler();
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_mapa);

        Intent intent = getIntent();
        tipoMapa = intent.getStringExtra("tipoMapa");
        navegacao = intent.getStringExtra("navegacao");
        iconeMarcador = intent.getStringExtra("iconeMarcador");
        exibirTrafego = intent.getBooleanExtra("exibirTrafego", false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        atualizarMapaComLocalizacao(location);
                    }
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            iniciarAtualizacaoDeLocalizacao();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        configurarMapa();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(this, "Permissão de localização não concedida", Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarMapa() {
        if (tipoMapa != null) {
            switch (tipoMapa) {
                case "Satélite":
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case "Híbrido":
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
                default:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
            }
        }

        UiSettings uiSettings = mMap.getUiSettings();
        switch (navegacao) {
            case "Course Up":
                uiSettings.setCompassEnabled(true);
                break;
            case "Nenhuma":
                uiSettings.setCompassEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                break;
            default:
                uiSettings.setCompassEnabled(true);
                uiSettings.setRotateGesturesEnabled(true);
                break;
        }

        mMap.setTrafficEnabled(exibirTrafego);
    }

    private void iniciarAtualizacaoDeLocalizacao() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void atualizarMapaComLocalizacao(Location location) {
        ultimaLocalizacao = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Atualizar marcador
        if (marcadorAtual == null) {
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Minha Localização");

            if (iconeMarcador.equals("Personalizado")) {
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.custom_marker));
            }

            marcadorAtual = mMap.addMarker(markerOptions);
        } else {
            marcadorAtual.setPosition(latLng);
        }

        // Atualizar círculo
        if (circuloPrecisao == null) {
            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .radius(location.getAccuracy())
                    .fillColor(0x440000FF)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(2);
            circuloPrecisao = mMap.addCircle(circleOptions);
        } else {
            circuloPrecisao.setCenter(latLng);
            circuloPrecisao.setRadius(location.getAccuracy());
        }

        // Atualizar rastro
        adicionarPontoComAnimacao(latLng);

        // Atualizar câmera
        float zoomLevel = mMap.getCameraPosition().zoom;
        if (navegacao.equals("Course Up") && location.hasBearing()) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(zoomLevel)
                    .bearing(location.getBearing())
                    .tilt(0)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
        }
    }


    private void adicionarPontoComAnimacao(LatLng novoPonto) {
        if (pathPoints.isEmpty()) {
            pathPoints.add(novoPonto);
            polyline = mMap.addPolyline(new PolylineOptions().color(Color.RED).width(5f).addAll(pathPoints));
            return;
        }

        LatLng ultimoPonto = pathPoints.get(pathPoints.size() - 1);
        final int steps = 10; // Quantidade de frames para interpolar
        final long duration = 500; // Duração da animação em ms
        final long delay = duration / steps; // Tempo entre cada frame
        final double latDiff = (novoPonto.latitude - ultimoPonto.latitude) / steps;
        final double lngDiff = (novoPonto.longitude - ultimoPonto.longitude) / steps;

        isAnimating = true;
        animationHandler.post(new Runnable() {
            int currentStep = 0;
            LatLng pontoInterpolado = ultimoPonto;

            @Override
            public void run() {
                if (currentStep <= steps) {
                    double newLat = pontoInterpolado.latitude + latDiff;
                    double newLng = pontoInterpolado.longitude + lngDiff;
                    pontoInterpolado = new LatLng(newLat, newLng);
                    pathPoints.add(pontoInterpolado);

                    if (pathPoints.size() > MAX_PATH_POINTS) {
                        pathPoints.remove(0);
                    }

                    if (polyline == null) {
                        polyline = mMap.addPolyline(new PolylineOptions()
                                .color(Color.RED)
                                .width(5f)
                                .addAll(pathPoints));
                    } else {
                        polyline.setPoints(pathPoints);
                    }

                    currentStep++;
                    animationHandler.postDelayed(this, delay);
                } else {
                    isAnimating = false;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarAtualizacaoDeLocalizacao();
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        iniciarAtualizacaoDeLocalizacao();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        animationHandler.removeCallbacksAndMessages(null);
    }
}