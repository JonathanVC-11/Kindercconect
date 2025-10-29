package com.example.kinderconnect.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.kinderconnect.utils.NotificationHelper;
import android.util.Log;

public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Processing notifications");

        try {
            // Crear canales de notificación si no existen
            NotificationHelper.createNotificationChannels(getApplicationContext());

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error processing notifications: " + e.getMessage());
            return Result.failure();
        }
    }
}
