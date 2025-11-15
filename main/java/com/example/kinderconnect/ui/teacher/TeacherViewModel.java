package com.example.kinderconnect.ui.teacher;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import com.example.kinderconnect.data.model.*;
import com.example.kinderconnect.data.repository.*;
import com.example.kinderconnect.utils.Resource;
import com.google.firebase.firestore.GeoPoint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherViewModel extends ViewModel {
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final NoticeRepository noticeRepository;
    private final GalleryRepository galleryRepository;
    private final BusTrackingRepository busTrackingRepository;
    // --- INICIO DE CÓDIGO AÑADIDO ---
    private final AuthRepository authRepository;
    // --- FIN DE CÓDIGO AÑADIDO ---


    public TeacherViewModel() {
        this.studentRepository = new StudentRepository();
        this.attendanceRepository = new AttendanceRepository();
        this.gradeRepository = new GradeRepository();
        this.noticeRepository = new NoticeRepository();
        this.galleryRepository = new GalleryRepository();
        this.busTrackingRepository = new BusTrackingRepository();
        this.authRepository = new AuthRepository(); // <-- AÑADIDO
    }

    // --- Student ---
    public LiveData<Resource<Student>> addStudent(Student student, String parentEmail, Uri imageUri) {
        return studentRepository.addStudent(student, parentEmail, imageUri);
    }

    // --- INICIO DE CÓDIGO MODIFICADO ---
    // Método 'updateStudent' ya NO recibe 'parentEmail'
    public LiveData<Resource<Student>> updateStudent(Student student, Uri newImageUri) {
        // Llama al nuevo método del repositorio
        return studentRepository.updateStudent(student, newImageUri);
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

    public LiveData<Resource<Student>> getStudentById(String studentId) {
        return studentRepository.getStudentById(studentId);
    }

    public LiveData<Resource<User>> getParentData(String parentId) {
        return authRepository.getUserData(parentId);
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    public LiveData<Resource<List<Student>>> getStudentsByTeacher(String teacherId) {
        return studentRepository.getStudentsByTeacher(teacherId);
    }

    // --- MÉTODO 'updateStudent' ANTIGUO ELIMINADO ---
    /*
    public LiveData<Resource<Void>> updateStudent(Student student) {
        return studentRepository.updateStudent(student);
    }
    */

    public LiveData<Resource<Void>> deleteStudent(String studentId) {
        return studentRepository.deleteStudent(studentId);
    }

    // --- Attendance ---
    // ... (sin cambios) ...
    public LiveData<Resource<Void>> saveAttendance(List<Attendance> attendanceList) {
        return attendanceRepository.saveAttendance(attendanceList);
    }
    public LiveData<Resource<List<Attendance>>> getAttendanceByDate(String teacherId, String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = null;
        try {
            date = sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            MutableLiveData<Resource<List<Attendance>>> errorResult = new MutableLiveData<>();
            errorResult.setValue(Resource.error("Formato de fecha inválido", null));
            return errorResult;
        }
        if (date == null) {
            MutableLiveData<Resource<List<Attendance>>> errorResult = new MutableLiveData<>();
            errorResult.setValue(Resource.error("Fecha nula después de parsear", null));
            return errorResult;
        }
        return attendanceRepository.getAttendanceByDate(teacherId, date);
    }
    public LiveData<Resource<String>> recordAttendance(Attendance attendance) {
        return attendanceRepository.recordAttendance(attendance);
    }


    // --- Grades ---
    // ... (sin cambios) ...
    public LiveData<Resource<String>> saveGrade(Grade grade) {
        return gradeRepository.saveGrade(grade);
    }
    public LiveData<Resource<Grade>> getGradeByStudentAndPeriod(String studentId, int period) {
        return gradeRepository.getGradeByStudentAndPeriod(studentId, period);
    }

    // --- Notices ---
    // ... (sin cambios) ...
    public LiveData<Resource<String>> publishNotice(Notice notice, Uri imageUri) {
        return noticeRepository.publishNotice(notice, imageUri);
    }
    public LiveData<Resource<List<Notice>>> getNoticesByGroup(String groupName) {
        return noticeRepository.getNoticesByGroup(groupName);
    }
    public LiveData<Resource<Void>> deleteNotice(String noticeId) {
        return noticeRepository.deleteNotice(noticeId);
    }
    public LiveData<Resource<Notice>> getNoticeById(String noticeId) {
        return noticeRepository.getNoticeById(noticeId);
    }
    public LiveData<Resource<String>> updateNotice(Notice notice, @Nullable Uri newImageUri, @Nullable String oldImageUrl) {
        return noticeRepository.updateNotice(notice, newImageUri, oldImageUrl);
    }


    // --- Gallery ---
    // ... (sin cambios) ...
    public LiveData<Resource<String>> uploadMedia(GalleryItem item, Uri mediaUri, Context context) {
        return galleryRepository.uploadMedia(item, mediaUri, context.getApplicationContext());
    }
    public LiveData<Resource<List<GalleryItem>>> getGalleryByGroup(String groupName) {
        return galleryRepository.getGalleryByGroup(groupName);
    }
    public LiveData<Resource<Void>> deleteGalleryItem(String itemId) {
        return galleryRepository.deleteGalleryItem(itemId);
    }

    // --- Bus Tracking ---
    // ... (sin cambios) ...
    public LiveData<Resource<Void>> startBusRoute() {
        return busTrackingRepository.updateBusStatus("ACTIVE");
    }
    public LiveData<Resource<Void>> finishBusRoute() {
        return busTrackingRepository.updateBusStatus("FINISHED");
    }
    public LiveData<Resource<String>> getCurrentBusStatus() {
        return busTrackingRepository.getCurrentBusStatus();
    }
    public void updateBusLocation(GeoPoint location) {
        busTrackingRepository.updateBusLocation(location);
    }
}