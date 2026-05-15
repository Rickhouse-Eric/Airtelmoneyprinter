package com.airtel.moneyprinter.parser;

import android.util.Log;

import com.airtel.moneyprinter.data.model.AirtelTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parseur de SMS Airtel Money.
 *
 * Reconnaît et extrait les informations des SMS de transaction
 * provenant du sender "161".
 *
 * Exemples de messages supportés :
 * - "Vous avez reçu 25 000 FCFA de Jean MBOULOU. Nouveau solde : 120 000 FCFA. ID Transaction : TX123456."
 * - "Paiement de 5 000 FCFA effectué chez BOUTIQUE ALPHA. Nouveau solde : 80 000 FCFA. Ref: TX789."
 */
public class AirtelSmsParser {

    private static final String TAG = "AirtelSmsParser";

    // ── Patterns de détection Airtel Money ──────────────────────────────────

    /** Mots-clés indiquant un SMS Airtel Money valide */
    private static final String[] AIRTEL_KEYWORDS = {
            "FCFA", "Airtel Money", "airtel money", "solde", "Transaction", "transaction",
            "reçu", "Paiement", "paiement", "transfert", "Transfert"
    };

    // ── Patterns d'extraction ────────────────────────────────────────────────

    /** Montant reçu : "reçu 25 000 FCFA" ou "reçu 25000 FCFA" */
    private static final Pattern PATTERN_MONTANT_RECU = Pattern.compile(
            "(?:reçu|recu)\\s+(\\d[\\d\\s]*?)\\s*FCFA",
            Pattern.CASE_INSENSITIVE
    );

    /** Montant payé : "Paiement de 5 000 FCFA" */
    private static final Pattern PATTERN_MONTANT_PAIEMENT = Pattern.compile(
            "(?:Paiement|paiement)\\s+de\\s+(\\d[\\d\\s]*?)\\s*FCFA",
            Pattern.CASE_INSENSITIVE
    );

    /** Montant générique : premier montant numérique + FCFA */
    private static final Pattern PATTERN_MONTANT_GENERIQUE = Pattern.compile(
            "(\\d[\\d\\s]{1,12})\\s*FCFA"
    );

    /** Nom du client : "de Jean MBOULOU" */
    private static final Pattern PATTERN_CLIENT = Pattern.compile(
            "(?:de|Du)\\s+([A-ZÀ-Ü][A-Za-zÀ-ÿ]+(?:\\s+[A-ZÀ-Ü][A-Za-zÀ-ÿ]+)*)"
    );

    /** Nouveau solde */
    private static final Pattern PATTERN_SOLDE = Pattern.compile(
            "(?:Nouveau solde|nouveau solde|solde)\\s*[:\\-]?\\s*(\\d[\\d\\s]*?)\\s*FCFA",
            Pattern.CASE_INSENSITIVE
    );

    /** ID Transaction : "TX123456", "Ref: TX789", "Transaction : ABC123" */
    private static final Pattern PATTERN_TRANSACTION_ID = Pattern.compile(
            "(?:ID\\s*Transaction|Ref|Référence|transaction)\\s*[:\\-]?\\s*([A-Z0-9]{4,20})",
            Pattern.CASE_INSENSITIVE
    );

    /** Numéro de téléphone */
    private static final Pattern PATTERN_PHONE = Pattern.compile(
            "(?:\\+?\\d{1,3}[\\s\\-]?)?(?:\\d{2}[\\s\\-]?){4}\\d{2}"
    );

    /** Nom de marchand : "chez BOUTIQUE ALPHA" */
    private static final Pattern PATTERN_MARCHAND = Pattern.compile(
            "chez\\s+([A-ZÀ-Ü][A-ZÀ-Üa-zà-ÿ\\s]{2,40}?)(?:\\.|,|$)",
            Pattern.CASE_INSENSITIVE
    );

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie si un SMS provient d'Airtel Money (sender = "161").
     */
    public static boolean isAirtelSender(String sender) {
        if (sender == null) return false;
        String s = sender.trim();
        return s.equals("161") || s.equalsIgnoreCase("Airtel") || s.equalsIgnoreCase("AirtelMoney");
    }

    /**
     * Vérifie si le corps du SMS contient des marqueurs Airtel Money.
     */
    public static boolean isAirtelMoneyTransaction(String body) {
        if (body == null || body.isEmpty()) return false;
        for (String keyword : AIRTEL_KEYWORDS) {
            if (body.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Parse un SMS Airtel Money et retourne un objet AirtelTransaction.
     * Retourne null si le SMS ne peut pas être parsé correctement.
     */
    public static AirtelTransaction parse(String sender, String body) {
        if (!isAirtelSender(sender) || !isAirtelMoneyTransaction(body)) {
            Log.w(TAG, "SMS ignoré - sender=" + sender);
            return null;
        }

        Log.d(TAG, "Parsing SMS: " + body);

        AirtelTransaction tx = new AirtelTransaction();
        tx.setRawMessage(body);
        tx.setSender(sender);

        // Date et heure actuelles
        Date now = new Date();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat heureFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        tx.setDate(dateFmt.format(now));
        tx.setHeure(heureFmt.format(now));
        tx.setTimestamp(now.getTime());

        // Déterminer le type
        if (body.toLowerCase().contains("reçu") || body.toLowerCase().contains("recu")) {
            tx.setType(AirtelTransaction.TYPE_RECEPTION);
        } else if (body.toLowerCase().contains("paiement")) {
            tx.setType(AirtelTransaction.TYPE_PAIEMENT);
        } else if (body.toLowerCase().contains("transfert")) {
            tx.setType(AirtelTransaction.TYPE_TRANSFERT);
        } else {
            tx.setType(AirtelTransaction.TYPE_AUTRE);
        }

        // Extraction montant
        String montant = extractMontant(body, tx.getType());
        tx.setMontant(montant);

        // Extraction client / expéditeur
        tx.setNomClient(extractClient(body));

        // Extraction marchand (si paiement)
        if (tx.getType().equals(AirtelTransaction.TYPE_PAIEMENT)) {
            String marchand = extractMarchand(body);
            if (marchand != null) tx.setNomClient(marchand);
        }

        // Extraction solde
        tx.setNouveauSolde(extractSolde(body));

        // Extraction ID transaction
        tx.setTransactionId(extractTransactionId(body));

        // Extraction numéro de téléphone
        tx.setNumeroTelephone(extractPhone(body));

        Log.i(TAG, "Transaction parsée: type=" + tx.getType()
                + " montant=" + tx.getMontant()
                + " txId=" + tx.getTransactionId());

        return tx;
    }

    // ── Méthodes d'extraction privées ───────────────────────────────────────

    private static String extractMontant(String body, String type) {
        Pattern p;
        if (AirtelTransaction.TYPE_RECEPTION.equals(type)) {
            p = PATTERN_MONTANT_RECU;
        } else if (AirtelTransaction.TYPE_PAIEMENT.equals(type)) {
            p = PATTERN_MONTANT_PAIEMENT;
        } else {
            p = PATTERN_MONTANT_GENERIQUE;
        }

        Matcher m = p.matcher(body);
        if (m.find()) {
            return cleanNumber(m.group(1)) + " FCFA";
        }
        // Fallback générique
        m = PATTERN_MONTANT_GENERIQUE.matcher(body);
        if (m.find()) {
            return cleanNumber(m.group(1)) + " FCFA";
        }
        return "N/A";
    }

    private static String extractClient(String body) {
        Matcher m = PATTERN_CLIENT.matcher(body);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractMarchand(String body) {
        Matcher m = PATTERN_MARCHAND.matcher(body);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractSolde(String body) {
        Matcher m = PATTERN_SOLDE.matcher(body);
        if (m.find()) return cleanNumber(m.group(1)) + " FCFA";
        return null;
    }

    private static String extractTransactionId(String body) {
        Matcher m = PATTERN_TRANSACTION_ID.matcher(body);
        if (m.find()) return m.group(1).trim().toUpperCase();
        return "N/A";
    }

    private static String extractPhone(String body) {
        Matcher m = PATTERN_PHONE.matcher(body);
        if (m.find()) return m.group().trim();
        return null;
    }

    /** Nettoie un nombre : supprime espaces superflus, garde chiffres */
    private static String cleanNumber(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\s+", " ").trim();
    }
}
