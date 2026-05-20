package com.airtel.moneyprinter.printer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.airtel.moneyprinter.data.model.AirtelTransaction;

public class PrinterManager {

    private static final String TAG = "PrinterManager";
    private final Context context;

    // Interface AIDL iPOS Lenvii
    private static final String IPOS_PACKAGE = "com.iposprinter.iposprinterservice";
    private static final String IPOS_ACTION  = "com.iposprinter.iposprinterservice.IPosPrintService";

    public PrinterManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean printTransaction(AirtelTransaction tx) {
        try {
            return printWithIPos(buildTicketText(tx));
        } catch (Exception e) {
            Log.e(TAG, "Erreur impression: " + e.getMessage());
            return false;
        }
    }

    public boolean printTestTicket() {
        AirtelTransaction test = new AirtelTransaction();
        test.setType(AirtelTransaction.TYPE_RECEPTION);
        test.setMontant("25 000 FCFA");
        test.setNomClient("CLIENT TEST");
        test.setNouveauSolde("120 000 FCFA");
        test.setTransactionId("TEST-001");
        test.setDate("01/01/2025");
        test.setHeure("12:00:00");
        test.setRawMessage("TEST - Vous avez recu 25 000 FCFA de CLIENT TEST.");
        return printTransaction(test);
    }

    public String testPort(String port) {
        return "Utilisation SDK iPOS Lenvii";
    }

    private boolean printWithIPos(String text) {
        try {
            // Utilise l'Intent iPOS pour imprimer du texte
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(IPOS_PACKAGE,
                    IPOS_PACKAGE + ".IPosPrintService"));

            // Méthode 1 : via broadcast
            Intent broadcastIntent = new Intent(IPOS_ACTION);
            broadcastIntent.setPackage(IPOS_PACKAGE);
            broadcastIntent.putExtra("printContent", text);
            broadcastIntent.putExtra("printType", 0); // 0 = texte
            context.sendBroadcast(broadcastIntent);

            // Méthode 2 : écriture directe sur ttyMT1
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream("/dev/ttyMT1");
                fos.write(buildEscPosBytes(text));
                fos.flush();
                fos.close();
                Log.i(TAG, "Impression ttyMT1 OK");
                return true;
            } catch (Exception e2) {
                Log.w(TAG, "ttyMT1 failed: " + e2.getMessage());
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "printWithIPos error: " + e.getMessage());
            return false;
        }
    }

    private byte[] buildEscPosBytes(String text) {
        byte[] reset = {0x1B, 0x40};
        byte[] center = {0x1B, 0x61, 0x01};
        byte[] left = {0x1B, 0x61, 0x00};
        byte[] bold_on = {0x1B, 0x45, 0x01};
        byte[] bold_off = {0x1B, 0x45, 0x00};
        byte[] cut = {0x1D, 0x56, 0x41, 0x00};
        byte[] feed = {0x0A, 0x0A, 0x0A};

        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bos.write(reset);
            bos.write(center);
            bos.write(bold_on);
            bos.write("================================\n".getBytes("GBK"));
            bos.write("     AIRTEL MONEY PRINTER
".getBytes("GBK"));
