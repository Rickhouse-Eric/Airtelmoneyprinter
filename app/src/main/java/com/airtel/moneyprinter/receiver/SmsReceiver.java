package com.airtel.moneyprinter.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.airtel.moneyprinter.data.repository.TransactionRepository;
import com.airtel.moneyprinter.parser.AirtelSmsParser;
import com.airtel.moneyprinter.service.PrintForegroundService;
import com.airtel.moneyprinter.utils.AppLogger;

/**
 * BroadcastReceiver pour l'interception des SMS entrants.
 *
 * Priorité maximale (2147483647) pour intercepter avant les autres apps.
 * Filtre : sender "161" uniquement + contenu Airtel Money.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        // Vérifier que l'impression automatique est activée
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoPrintEnabled = prefs.getBoolean("auto_print_enabled", true);

        Log.d(TAG, "SMS reçu - impression auto: " + autoPrintEnabled);
        AppLogger.log(TAG, "SMS entrant intercepté");

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;

        String format = bundle.getString("format");

        // Reconstruction du message complet (multi-parts SMS)
        StringBuilder fullBody = new StringBuilder();
        String sender = null;

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (format != null) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (sms == null) continue;

            if (sender == null) {
                sender = sms.getDisplayOriginatingAddress();
            }
            fullBody.append(sms.getMessageBody());
        }

        String body = fullBody.toString().trim();
        Log.d(TAG, "Sender: " + sender + " | Body: " + body);

        // ── Vérification sender Airtel (161) ─────────────────────────────
        if (!AirtelSmsParser.isAirtelSender(sender)) {
            Log.d(TAG, "Sender non Airtel, ignoré: " + sender);
            return;
        }

        // ── Vérification contenu Airtel Money ────────────────────────────
        if (!AirtelSmsParser.isAirtelMoneyTransaction(body)) {
            Log.d(TAG, "SMS Airtel mais pas une transaction Money, ignoré");
            return;
        }

        Log.i(TAG, "Transaction Airtel Money détectée!");
        AppLogger.log(TAG, "Transaction Airtel Money détectée: " + sender);

        // ── Parsing du SMS ────────────────────────────────────────────────
        AirtelTransaction transaction = AirtelSmsParser.parse(sender, body);
        if (transaction == null) {
            Log.e(TAG, "Échec parsing SMS");
            return;
        }

        // ── Sauvegarde en BDD (vérification doublon incluse) ──────────────
        TransactionRepository repo = new TransactionRepository();
        repo.insert(transaction, (id, isDuplicate) -> {
            if (isDuplicate) {
                Log.w(TAG, "SMS doublon détecté, impression annulée");
                return;
            }
            transaction.setId((int) id);
            Log.i(TAG, "Transaction sauvegardée avec id=" + id);

            // ── Déclenchement impression si auto activée ──────────────────
            if (autoPrintEnabled) {
                triggerPrint(context, transaction);
            } else {
                Log.i(TAG, "Impression auto désactivée, transaction sauvegardée uniquement");
            }
        });
    }

    /**
     * Lance le service d'impression en foreground.
     */
    private void triggerPrint(Context context, AirtelTransaction tx) {
        Intent serviceIntent = new Intent(context, PrintForegroundService.class);
        serviceIntent.setAction(PrintForegroundService.ACTION_PRINT_TRANSACTION);
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TRANSACTION_ID, tx.getId());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_MONTANT, tx.getMontant());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_CLIENT, tx.getNomClient());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_SOLDE, tx.getNouveauSolde());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_ID_TX, tx.getTransactionId());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_DATE, tx.getDate());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_HEURE, tx.getHeure());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_TYPE, tx.getType());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_RAW, tx.getRawMessage());
        serviceIntent.putExtra(PrintForegroundService.EXTRA_TX_PHONE, tx.getNumeroTelephone());

        context.startForegroundService(serviceIntent);
        Log.i(TAG, "Service d'impression démarré pour tx=" + tx.getTransactionId());
    }
}
