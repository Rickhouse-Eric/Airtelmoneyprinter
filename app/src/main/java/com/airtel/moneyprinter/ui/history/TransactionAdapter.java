package com.airtel.moneyprinter.ui.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.airtel.moneyprinter.R;
import com.airtel.moneyprinter.data.model.AirtelTransaction;
import com.airtel.moneyprinter.databinding.ItemTransactionBinding;

/**
 * Adapter RecyclerView pour l'historique des transactions.
 */
public class TransactionAdapter
        extends ListAdapter<AirtelTransaction, TransactionAdapter.ViewHolder> {

    private final OnTransactionListener listener;

    public interface OnTransactionListener {
        void onReprintClick(AirtelTransaction tx);
        void onDeleteClick(AirtelTransaction tx);
    }

    public TransactionAdapter(OnTransactionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        ViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AirtelTransaction tx, OnTransactionListener listener) {
            binding.tvMontant.setText(tx.getMontant() != null ? tx.getMontant() : "N/A");
            binding.tvType.setText(tx.getTypeLabel());
            binding.tvClient.setText(tx.getNomClient() != null ? tx.getNomClient() : "-");
            binding.tvTxId.setText("ID: " + tx.getTransactionId());
            binding.tvDateTime.setText(tx.getDate() + " " + tx.getHeure());
            binding.tvPrintStatus.setText(tx.getPrintStatusLabel());

            // Couleur statut impression
            int color;
            switch (tx.getPrintStatus()) {
                case AirtelTransaction.PRINT_STATUS_SUCCESS:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.status_active);
                    break;
                case AirtelTransaction.PRINT_STATUS_FAILED:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.status_inactive);
                    break;
                default:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.status_pending);
            }
            binding.tvPrintStatus.setTextColor(color);

            // Actions
            binding.btnReprint.setOnClickListener(v -> listener.onReprintClick(tx));
            binding.btnDelete.setOnClickListener(v -> listener.onDeleteClick(tx));

            // Nombre d'impressions
            if (tx.getPrintCount() > 1) {
                binding.tvPrintCount.setText("Imprimé " + tx.getPrintCount() + "x");
                binding.tvPrintCount.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvPrintCount.setVisibility(android.view.View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<AirtelTransaction> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AirtelTransaction>() {
                @Override
                public boolean areItemsTheSame(@NonNull AirtelTransaction a, @NonNull AirtelTransaction b) {
                    return a.getId() == b.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull AirtelTransaction a, @NonNull AirtelTransaction b) {
                    return a.getPrintStatus() == b.getPrintStatus()
                            && a.getPrintCount() == b.getPrintCount();
                }
            };
}
