package com.airtel.moneyprinter;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.airtel.moneyprinter.data.db.AppDatabase;

public class AirtelMoneyApp extends Application {

    public static final String CHANNEL_SERVICE_ID = "airtel_service_channel";
    public static final String CHANNEL_PRINT_ID = "airtel_print_channel";

    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("AirtelMoneyApp", "Démarrage application");

        try {
            database = AppDatabase.getInstance(this);
        } catch (Exception e) {
            Log.e("AirtelMoneyApp", "Erreur BDD: " + e.getMessage());
        }

        try {
            createNotificationChannels();
        } catch (Exception e) {
            Log.e("AirtelMoneyApp", "Erreur canaux: " + e.getMessage());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_SERVICE_ID,
                    "Service Airtel Money",
                    NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(serviceChannel);

            NotificationChannel printChannel = new NotificationChannel(
                    CHANNEL_PRINT_ID,
                    "Impressions",
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(printChannel);
        }
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}
