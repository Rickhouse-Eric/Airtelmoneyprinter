package com.airtel.moneyprinter.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.airtel.moneyprinter.data.db.AppDatabase;
import com.airtel.moneyprinter.data.db.TransactionDao;
import com.airtel.moneyprinter.data.model.AirtelTransaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private static final String TAG = "TransactionRepository";
    private final TransactionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TransactionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.dao = db.transactionDao();
    }

    public TransactionRepository() {
        android.content.Context ctx = android.app.ActivityThread.currentApplication();
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.dao = db.transactionDao();
    }

    public void insert(AirtelTransaction tx, InsertCallback callback) {
        executor.execute(() -> {
            int count = dao.countBySmsHash(tx.getSmsHash());
            if (count > 0) {
                if (callback != null) callback.onResult(-1, true);
                return;
            }
            long id = dao.insert(tx);
            if (callback != null) callback.onResult(id, false);
        });
    }

    public void insert(AirtelTransaction tx) { insert(tx, null); }

    public void updatePrintStatus(int txId, int status) {
        executor.execute(() -> dao.updatePrintStatus(txId, status));
    }

    public LiveData<List<AirtelTransaction>> getAllTransactionsLive() {
        return dao.getAllTransactionsLive();
    }

    public void delete(AirtelTransaction tx) {
        executor.execute(() -> dao.delete(tx));
    }

    public void deleteAll() {
        executor.execute(dao::deleteAll);
    }

    public interface InsertCallback {
        void onResult(long id, boolean isDuplicate);
    }
}
