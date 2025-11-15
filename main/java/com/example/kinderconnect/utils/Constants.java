package com.example.kinderconnect.utils;

import com.google.firebase.firestore.GeoPoint;
import java.util.Arrays;
import java.util.List;


public class Constants {
    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_STUDENTS = "students";
    public static final String COLLECTION_GRADES = "grades";
    public static final String COLLECTION_ATTENDANCE = "attendance";
    public static final String COLLECTION_NOTICES = "notices";
    public static final String COLLECTION_GALLERY = "gallery";

    // User Types
    public static final String USER_TYPE_TEACHER = "TEACHER";
    public static final String USER_TYPE_PARENT = "PARENT";

    // Attendance Status
    public static final String ATTENDANCE_PRESENT = "PRESENT";
    public static final String ATTENDANCE_LATE = "LATE";
    public static final String ATTENDANCE_ABSENT = "ABSENT";

    // Grade Levels
    public static final String GRADE_REQUIERE_APOYO = "REQUIERE_APOYO";
    public static final String GRADE_EN_DESARROLLO = "EN_DESARROLLO";
    public static final String GRADE_ESPERADO = "ESPERADO";
    public static final String GRADE_SOBRESALIENTE = "SOBRESALIENTE";

    // Notice Categories
    public static final String NOTICE_TAREA = "TAREA";
    public static final String NOTICE_EVENTO = "EVENTO";
    public static final String NOTICE_RECORDATORIO = "RECORDATORIO";
    public static final String NOTICE_URGENTE = "URGENTE";

    // Notice Scope
    public static final String SCOPE_GROUP = "GROUP";
    public static final String SCOPE_SCHOOL = "SCHOOL";

    // Media Types
    public static final String MEDIA_IMAGE = "IMAGE";
    public static final String MEDIA_VIDEO = "VIDEO";

    // Storage Paths
    public static final String STORAGE_STUDENTS = "students/";
    public static final String STORAGE_GALLERY = "gallery/";
    public static final String STORAGE_NOTICES = "notices/";
    public static final String STORAGE_PROFILES = "profiles/";

    // SharedPreferences Keys
    public static final String PREF_NAME = "KinderConnectPrefs";
    public static final String PREF_USER_TYPE = "user_type";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";

    // Request Codes
    public static final int REQUEST_IMAGE_CAPTURE = 1001;
    public static final int REQUEST_IMAGE_PICK = 1002;
    public static final int REQUEST_VIDEO_CAPTURE = 1003;
    public static final int REQUEST_VIDEO_PICK = 1004;
    public static final int REQUEST_LOCATION_PERMISSION = 1005;
    public static final int REQUEST_CAMERA_PERMISSION = 1006;
    public static final int REQUEST_STORAGE_PERMISSION = 1007;

    // Development Areas
    public static final String[] DEVELOPMENT_AREAS = {
            "Lenguaje y Comunicación",
            "Pensamiento Matemático",
            "Exploración del Mundo Natural",
            "Desarrollo Personal y Social",
            "Expresión Artística",
            "Desarrollo Físico y Salud"
    };

    // Notification Channels
    public static final String CHANNEL_ATTENDANCE = "attendance_channel";
    public static final String CHANNEL_NOTICES = "notices_channel";
    public static final String CHANNEL_ALERTS = "alerts_channel";

    // WorkManager Tags
    public static final String WORK_TAG_SYNC = "sync_work";
    public static final String WORK_TAG_NOTIFICATION = "notification_work";

    // Periods
    public static final int PERIOD_1 = 1;
    public static final int PERIOD_2 = 2;
    public static final int PERIOD_3 = 3;


    // --- RUTA DE SIMULACIÓN MODIFICADA (MÁS CORTA Y VISIBLE) ---
    public static final List<GeoPoint> SIMULATION_ROUTE = Arrays.asList(
            new GeoPoint(19.17463,-99.46443),
            new GeoPoint(19.17476,-99.46463),
            new GeoPoint(19.17522,-99.46464),
            new GeoPoint(19.17530,-99.46471),
            new GeoPoint(19.17533,-99.46480),
            new GeoPoint(19.17544,-99.46535),
            new GeoPoint(19.17718,-99.46505),
            new GeoPoint(19.17743,-99.46629),
            new GeoPoint(19.18008,-99.46587),
            new GeoPoint(19.17989,-99.46458),
            new GeoPoint(19.18280,-99.46399),
            new GeoPoint(19.18395,-99.47119),
            new GeoPoint(19.18097,-99.47184),
            new GeoPoint(19.18167,-99.47865),
            new GeoPoint(19.18310,-99.48763),
            new GeoPoint(19.17672,-99.48926),
            new GeoPoint(19.17640,-99.48773),
            new GeoPoint(19.17571,-99.48717),
            new GeoPoint(19.17511,-99.48727),
            new GeoPoint(19.17165,-99.48678),
            new GeoPoint(19.16035,-99.48756),
            new GeoPoint(19.15767,-99.48781),
            new GeoPoint(19.15654,-99.48731),
            new GeoPoint(19.15315,-99.48783),
            new GeoPoint(19.15126,-99.48801),
            new GeoPoint(19.14495,-99.48726),
            new GeoPoint(19.14312,-99.48622),
            new GeoPoint(19.14736,-99.48220),
            new GeoPoint(19.15577,-99.47451),
            new GeoPoint(19.16184,-99.46460),
            new GeoPoint(19.17270,-99.46713),
            new GeoPoint(19.17472,-99.46676),
            new GeoPoint(19.17448,-99.46467),
            new GeoPoint(19.17448,-99.46467),
            new GeoPoint(19.17472,-99.46463)
    );
    // -----------------------------------------------------------
}