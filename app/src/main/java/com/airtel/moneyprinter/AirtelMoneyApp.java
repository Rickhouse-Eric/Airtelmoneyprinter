package com.airtel.moneyprinter;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.airtel.moneyprinter.data.db.AppDatabase;
import com.airtel.moneyprinter.utils.AppLogger;

/**
 * Classe Application principale.
 * Initialise la base de données, les canaux de notification et les logs.
 */
public class AirtelMoneyApp extends Application {

    private static final String TAG = "AirtelMoneyApp";

    // Identifiants des canaux de notification
    public static final String CHANNEL_SERVICE_ID = "airtel_service_channel";
    public static final String CHANNEL_PRINT_ID   = "airtel_print_channel";

    // Instance singleton de la BDD
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.init(this);
        Log.i(TAG, "Application démarrée - Airtel Money Printer v1.0");

        // Initialisation Room (base de données locale)
        database = AppDatabase.getInstance(this);

        // Création des canaux de notification (Android 8+)
        createNotificationChannels();
    }

    /**
     * Crée les canaux de notification requis par Android 8+.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Canal service de fond
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_SERVICE_ID,
                    "Service Airtel Money",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Service de surveillance SMS Airtel Money");
            serviceChannel.setShowBadge(false);
            manager.createNotificationChannel(serviceChannel);

            // Canal notifications impression
            NotificationChannel printChannel = new NotificationChannel(
                    CHANNEL_PRINT_ID,
                    "Impressions Airtel Money",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            printChannel.setDescription("Notifications d'impression de reçus");
            printChannel.enableVibration(true);
            manager.createNotificationChannel(printChannel);

            Log.d(TAG, "Canaux de notification créés");
        }
    }

    /**
     * Retourne l'instance de la base de données.
     */
    public static AppDatabase getDatabase() {
        return database;
    }
}
