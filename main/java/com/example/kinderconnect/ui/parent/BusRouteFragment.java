package com.example.kinderconnect.ui.parent;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;


import com.example.kinderconnect.R;
import com.example.kinderconnect.data.model.BusStatus;
import com.example.kinderconnect.databinding.FragmentBusRouteBinding;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BusRouteFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "BusRouteFragment";
    private enum BusState { UNKNOWN, STOPPED, ACTIVE, FINISHED }

    private FragmentBusRouteBinding binding;
    private ParentViewModel viewModel;
    private GoogleMap mMap;
    private Marker busMarker;
    private ValueAnimator busAnimator;
    private BusState currentBusState = BusState.UNKNOWN;
    private LatLng lastKnownBusLocation = null;

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

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        observeBusStatusUpdates();
    }

    private List<LatLng> getRoutePoints() {
        List<LatLng> points = new ArrayList<>();
        if (Constants.SIMULATION_ROUTE == null) return points;

        for (GeoPoint geoPoint : Constants.SIMULATION_ROUTE) {
            points.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
        }
        return points;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Mapa listo.");

        if (getRoutePoints().isEmpty()) {
            Log.e(TAG, "La lista de puntos de la ruta está vacía.");
            return;
        }

        drawRoutePolyline();
        addStartEndMarkers();
        zoomToRoute(); // <-- MÉTODO MODIFICADO

        Log.d(TAG, "Mapa listo. Verificando estado actual: " + currentBusState);
        handleMapActionForState(currentBusState, lastKnownBusLocation);
    }

    private void drawRoutePolyline() {
        if (mMap == null || getRoutePoints().size() < 2 || getContext() == null) return;
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(getRoutePoints())
                .color(ContextCompat.getColor(requireContext(), R.color.purple_500))
                .width(10);
        mMap.addPolyline(polylineOptions);
        Log.d(TAG, "Polilínea de la ruta dibujada.");
    }

    private void addStartEndMarkers() {
        if (mMap == null || getRoutePoints().isEmpty()) return;
        mMap.addMarker(new MarkerOptions()
                .position(getRoutePoints().get(0))
                .title("Inicio de la Ruta")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mMap.addMarker(new MarkerOptions()
                .position(getRoutePoints().get(getRoutePoints().size() - 1))
                .title("Fin de la Ruta")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        Log.d(TAG, "Marcadores de inicio y fin añadidos.");
    }

    // --- LÓGICA DE ZOOM MODIFICADA ---
    private void zoomToRoute() {
        if (mMap == null || getRoutePoints().isEmpty()) return;

        // Antes: Zoom para ver TODA la ruta (muy lejos)
        /*
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : getRoutePoints()) {
            builder.include(point);
        }
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error al mover la cámara, el mapa puede no estar listo: " + e.getMessage());
        }
        */

        // Ahora: Zoom CERCANO al primer punto (nivel 17)
        try {
            LatLng startPoint = getRoutePoints().get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 17f));
            Log.d(TAG, "Zoom ajustado al punto de inicio (Nivel 17).");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error al mover la cámara: " + e.getMessage());
        }
    }
    // ---------------------------------

    private void observeBusStatusUpdates() {
        viewModel.getBusStatusUpdates().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    break;
                case SUCCESS:
                    if (resource.getData() != null) {
                        Log.d(TAG, "observeBusStatusUpdates: Estado SUCCESS recibido.");
                        BusStatus busStatus = resource.getData();
                        String status = busStatus.getStatus();

                        LatLng newLatLng = null;
                        if (busStatus.getCurrentLocation() != null) {
                            GeoPoint geoPoint = busStatus.getCurrentLocation();
                            newLatLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            lastKnownBusLocation = newLatLng;
                        }

                        BusState newState = getBusStateFromString(status);
                        handleBusStatusUpdate(newState, newLatLng);
                    } else {
                        Log.w(TAG, "Datos de BusStatus recibidos pero son nulos.");
                        handleBusStatusUpdate(BusState.UNKNOWN, null);
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "Error al observar estado del bus: " + resource.getMessage());
                    if (binding != null) {
                        binding.tvBusStatus.setText(resource.getMessage());
                        binding.tvBusStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
                    }
                    handleBusStatusUpdate(BusState.UNKNOWN, null);
                    break;
            }
        });
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
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "handleBusStatusUpdate llamado pero el fragmento no está adjunto.");
            return;
        }

        if (newState == currentBusState && newState == BusState.ACTIVE && newLocation != null) {
            animateBus(newLocation);
            return;
        }

        if (newState == currentBusState) {
            return;
        }


        Log.d(TAG, "Estado CAMBIADO de '" + currentBusState + "' a '" + newState + "'. Actualizando UI/Mapa.");
        currentBusState = newState;

        updateUIForState(newState);

        if (mMap == null) {
            Log.w(TAG, "El mapa aún no está listo. Acción para estado '" + newState + "' pospuesta para onMapReady.");
            return;
        }

        handleMapActionForState(newState, newLocation);
    }

    private void updateUIForState(BusState state) {
        if (binding == null || getContext() == null) return;
        TextView tvStatus = binding.tvBusStatus;
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
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_500));
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
        if (!isAdded() || mMap == null) {
            Log.w(TAG, "handleMapActionForState llamado pero el fragmento no está adjunto o el mapa es nulo.");
            return;
        }

        Log.d(TAG, "Manejando estado '" + state + "' en el mapa (mapa listo).");

        if (busAnimator != null) {
            busAnimator.cancel();
            Log.d(TAG, "Animación previa cancelada.");
        }

        LatLng targetPosition = location;

        switch (state) {
            case ACTIVE:
                if (targetPosition == null) {
                    targetPosition = (lastKnownBusLocation != null) ? lastKnownBusLocation : getRoutePoints().get(0);
                    Log.w(TAG,"Estado ACTIVE pero sin nueva ubicación, usando: " + targetPosition);
                }

                if (busMarker == null) {
                    createBusMarker(targetPosition);
                }
                animateBus(targetPosition);
                Log.d(TAG, "Estado ACTIVE: Iniciando/actualizando animación del bus a: " + targetPosition);
                break;
            case FINISHED:
                targetPosition = getRoutePoints().get(getRoutePoints().size() - 1);
                if (busMarker != null) {
                    busMarker.setPosition(targetPosition);
                    busMarker.setVisible(true);
                } else {
                    createBusMarker(targetPosition);
                }
                Log.d(TAG, "Estado FINISHED: Moviendo marcador al final: " + targetPosition);
                break;
            case STOPPED:
                if (targetPosition == null) {
                    targetPosition = lastKnownBusLocation;
                }

                if(targetPosition != null) {
                    if (busMarker == null) {
                        createBusMarker(targetPosition);
                    } else {
                        busMarker.setPosition(targetPosition);
                        busMarker.setVisible(true);
                    }
                    Log.d(TAG, "Estado STOPPED: Posicionando marcador en: " + targetPosition);
                } else {
                    if (busMarker != null) {
                        busMarker.setVisible(false);
                    }
                    Log.d(TAG, "Estado STOPPED: Sin ubicación conocida, ocultando marcador.");
                }
                break;
            case UNKNOWN:
                if (busMarker != null) {
                    busMarker.setVisible(false);
                }
                Log.d(TAG, "Estado UNKNOWN: Ocultando marcador.");
                break;

        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable == null) {
            Log.e(TAG, "Vector drawable no encontrado: " + vectorResId);
            return BitmapDescriptorFactory.defaultMarker();
        }
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    private void createBusMarker(LatLng startPosition) {
        if (mMap == null || getContext() == null) return;

        BitmapDescriptor icon = bitmapDescriptorFromVector(requireContext(), R.drawable.ic_bus_marker);

        busMarker = mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("Autobús Escolar")
                .icon(icon)
                .anchor(0.5f, 0.5f)
                .flat(true));
        Log.d(TAG, "Marcador del bus creado en lat/lng: " + startPosition.toString());
    }

    // --- LÓGICA DE ANIMACIÓN MODIFICADA (PARA SEGUIR CON LA CÁMARA) ---
    private void animateBus(LatLng newPosition) {
        if (!isAdded() || getContext() == null || busMarker == null) {
            if (mMap != null && busMarker == null) {
                Log.w(TAG,"animateBus llamado pero el marcador era nulo. Creando en: " + newPosition);
                createBusMarker(newPosition);
                if (busMarker == null) {
                    Log.e(TAG, "Fallo al crear el marcador en animateBus.");
                    return;
                }
            } else if (busMarker == null) {
                Log.w(TAG, "animateBus llamado pero el marcador es nulo y/o el fragmento no está adjunto.");
                return;
            }
        }


        busMarker.setVisible(true);
        final LatLng startPosition = busMarker.getPosition();
        final LatLng endPosition = newPosition;

        if (Math.abs(startPosition.latitude - endPosition.latitude) < 0.00001 &&
                Math.abs(startPosition.longitude - endPosition.longitude) < 0.00001) {
            busMarker.setPosition(endPosition);
            // Mover la cámara también, incluso si no hay animación
            if(mMap != null) mMap.moveCamera(CameraUpdateFactory.newLatLng(endPosition));
            return;
        }

        if (busAnimator != null) {
            busAnimator.cancel();
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            if (busMarker == null || !isAdded()) return;

            busAnimator = ValueAnimator.ofFloat(0, 1);
            busAnimator.setDuration(3000); // 3 segundos para animar (esto es la animación del *marcador*)
            busAnimator.setInterpolator(new LinearInterpolator());
            busAnimator.addUpdateListener(animator -> {
                try {
                    if (busMarker == null) {
                        animator.cancel();
                        return;
                    }
                    float v = animator.getAnimatedFraction();
                    double lat = (1 - v) * startPosition.latitude + v * endPosition.latitude;
                    double lng = (1 - v) * startPosition.longitude + v * endPosition.longitude;
                    LatLng interpolatedPosition = new LatLng(lat, lng);

                    busMarker.setPosition(interpolatedPosition);

                    if (Math.abs(startPosition.latitude - endPosition.latitude) > 0.00001 ||
                            Math.abs(startPosition.longitude - endPosition.longitude) > 0.00001) {
                        busMarker.setRotation(getBearing(startPosition, endPosition));
                    }

                    // --- ESTA ES LA LÍNEA MÁGICA ---
                    // Mueve la cámara para que siga al marcador en cada frame de la animación
                    if (mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(interpolatedPosition));
                    }
                    // ---------------------------------

                } catch (Exception e) {
                    Log.e(TAG, "Error durante la animación del bus", e);
                    if (busAnimator != null) busAnimator.cancel();
                }
            });
            busAnimator.start();
            Log.d(TAG, "Animación iniciada de " + startPosition + " a " + endPosition);
        });
    }


    private float getBearing(LatLng begin, LatLng end) {
        double lat1 = Math.toRadians(begin.latitude);
        double lon1 = Math.toRadians(begin.longitude);
        double lat2 = Math.toRadians(end.latitude);
        double lon2 = Math.toRadians(end.longitude);

        double longitudeDifference = lon2 - lon1;

        double y = Math.sin(longitudeDifference) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(longitudeDifference);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float)(bearing + 360) % 360;
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