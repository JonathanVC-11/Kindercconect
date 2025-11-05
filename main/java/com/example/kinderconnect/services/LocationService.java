package com.example.kinderconnect.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent; // ¡Importante!
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.example.kinderconnect.R;
import com.example.kinderconnect.data.repository.BusTrackingRepository; // ¡Importante!
import com.example.kinderconnect.ui.teacher.TeacherMainActivity; // ¡Importante! Para el PendingIntent
import com.google.firebase.firestore.GeoPoint; // ¡Importante!

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1001; // ID único para la notificación foreground

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // --- ¡Añadido! ---
    private BusTrackingRepository busTrackingRepository;
    private boolean isServiceRunning = false; // Bandera para controlar inicio/parada

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true; // Marcar como iniciado al crearse
        Log.d(TAG, "Service created and marked as running.");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        busTrackingRepository = new BusTrackingRepository(); // Instanciar el repositorio

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (!isServiceRunning || locationResult == null) { // Verificar si el servicio debe estar corriendo
                    return;
                }

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    Log.d(TAG, "Nueva Ubicación Recibida: Lat=" + lastLocation.getLatitude() +
                            ", Lon=" + lastLocation.getLongitude() + ", Accuracy=" + lastLocation.getAccuracy());

                    // --- ¡ACTUALIZAR UBICACIÓN EN FIREBASE! ---
                    GeoPoint currentGeoPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                    updateLocationInFirebase(currentGeoPoint);
                    // ----------------------------------------
                } else {
                    Log.w(TAG, "LocationResult recibido pero getLastLocation es nulo.");
                }
            }
        };

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            Log.w(TAG, "onStartCommand llamado pero el servicio ya estaba marcado como detenido. Ignorando.");
            stopSelf(); // Asegurarse de que se detenga si se llama incorrectamente
            return START_NOT_STICKY;
        }

        Log.d(TAG, "Service started or restarted.");

        Notification notification = createNotification();
        // Asegurarse de que el tipo de servicio en primer plano sea location
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.d(TAG, "Servicio iniciado en primer plano.");
            startLocationUpdates(); // Iniciar actualizaciones *después* de startForeground
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar servicio en primer plano.", e);
            stopSelf(); // Detener si no se puede iniciar en primer plano
            return START_NOT_STICKY;
        }

        // START_STICKY: Si el sistema mata el servicio, lo intentará recrear,
        // llamando de nuevo a onStartCommand (sin el Intent original).
        return START_STICKY;
    }

    private void startLocationUpdates() {
        // Intervalo: 10 segundos, mínimo 5 segundos. Prioridad alta.
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 segundos
                .setMinUpdateIntervalMillis(5000)       // 5 segundos
                .build();

        // Verificar permisos DENTRO del método, justo antes de solicitar
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Permisos de ubicación no concedidos. No se pueden solicitar actualizaciones.");
            // Notificar al usuario o detener el servicio
            stopSelf(); // Detener el servicio si no hay permisos
            return;
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper() // Usar Looper principal está bien para actualizaciones no muy frecuentes
            );
            Log.d(TAG, "Solicitando actualizaciones de ubicación...");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException al solicitar actualizaciones de ubicación.", e);
            stopSelf(); // Detener si falla por seguridad
        }
    }

    private void updateLocationInFirebase(GeoPoint location) {
        if (!isServiceRunning) return; // No actualizar si el servicio está detenido

        if (busTrackingRepository != null) {
            Log.d(TAG, "Enviando ubicación a Firestore...");
            busTrackingRepository.updateBusLocation(location);
        } else {
            Log.e(TAG, "busTrackingRepository es nulo, no se puede actualizar la ubicación.");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Servicio de Ubicación del Bus", // Nombre más descriptivo
                    NotificationManager.IMPORTANCE_LOW // Baja importancia
            );
            channel.setDescription("Mantiene activa la actualización de la ubicación del autobús escolar.");
            channel.setSound(null, null); // Sin sonido

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Canal de notificación creado o ya existente.");
            } else {
                Log.e(TAG, "NotificationManager no disponible.");
            }
        }
    }

    private Notification createNotification() {
        // Intent para abrir la app (TeacherMainActivity) al tocar la notificación
        Intent notificationIntent = new Intent(this, TeacherMainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // Flag IMMUTABLE es importante

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("KinderConnect - Ruta del Bus Activa") // Título claro
                .setContentText("Enviando ubicación...") // Texto conciso
                .setSmallIcon(R.drawable.ic_bus) // Ícono relevante
                .setContentIntent(pendingIntent) // Acción al tocar
                .setPriority(NotificationCompat.PRIORITY_LOW) // Baja prioridad
                .setOngoing(true) // No se puede descartar deslizando
                .setSilent(true); // Sin sonido/vibración para esta notificación persistente

        return builder.build();
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false; // Marcar como detenido
        Log.d(TAG, "Service being destroyed and marked as stopped.");

        // Detener actualizaciones de ubicación
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Log.d(TAG, "Actualizaciones de ubicación detenidas.");
            } catch (Exception e) {
                Log.e(TAG, "Error al remover actualizaciones de ubicación.", e);
            }
        }
        // Asegurarse de quitar la notificación foreground
        stopForeground(true);
        Log.d(TAG, "Servicio detenido de primer plano.");

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // No usamos binding
        return null;
    }
}