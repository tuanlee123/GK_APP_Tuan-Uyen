package com.example.giuaky.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.models.ShippingAddress;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ShippingAddressAdapter extends RecyclerView.Adapter<ShippingAddressAdapter.AddressViewHolder> {

    public interface OnAddressActionListener {
        void onEdit(ShippingAddress address);
        void onDelete(ShippingAddress address);
        void onSetDefault(ShippingAddress address);
    }

    private final Context context;
    private final List<ShippingAddress> addressList = new ArrayList<>();
    private final OnAddressActionListener listener;

    public ShippingAddressAdapter(Context context, OnAddressActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<ShippingAddress> newList) {
        addressList.clear();
        if (newList != null) {
            addressList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shipping_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        ShippingAddress address = addressList.get(position);

        holder.tvReceiverName.setText(address.getReceiverName() != null ? address.getReceiverName() : "");
        holder.tvReceiverPhone.setText(address.getPhone() != null ? address.getPhone() : "");
        holder.tvDetailAddress.setText(address.getDetailAddress() != null ? address.getDetailAddress() : "");

        if (address.isDefault()) {
            holder.tvDefaultBadge.setVisibility(View.VISIBLE);
            holder.btnSetDefault.setVisibility(View.GONE);
        } else {
            holder.tvDefaultBadge.setVisibility(View.GONE);
            holder.btnSetDefault.setVisibility(View.VISIBLE);
        }

        holder.btnSetDefault.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefault(address);
            }
        });

        holder.btnEditAddress.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(address);
            }
        });

        holder.btnDeleteAddress.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        final TextView tvReceiverName;
        final TextView tvReceiverPhone;
        final TextView tvDefaultBadge;
        final TextView tvDetailAddress;
        final MaterialButton btnSetDefault;
        final MaterialButton btnEditAddress;
        final MaterialButton btnDeleteAddress;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReceiverName = itemView.findViewById(R.id.tvReceiverName);
            tvReceiverPhone = itemView.findViewById(R.id.tvReceiverPhone);
            tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
            tvDetailAddress = itemView.findViewById(R.id.tvDetailAddress);
            btnSetDefault = itemView.findViewById(R.id.btnSetDefault);
            btnEditAddress = itemView.findViewById(R.id.btnEditAddress);
            btnDeleteAddress = itemView.findViewById(R.id.btnDeleteAddress);
        }
    }
}
