package com.example.kinderconnect.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kinderconnect.R;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Notification;
import com.example.kinderconnect.databinding.FragmentNotificationsBinding;
import com.example.kinderconnect.ui.adapters.NotificationAdapter;
import com.example.kinderconnect.ui.parent.ParentViewModel;
import com.example.kinderconnect.ui.teacher.TeacherViewModel;
import com.example.kinderconnect.utils.Resource;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private PreferencesManager preferencesManager;
    private NotificationAdapter adapter;
    private ParentViewModel parentViewModel;
    private TeacherViewModel teacherViewModel;
    private String currentUserId;
    private boolean isTeacher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferencesManager = new PreferencesManager(requireContext());
        currentUserId = preferencesManager.getUserId();
        isTeacher = preferencesManager.isTeacher();

        // Instanciar el ViewModel correcto
        if (isTeacher) {
            teacherViewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        } else {
            parentViewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        }

        setupRecyclerView();
        setupListeners();
        loadNotifications();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(notification -> {
            if (!notification.isRead()) {
                markNotificationAsRead(notification.getNotificationId());
            }
            // Opcional: Navegar a algún sitio al hacer clic
            // (ej. si es "ATTENDANCE", navegar a la pantalla de asistencia)
            Toast.makeText(requireContext(), "Notificación leída", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        binding.btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void loadNotifications() {
        if (currentUserId == null) return;

        LiveData<Resource<List<Notification>>> source;
        if (isTeacher) {
            source = teacherViewModel.getNotifications(currentUserId);
        } else {
            source = parentViewModel.getNotifications(currentUserId);
        }

        source.observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || binding == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.emptyView.getRoot().setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    List<Notification> notifications = resource.getData();
                    if (notifications != null && !notifications.isEmpty()) {
                        binding.recyclerView.setVisibility(View.VISIBLE);
                        binding.emptyView.getRoot().setVisibility(View.GONE);
                        adapter.submitList(notifications);

                        // Chequear si hay alguna no leída para mostrar el botón
                        boolean hasUnread = notifications.stream().anyMatch(n -> !n.isRead());
                        binding.btnMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);

                    } else {
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_notifications);
                        binding.emptyView.tvEmptyTitle.setText("Sin notificaciones");
                        binding.emptyView.tvEmptySubtitle.setText("Aquí aparecerán todas tus alertas y novedades.");
                        binding.btnMarkAllRead.setVisibility(View.GONE);
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                    binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                    binding.emptyView.tvEmptyTitle.setText("Error");
                    binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
                    binding.btnMarkAllRead.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void markNotificationAsRead(String notificationId) {
        if (isTeacher) {
            teacherViewModel.markNotificationAsRead(notificationId).observe(getViewLifecycleOwner(), res -> {});
        } else {
            parentViewModel.markNotificationAsRead(notificationId).observe(getViewLifecycleOwner(), res -> {});
        }
    }

    private void markAllAsRead() {
        if (currentUserId == null) return;

        binding.btnMarkAllRead.setEnabled(false);

        LiveData<Resource<Void>> source;
        if (isTeacher) {
            source = teacherViewModel.markAllNotificationsAsRead(currentUserId);
        } else {
            source = parentViewModel.markAllNotificationsAsRead(currentUserId);
        }

        source.observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() != Resource.Status.LOADING) {
                binding.btnMarkAllRead.setEnabled(true);
                if (resource.getStatus() == Resource.Status.ERROR) {
                    Toast.makeText(requireContext(), "Error: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}