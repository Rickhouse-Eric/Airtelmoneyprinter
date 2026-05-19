package com.airtel.moneyprinter;

import android.app.Application;
import android.util.Log;

public class AirtelMoneyApp extends Application {

    public static final String CHANNEL_SERVICE_ID = "airtel_service";
    public static final String CHANNEL_PRINT_ID = "airtel_print";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("AirtelMoneyApp", "OK");
    }

    public static com.airtel.moneyprinter.data.db.AppDatabase getDatabase() {
        return null;
    }
}
