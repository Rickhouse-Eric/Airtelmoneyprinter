package com.airtel.moneyprinter;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.airtel.moneyprinter.data.db.AppDatabase;

public class AirtelMoneyApp extends Application {

    public static final String CHANNEL_SERVICE_ID = "airtel_service";
    public static final String CHANNEL_PRINT_ID = "airtel_print";
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("AirtelMoneyApp", "Démarrage");
        database = AppDatabase.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) {
                mgr.createNotificationChannel(new NotificationChannel(
                    CHANNEL_SERVICE_ID, "Service", NotificationManager.IMPORTANCE_LOW));
                mgr.createNotificationChannel(new NotificationChannel(
                    CHANNEL_PRINT_ID, "Impressions", NotificationManager.IMPORTANCE_DEFAULT));
            }
        }
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}
