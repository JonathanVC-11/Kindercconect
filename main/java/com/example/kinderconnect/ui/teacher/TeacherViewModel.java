package com.example.kinderconnect.ui.teacher;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.kinderconnect.data.model.*;
import com.example.kinderconnect.data.repository.*;
import com.example.kinderconnect.utils.Resource;
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

    public TeacherViewModel() {
        this.studentRepository = new StudentRepository();
        this.attendanceRepository = new AttendanceRepository();
        this.gradeRepository = new GradeRepository();
        this.noticeRepository = new NoticeRepository();
        this.galleryRepository = new GalleryRepository();
    }

    // MÉTODO CORREGIDO: Acepta parentEmail
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

    // Attendance
    public LiveData<Resource<Void>> saveAttendance(List<Attendance> attendanceList) {
        return attendanceRepository.saveAttendance(attendanceList);
    }

    public LiveData<Resource<List<Attendance>>> getAttendanceByDate(String teacherId, String dateStr) {
        // Convierte dateStr (ej: "2025-10-16") a Date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = null;
        try {
            date = sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            // Aquí puedes manejar el error según tus necesidades
        }
        if (date == null) {
            // Retorna LiveData con error o un LiveData vacío
            return new androidx.lifecycle.MutableLiveData<>();
        }
        return attendanceRepository.getAttendanceByDate(teacherId, date);
    }

    // Grades
// Cambiar de LiveData<Resource<Void>> a LiveData<Resource<String>>
    public LiveData<Resource<String>> saveGrade(Grade grade) {
        return gradeRepository.saveGrade(grade);
    }

    public LiveData<Resource<Grade>> getGradeByStudentAndPeriod(String studentId, int period) {
        return gradeRepository.getGradeByStudentAndPeriod(studentId, period);
    }

    // Notices
    public LiveData<Resource<String>> publishNotice(Notice notice, Uri imageUri) {
        return noticeRepository.publishNotice(notice, imageUri);
    }

    public LiveData<Resource<List<Notice>>> getNoticesByGroup(String groupName) {
        return noticeRepository.getNoticesByGroup(groupName);
    }

    // Gallery
    public LiveData<Resource<String>> uploadMedia(GalleryItem item, Uri mediaUri) {
        return galleryRepository.uploadMedia(item, mediaUri);
    }

    public LiveData<Resource<String>> recordAttendance(Attendance attendance) {
        return attendanceRepository.recordAttendance(attendance);
    }


    public LiveData<Resource<List<GalleryItem>>> getGalleryByGroup(String groupName) {
        return galleryRepository.getGalleryByGroup(groupName);
    }

    public LiveData<Resource<Void>> deleteGalleryItem(String itemId) {
        return galleryRepository.deleteGalleryItem(itemId);
    }
}
