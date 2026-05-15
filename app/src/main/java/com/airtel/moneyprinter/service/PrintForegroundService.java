package com.airtel.moneyprinter.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.airtel.moneyprinter.AirtelMoneyApp;
import com.airtel.moneyprinter.R;
import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.airtel.moneyprinter.data.repository.TransactionRepository;
import com.airtel.moneyprinter.printer.PrinterManager;
import com.airtel.moneyprinter.ui.main.MainActivity;
import com.airtel.moneyprinter.utils.AppLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service de premier plan (Foreground Service) permanent.
 *
 * Maintient l'application active en arrière-plan,
 * gère les impressions asynchrones.
 */
public class PrintForegroundService extends Service {

    private static final String TAG = "PrintForegroundService";
    private static final int NOTIFICATION_ID_SERVICE = 1001;
    private static final int NOTIFICATION_ID_PRINT   = 1002;

    // Actions Intent
    public static final String ACTION_START_SERVICE       = "com.airtel.START_SERVICE";
    public static final String ACTION_PRINT_TRANSACTION   = "com.airtel.PRINT_TRANSACTION";
    public static final String ACTION_REPRINT_TRANSACTION = "com.airtel.REPRINT";
    public static final String ACTION_STOP_SERVICE        = "com.airtel.STOP_SERVICE";

    // Extras Intent
    public static final String EXTRA_TRANSACTION_ID = "tx_db_id";
    public static final String EXTRA_TX_MONTANT  = "tx_montant";
    public static final String EXTRA_TX_CLIENT   = "tx_client";
    public static final String EXTRA_TX_SOLDE    = "tx_solde";
    public static final String EXTRA_TX_ID_TX    = "tx_id_tx";
    public static final String EXTRA_TX_DATE     = "tx_date";
    public static final String EXTRA_TX_HEURE    = "tx_heure";
    public static final String EXTRA_TX_TYPE     = "tx_type";
    public static final String EXTRA_TX_RAW      = "tx_raw";
    public static final String EXTRA_TX_PHONE    = "tx_phone";

    private PrinterManager printerManager;
    private TransactionRepository repository;
    private ExecutorService printExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        printerManager = new PrinterManager(this);
        repository = new TransactionRepository();
        printExecutor = Executors.newSingleThreadExecutor();
        Log.i(TAG, "Service créé");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Lance immédiatement en foreground
        startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification());

        if (intent == null) {
            Log.w(TAG, "Intent null, service redémarré par système");
            return START_STICKY;
        }

        String action = intent.getAction();
        Log.d(TAG, "Action reçue: " + action);

        if (ACTION_PRINT_TRANSACTION.equals(action) || ACTION_REPRINT_TRANSACTION.equals(action)) {
            AirtelTransaction tx = extractTransactionFromIntent(intent);
            if (tx != null) {
                printExecutor.execute(() -> handlePrint(tx));
            }
        } else if (ACTION_STOP_SERVICE.equals(action)) {
            stopForeground(true);
            stopSelf();
        }
        // ACTION_START_SERVICE : juste démarrer le foreground (déjà fait)

        return START_STICKY; // Redémarre automatiquement si tué par le système
    }

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Exécute l'impression (dans le thread executor).
     */
    private void handlePrint(AirtelTransaction tx) {
        Log.i(TAG, "Impression de: " + tx.getTransactionId());
        AppLogger.log(TAG, "Impression: " + tx.getTransactionId());

        boolean success = printerManager.printTransaction(tx);

        if (success) {
            Log.i(TAG, "Impression réussie: " + tx.getTransactionId());
            repository.updatePrintStatus(tx.getId(), AirtelTransaction.PRINT_STATUS_SUCCESS);
            showPrintNotification(tx, true);
            playSuccessSound();
        } else {
            Log.e(TAG, "Échec impression: " + tx.getTransactionId());
            repository.updatePrintStatus(tx.getId(), AirtelTransaction.PRINT_STATUS_FAILED);
            showPrintNotification(tx, false);
        }
    }

    /**
     * Reconstruit un objet AirtelTransaction depuis les extras de l'Intent.
     */
    private AirtelTransaction extractTransactionFromIntent(Intent intent) {
        try {
            AirtelTransaction tx = new AirtelTransaction();
            tx.setId(intent.getIntExtra(EXTRA_TRANSACTION_ID, 0));
            tx.setMontant(intent.getStringExtra(EXTRA_TX_MONTANT));
            tx.setNomClient(intent.getStringExtra(EXTRA_TX_CLIENT));
            tx.setNouveauSolde(intent.getStringExtra(EXTRA_TX_SOLDE));
            tx.setTransactionId(intent.getStringExtra(EXTRA_TX_ID_TX));
            tx.setDate(intent.getStringExtra(EXTRA_TX_DATE));
            tx.setHeure(intent.getStringExtra(EXTRA_TX_HEURE));
            tx.setType(intent.getStringExtra(EXTRA_TX_TYPE));
            tx.setRawMessage(intent.getStringExtra(EXTRA_TX_RAW));
            tx.setNumeroTelephone(intent.getStringExtra(EXTRA_TX_PHONE));
            tx.setSmsHash(tx.getRawMessage() != null
                    ? String.valueOf(tx.getRawMessage().hashCode()) : "0");
            return tx;
        } catch (Exception e) {
            Log.e(TAG, "Erreur extraction Intent: " + e.getMessage());
            return null;
        }
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private Notification buildServiceNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, mainIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, AirtelMoneyApp.CHANNEL_SERVICE_ID)
                .setContentTitle("Airtel Money Printer")
                .setContentText("En attente de transactions SMS...")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void showPrintNotification(AirtelTransaction tx, boolean success) {
        String title = success ? "✅ Reçu imprimé" : "❌ Échec impression";
        String text = success
                ? tx.getMontant() + " - " + tx.getTransactionId()
                : "Transaction " + tx.getTransactionId() + " non imprimée";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, AirtelMoneyApp.CHANNEL_PRINT_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID_PRINT + (int) System.currentTimeMillis() % 1000, builder.build());
    }

    private void playSuccessSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (r != null) r.play();
        } catch (Exception e) {
            Log.w(TAG, "Impossible de jouer le son: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service non bindable
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (printExecutor != null) printExecutor.shutdown();
        Log.i(TAG, "Service détruit");
    }
}
