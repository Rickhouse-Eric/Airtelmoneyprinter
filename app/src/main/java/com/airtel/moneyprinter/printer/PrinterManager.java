package com.airtel.moneyprinter.printer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.airtel.moneyprinter.R;
import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.FileOutputStream;

public class PrinterManager {

    private static final String TAG = "PrinterManager";
    private static final String[] SERIAL_PORTS = {"/dev/ttyMT1", "/dev/ttyMT0", "/dev/ttyS1", "/dev/ttyS0"};
    private static final int BAUD_RATE = 9600;
    private static final int PRINTER_DPI = 203;
    private static final int PRINTER_WIDTH = 58;

    private final Context context;
    private final SharedPreferences prefs;

    public PrinterManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public boolean printTransaction(AirtelTransaction tx) {
        return printWithRawSerial(tx);
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
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(port);
            fos.write(new byte[]{0x1B, 0x40}); // ESC @ reset
            fos.close();
            return "OK - Port " + port + " accessible";
        } catch (Exception e) {
            return "ERREUR - " + port + ": " + e.getMessage();
        }
    }

    private boolean printWithRawSerial(AirtelTransaction tx) {
        String preferredPort = prefs.getString("printer_port", "auto");
        String[] portsToTry = "auto".equals(preferredPort) ? SERIAL_PORTS : new String[]{preferredPort};

        for (String port : portsToTry) {
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(port);
                byte[] data = buildRawTicket(tx);
                fos.write(data);
                fos.flush();
                fos.close();
                Log.i(TAG, "Impression reussie sur " + port);
                prefs.edit().putString("last_working_port", port).apply();
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Echec sur " + port + ": " + e.getMessage());
            }
        }
        return false;
    }

    private byte[] buildRawTicket(AirtelTransaction tx) {
        StringBuilder sb = new StringBuilder();

        // Reset imprimante
        sb.append("\u001B@");

        // Centrer + titre
        sb.append("\u001Ba\u0001");
        sb.append("================================\n");
        sb.append("\u001BE\u0001"); // gras ON
        sb.append("AIRTEL MONEY\n");
        sb.append("\u001BE\u0000"); // gras OFF
        sb.append("NOTIFICATION TRANSACTION\n");
        sb.append("================================\n\n");

        // Type
        sb.append("\u001BE\u0001");
        sb.append("** " + tx.getTypeLabel().toUpperCase() + " **\n\n");
        sb.append("\u001BE\u0000");

        // Montant centré en grand
        sb.append("MONTANT :\n");
        sb.append("\u001BE\u0001");
        sb.append(tx.getMontant() != null ? tx.getMontant() : "N/A");
        sb.append("\n\n");
        sb.append("\u001BE\u0000");

        // Aligner gauche pour les détails
        sb.append("\u001Ba\u0000");
        sb.append("--------------------------------\n");

        if (tx.getNomClient() != null && !tx.getNomClient().isEmpty()) {
            sb.append("Client       : " + tx.getNomClient() + "\n");
        }
        sb.append("Transaction  : " + (tx.getTransactionId() != null ? tx.getTransactionId() : "N/A") + "\n");
        sb.append("Date         : " + (tx.getDate() != null ? tx.getDate() : "") + "\n");
        sb.append("Heure        : " + (tx.getHeure() != null ? tx.getHeure() : "") + "\n");
        if (tx.getNouveauSolde() != null && !tx.getNouveauSolde().isEmpty()) {
            sb.append("Nouveau solde: " + tx.getNouveauSolde() + "\n");
        }
        sb.append("--------------------------------\n\n");

        // Message original
        sb.append("Message original :\n");
        if (tx.getRawMessage() != null) {
            String raw = tx.getRawMessage();
            for (int i = 0; i < raw.length(); i += 30) {
                sb.append(raw.substring(i, Math.min(i + 30, raw.length()))).append("\n");
            }
        }
        sb.append("\n");

        // Pied de page centré
        sb.append("\u001Ba\u0001");
        sb.append("================================\n");
        sb.append("\u001BE\u0001");
        sb.append("Merci d'utiliser Airtel Money\n");
        sb.append("\u001BE\u0000");
        sb.append("================================\n");

        // Sauts de ligne + coupe papier
        sb.append("\n\n\n");
        sb.append("\u001Bi"); // coupe papier

        return sb.toString().getBytes();
    }
}
