package com.example.kinderconnect;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.NotificationHelper;
import com.example.kinderconnect.workers.SyncWorker;
import java.util.concurrent.TimeUnit;

public class KinderConnectApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Crear canales de notificación
        NotificationHelper.createNotificationChannels(this);

        // Configurar sincronización periódica
        setupPeriodicSync();
    }

    private void setupPeriodicSync() {
        // Configurar restricciones
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        // Crear trabajo periódico (cada 15 minutos es el mínimo)
        PeriodicWorkRequest syncWorkRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag(Constants.WORK_TAG_SYNC)
                        .build();

        // Encolar el trabajo
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_SYNC,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
        );
    }
}
