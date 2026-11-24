package com.example.giuaky.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.giuaky.R;
import com.example.giuaky.models.CartItem;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.SummaryViewHolder> {

    private final Context context;
    private final List<CartItem> items;

    public OrderSummaryAdapter(Context context, List<CartItem> items) {
        this.context = context;
        this.items = items != null ? items : new ArrayList<>();
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        CartItem item = items.get(position);
        if (item == null) return;

        holder.tvName.setText(item.getProductName());
        holder.tvQty.setText(String.format(Locale.getDefault(), "Số lượng: %d", item.getQuantity()));
        holder.tvPrice.setText(CurrencyUtils.formatVnd(item.getPrice() * item.getQuantity()));

        Glide.with(context)
                .load(item.getProductImage())
                .placeholder(R.drawable.ic_placeholder_tech)
                .error(R.drawable.ic_placeholder_tech)
                .into(holder.img);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class SummaryViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView img;
        final TextView tvName;
        final TextView tvQty;
        final TextView tvPrice;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgOrderSummary);
            tvName = itemView.findViewById(R.id.tvOrderSummaryName);
            tvQty = itemView.findViewById(R.id.tvOrderSummaryQuantity);
            tvPrice = itemView.findViewById(R.id.tvOrderSummaryPrice);
        }
    }
}
