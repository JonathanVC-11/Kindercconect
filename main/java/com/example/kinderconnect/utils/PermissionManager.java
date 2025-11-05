package com.example.kinderconnect.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList; // ¡Importante!

public class PermissionManager {

    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // --- ¡MÉTODO MODIFICADO! ---
    public static boolean hasLocationPermission(Context context) {
        boolean hasBaseLocation = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // En Android 14 (API 34) y superior, también necesitamos FOREGROUND_SERVICE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            return hasBaseLocation && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        // En versiones anteriores, solo se necesita el permiso de ubicación base
        return hasBaseLocation;
    }

    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                Constants.REQUEST_CAMERA_PERMISSION);
    }

    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                    },
                    Constants.REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    Constants.REQUEST_STORAGE_PERMISSION);
        }
    }

    // --- ¡MÉTODO MODIFICADO! ---
    public static void requestLocationPermission(Activity activity) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        // Permisos de ubicación base (siempre necesarios)
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // Permiso de servicio en primer plano (API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        }
        // Permiso de servicio en primer plano (General, para APIs 28+)
        // Aunque el de ubicación es el crítico para el crash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        ActivityCompat.requestPermissions(activity,
                permissionsToRequest.toArray(new String[0]),
                Constants.REQUEST_LOCATION_PERMISSION);
    }

    public static boolean hasAllMediaPermissions(Context context) {
        return hasCameraPermission(context) && hasStoragePermission(context);
    }
}