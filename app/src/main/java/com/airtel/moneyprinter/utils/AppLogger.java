package com.airtel.moneyprinter.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logger applicatif avec export vers fichier texte.
 * Enregistre tous les événements importants pour le débogage.
 */
public class AppLogger {

    private static final String TAG = "AppLogger";
    private static final String LOG_FILENAME = "airtel_printer.log";
    private static File logFile;
    private static boolean initialized = false;

    public static void init(Context context) {
        try {
            File logDir = context.getExternalFilesDir(null);
            if (logDir == null) logDir = context.getFilesDir();
            logFile = new File(logDir, LOG_FILENAME);
            initialized = true;
            log("AppLogger", "Logger initialisé - " + logFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Erreur init logger: " + e.getMessage());
        }
    }

    public static void log(String tag, String message) {
        Log.i(tag, message);
        if (!initialized || logFile == null) return;

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String line = "[" + timestamp + "] [" + tag + "] " + message + "\n";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
        } catch (IOException e) {
            Log.w(TAG, "Erreur écriture log: " + e.getMessage());
        }
    }

    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "N/A";
    }

    public static void clearLogs() {
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }
}
