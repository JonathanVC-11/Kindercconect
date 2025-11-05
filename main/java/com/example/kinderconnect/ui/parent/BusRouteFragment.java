package com.example.kinderconnect.ui.parent;

import android.animation.ValueAnimator;
import android.content.Context; // ¡Añadido!
import android.graphics.Bitmap; // ¡Añadido!
import android.graphics.Canvas; // ¡Añadido!
import android.graphics.drawable.Drawable; // ¡Añadido!
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

import androidx.annotation.DrawableRes; // ¡Añadido!
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat; // ¡Añadido!


import com.example.kinderconnect.R;
import com.example.kinderconnect.data.model.BusStatus;
import com.example.kinderconnect.databinding.FragmentBusRouteBinding;
import com.example.kinderconnect.utils.Resource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor; // ¡Añadido!
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
    private enum BusState { UNKNOWN, STOPPED, ACTIVE, FINISHED }

    private FragmentBusRouteBinding binding;
    private ParentViewModel viewModel;
    private GoogleMap mMap;
    private Marker busMarker;
    private ValueAnimator busAnimator;
    private BusState currentBusState = BusState.UNKNOWN;
    private LatLng lastKnownBusLocation = null;

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

        drawRoutePolyline();
        addStartEndMarkers();
        zoomToRoute();

        Log.d(TAG, "Mapa listo. Verificando estado actual: " + currentBusState);
        handleMapActionForState(currentBusState, lastKnownBusLocation);
    }

    private void drawRoutePolyline() {
        if (mMap == null || routePoints.size() < 2 || getContext() == null) return;
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
        try {
            // Agregamos un try-catch por si el mapa se destruye justo antes de mover la cámara
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            Log.d(TAG, "Zoom ajustado para mostrar la ruta completa.");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error al mover la cámara, el mapa puede no estar listo: " + e.getMessage());
        }

    }

    private void observeBusStatusUpdates() {
        viewModel.getBusStatusUpdates().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    //binding.progressStatus.setVisibility(View.VISIBLE); // Opcional
                    break;
                case SUCCESS:
                    //binding.progressStatus.setVisibility(View.GONE); // Opcional
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
                        // Manejar caso donde el documento existe pero no tiene datos esperados?
                        Log.w(TAG, "Datos de BusStatus recibidos pero son nulos.");
                        handleBusStatusUpdate(BusState.UNKNOWN, null); // Volver a estado desconocido
                    }
                    break;
                case ERROR:
                    //binding.progressStatus.setVisibility(View.GONE); // Opcional
                    Log.e(TAG, "Error al observar estado del bus: " + resource.getMessage());
                    // Mostrar error en UI en lugar de Toast que desaparece
                    if (binding != null) {
                        binding.tvBusStatus.setText(resource.getMessage());
                        binding.tvBusStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
                    }
                    //Toast.makeText(getContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                    handleBusStatusUpdate(BusState.UNKNOWN, null); // Indicar estado desconocido en mapa
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
            case "STOPPED": // Asumimos STOPPED si no es ACTIVE o FINISHED
            default:
                return BusState.STOPPED; // Cambiado de UNKNOWN a STOPPED como default razonable
        }
    }


    private void handleBusStatusUpdate(BusState newState, @Nullable LatLng newLocation) {
        // Salir si el fragmento ya no está adjunto a una actividad
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "handleBusStatusUpdate llamado pero el fragmento no está adjunto.");
            return;
        }

        if (newState == currentBusState && newState == BusState.ACTIVE && newLocation != null) {
            // El estado no cambió, pero si está activo y hay nueva ubicación, animar
            animateBus(newLocation);
            return; // No necesita actualizar UI ni otras acciones del mapa
        }

        if (newState == currentBusState) {
            // Si el estado no cambió y no es un movimiento activo, no hacer nada más
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
        if (binding == null || getContext() == null) return; // Chequeo extra
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
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_500)); // Usar naranja para detenido
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
            return; // Salir si el fragmento o el mapa ya no existen
        }

        Log.d(TAG, "Manejando estado '" + state + "' en el mapa (mapa listo).");

        if (busAnimator != null) {
            busAnimator.cancel();
            Log.d(TAG, "Animación previa cancelada.");
        }

        LatLng targetPosition = location; // Usar la ubicación recibida si existe

        switch (state) {
            case ACTIVE:
                if (targetPosition == null) {
                    // Si está activo pero no hay ubicación, usar la última conocida o el inicio
                    targetPosition = (lastKnownBusLocation != null) ? lastKnownBusLocation : routePoints.get(0);
                    Log.w(TAG,"Estado ACTIVE pero sin nueva ubicación, usando: " + targetPosition);
                }

                if (busMarker == null) {
                    createBusMarker(targetPosition); // Crear en la posición actual
                }
                // Animar siempre si está activo (incluso si la posición no cambió, para asegurar visibilidad)
                animateBus(targetPosition);
                Log.d(TAG, "Estado ACTIVE: Iniciando/actualizando animación del bus a: " + targetPosition);
                break;
            case FINISHED:
                // Mover el marcador al final de la ruta si existe, sino ocultarlo
                targetPosition = routePoints.get(routePoints.size() - 1);
                if (busMarker != null) {
                    busMarker.setPosition(targetPosition);
                    busMarker.setVisible(true); // Asegurar que sea visible al finalizar
                } else {
                    createBusMarker(targetPosition); // Crear en el punto final
                }
                Log.d(TAG, "Estado FINISHED: Moviendo marcador al final: " + targetPosition);
                break;
            case STOPPED:
                if (targetPosition == null) {
                    // Si está detenido y no hay ubicación, usar la última conocida o ocultar
                    targetPosition = lastKnownBusLocation;
                }

                if(targetPosition != null) {
                    if (busMarker == null) {
                        createBusMarker(targetPosition);
                    } else {
                        busMarker.setPosition(targetPosition); // Mover a la última posición sin animar
                        busMarker.setVisible(true); // Asegurar visibilidad
                    }
                    Log.d(TAG, "Estado STOPPED: Posicionando marcador en: " + targetPosition);
                } else {
                    // Si está detenido y no tenemos ninguna ubicación, ocultar
                    if (busMarker != null) {
                        busMarker.setVisible(false);
                    }
                    Log.d(TAG, "Estado STOPPED: Sin ubicación conocida, ocultando marcador.");
                }
                break;
            case UNKNOWN:
                // Ocultar marcador si el estado es desconocido
                if (busMarker != null) {
                    busMarker.setVisible(false);
                }
                Log.d(TAG, "Estado UNKNOWN: Ocultando marcador.");
                break;

        }
    }

    // --- ¡¡NUEVA FUNCIÓN DE AYUDA!! ---
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable == null) {
            Log.e(TAG, "Vector drawable no encontrado: " + vectorResId);
            // Fallback a un marcador default si no se encuentra el drawable
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

        // --- ¡¡LÍNEA MODIFICADA AQUÍ!! ---
        BitmapDescriptor icon = bitmapDescriptorFromVector(requireContext(), R.drawable.ic_bus_marker);

        busMarker = mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("Autobús Escolar")
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_marker)) // <-- Línea original que fallaba
                .icon(icon) // <-- Usamos el BitmapDescriptor generado
                .anchor(0.5f, 0.5f)
                .flat(true));
        Log.d(TAG, "Marcador del bus creado en lat/lng: " + startPosition.toString());
    }

    private void animateBus(LatLng newPosition) {
        // Asegurarse que el fragmento esté añadido y el contexto disponible
        if (!isAdded() || getContext() == null || busMarker == null) {
            // Si el marcador no existe aún (puede pasar si el estado cambió antes de onMapReady),
            // lo creamos ahora en la nueva posición.
            if (mMap != null && busMarker == null) {
                Log.w(TAG,"animateBus llamado pero el marcador era nulo. Creando en: " + newPosition);
                createBusMarker(newPosition);
                if (busMarker == null) { // Si aún así falla la creación, salir
                    Log.e(TAG, "Fallo al crear el marcador en animateBus.");
                    return;
                }
            } else if (busMarker == null) {
                Log.w(TAG, "animateBus llamado pero el marcador es nulo y/o el fragmento no está adjunto.");
                return; // Salir si no se puede crear o el fragmento no está listo
            }
        }


        busMarker.setVisible(true);
        final LatLng startPosition = busMarker.getPosition();
        final LatLng endPosition = newPosition;

        // Si la posición inicial y final son muy cercanas, solo mover, no animar
        if (Math.abs(startPosition.latitude - endPosition.latitude) < 0.00001 &&
                Math.abs(startPosition.longitude - endPosition.longitude) < 0.00001) {
            busMarker.setPosition(endPosition);
            return;
        }

        if (busAnimator != null) {
            busAnimator.cancel();
        }

        // Usar post para asegurar ejecución en UI thread
        new Handler(Looper.getMainLooper()).post(() -> {
            // Chequeo adicional dentro del post por si el estado cambió mientras esperaba
            if (busMarker == null || !isAdded()) return;

            busAnimator = ValueAnimator.ofFloat(0, 1);
            busAnimator.setDuration(3000); // 3 segundos para animar
            busAnimator.setInterpolator(new LinearInterpolator());
            busAnimator.addUpdateListener(animator -> {
                // Try-catch para evitar crashes si el marcador se vuelve nulo durante la animación
                try {
                    if (busMarker == null) {
                        animator.cancel(); // Detener si el marcador desapareció
                        return;
                    }
                    float v = animator.getAnimatedFraction();
                    double lat = (1 - v) * startPosition.latitude + v * endPosition.latitude;
                    double lng = (1 - v) * startPosition.longitude + v * endPosition.longitude;
                    LatLng interpolatedPosition = new LatLng(lat, lng);
                    busMarker.setPosition(interpolatedPosition);
                    // Solo rotar si hay un cambio significativo de posición para evitar rotación errática
                    if (Math.abs(startPosition.latitude - endPosition.latitude) > 0.00001 ||
                            Math.abs(startPosition.longitude - endPosition.longitude) > 0.00001) {
                        busMarker.setRotation(getBearing(startPosition, endPosition));
                    }
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
        // Corrección para evitar división por cero y manejar cálculo de ángulo
        double lat1 = Math.toRadians(begin.latitude);
        double lon1 = Math.toRadians(begin.longitude);
        double lat2 = Math.toRadians(end.latitude);
        double lon2 = Math.toRadians(end.longitude);

        double longitudeDifference = lon2 - lon1;

        double y = Math.sin(longitudeDifference) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(longitudeDifference);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        // Normalizar a 0-360 grados
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
        mMap = null; // Liberar referencia al mapa
        busMarker = null; // Liberar referencia al marcador
        binding = null; // ¡Importante! Liberar binding
    }
}