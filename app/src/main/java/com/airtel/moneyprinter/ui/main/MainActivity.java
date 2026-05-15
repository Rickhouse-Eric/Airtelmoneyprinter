package com.airtel.moneyprinter.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.airtel.moneyprinter.R;
import com.airtel.moneyprinter.databinding.ActivityMainBinding;
import com.airtel.moneyprinter.printer.PrinterManager;
import com.airtel.moneyprinter.service.PrintForegroundService;
import com.airtel.moneyprinter.ui.history.HistoryActivity;
import com.airtel.moneyprinter.ui.settings.SettingsActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activité principale.
 * - Affiche le statut du service
 * - Toggle impression auto
 * - Bouton test impression
 * - Accès historique et paramètres
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_SMS_PERMISSIONS = 100;

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Permissions requises
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupUI();
        checkAndRequestPermissions();
        startPrintService();
        observeTransactions();
    }

    private void setupUI() {
        // Toggle impression automatique
        boolean autoEnabled = prefs.getBoolean("auto_print_enabled", true);
        binding.switchAutoPrint.setChecked(autoEnabled);
        updateStatusLabel(autoEnabled);

        binding.switchAutoPrint.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("auto_print_enabled", isChecked).apply();
            updateStatusLabel(isChecked);
            Toast.makeText(this,
                    isChecked ? "Impression auto ACTIVÉE" : "Impression auto DÉSACTIVÉE",
                    Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Impression auto: " + isChecked);
        });

        // Bouton test impression
        binding.btnTestPrint.setOnClickListener(v -> testPrint());

        // Bouton historique
        binding.btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        // Bouton paramètres
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // Bouton diagnostic port série
        binding.btnDiagnostic.setOnClickListener(v -> runDiagnostic());
    }

    private void updateStatusLabel(boolean active) {
        if (active) {
            binding.tvStatus.setText("● ACTIF - En attente de SMS Airtel Money");
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active));
        } else {
            binding.tvStatus.setText("○ INACTIF - Impression automatique désactivée");
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inactive));
        }
    }

    private void observeTransactions() {
        viewModel.getAllTransactions().observe(this, transactions -> {
            int count = transactions != null ? transactions.size() : 0;
            binding.tvTotalCount.setText("Total transactions : " + count);

            // Affiche la dernière transaction
            if (transactions != null && !transactions.isEmpty()) {
                var last = transactions.get(0);
                binding.tvLastTx.setText(
                        "Dernière : " + last.getMontant()
                                + " | " + last.getDate() + " " + last.getHeure());
                binding.tvLastTx.setVisibility(View.VISIBLE);
            } else {
                binding.tvLastTx.setText("Aucune transaction encore");
            }
        });
    }

    private void testPrint() {
        binding.btnTestPrint.setEnabled(false);
        binding.btnTestPrint.setText("Impression...");

        executor.execute(() -> {
            PrinterManager pm = new PrinterManager(this);
            boolean success = pm.printTestTicket();
            runOnUiThread(() -> {
                binding.btnTestPrint.setEnabled(true);
                binding.btnTestPrint.setText("Tester l'impression");
                if (success) {
                    Toast.makeText(this, "✅ Test d'impression réussi!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "❌ Échec impression - vérifiez le port série", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void runDiagnostic() {
        String[] ports = {"/dev/ttyS1", "/dev/ttyS0", "/dev/ttyUSB0"};
        StringBuilder result = new StringBuilder("=== DIAGNOSTIC PORTS SÉRIE ===\n\n");
        PrinterManager pm = new PrinterManager(this);

        binding.btnDiagnostic.setEnabled(false);

        executor.execute(() -> {
            for (String port : ports) {
                String status = pm.testPort(port);
                result.append(status).append("\n");
            }
            runOnUiThread(() -> {
                binding.btnDiagnostic.setEnabled(true);
                new AlertDialog.Builder(this)
                        .setTitle("Diagnostic Imprimante")
                        .setMessage(result.toString())
                        .setPositiveButton("OK", null)
                        .show();
            });
        });
    }

    private void startPrintService() {
        Intent intent = new Intent(this, PrintForegroundService.class);
        intent.setAction(PrintForegroundService.ACTION_START_SERVICE);
        startForegroundService(intent);
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private void checkAndRequestPermissions() {
        boolean allGranted = true;
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_SMS_PERMISSIONS);
        } else {
            Log.d(TAG, "Toutes les permissions accordées");
            binding.tvPermissionStatus.setText("✅ Permissions SMS accordées");
            binding.tvPermissionStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_active));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                binding.tvPermissionStatus.setText("✅ Permissions SMS accordées");
                binding.tvPermissionStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.status_active));
                Toast.makeText(this, "Permissions accordées!", Toast.LENGTH_SHORT).show();
            } else {
                binding.tvPermissionStatus.setText("❌ Permissions SMS manquantes!");
                binding.tvPermissionStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.status_inactive));
                Toast.makeText(this,
                        "⚠️ Permissions SMS nécessaires pour l'interception",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
