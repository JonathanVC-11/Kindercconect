package com.example.kinderconnect.ui.parent;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ActivityParentMainBinding;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

public class ParentMainActivity extends AppCompatActivity {
    private ActivityParentMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityParentMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        subscribeToBusTopic();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }

    private void subscribeToBusTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("bus_route")
                .addOnCompleteListener(task -> {
                    String msg = "Suscripción al tema 'bus_route' EXITOSA";
                    if (!task.isSuccessful()) {
                        msg = "ERROR al suscribirse al tema 'bus_route'";
                    }
                    Log.d("FCM_Subscription", msg);
                    // Opcional: Mostrar Toast si quieres confirmar visualmente
                    // Toast.makeText(ParentMainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
    }
}
