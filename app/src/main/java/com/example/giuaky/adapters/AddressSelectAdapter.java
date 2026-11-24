package com.example.giuaky.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.models.ShippingAddress;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class AddressSelectAdapter extends RecyclerView.Adapter<AddressSelectAdapter.SelectViewHolder> {

    public interface OnAddressSelectedListener {
        void onSelected(ShippingAddress address);
    }

    private final Context context;
    private final List<ShippingAddress> addressList = new ArrayList<>();
    private String selectedAddressId;
    private final OnAddressSelectedListener listener;

    public AddressSelectAdapter(Context context, String selectedAddressId, OnAddressSelectedListener listener) {
        this.context = context;
        this.selectedAddressId = selectedAddressId;
        this.listener = listener;
    }

    public void updateList(List<ShippingAddress> newList, String currentSelectedId) {
        this.selectedAddressId = currentSelectedId;
        addressList.clear();
        if (newList != null) {
            addressList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public void setSelectedAddressId(String id) {
        this.selectedAddressId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_address_selectable, parent, false);
        return new SelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectViewHolder holder, int position) {
        ShippingAddress address = addressList.get(position);

        holder.tvSelectReceiverName.setText(address.getReceiverName() != null ? address.getReceiverName() : "");
        holder.tvSelectReceiverPhone.setText(address.getPhone() != null ? address.getPhone() : "");
        holder.tvSelectDetailAddress.setText(address.getDetailAddress() != null ? address.getDetailAddress() : "");

        boolean isDefault = address.isDefault();
        holder.tvSelectDefaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);

        boolean isSelected = address.getId() != null && address.getId().equals(selectedAddressId);
        if (isSelected) {
            holder.cardAddressSelect.setStrokeColor(ContextCompat.getColor(context, R.color.colorSecondary));
            holder.cardAddressSelect.setStrokeWidth(4);
            holder.ivSelectRadio.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary)));
            holder.ivSelectRadio.setAlpha(1.0f);
        } else {
            holder.cardAddressSelect.setStrokeColor(ContextCompat.getColor(context, R.color.colorOutline));
            holder.cardAddressSelect.setStrokeWidth(2);
            holder.ivSelectRadio.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorOutline)));
            holder.ivSelectRadio.setAlpha(0.4f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelected(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class SelectViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardAddressSelect;
        final ImageView ivSelectRadio;
        final TextView tvSelectReceiverName;
        final TextView tvSelectReceiverPhone;
        final TextView tvSelectDefaultBadge;
        final TextView tvSelectDetailAddress;

        public SelectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAddressSelect = itemView.findViewById(R.id.cardAddressSelect);
            ivSelectRadio = itemView.findViewById(R.id.ivSelectRadio);
            tvSelectReceiverName = itemView.findViewById(R.id.tvSelectReceiverName);
            tvSelectReceiverPhone = itemView.findViewById(R.id.tvSelectReceiverPhone);
            tvSelectDefaultBadge = itemView.findViewById(R.id.tvSelectDefaultBadge);
            tvSelectDetailAddress = itemView.findViewById(R.id.tvSelectDetailAddress);
        }
    }
}
