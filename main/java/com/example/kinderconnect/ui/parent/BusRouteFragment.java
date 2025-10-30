package com.example.kinderconnect.ui.parent;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.kinderconnect.R;
import com.example.kinderconnect.data.model.BusStatus; // <-- IMPORTANTE: Usamos el nuevo modelo
import com.example.kinderconnect.databinding.FragmentBusRouteBinding;
import com.example.kinderconnect.utils.Resource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.GeoPoint;

import java.util.Arrays;
import java.util.List;

public class BusRouteFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "BusRouteFragment";

    // Enum para los estados del bus
    private enum BusState { UNKNOWN, STOPPED, ACTIVE, FINISHED }

    private FragmentBusRouteBinding binding;
    private ParentViewModel viewModel;
    private GoogleMap mMap;
    private Marker busMarker;
    private ValueAnimator busAnimator;
    private BusState currentBusState = BusState.UNKNOWN;
    private LatLng lastKnownBusLocation = null;

    // Define tu ruta aquí
    private final List<LatLng> routePoints = Arrays.asList(
            new LatLng(19.4326, -99.1332), // Zócalo (Inicio)
            new LatLng(19.4340, -99.1405), // Catedral
            new LatLng(19.4350, -99.1414), // Templo Mayor
            new LatLng(19.4354, -99.1332), // Palacio Nacional
            new LatLng(19.4285, -99.1276)  // Fin de la ruta (Ej: Cerca de Zócalo)
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBusRouteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);

        // Configurar el SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        observeBusStatusUpdates();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Mapa listo.");

        if (routePoints.isEmpty()) {
            Log.e(TAG, "La lista de puntos de la ruta está vacía.");
            return;
        }

        // Dibuja la ruta y los marcadores
        drawRoutePolyline();
        addStartEndMarkers();
        zoomToRoute();

        // Verificar si hay una acción pendiente por el estado del bus
        Log.d(TAG, "Mapa listo. Verificando estado actual: " + currentBusState);
        handleMapActionForState(currentBusState, lastKnownBusLocation);
    }

    private void drawRoutePolyline() {
        if (mMap == null || routePoints.size() < 2) return;
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .color(ContextCompat.getColor(requireContext(), R.color.purple_500))
                .width(10);
        mMap.addPolyline(polylineOptions);
        Log.d(TAG, "Polilínea de la ruta dibujada.");
    }

    private void addStartEndMarkers() {
        if (mMap == null || routePoints.isEmpty()) return;
        mMap.addMarker(new MarkerOptions()
                .position(routePoints.get(0))
                .title("Inicio de la Ruta")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mMap.addMarker(new MarkerOptions()
                .position(routePoints.get(routePoints.size() - 1))
                .title("Fin de la Ruta")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        Log.d(TAG, "Marcadores de inicio y fin añadidos.");
    }

    private void zoomToRoute() {
        if (mMap == null || routePoints.isEmpty()) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        Log.d(TAG, "Zoom ajustado para mostrar la ruta completa.");
    }

    private void observeBusStatusUpdates() {
        // --- ESTA ES LA SECCIÓN CORREGIDA ---
        viewModel.getBusStatusUpdates().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    // Puedes mostrar un indicador de carga si lo deseas
                    break;
                case SUCCESS:
                    if (resource.getData() != null) {
                        Log.d(TAG, "observeBusStatusUpdates: Estado SUCCESS recibido.");
                        BusStatus busStatus = resource.getData(); // <-- EL CAMBIO CLAVE
                        String status = busStatus.getStatus();

                        LatLng newLatLng = null;
                        if (busStatus.getCurrentLocation() != null) {
                            GeoPoint geoPoint = busStatus.getCurrentLocation();
                            newLatLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            lastKnownBusLocation = newLatLng; // Guardar última ubicación
                        }

                        BusState newState = getBusStateFromString(status);
                        handleBusStatusUpdate(newState, newLatLng);
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "Error al observar estado del bus: " + resource.getMessage());
                    Toast.makeText(getContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        // --- FIN DE LA SECCIÓN CORREGIDA ---
    }

    private BusState getBusStateFromString(String status) {
        if (status == null) return BusState.UNKNOWN;
        switch (status) {
            case "ACTIVE":
                return BusState.ACTIVE;
            case "FINISHED":
                return BusState.FINISHED;
            case "STOPPED":
            default:
                return BusState.STOPPED;
        }
    }

    private void handleBusStatusUpdate(BusState newState, @Nullable LatLng newLocation) {
        if (newState == currentBusState) {
            // El estado no cambió, solo actualizar la ubicación si está activo
            if (newState == BusState.ACTIVE && newLocation != null) {
                animateBus(newLocation);
            }
            return;
        }

        Log.d(TAG, "Estado CAMBIADO de '" + currentBusState + "' a '" + newState + "'. Actualizando UI/Mapa.");
        currentBusState = newState;

        // Actualizar la UI (textos)
        updateUIForState(newState);

        // Si el mapa no está listo, la acción se ejecutará en onMapReady
        if (mMap == null) {
            Log.w(TAG, "El mapa aún no está listo. Acción para estado '" + newState + "' pospuesta para onMapReady.");
            return;
        }

        handleMapActionForState(newState, newLocation);
    }

    private void updateUIForState(BusState state) {
        TextView tvStatus = binding.tvBusStatus; // Asegúrate de tener este ID en tu XML
        switch (state) {
            case ACTIVE:
                tvStatus.setText("Recorrido en curso...");
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                break;
            case FINISHED:
                tvStatus.setText("El recorrido ha finalizado");
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500));
                break;
            case STOPPED:
                tvStatus.setText("El autobús está detenido");
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
                break;
            case UNKNOWN:
            default:
                tvStatus.setText("Consultando estado...");
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                break;
        }
        Log.d(TAG, "TextView actualizado a: " + tvStatus.getText());
    }

    private void handleMapActionForState(BusState state, @Nullable LatLng location) {
        Log.d(TAG, "Manejando estado '" + state + "' en el mapa (mapa listo).");

        // Detener animación anterior
        if (busAnimator != null) {
            busAnimator.cancel();
            Log.d(TAG, "Animación previa cancelada.");
        }

        switch (state) {
            case ACTIVE:
                if (busMarker == null) {
                    createBusMarker(location != null ? location : routePoints.get(0));
                }
                if (location != null) {
                    animateBus(location);
                } else {
                    // Si el estado es activo pero no hay location, usar el inicio de la ruta
                    animateBus(routePoints.get(0));
                }
                Log.d(TAG, "Iniciando animación del bus...");
                break;
            case FINISHED:
                if (busMarker != null) {
                    busMarker.setPosition(routePoints.get(routePoints.size() - 1));
                }
                break;
            case STOPPED:
            case UNKNOWN:
                if (busMarker != null) {
                    busMarker.setVisible(false);
                }
                break;
        }
    }

    private void createBusMarker(LatLng startPosition) {
        if (mMap == null) return;
        busMarker = mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("Autobús Escolar")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_marker)) // Asegúrate de tener este drawable
                .anchor(0.5f, 0.5f)
                .flat(true));
        Log.d(TAG, "Marcador del bus creado en lat/lng: " + startPosition.toString());
    }

    private void animateBus(LatLng newPosition) {
        if (busMarker == null) {
            createBusMarker(newPosition);
        }
        if (busMarker == null) return; // Si sigue siendo nulo, salir

        busMarker.setVisible(true);
        final LatLng startPosition = busMarker.getPosition();
        final LatLng endPosition = newPosition;

        if (busAnimator != null) {
            busAnimator.cancel();
        }

        // Usar un Handler para asegurar que la animación ocurra en el hilo principal
        new Handler(Looper.getMainLooper()).post(() -> {
            busAnimator = ValueAnimator.ofFloat(0, 1);
            busAnimator.setDuration(3000); // 3 segundos para la animación entre puntos
            busAnimator.setInterpolator(new LinearInterpolator());
            busAnimator.addUpdateListener(animator -> {
                try {
                    float v = animator.getAnimatedFraction();
                    double lat = (1 - v) * startPosition.latitude + v * endPosition.latitude;
                    double lng = (1 - v) * startPosition.longitude + v * endPosition.longitude;
                    LatLng interpolatedPosition = new LatLng(lat, lng);
                    busMarker.setPosition(interpolatedPosition);
                    busMarker.setRotation(getBearing(startPosition, endPosition));
                } catch (Exception e) {
                    Log.e(TAG, "Error durante la animación", e);
                    if (busAnimator != null) busAnimator.cancel();
                }
            });
            busAnimator.start();
            Log.d(TAG, "Animación iniciada (duración: 3000ms).");
        });
    }

    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Deteniendo animación y limpiando referencias.");
        if (busAnimator != null) {
            busAnimator.cancel();
            Log.d(TAG, "Animación cancelada.");
        }
        mMap = null;
        busMarker = null;
        binding = null;
    }
}