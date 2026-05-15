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
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;

/**
 * Gestionnaire d'impression ESC/POS pour TPE Lenvii.
 *
 * Gère la connexion aux ports série (/dev/ttyS0, /dev/ttyS1, /dev/ttyUSB0),
 * la génération du reçu formaté et l'impression du logo Airtel Money.
 *
 * Bibliothèque : ESCPOS-ThermalPrinter-Android de DantSu (v3.3.0)
 */
public class PrinterManager {

    private static final String TAG = "PrinterManager";

    // Ports série à tester (ordre de priorité pour Lenvii)
    private static final String[] SERIAL_PORTS = {
            "/dev/ttyS1",
            "/dev/ttyS0",
            "/dev/ttyUSB0"
    };

    // Baudrate standard Lenvii
    private static final int BAUD_RATE = 115200;

    // Largeur de l'imprimante 58mm (en nombre de caractères)
    private static final int PRINTER_DPI   = 203;
    private static final int PRINTER_WIDTH  = 58;

    private final Context context;
    private final SharedPreferences prefs;

    public PrinterManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    // ────────────────────────────────────────────────────────────────────────
    // API publique
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Imprime un reçu de transaction Airtel Money.
     * Teste automatiquement les ports série disponibles.
     *
     * @return true si impression réussie, false sinon
     */
    public boolean printTransaction(AirtelTransaction tx) {
        String preferredPort = prefs.getString("printer_port", "auto");

        if ("auto".equals(preferredPort)) {
            return printOnAnyPort(tx);
        } else {
            return printOnPort(preferredPort, tx);
        }
    }

    /**
     * Imprime un ticket de test.
     * @return true si succès
     */
    public boolean printTestTicket() {
        AirtelTransaction test = new AirtelTransaction();
        test.setType(AirtelTransaction.TYPE_RECEPTION);
        test.setMontant("25 000 FCFA");
        test.setNomClient("CLIENT TEST");
        test.setNouveauSolde("120 000 FCFA");
        test.setTransactionId("TEST-001");
        test.setDate("01/01/2025");
        test.setHeure("12:00:00");
        test.setRawMessage("TEST - Vous avez reçu 25 000 FCFA de CLIENT TEST.");
        return printTransaction(test);
    }

    /**
     * Teste la connexion à un port série.
     * @return message de statut
     */
    public String testPort(String port) {
        try {
            SerialConnection connection = new SerialConnection(port, BAUD_RATE);
            EscPosPrinter printer = new EscPosPrinter(connection, PRINTER_DPI, PRINTER_WIDTH, 32);
            printer.disconnectPrinter();
            return "OK - Port " + port + " accessible";
        } catch (Exception e) {
            return "ERREUR - " + port + ": " + e.getMessage();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ────────────────────────────────────────────────────────────────────────

    /** Essaie l'impression sur tous les ports jusqu'au succès */
    private boolean printOnAnyPort(AirtelTransaction tx) {
        for (String port : SERIAL_PORTS) {
            Log.d(TAG, "Tentative impression sur: " + port);
            if (printOnPort(port, tx)) {
                // Mémorise le port qui fonctionne
                prefs.edit().putString("last_working_port", port).apply();
                return true;
            }
        }
        Log.e(TAG, "Échec sur tous les ports série");
        return false;
    }

    /** Imprime sur un port série spécifique */
    private boolean printOnPort(String port, AirtelTransaction tx) {
        EscPosPrinter printer = null;
        try {
            SerialConnection connection = new SerialConnection(port, BAUD_RATE);
            printer = new EscPosPrinter(connection, PRINTER_DPI, PRINTER_WIDTH, 32);

            // Construit et imprime le ticket
            String ticket = buildTicket(tx);
            printer.printFormattedTextAndCut(ticket);

            Log.i(TAG, "Impression réussie sur " + port);
            return true;

        } catch (EscPosConnectionException e) {
            Log.w(TAG, "Connexion impossible sur " + port + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Erreur impression sur " + port + ": " + e.getMessage());
            return false;
        } finally {
            if (printer != null) {
                try {
                    printer.disconnectPrinter();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Construit le texte formaté ESC/POS du reçu.
     *
     * Syntaxe DantSu :
     * [L]  = Align Left
     * [C]  = Align Center
     * [R]  = Align Right
     * [B]  = Bold
     * [U]  = Underline
     * [BIG]= Double taille
     * [NORMAL] = Taille normale
     */
    private String buildTicket(AirtelTransaction tx) {
        StringBuilder sb = new StringBuilder();

        // ── Logo Airtel Money (image bitmap) ──────────────────────────────
        // Note: DantSu supporte l'impression d'images via printFormattedText
        // Le logo est inséré comme image bitmap redimensionnée à 150px de large
        // Si l'image n'est pas disponible, on affiche le texte de remplacement
        try {
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.airtel_logo);
            if (logo != null) {
                // Resize logo pour 58mm (max ~384px à 203dpi, ici ~150px pour centrage)
                int targetWidth = 150;
                int targetHeight = (int) (logo.getHeight() * ((float) targetWidth / logo.getWidth()));
                Bitmap resized = Bitmap.createScaledBitmap(logo, targetWidth, targetHeight, true);
                // L'image sera insérée via le tag [C] avant le texte
                // DantSu gère les images séparément ; on procède texte complet ci-après
                resized.recycle();
            }
        } catch (Exception e) {
            Log.w(TAG, "Logo non disponible, utilisation texte: " + e.getMessage());
        }

        // ── En-tête ───────────────────────────────────────────────────────
        sb.append("[C]<img>" + getLogoBase64Tag() + "</img>\n");
        sb.append("[C]================================\n");
        sb.append("[C]<b><font size='big'>AIRTEL MONEY</font></b>\n");
        sb.append("[C]================================\n");
        sb.append("[C]<b>NOTIFICATION TRANSACTION</b>\n");
        sb.append("[C]--------------------------------\n");
        sb.append("\n");

        // ── Type de transaction ───────────────────────────────────────────
        sb.append("[C]<b>** ").append(tx.getTypeLabel().toUpperCase()).append(" **</b>\n");
        sb.append("\n");

        // ── Montant ───────────────────────────────────────────────────────
        sb.append("[L]<b>MONTANT :</b>\n");
        sb.append("[C]<font size='big'><b>").append(tx.getMontant()).append("</b></font>\n");
        sb.append("\n");

        // ── Détails ───────────────────────────────────────────────────────
        sb.append("[L]--------------------------------\n");

        if (tx.getNomClient() != null && !tx.getNomClient().isEmpty()) {
            sb.append("[L]Client       : ").append(tx.getNomClient()).append("\n");
        }

        sb.append("[L]Transaction  : ").append(tx.getTransactionId()).append("\n");
        sb.append("[L]Date         : ").append(tx.getDate()).append("\n");
        sb.append("[L]Heure        : ").append(tx.getHeure()).append("\n");

        if (tx.getNouveauSolde() != null && !tx.getNouveauSolde().isEmpty()) {
            sb.append("[L]Nouveau solde: ").append(tx.getNouveauSolde()).append("\n");
        }

        if (tx.getNumeroTelephone() != null && !tx.getNumeroTelephone().isEmpty()) {
            sb.append("[L]Tel          : ").append(tx.getNumeroTelephone()).append("\n");
        }

        sb.append("[L]--------------------------------\n");
        sb.append("\n");

        // ── Message original ─────────────────────────────────────────────
        sb.append("[L]<b>Message original :</b>\n");
        sb.append("[L]").append(wrapText(tx.getRawMessage(), 32)).append("\n");
        sb.append("\n");

        // ── Pied de page ─────────────────────────────────────────────────
        sb.append("[C]================================\n");
        sb.append("[C]<b>Merci d'utiliser Airtel Money</b>\n");
        sb.append("[C]www.airtel.com\n");
        sb.append("[C]================================\n");
        sb.append("\n\n\n");

        return sb.toString();
    }

    /**
     * Retourne le tag image DantSu pour le logo Airtel Money.
     * Si le logo est en ressource drawable, il est encodé en base64.
     * Sinon, retourne une chaîne vide (le texte de remplacement est utilisé).
     */
    private String getLogoBase64Tag() {
        try {
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.airtel_logo);
            if (logo != null) {
                // Redimensionnement pour 58mm
                int w = Math.min(logo.getWidth(), 200);
                int h = (int) (logo.getHeight() * ((float) w / logo.getWidth()));
                Bitmap scaled = Bitmap.createScaledBitmap(logo, w, h, true);

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] bytes = baos.toByteArray();
                String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);

                scaled.recycle();
                logo.recycle();
                return b64;
            }
        } catch (Exception e) {
            Log.w(TAG, "Impossible d'encoder le logo: " + e.getMessage());
        }
        return "";
    }

    /**
     * Découpe un texte long en lignes de maxWidth caractères.
     */
    private String wrapText(String text, int maxWidth) {
        if (text == null) return "";
        if (text.length() <= maxWidth) return text;

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + maxWidth, text.length());
            result.append(text, i, end);
            if (end < text.length()) result.append("\n[L]");
            i = end;
        }
        return result.toString();
    }
}
