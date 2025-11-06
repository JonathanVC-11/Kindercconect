package com.example.kinderconnect.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.kinderconnect.R;
import com.example.kinderconnect.data.repository.BusTrackingRepository;
import com.example.kinderconnect.ui.teacher.TeacherMainActivity;
import com.google.firebase.firestore.GeoPoint;

import android.os.Handler;
import com.example.kinderconnect.utils.Constants;
import java.util.List;


public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private Handler simulationHandler;
    private Runnable simulationRunnable;
    private List<GeoPoint> routePoints;
    private int currentRouteIndex;

    // --- TIEMPO DE INTERVALO MODIFICADO ---
    // Antes: 180000 (3 minutos)
    // Ahora: 5000 (5 segundos) para pruebas rápidas
    private static final long SIMULATION_INTERVAL_MS = 5000;
    // ---------------------------------------

    private BusTrackingRepository busTrackingRepository;
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        Log.d(TAG, "Service created and marked as running.");

        busTrackingRepository = new BusTrackingRepository();
        simulationHandler = new Handler(Looper.getMainLooper());
        routePoints = Constants.SIMULATION_ROUTE; // Carga la ruta desde Constants
        currentRouteIndex = 0;

        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning || routePoints == null || routePoints.isEmpty()) {
                    return;
                }

                GeoPoint currentPoint = routePoints.get(currentRouteIndex);

                Log.d(TAG, "Simulación: Enviando punto " + currentRouteIndex + ": " + currentPoint);
                updateLocationInFirebase(currentPoint);

                currentRouteIndex++;

                if (currentRouteIndex < routePoints.size()) {
                    simulationHandler.postDelayed(this, SIMULATION_INTERVAL_MS);
                } else {
                    Log.d(TAG, "Simulación: Ruta completada.");
                    // Si quieres que la ruta se repita en bucle, descomenta la siguiente línea:
                    // currentRouteIndex = 0;
                    // simulationHandler.postDelayed(this, SIMULATION_INTERVAL_MS);
                }
            }
        };

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            Log.w(TAG, "onStartCommand llamado pero el servicio ya estaba marcado como detenido. Ignorando.");
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.d(TAG, "Service started or restarted.");

        Notification notification = createNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.d(TAG, "Servicio iniciado en primer plano.");

            startSimulation(); // Iniciar nuestra simulación

        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar servicio en primer plano.", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }


    private void startSimulation() {
        if (simulationHandler != null && simulationRunnable != null) {
            Log.d(TAG, "Iniciando simulación de ruta...");
            currentRouteIndex = 0; // Reiniciar al inicio
            simulationHandler.removeCallbacks(simulationRunnable); // Quitar callbacks antiguos
            simulationHandler.post(simulationRunnable); // Empezar el bucle
        }
    }

    private void updateLocationInFirebase(GeoPoint location) {
        if (!isServiceRunning) return;

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
                    "Servicio de Ubicación del Bus",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mantiene activa la actualización de la ubicación del autobús escolar.");
            channel.setSound(null, null);

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
        Intent notificationIntent = new Intent(this, TeacherMainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("KinderConnect - Ruta del Bus Activa")
                .setContentText("Enviando ubicación (Simulación)...")
                .setSmallIcon(R.drawable.ic_bus)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        Log.d(TAG, "Service being destroyed and marked as stopped.");

        if (simulationHandler != null && simulationRunnable != null) {
            simulationHandler.removeCallbacks(simulationRunnable); // Detener el bucle
            Log.d(TAG, "Simulación detenida.");
        }

        stopForeground(true);
        Log.d(TAG, "Servicio detenido de primer plano.");

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}