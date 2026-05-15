package com.airtel.moneyprinter.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.airtel.moneyprinter.AirtelMoneyApp;
import com.airtel.moneyprinter.data.db.AppDatabase;
import com.airtel.moneyprinter.data.db.TransactionDao;
import com.airtel.moneyprinter.data.model.AirtelTransaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository pattern pour les transactions.
 * Abstrait la source de données (Room) de l'UI.
 */
public class TransactionRepository {

    private static final String TAG = "TransactionRepository";

    private final TransactionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TransactionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.dao = db.transactionDao();
    }

    public TransactionRepository() {
        this.dao = AirtelMoneyApp.getDatabase().transactionDao();
    }

    /** Insère une transaction (async) */
    public void insert(AirtelTransaction tx, InsertCallback callback) {
        executor.execute(() -> {
            // Vérification doublon
            int count = dao.countBySmsHash(tx.getSmsHash());
            if (count > 0) {
                Log.w(TAG, "Doublon détecté, SMS ignoré: " + tx.getSmsHash());
                if (callback != null) callback.onResult(-1, true);
                return;
            }
            long id = dao.insert(tx);
            Log.d(TAG, "Transaction insérée avec id=" + id);
            if (callback != null) callback.onResult(id, false);
        });
    }

    /** Insère sans callback */
    public void insert(AirtelTransaction tx) {
        insert(tx, null);
    }

    /** Met à jour le statut d'impression */
    public void updatePrintStatus(int txId, int status) {
        executor.execute(() -> dao.updatePrintStatus(txId, status));
    }

    /** Retourne toutes les transactions (LiveData) */
    public LiveData<List<AirtelTransaction>> getAllTransactionsLive() {
        return dao.getAllTransactionsLive();
    }

    /** Supprime une transaction */
    public void delete(AirtelTransaction tx) {
        executor.execute(() -> dao.delete(tx));
    }

    /** Supprime tout l'historique */
    public void deleteAll() {
        executor.execute(dao::deleteAll);
    }

    /** Callback pour l'insertion */
    public interface InsertCallback {
        void onResult(long id, boolean isDuplicate);
    }
}
