package com.example.kinderconnect.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.example.kinderconnect.data.model.Attendance;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AttendanceRepository {
    private final FirebaseFirestore firestore;

    public AttendanceRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // NUEVO MÉTODO AGREGADO - Bulk Save
    public LiveData<Resource<Void>> saveAttendance(List<Attendance> attendanceList) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (attendanceList == null || attendanceList.isEmpty()) {
            result.setValue(Resource.error("Lista de asistencia vacía", null));
            return result;
        }

        WriteBatch batch = firestore.batch();

        for (Attendance attendance : attendanceList) {
            Date normalizedDate = normalizeDate(attendance.getAttendanceDate());
            attendance.setAttendanceDate(normalizedDate);

            String docId = firestore.collection(Constants.COLLECTION_ATTENDANCE).document().getId();
            attendance.setAttendanceId(docId);
            batch.set(firestore.collection(Constants.COLLECTION_ATTENDANCE).document(docId), attendance);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error("Error al guardar asistencia: " + e.getMessage(), null)));

        return result;
    }

    public LiveData<Resource<String>> recordAttendance(Attendance attendance) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Date normalizedDate = normalizeDate(attendance.getAttendanceDate());

        firestore.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("studentId", attendance.getStudentId())
                .whereEqualTo("attendanceDate", normalizedDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String attendanceId = querySnapshot.getDocuments().get(0).getId();
                        updateExistingAttendance(attendanceId, attendance, result);
                    } else {
                        attendance.setAttendanceDate(normalizedDate);
                        createNewAttendance(attendance, result);
                    }
                })
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }

    private Date normalizeDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void updateExistingAttendance(String attendanceId, Attendance attendance,
                                          MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_ATTENDANCE)
                .document(attendanceId)
                .set(attendance)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(attendanceId)))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));
    }

    private void createNewAttendance(Attendance attendance,
                                     MutableLiveData<Resource<String>> result) {
        firestore.collection(Constants.COLLECTION_ATTENDANCE)
                .add(attendance)
                .addOnSuccessListener(documentReference ->
                        result.setValue(Resource.success(documentReference.getId())))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));
    }

    public LiveData<Resource<List<Attendance>>> getAttendanceByDate(String teacherId, Date date) {
        MutableLiveData<Resource<List<Attendance>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Date normalizedDate = normalizeDate(date);

        firestore.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("attendanceDate", normalizedDate)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<Attendance> attendanceList = value.toObjects(Attendance.class);
                        result.setValue(Resource.success(attendanceList));
                    }
                });

        return result;
    }

    public LiveData<Resource<List<Attendance>>> getAttendanceByStudent(String studentId,
                                                                       Date startDate,
                                                                       Date endDate) {
        MutableLiveData<Resource<List<Attendance>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("studentId", studentId)
                .whereGreaterThanOrEqualTo("attendanceDate", startDate)
                .whereLessThanOrEqualTo("attendanceDate", endDate)
                .orderBy("attendanceDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        result.setValue(Resource.error("Error: " + error.getMessage(), null));
                        return;
                    }

                    if (value != null) {
                        List<Attendance> attendanceList = value.toObjects(Attendance.class);
                        result.setValue(Resource.success(attendanceList));
                    }
                });

        return result;
    }

    public LiveData<Resource<Void>> markAsNotified(String attendanceId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_ATTENDANCE)
                .document(attendanceId)
                .update("parentNotified", true)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success(null)))
                .addOnFailureListener(e -> result.setValue(Resource.error(
                        "Error: " + e.getMessage(), null)));

        return result;
    }
}
