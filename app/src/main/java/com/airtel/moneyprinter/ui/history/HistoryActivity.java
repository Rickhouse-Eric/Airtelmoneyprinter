package com.airtel.moneyprinter.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.airtel.moneyprinter.databinding.ActivityHistoryBinding;
import com.airtel.moneyprinter.service.PrintForegroundService;
import com.airtel.moneyprinter.ui.main.MainViewModel;

/**
 * Activité affichant l'historique de toutes les transactions imprimées.
 */
public class HistoryActivity extends AppCompatActivity
        implements TransactionAdapter.OnTransactionListener {

    private ActivityHistoryBinding binding;
    private MainViewModel viewModel;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Historique des transactions");
        }

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupRecyclerView();
        observeData();

        binding.btnClearHistory.setOnClickListener(v -> confirmClear());
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getAllTransactions().observe(this, transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvHistory.setVisibility(View.GONE);
                binding.tvCount.setText("Aucune transaction");
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvHistory.setVisibility(View.VISIBLE);
                binding.tvCount.setText(transactions.size() + " transaction(s)");
                adapter.submitList(transactions);
            }
        });
    }

    @Override
    public void onReprintClick(AirtelTransaction tx) {
        Intent intent = new Intent(this, PrintForegroundService.class);
        intent.setAction(PrintForegroundService.ACTION_REPRINT_TRANSACTION);
        intent.putExtra(PrintForegroundService.EXTRA_TRANSACTION_ID, tx.getId());
        intent.putExtra(PrintForegroundService.EXTRA_TX_MONTANT, tx.getMontant());
        intent.putExtra(PrintForegroundService.EXTRA_TX_CLIENT, tx.getNomClient());
        intent.putExtra(PrintForegroundService.EXTRA_TX_SOLDE, tx.getNouveauSolde());
        intent.putExtra(PrintForegroundService.EXTRA_TX_ID_TX, tx.getTransactionId());
        intent.putExtra(PrintForegroundService.EXTRA_TX_DATE, tx.getDate());
        intent.putExtra(PrintForegroundService.EXTRA_TX_HEURE, tx.getHeure());
        intent.putExtra(PrintForegroundService.EXTRA_TX_TYPE, tx.getType());
        intent.putExtra(PrintForegroundService.EXTRA_TX_RAW, tx.getRawMessage());
        intent.putExtra(PrintForegroundService.EXTRA_TX_PHONE, tx.getNumeroTelephone());
        startForegroundService(intent);
        Toast.makeText(this, "Réimpression lancée...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(AirtelTransaction tx) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer cette transaction ?")
                .setPositiveButton("Oui", (d, w) -> viewModel.deleteTransaction(tx))
                .setNegativeButton("Non", null)
                .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Vider l'historique")
                .setMessage("Supprimer TOUTES les transactions ? Cette action est irréversible.")
                .setPositiveButton("Tout supprimer", (d, w) -> {
                    viewModel.deleteAll();
                    Toast.makeText(this, "Historique effacé", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
