package com.airtel.moneyprinter.printer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.iposprinter.iposprinterservice.IPosPrinterService;

import java.io.FileOutputStream;

public class PrinterManager {

    private static final String TAG = "PrinterManager";
    private final Context context;
    private IPosPrinterService printerService;
    private boolean connected = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            printerService = IPosPrinterService.Stub.asInterface(service);
            connected = true;
            Log.i(TAG, "iPOS connecte!");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            printerService = null;
            connected = false;
            Log.w(TAG, "iPOS deconnecte");
        }
    };

    public PrinterManager(Context context) {
        this.context = context.getApplicationContext();
        bindPrinterService();
    }

    private void bindPrinterService() {
        try {
            Intent intent = new Intent();
            intent.setPackage("com.iposprinter.iposprinterservice");
            intent.setAction("com.iposprinter.iposprinterservice.IPosPrintService");
            boolean bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.i(TAG, "bindService: " + bound);
        } catch (Exception e) {
            Log.e(TAG, "Erreur bind: " + e.getMessage());
        }
    }

    public boolean printTransaction(AirtelTransaction tx) {
        if (connected && printerService != null) {
            return printWithIPOS(tx);
        }
        Log.w(TAG, "Service non connecte, tentative serie");
        return printViaSerial(tx);
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
        test.setRawMessage("TEST - Vous avez recu 25 000 FCFA.");
        return printTransaction(test);
    }

    public String testPort(String port) {
        if (connected) return "OK - iPOS Service connecte";
        bindPrinterService();
        return "Connexion iPOS en cours...";
    }

    private boolean printWithIPOS(AirtelTransaction tx) {
        try {
            printerService.printerInit(null);
            Thread.sleep(300);
            printerService.setPrinterPrintAlignment(1, null);
            printerService.printText("================================", null);
            printerService.printText("      AIRTEL MONEY", null);
            printerService.printText("  NOTIFICATION TRANSACTION", null);
            printerService.printText("================================", null);
            printerService.setPrinterPrintAlignment(0, null);
            printerService.printText("", null);
            printerService.printText("Type    : " + tx.getTypeLabel(), null);
            printerService.printText("Montant : " + (tx.getMontant() != null ? tx.getMontant() : "N/A"), null);
            if (tx.getNomClient() != null)
                printerService.printText("Client  : " + tx.getNomClient(), null);
            printerService.printText("Tx ID   : " + tx.getTransactionId(), null);
            printerService.printText("Date    : " + tx.getDate() + " " + tx.getHeure(), null);
            if (tx.getNouveauSolde() != null)
                printerService.printText("Solde   : " + tx.getNouveauSolde(), null);
            printerService.printText("--------------------------------", null);
            printerService.setPrinterPrintAlignment(1, null);
            printerService.printText("Merci d'utiliser Airtel Money", null);
            printerService.printText("================================", null);
            printerService.printerPerformPrint(5, null);
            Log.i(TAG, "Impression iPOS OK");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur iPOS: " + e.getMessage());
            return printViaSerial(tx);
        }
    }

    private boolean printViaSerial(AirtelTransaction tx) {
        try {
            FileOutputStream fos = new FileOutputStream("/dev/ttyMT1");
            fos.write(new byte[]{0x1B, 0x40});
            fos.write(("AIRTEL MONEY\n").getBytes());
            fos.write(("Montant: " + tx.getMontant() + "\n").getBytes());
            fos.write(("Tx: " + tx.getTransactionId() + "\n").getBytes());
            fos.write(new byte[]{0x0A, 0x0A, 0x0A});
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Serial error: " + e.getMessage());
            return false;
        }
    }
}
