package com.airtel.moneyprinter.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Modèle de données représentant une transaction Airtel Money.
 * Persisté en base de données Room.
 */
@Entity(tableName = "transactions")
public class AirtelTransaction {

    // Constantes de type de transaction
    public static final String TYPE_RECEPTION  = "RECEPTION";
    public static final String TYPE_PAIEMENT   = "PAIEMENT";
    public static final String TYPE_TRANSFERT  = "TRANSFERT";
    public static final String TYPE_AUTRE      = "AUTRE";

    // Statuts d'impression
    public static final int PRINT_STATUS_PENDING  = 0;
    public static final int PRINT_STATUS_SUCCESS  = 1;
    public static final int PRINT_STATUS_FAILED   = 2;

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "sender")
    private String sender;

    @ColumnInfo(name = "raw_message")
    private String rawMessage;

    @ColumnInfo(name = "type")
    private String type;

    @ColumnInfo(name = "montant")
    private String montant;

    @ColumnInfo(name = "nom_client")
    private String nomClient;

    @ColumnInfo(name = "nouveau_solde")
    private String nouveauSolde;

    @ColumnInfo(name = "transaction_id")
    private String transactionId;

    @ColumnInfo(name = "date")
    private String date;

    @ColumnInfo(name = "heure")
    private String heure;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "numero_telephone")
    private String numeroTelephone;

    @ColumnInfo(name = "print_status")
    private int printStatus = PRINT_STATUS_PENDING;

    @ColumnInfo(name = "print_count")
    private int printCount = 0;

    @ColumnInfo(name = "sms_hash")
    private String smsHash; // Pour détecter les doublons

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
        // Génère automatiquement le hash pour détecter les doublons
        this.smsHash = String.valueOf(rawMessage.hashCode());
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMontant() { return montant; }
    public void setMontant(String montant) { this.montant = montant; }

    public String getNomClient() { return nomClient; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }

    public String getNouveauSolde() { return nouveauSolde; }
    public void setNouveauSolde(String nouveauSolde) { this.nouveauSolde = nouveauSolde; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getHeure() { return heure; }
    public void setHeure(String heure) { this.heure = heure; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getNumeroTelephone() { return numeroTelephone; }
    public void setNumeroTelephone(String numeroTelephone) { this.numeroTelephone = numeroTelephone; }

    public int getPrintStatus() { return printStatus; }
    public void setPrintStatus(int printStatus) { this.printStatus = printStatus; }

    public int getPrintCount() { return printCount; }
    public void setPrintCount(int printCount) { this.printCount = printCount; }

    public String getSmsHash() { return smsHash; }
    public void setSmsHash(String smsHash) { this.smsHash = smsHash; }

    /** Libellé lisible du type */
    public String getTypeLabel() {
        switch (type) {
            case TYPE_RECEPTION: return "Réception";
            case TYPE_PAIEMENT:  return "Paiement";
            case TYPE_TRANSFERT: return "Transfert";
            default:             return "Transaction";
        }
    }

    /** Libellé lisible du statut d'impression */
    public String getPrintStatusLabel() {
        switch (printStatus) {
            case PRINT_STATUS_SUCCESS: return "Imprimé";
            case PRINT_STATUS_FAILED:  return "Échec";
            default:                   return "En attente";
        }
    }
}
