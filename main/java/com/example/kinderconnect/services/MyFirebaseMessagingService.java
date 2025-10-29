package com.example.kinderconnect.services;

import android.app.NotificationManager;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.kinderconnect.R;
import com.example.kinderconnect.ui.auth.LoginActivity;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);

        // Aquí puedes enviar el token al servidor si es necesario
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Verificar si el mensaje contiene datos
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage);
        }

        // Verificar si el mensaje contiene notificación
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " +
                    remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage);
        }
    }

    private void handleDataMessage(RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get("type");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        if (type != null) {
            switch (type) {
                case "attendance":
                    showAttendanceNotification(title, body);
                    break;
                case "notice":
                    showNoticeNotification(title, body);
                    break;
                case "grade":
                    showGradeNotification(title, body);
                    break;
                default:
                    showDefaultNotification(title, body);
                    break;
            }
        }
    }

    private void handleNotificationMessage(RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        showDefaultNotification(title, body);
    }

    private void showAttendanceNotification(String title, String body) {
        Intent intent = new Intent(this, LoginActivity.class);
        NotificationHelper.showAttendanceNotification(
                this,
                body != null ? body : "Actualización de asistencia",
                Constants.ATTENDANCE_PRESENT,
                intent
        );
    }

    private void showNoticeNotification(String title, String body) {
        Intent intent = new Intent(this, LoginActivity.class);
        NotificationHelper.showNoticeNotification(
                this,
                title != null ? title : "Nuevo aviso",
                body != null ? body : "Tienes un nuevo aviso escolar",
                intent
        );
    }

    private void showGradeNotification(String title, String body) {
        Intent intent = new Intent(this, LoginActivity.class);
        NotificationHelper.showNoticeNotification(
                this,
                title != null ? title : "Nuevas calificaciones",
                body != null ? body : "Se han publicado nuevas calificaciones",
                intent
        );
    }

    private void showDefaultNotification(String title, String body) {
        Intent intent = new Intent(this, LoginActivity.class);
        NotificationHelper.showNoticeNotification(
                this,
                title != null ? title : "KinderConnect",
                body != null ? body : "Tienes una nueva notificación",
                intent
        );
    }

    private void sendTokenToServer(String token) {
        // Aquí puedes implementar la lógica para enviar el token al servidor
        // Por ejemplo, guardarlo en Firestore asociado al usuario actual
        Log.d(TAG, "Sending token to server: " + token);
    }
}
