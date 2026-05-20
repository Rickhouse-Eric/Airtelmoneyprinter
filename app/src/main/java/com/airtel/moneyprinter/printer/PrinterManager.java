package com.airtel.moneyprinter.printer;

import android.content.Context;
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
        try {
            byte[] data = buildTicket(tx);
            FileOutputStream fos = new FileOutputStream("/dev/ttyMT1");
            fos.write(data);
            fos.flush();
            fos.close();
            Log.i(TAG, "Impression OK");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
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

    private byte[] buildTicket(AirtelTransaction tx) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(new byte[]{0x1B, 0x40});
        bos.write(new byte[]{0x1B, 0x61, 0x01});
        bos.write(new byte[]{0x1B, 0x45, 0x01});
        bos.write("================================\n".getBytes());
        bos.write("      AIRTEL MONEY\n".getBytes());
        bos.write("================================\n".getBytes());
        bos.write("  NOTIFICATION TRANSACTION\n".getBytes());
        bos.write("================================\n".getBytes());
        bos.write(new byte[]{0x1B, 0x45, 0x00});
        bos.write(new byte[]{0x1B, 0x61, 0x00});
        bos.write("\n".getBytes());
        bos.write(("Type        : " + tx.getTypeLabel() + "\n").getBytes());
        bos.write(("Montant     : " + (tx.getMontant() != null ? tx.getMontant() : "N/A") + "\n").getBytes());
        if (tx.getNomClient() != null) {
            bos.write(("Client      : " + tx.getNomClient() + "\n").getBytes());
        }
        bos.write(("Transaction : " + tx.getTransactionId() + "\n").getBytes());
        bos.write(("Date        : " + tx.getDate() + "\n").getBytes());
        bos.write(("Heure       : " + tx.getHeure() + "\n").getBytes());
        if (tx.getNouveauSolde() != null) {
            bos.write(("Solde       : " + tx.getNouveauSolde() + "\n").getBytes());
        }
        bos.write("--------------------------------\n".getBytes());
        if (tx.getRawMessage() != null) {
            bos.write("Message:\n".getBytes());
            String raw = tx.getRawMessage();
            for (int i = 0; i < raw.length(); i += 30) {
                bos.write(raw.substring(i, Math.min(i + 30, raw.length())).getBytes());
                bos.write("\n".getBytes());
            }
        }
        bos.write(new byte[]{0x1B, 0x61, 0x01});
        bos.write(new byte[]{0x1B, 0x45, 0x01});
        bos.write("================================\n".getBytes());
        bos.write("Merci d'utiliser Airtel Money\n".getBytes());
        bos.write("================================\n".getBytes());
        bos.write(new byte[]{0x1B, 0x45, 0x00});
        bos.write(new byte[]{0x0A, 0x0A, 0x0A});
        bos.write(new byte[]{0x1D, 0x56, 0x41, 0x00});
        return bos.toByteArray();
    }
}
