package com.example.kinderconnect.utils;

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
}
