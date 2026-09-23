package com.example.appfinancas.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.appfinancas.R;
import com.example.appfinancas.database.AppDatabase;
import com.example.appfinancas.model.NotificationEvent;
import com.example.appfinancas.service.ReminderWorker;
import com.example.appfinancas.util.BackupUtils;
import com.example.appfinancas.util.SecurityUtils;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {
    private TextView textStatus;
    private Button btnEnable, btnManageCards, btnManageCategories, btnReset, btnClearEvents, btnReminderTime;
    private Button btnExportDownload, btnImportJson, btnViewJson;
    private SwitchCompat switchMonitoring, switchSecurity, switchReminders;
    private android.widget.Spinner spinnerReminderFrequency;
    private View layoutReminderFrequency, layoutReminderTime;
    private NotificationEventViewModel viewModel;
    private NotificationEventAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        // Apply Window Insets for responsiveness
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top + 24, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        textStatus = root.findViewById(R.id.text_status);
        btnEnable = root.findViewById(R.id.btn_enable_permission);
        btnManageCards = root.findViewById(R.id.btn_manage_cards);
        btnManageCategories = root.findViewById(R.id.btn_manage_categories);
        btnReset = root.findViewById(R.id.btn_reset);
        btnClearEvents = root.findViewById(R.id.btn_clear_events);
        btnReminderTime = root.findViewById(R.id.btn_reminder_time);
        
        btnExportDownload = root.findViewById(R.id.btn_export_backup);
        btnImportJson = root.findViewById(R.id.btn_import_json);
        btnViewJson = root.findViewById(R.id.btn_view_json);

        switchMonitoring = root.findViewById(R.id.switch_monitoring);
        switchSecurity = root.findViewById(R.id.switch_security);
        switchReminders = root.findViewById(R.id.switch_reminders);
        spinnerReminderFrequency = root.findViewById(R.id.spinner_reminder_frequency);
        layoutReminderFrequency = root.findViewById(R.id.layout_reminder_frequency);
        layoutReminderTime = root.findViewById(R.id.layout_reminder_time);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_events);

        btnManageCards.setOnClickListener(v -> startActivity(new Intent(getActivity(), CardListActivity.class)));
        btnManageCategories.setOnClickListener(v -> startActivity(new Intent(getActivity(), CategoryListActivity.class)));

        switchSecurity.setChecked(SecurityUtils.isBiometricEnabled(getContext()));
        switchSecurity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SecurityUtils.setBiometricEnabled(getContext(), isChecked);
        });

        btnExportDownload.setOnClickListener(v -> showExportConfirmation());
        
        btnImportJson.setOnClickListener(v -> showJsonImportDialog());
        btnViewJson.setOnClickListener(v -> showCurrentJsonDialog());

        btnReset.setOnClickListener(v -> showResetConfirmation());
        btnClearEvents.setOnClickListener(v -> clearNotificationEvents());
        btnReminderTime.setOnClickListener(v -> showTimePicker());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationEventAdapter();
        adapter.setListener(new NotificationEventAdapter.OnEventListener() {
            @Override
            public void onIgnore(NotificationEvent event) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getDatabase(getContext()).notificationEventDao().delete(event);
                });
            }
        });
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getContext().getSharedPreferences("finance_prefs", Context.MODE_PRIVATE);
        
        switchMonitoring.setChecked(prefs.getBoolean("monitoring_enabled", true));
        switchMonitoring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("monitoring_enabled", isChecked).apply();
        });

        switchReminders.setChecked(prefs.getBoolean("reminders_enabled", false));
        boolean remindersOn = switchReminders.isChecked();
        layoutReminderFrequency.setVisibility(remindersOn ? View.VISIBLE : View.GONE);
        layoutReminderTime.setVisibility(remindersOn ? View.VISIBLE : View.GONE);
        
        updateTimeButtonLabel(prefs);
        setupFrequencySpinner(prefs);

        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("reminders_enabled", isChecked).apply();
            layoutReminderFrequency.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            layoutReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                scheduleReminders();
            } else {
                cancelReminders();
            }
        });

        btnEnable.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });

        viewModel = new ViewModelProvider(this).get(NotificationEventViewModel.class);
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            adapter.setEvents(events);
        });

        return root;
    }


    private void setupFrequencySpinner(SharedPreferences prefs) {
        String[] options = {"Diário (Pendentes e Atrasados)", "No dia do vencimento", "1 dia antes do vencimento", "1 semana antes do vencimento"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_small, options);
        adapter.setDropDownViewResource(R.layout.spinner_item_small);
        spinnerReminderFrequency.setAdapter(adapter);

        int savedFreq = prefs.getInt("reminder_frequency_days", 1);
        if (savedFreq == 1) spinnerReminderFrequency.setSelection(0);
        else if (savedFreq == 0) spinnerReminderFrequency.setSelection(1);
        else if (savedFreq == -1) spinnerReminderFrequency.setSelection(2);
        else if (savedFreq == -7) spinnerReminderFrequency.setSelection(3);

        spinnerReminderFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int days = 1;
                if (position == 1) days = 0;
                else if (position == 2) days = -1;
                else if (position == 3) days = -7;

                if (days != prefs.getInt("reminder_frequency_days", 1)) {
                    prefs.edit().putInt("reminder_frequency_days", days).apply();
                    if (switchReminders.isChecked()) {
                        scheduleReminders();
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void scheduleReminders() {
        SharedPreferences prefs = requireContext().getSharedPreferences("finance_prefs", Context.MODE_PRIVATE);
        int hour = prefs.getInt("reminder_hour", 9);
        int minute = prefs.getInt("reminder_minute", 0);

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("payment_reminder")
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "payment_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest
                );

        Toast.makeText(getContext(), String.format("Lembrete agendado para às %02d:%02d", hour, minute), Toast.LENGTH_SHORT).show();
    }

    private void showTimePicker() {
        SharedPreferences prefs = requireContext().getSharedPreferences("finance_prefs", Context.MODE_PRIVATE);
        int hour = prefs.getInt("reminder_hour", 9);
        int minute = prefs.getInt("reminder_minute", 0);

        new android.app.TimePickerDialog(getContext(), (view, hourOfDay, selectedMinute) -> {
            prefs.edit()
                .putInt("reminder_hour", hourOfDay)
                .putInt("reminder_minute", selectedMinute)
                .apply();
            updateTimeButtonLabel(prefs);
            if (switchReminders.isChecked()) {
                scheduleReminders();
            }
        }, hour, minute, true).show();
    }

    private void updateTimeButtonLabel(SharedPreferences prefs) {
        int hour = prefs.getInt("reminder_hour", 9);
        int minute = prefs.getInt("reminder_minute", 0);
        btnReminderTime.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void cancelReminders() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("payment_reminder");
    }


    private void showJsonImportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_json, null);
        EditText editJson = dialogView.findViewById(R.id.edit_import_json);
        Button btnExample = dialogView.findViewById(R.id.btn_json_example);

        String exampleJson = "{\n" +
                "  \"categories\": [\n" +
                "    { \"name\": \"Alimentação\", \"icon\": \"ic_category\", \"color\": \"#F44336\" },\n" +
                "    { \"name\": \"Lazer\", \"icon\": \"ic_category\", \"color\": \"#9C27B0\" },\n" +
                "    { \"name\": \"Salário\", \"icon\": \"ic_category\", \"color\": \"#4CAF50\" }\n" +
                "  ],\n" +
                "  \"cards\": [\n" +
                "    { \"name\": \"Nubank\", \"limit\": 5000.0, \"close\": 5, \"due\": 10, \"color\": \"#9C27B0\" },\n" +
                "    { \"name\": \"Itaú\", \"limit\": 2500.0, \"close\": 15, \"due\": 25, \"color\": \"#FF9800\" }\n" +
                "  ],\n" +
                "  \"transactions\": [\n" +
                "    {\n" +
                "      \"desc\": \"Salário Mensal\",\n" +
                "      \"amount\": 4500.0, \"type\": \"RECEITA\", \"cat\": \"Salário\",\n" +
                "      \"date\": " + System.currentTimeMillis() + ",\n" +
                "      \"isCard\": false, \"isPaid\": true, \"isFixed\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"desc\": \"Restaurante\",\n" +
                "      \"amount\": 85.50, \"type\": \"DESPESA\", \"cat\": \"Alimentação\",\n" +
                "      \"date\": " + System.currentTimeMillis() + ",\n" +
                "      \"isCard\": false, \"isPaid\": true, \"isFixed\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"desc\": \"Cinema\",\n" +
                "      \"amount\": 60.0, \"type\": \"DESPESA\", \"cat\": \"Lazer\",\n" +
                "      \"date\": " + System.currentTimeMillis() + ",\n" +
                "      \"isCard\": true, \"isPaid\": false, \"isFixed\": false\n" +
                "      ,\"bank\": \"Nubank\", \"invM\": " + Calendar.getInstance().get(Calendar.MONTH) + ", \"invY\": " + Calendar.getInstance().get(Calendar.YEAR) + "\n" +
                "    },\n" +
                "    {\n" +
                "      \"desc\": \"Supermercado\",\n" +
                "      \"amount\": 350.75, \"type\": \"DESPESA\", \"cat\": \"Alimentação\",\n" +
                "      \"date\": " + System.currentTimeMillis() + ",\n" +
                "      \"isCard\": true, \"isPaid\": false, \"isFixed\": false\n" +
                "      ,\"bank\": \"Itaú\", \"invM\": " + Calendar.getInstance().get(Calendar.MONTH) + ", \"invY\": " + Calendar.getInstance().get(Calendar.YEAR) + "\n" +
                "    },\n" +
                "    {\n" +
                "      \"desc\": \"Assinatura Streaming\",\n" +
                "      \"amount\": 39.90, \"type\": \"DESPESA\", \"cat\": \"Lazer\",\n" +
                "      \"date\": " + System.currentTimeMillis() + ",\n" +
                "      \"isCard\": false, \"isPaid\": true, \"isFixed\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        editJson.setHint("Cole o conteúdo do backup.txt aqui...");

        btnExample.setOnClickListener(v -> editJson.setText(exampleJson));

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("Enviar JSON", null) // Set listener later
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String json = editJson.getText().toString();
            if (!json.isEmpty()) {
                try {
                    String expandedJson = BackupUtils.processAndExpandJson(json);
                    if (!expandedJson.equals(json)) {
                        editJson.setText(expandedJson);
                        Toast.makeText(getContext(), "JSON expandido! Confira as parcelas geradas e clique em Enviar novamente.", Toast.LENGTH_LONG).show();
                    } else {
                        BackupUtils.importFromJson(getActivity(), json);
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Erro ao processar JSON", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCurrentJsonDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_json, null);
        EditText editJson = dialogView.findViewById(R.id.edit_import_json);
        Button btnExample = dialogView.findViewById(R.id.btn_json_example);
        TextView textInstr = dialogView.findViewById(R.id.text_import_instructions);
        TextView textTitle = dialogView.findViewById(R.id.text_import_title);

        if (textTitle != null) textTitle.setText("JSON Atual do App");
        
        if (textInstr != null) {
            textInstr.setText("Abaixo está o conteúdo JSON atual do seu aplicativo. Você pode copiar este texto para salvar externamente.");
        }
        
        btnExample.setVisibility(View.GONE);
        editJson.setFocusable(true);
        editJson.setClickable(true);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String currentJson = BackupUtils.getFullBackupJson(getContext());
                requireActivity().runOnUiThread(() -> editJson.setText(currentJson));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro ao gerar JSON", Toast.LENGTH_SHORT).show());
            }
        });

        new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(dialogView)
                .setPositiveButton("Fechar", null)
                .show();
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("Esta ação é IRREVERSÍVEL.\n\nTodos os seus dados (transações, cartões e categorias) serão apagados permanentemente.\n\nDeseja continuar?")
                .setPositiveButton("Sim, Resetar Tudo", (dialog, which) -> resetApp())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showExportConfirmation() {
        new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("Deseja gerar um novo arquivo de backup com seus dados atuais na pasta Downloads?")
                .setPositiveButton("Sim, Exportar", (dialog, which) -> BackupUtils.exportToDownloads(getActivity()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void clearNotificationEvents() {
        new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage("Deseja remover todos os eventos identificados da lista?")
                .setPositiveButton("Sim, Limpar", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getDatabase(getContext()).notificationEventDao().deleteAll();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void resetApp() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) return;

            // 1. Clear Database
            AppDatabase db = AppDatabase.getDatabase(context);
            db.clearAllTables();
            
            // 2. Clear all internal export files (Manual and Auto)
            File dir = context.getExternalFilesDir(null);
            if (dir != null && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
            }

            // 3. Clear all SharedPreferences (Settings, Monitoring, Dashboard Order)
            context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE).edit().clear().apply();
            context.getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE).edit().clear().apply();

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(context, "Aplicativo resetado para o estado de fábrica!", Toast.LENGTH_SHORT).show();
                // Recreate activity to apply all resets (UI and settings)
                requireActivity().recreate();
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermission();
    }

    private void checkPermission() {
        if (getContext() == null) return;
        String packageName = getContext().getPackageName();
        String flat = Settings.Secure.getString(getContext().getContentResolver(), "enabled_notification_listeners");
        boolean enabled = flat != null && flat.contains(packageName);
        
        if (enabled) {
            textStatus.setText("Permissão: Ativada");
            textStatus.setTextColor(0xFF4CAF50);
            btnEnable.setVisibility(View.GONE);
        } else {
            textStatus.setText("Permissão: Desativada");
            textStatus.setTextColor(0xFFF44336);
            btnEnable.setVisibility(View.VISIBLE);
        }
    }
}
