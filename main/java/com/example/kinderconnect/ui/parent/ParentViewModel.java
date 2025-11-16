package com.example.kinderconnect.ui.parent;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.kinderconnect.data.model.Attendance;
import com.example.kinderconnect.data.model.BusStatus;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.data.model.Grade;
import com.example.kinderconnect.data.model.Group;
import com.example.kinderconnect.data.model.Notice;
import com.example.kinderconnect.data.model.Notification; // <-- AÑADIDO
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.data.repository.AttendanceRepository;
import com.example.kinderconnect.data.repository.BusRepository;
import com.example.kinderconnect.data.repository.GalleryRepository;
import com.example.kinderconnect.data.repository.GradeRepository;
import com.example.kinderconnect.data.repository.GroupRepository;
import com.example.kinderconnect.data.repository.NoticeRepository;
import com.example.kinderconnect.data.repository.NotificationRepository; // <-- AÑADIDO
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
    private final GroupRepository groupRepository;
    private final NotificationRepository notificationRepository; // <-- AÑADIDO

    public ParentViewModel() {
        this.studentRepository = new StudentRepository();
        this.attendanceRepository = new AttendanceRepository();
        this.gradeRepository = new GradeRepository();
        this.noticeRepository = new NoticeRepository();
        this.galleryRepository = new GalleryRepository();
        this.busRepository = new BusRepository();
        this.groupRepository = new GroupRepository();
        this.notificationRepository = new NotificationRepository(); // <-- AÑADIDO
    }

    // --- (Métodos de Bus, Estudiantes, Asistencia, Calificaciones, Avisos, Galería sin cambios) ---
    // ...

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // --- Notificaciones ---
    public LiveData<Resource<List<Notification>>> getNotifications(String userId) {
        return notificationRepository.getNotificationsForUser(userId);
    }

    public LiveData<Resource<Void>> markNotificationAsRead(String notificationId) {
        return notificationRepository.markNotificationAsRead(notificationId);
    }

    public LiveData<Resource<Void>> markAllNotificationsAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId);
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    // ... (El resto de métodos: getBusStatusUpdates, getStudentsByParent, etc.)
    // (Asegúrate de que estén todos aquí)

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

    public LiveData<Resource<Student>> registerStudent(Student student, String teacherEmail, String parentId, Uri imageUri) {
        MediatorLiveData<Resource<Student>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading(null));
        LiveData<Resource<Group>> groupSource = groupRepository.getGroupByTeacherEmail(teacherEmail);

        result.addSource(groupSource, groupResource -> {
            if (groupResource.getStatus() == Resource.Status.SUCCESS) {
                Group group = groupResource.getData();
                if (group != null) {
                    student.setParentId(parentId);
                    student.setTeacherId(group.getTeacherId());
                    student.setGroupName(group.getGrade() + " " + group.getGroupName());
                    student.setActive(true);
                    studentRepository.uploadAndRegisterStudent(student, imageUri, result);
                } else {
                    result.setValue(Resource.error("No se encontró ningún grupo para el correo: " + teacherEmail, null));
                }
            } else if (groupResource.getStatus() == Resource.Status.ERROR) {
                result.setValue(Resource.error(groupResource.getMessage(), null));
            }
            result.removeSource(groupSource);
        });
        return result;
    }

    public LiveData<Resource<Student>> updateStudent(Student student, Uri newImageUri) {
        return studentRepository.updateStudent(student, newImageUri);
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
    public LiveData<Resource<List<Notice>>> getNoticesForParent(String groupName) {
        return noticeRepository.getNoticesForParent(groupName);
    }

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