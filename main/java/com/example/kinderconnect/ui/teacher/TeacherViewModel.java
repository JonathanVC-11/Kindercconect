package com.example.kinderconnect.ui.teacher;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.kinderconnect.data.model.*;
import com.example.kinderconnect.data.repository.*;
import com.example.kinderconnect.utils.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
// Importar nuevo repo
import com.example.kinderconnect.data.repository.BusTrackingRepository;

public class TeacherViewModel extends ViewModel {
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final NoticeRepository noticeRepository;
    private final GalleryRepository galleryRepository;
    private final BusTrackingRepository busTrackingRepository; // ¡Añadir!

    public TeacherViewModel() {
        this.studentRepository = new StudentRepository();
        this.attendanceRepository = new AttendanceRepository();
        this.gradeRepository = new GradeRepository();
        this.noticeRepository = new NoticeRepository();
        this.galleryRepository = new GalleryRepository();
        this.busTrackingRepository = new BusTrackingRepository(); // ¡Inicializar!
    }

    // --- MÉTODO CORREGIDO: Acepta parentEmail ---
    public LiveData<Resource<Student>> addStudent(Student student, String parentEmail, Uri imageUri) {
        return studentRepository.addStudent(student, parentEmail, imageUri);
    }

    public LiveData<Resource<List<Student>>> getStudentsByTeacher(String teacherId) {
        return studentRepository.getStudentsByTeacher(teacherId);
    }

    public LiveData<Resource<Void>> updateStudent(Student student) {
        return studentRepository.updateStudent(student);
    }

    public LiveData<Resource<Void>> deleteStudent(String studentId) {
        return studentRepository.deleteStudent(studentId);
    }

    // --- Attendance ---
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
            // Retorna LiveData con error o un LiveData vacío si la fecha es inválida
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
    public LiveData<Resource<String>> saveGrade(Grade grade) {
        return gradeRepository.saveGrade(grade);
    }

    public LiveData<Resource<Grade>> getGradeByStudentAndPeriod(String studentId, int period) {
        return gradeRepository.getGradeByStudentAndPeriod(studentId, period);
    }

    // --- Notices ---
    public LiveData<Resource<String>> publishNotice(Notice notice, Uri imageUri) {
        return noticeRepository.publishNotice(notice, imageUri);
    }

    public LiveData<Resource<List<Notice>>> getNoticesByGroup(String groupName) {
        return noticeRepository.getNoticesByGroup(groupName);
    }

    // --- Gallery ---
    public LiveData<Resource<String>> uploadMedia(GalleryItem item, Uri mediaUri) {
        return galleryRepository.uploadMedia(item, mediaUri);
    }

    public LiveData<Resource<List<GalleryItem>>> getGalleryByGroup(String groupName) {
        return galleryRepository.getGalleryByGroup(groupName);
    }

    public LiveData<Resource<Void>> deleteGalleryItem(String itemId) {
        return galleryRepository.deleteGalleryItem(itemId);
    }

    // --- MÉTODOS NUEVOS PARA EL AUTOBÚS ---
    public LiveData<Resource<Void>> startBusRoute() {
        // Llama al repositorio para cambiar el estado a ACTIVE
        return busTrackingRepository.updateBusStatus("ACTIVE");
    }

    public LiveData<Resource<Void>> finishBusRoute() {
        // Llama al repositorio para cambiar el estado a FINISHED (o INACTIVE si prefieres)
        return busTrackingRepository.updateBusStatus("FINISHED");
    }

    // Método para obtener el estado actual y mostrar los botones correctamente
    public LiveData<Resource<String>> getCurrentBusStatus() {
        return busTrackingRepository.getCurrentBusStatus();
    }
    // --- FIN MÉTODOS NUEVOS ---
}