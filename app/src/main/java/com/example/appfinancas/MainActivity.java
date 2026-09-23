package com.example.appfinancas;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.activity.EdgeToEdge;

import com.example.appfinancas.util.SecurityUtils;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.service.FixedTransactionWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private boolean authenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        
        scheduleFixedTransactionWorker();
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getDatabase(this).transactionDao().cleanOutrosCategory();
        });
        
        if (SecurityUtils.isBiometricEnabled(this) && !authenticated) {
            setContentView(R.layout.activity_lock);
            authenticate();
        } else {
            showMainUI();
        }
    }

    private void authenticate() {
        SecurityUtils.authenticate(this, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                authenticated = true;
                showMainUI();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                finish(); // Exit app if auth fails
            }
        });
    }

    private void showMainUI() {
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.bottom_nav);
        
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);
        }
    }

    private void scheduleFixedTransactionWorker() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                FixedTransactionWorker.class, 3, TimeUnit.DAYS)
                .addTag("fixed_transactions_gen")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "fixed_transactions_gen",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
