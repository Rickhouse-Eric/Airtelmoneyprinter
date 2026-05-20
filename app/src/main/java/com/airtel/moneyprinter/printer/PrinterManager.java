package com.airtel.moneyprinter.printer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.airtel.moneyprinter.data.model.AirtelTransaction;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

public class PrinterManager {

    private static final String TAG = "PrinterManager";
    private final Context context;

    public PrinterManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean printTransaction(AirtelTransaction tx) {
        if (printViaIPoS(buildTicketText(tx))) return true;
        return printViaSerial(buildTicketBytes(tx));
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
        try {
            FileOutputStream fos = new FileOutputStream("/dev/ttyMT1");
            fos.write(new byte[]{0x1B, 0x40});
            fos.close();
            return "OK - /dev/ttyMT1 accessible";
        } catch (Exception e) {
            return "ERREUR - " + port + ": " + e.getMessage();
        }
    }

    private boolean printViaIPoS(String text) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.iposprinter.iposprinterservice",
                "com.iposprinter.iposprinterservice.IPosPrintService"
            ));
            intent.setAction("com.iposprinter.iposprinterservice.IPosPrintService");
            intent.putExtra("printText", text);
            intent.putExtra("printType", 1);
            context.startService(intent);
            Log.i(TAG, "iPOS service appelé");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "iPOS failed: " + e.getMessage());
            return false;
        }
    }

    private boolean printViaSerial(byte[] data) {
        String[] ports = {"/dev/ttyMT1", "/dev/ttyMT0"};
        for (String port : ports) {
            try {
                FileOutputStream fos = new FileOutputStream(port);
                fos.write(data);
                fos.flush();
                fos.close();
                Log.i(TAG, "Serial OK: " + port);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Serial failed " + port + ": " + e.getMessage());
            }
        }
        return false;
    }

    private byte[] buildTicketBytes(AirtelTransaction tx) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(new byte[]{0x1B, 0x40});
            bos.write(new byte[]{0x1B, 0x61, 0x01});
            bos.write(new byte[]{0x1B, 0x45, 0x01});
            bos.write("================================\n".getBytes());
            bos.write("      AIRTEL MONEY\n".getBytes());
            bos.write("NOTIFICATION TRANSACTION\n".getBytes());
            bos.write("================================\n".getBytes());
            bos.write(new byte[]{0x1B, 0x45, 0x00});
            bos.write(new byte[]{0x1B, 0x61, 0x00});
            bos.write(("\nType    : " + tx.getTypeLabel() + "\n").getBytes());
            bos.write(("Montant : " + (tx.getMontant() != null ? tx.getMontant() : "N/A") + "\n").getBytes());
            if (tx.getNomClient() != null)
                bos.write(("Client  : " + tx.getNomClient() + "\n").getBytes());
            bos.write(("Tx ID   : " + tx.getTransactionId() + "\n").getBytes());
            bos.write(("Date    : " + tx.getDate() + " " + tx.getHeure() + "\n").getBytes());
            if (tx.getNouveauSolde() != null)
                bos.write(("Solde   : " + tx.getNouveauSolde() + "\n").getBytes());
            bos.write("--------------------------------\n".getBytes());
            bos.write(new byte[]{0x1B, 0x61, 0x01});
            bos.write(new byte[]{0x1B, 0x45, 0x01});
            bos.write("Merci d'utiliser Airtel Money\n".getBytes());
            bos.write(new byte[]{0x1B, 0x45, 0x00});
            bos.write(new byte[]{0x0A, 0x0A, 0x0A});
            bos.write(new byte[]{0x1D, 0x56, 0x41, 0x00});
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String buildTicketText(AirtelTransaction tx) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================\n");
        sb.append("      AIRTEL MONEY\n");
        sb.append("NOTIFICATION TRANSACTION\n");
        sb.append("================================\n\n");
        sb.append("Type    : ").append(tx.getTypeLabel()).append("\n");
        sb.append("Montant : ").append(tx.getMontant() != null ? tx.getMontant() : "N/A").append("\n");
        if (tx.getNomClient() != null)
            sb.append("Client  : ").append(tx.getNomClient()).append("\n");
        sb.append("Tx ID   : ").append(tx.getTransactionId()).append("\n");
        sb.append("Date    : ").append(tx.getDate()).append(" ").append(tx.getHeure()).append("\n");
        if (tx.getNouveauSolde() != null)
            sb.append("Solde   : ").append(tx.getNouveauSolde()).append("\n");
        sb.append("--------------------------------\n");
        sb.append("Merci d'utiliser Airtel Money\n");
        return sb.toString();
    }
}
