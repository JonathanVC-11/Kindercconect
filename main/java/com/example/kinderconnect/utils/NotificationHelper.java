package com.example.kinderconnect.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.kinderconnect.R;

public class NotificationHelper {

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            // Canal de Asistencia
            NotificationChannel attendanceChannel = new NotificationChannel(
                    Constants.CHANNEL_ATTENDANCE,
                    "Asistencia",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            attendanceChannel.setDescription("Notificaciones de asistencia de alumnos");
            notificationManager.createNotificationChannel(attendanceChannel);

            // Canal de Avisos
            NotificationChannel noticesChannel = new NotificationChannel(
                    Constants.CHANNEL_NOTICES,
                    "Avisos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            noticesChannel.setDescription("Notificaciones de avisos escolares");
            notificationManager.createNotificationChannel(noticesChannel);

            // Canal de Alertas
            NotificationChannel alertsChannel = new NotificationChannel(
                    Constants.CHANNEL_ALERTS,
                    "Alertas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("Alertas importantes de la escuela");
            notificationManager.createNotificationChannel(alertsChannel);
        }
    }

    public static void showAttendanceNotification(Context context, String studentName,
                                                  String status, Intent intent) {
        String title = "Asistencia registrada";
        String message = studentName + " - " + getStatusText(status);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, Constants.CHANNEL_ATTENDANCE)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public static void showNoticeNotification(Context context, String title,
                                              String message, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, Constants.CHANNEL_NOTICES)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private static String getStatusText(String status) {
        switch (status) {
            case Constants.ATTENDANCE_PRESENT:
                return "Asistió";
            case Constants.ATTENDANCE_LATE:
                return "Retardo";
            case Constants.ATTENDANCE_ABSENT:
                return "Falta";
            default:
                return "";
        }
    }
}
