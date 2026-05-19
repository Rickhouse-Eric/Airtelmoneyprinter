package com.airtel.moneyprinter.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.airtel.moneyprinter.AirtelMoneyApp;
import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.airtel.moneyprinter.data.repository.TransactionRepository;
import com.airtel.moneyprinter.printer.PrinterManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrintForegroundService extends Service {

    private static final String TAG = "PrintForegroundService";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START_SERVICE = "com.airtel.START_SERVICE";
    public static final String ACTION_PRINT_TRANSACTION = "com.airtel.PRINT_TRANSACTION";
    public static final String ACTION_REPRINT_TRANSACTION = "com.airtel.REPRINT";
    public static final String ACTION_STOP_SERVICE = "com.airtel.STOP_SERVICE";

    public static final String EXTRA_TRANSACTION_ID = "tx_db_id";
    public static final String EXTRA_TX_MONTANT = "tx_montant";
    public static final String EXTRA_TX_CLIENT = "tx_client";
    public static final String EXTRA_TX_SOLDE = "tx_solde";
    public static final String EXTRA_TX_ID_TX = "tx_id_tx";
    public static final String EXTRA_TX_DATE = "tx_date";
    public static final String EXTRA_TX_HEURE = "tx_heure";
    public static final String EXTRA_TX_TYPE = "tx_type";
    public static final String EXTRA_TX_RAW = "tx_raw";
    public static final String EXTRA_TX_PHONE = "tx_phone";

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
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, AirtelMoneyApp.CHANNEL_SERVICE_ID)
                .setContentTitle("Airtel Money Printer")
                .setContentText("En attente de transactions...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        if (ACTION_PRINT_TRANSACTION.equals(action) || ACTION_REPRINT_TRANSACTION.equals(action)) {
            AirtelTransaction tx = extractTransaction(intent);
            if (tx != null) {
                printExecutor.execute(() -> handlePrint(tx));
            }
        }
        return START_STICKY;
    }

    private void handlePrint(AirtelTransaction tx) {
        boolean success = printerManager.printTransaction(tx);
        if (success) {
            repository.updatePrintStatus(tx.getId(), AirtelTransaction.PRINT_STATUS_SUCCESS);
        } else {
            repository.updatePrintStatus(tx.getId(), AirtelTransaction.PRINT_STATUS_FAILED);
        }
    }

    private AirtelTransaction extractTransaction(Intent intent) {
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
            return tx;
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (printExecutor != null) printExecutor.shutdown();
    }
}
