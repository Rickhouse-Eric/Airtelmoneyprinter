package com.airtel.moneyprinter.ui.main;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.airtel.moneyprinter.data.repository.TransactionRepository;

import java.util.List;

/**
 * ViewModel principal - Architecture MVVM.
 * Survit aux rotations d'écran et fournit les données en LiveData.
 */
public class MainViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<AirtelTransaction>> allTransactions;

    public MainViewModel(Application application) {
        super(application);
        repository = new TransactionRepository(application);
        allTransactions = repository.getAllTransactionsLive();
    }

    public LiveData<List<AirtelTransaction>> getAllTransactions() {
        return allTransactions;
    }

    public void deleteTransaction(AirtelTransaction tx) {
        repository.delete(tx);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public void updatePrintStatus(int id, int status) {
        repository.updatePrintStatus(id, status);
    }
}
