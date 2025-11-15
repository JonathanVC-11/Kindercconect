package com.example.kinderconnect.ui.parent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.kinderconnect.data.model.Attendance;
import com.example.kinderconnect.data.model.BusStatus;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.data.model.Grade;
import com.example.kinderconnect.data.model.Notice;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.data.repository.AttendanceRepository;
import com.example.kinderconnect.data.repository.BusRepository;
import com.example.kinderconnect.data.repository.GalleryRepository;
import com.example.kinderconnect.data.repository.GradeRepository;
import com.example.kinderconnect.data.repository.NoticeRepository;
import com.example.kinderconnect.data.repository.StudentRepository;
import com.example.kinderconnect.utils.Resource;
import java.util.Date;
import java.util.List;

public class ParentViewModel extends ViewModel {
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final NoticeRepository noticeRepository;
    private final GalleryRepository galleryRepository;
    private final BusRepository busRepository;

    public ParentViewModel() {
        this.studentRepository = new StudentRepository();
        this.attendanceRepository = new AttendanceRepository();
        this.gradeRepository = new GradeRepository();
        this.noticeRepository = new NoticeRepository();
        this.galleryRepository = new GalleryRepository();
        this.busRepository = new BusRepository();
    }

    // --- Bus ---
    public LiveData<Resource<BusStatus>> getBusStatusUpdates() {
        return busRepository.getBusStatusUpdates();
    }

    // --- Estudiantes ---
    public LiveData<Resource<List<Student>>> getStudentsByParent(String parentId) {
        return studentRepository.getStudentsByParent(parentId);
    }

    public LiveData<Resource<Student>> getStudentById(String studentId) {
        return studentRepository.getStudentById(studentId);
    }

    // --- Asistencia ---
    public LiveData<Resource<List<Attendance>>> getAttendanceByStudent(String studentId,
                                                                       Date startDate,
                                                                       Date endDate) {
        return attendanceRepository.getAttendanceByStudent(studentId, startDate, endDate);
    }

    // --- Calificaciones ---
    public LiveData<Resource<List<Grade>>> getGradesByStudent(String studentId) {
        return gradeRepository.getGradesByStudent(studentId);
    }

    public LiveData<Resource<Grade>> getGradeByStudentAndPeriod(String studentId, int period) {
        return gradeRepository.getGradeByStudentAndPeriod(studentId, period);
    }

    // --- Avisos ---

    // --- INICIO DE CÓDIGO MODIFICADO ---
    /**
     * Obtiene una lista combinada de avisos de GRUPO y de ESCUELA para el padre.
     */
    public LiveData<Resource<List<Notice>>> getNoticesForParent(String groupName) {
        // Este método asume que has implementado 'getNoticesForParent'
        // en tu NoticeRepository, como discutimos.
        return noticeRepository.getNoticesForParent(groupName);
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

    public LiveData<Resource<List<Notice>>> getNoticesByGroup(String groupName) {
        return noticeRepository.getNoticesByGroup(groupName);
    }

    public LiveData<Resource<List<Notice>>> getAllNotices() {
        return noticeRepository.getAllNotices();
    }

    public LiveData<Resource<Void>> markNoticeAsRead(String noticeId, String userId) {
        return noticeRepository.markAsRead(noticeId, userId);
    }

    public LiveData<Resource<Notice>> getNoticeById(String noticeId) {
        return noticeRepository.getNoticeById(noticeId);
    }

    // --- Galería ---
    public LiveData<Resource<List<GalleryItem>>> getGalleryByStudent(String studentId) {
        return galleryRepository.getGalleryByStudent(studentId);
    }

    public LiveData<Resource<List<GalleryItem>>> getGalleryByGroup(String groupName) {
        return galleryRepository.getGalleryByGroup(groupName);
    }
}