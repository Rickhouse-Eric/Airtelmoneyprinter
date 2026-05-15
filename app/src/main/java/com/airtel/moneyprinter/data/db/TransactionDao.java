package com.airtel.moneyprinter.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.airtel.moneyprinter.data.model.AirtelTransaction;

import java.util.List;

/**
 * DAO Room pour les transactions Airtel Money.
 */
@Dao
public interface TransactionDao {

    /** Insère une transaction */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(AirtelTransaction transaction);

    /** Met à jour une transaction (ex: statut impression) */
    @Update
    void update(AirtelTransaction transaction);

    /** Supprime une transaction */
    @Delete
    void delete(AirtelTransaction transaction);

    /** Toutes les transactions, les plus récentes en premier (LiveData pour UI réactive) */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    LiveData<List<AirtelTransaction>> getAllTransactionsLive();

    /** Toutes les transactions (synchrone, pour le service) */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<AirtelTransaction> getAllTransactions();

    /** Les N dernières transactions */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    List<AirtelTransaction> getRecentTransactions(int limit);

    /** Recherche par hash SMS (détection doublons) */
    @Query("SELECT COUNT(*) FROM transactions WHERE sms_hash = :hash")
    int countBySmsHash(String hash);

    /** Transactions par statut d'impression */
    @Query("SELECT * FROM transactions WHERE print_status = :status ORDER BY timestamp DESC")
    List<AirtelTransaction> getTransactionsByPrintStatus(int status);

    /** Compte total de transactions */
    @Query("SELECT COUNT(*) FROM transactions")
    int getTotalCount();

    /** Transactions du jour */
    @Query("SELECT * FROM transactions WHERE date = :date ORDER BY timestamp DESC")
    List<AirtelTransaction> getTransactionsByDate(String date);

    /** Supprime toutes les transactions */
    @Query("DELETE FROM transactions")
    void deleteAll();

    /** Met à jour le statut d'impression */
    @Query("UPDATE transactions SET print_status = :status, print_count = print_count + 1 WHERE id = :id")
    void updatePrintStatus(int id, int status);
}
