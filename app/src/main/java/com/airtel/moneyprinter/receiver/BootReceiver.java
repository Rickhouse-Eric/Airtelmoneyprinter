package com.airtel.moneyprinter.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.airtel.moneyprinter.service.PrintForegroundService;

/**
 * Receiver déclenché au démarrage du TPE.
 * Relance le service de fond automatiquement.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.i(TAG, "Boot détecté - démarrage du service Airtel Money Printer");

            Intent serviceIntent = new Intent(context, PrintForegroundService.class);
            serviceIntent.setAction(PrintForegroundService.ACTION_START_SERVICE);
            context.startForegroundService(serviceIntent);
        }
    }
}
