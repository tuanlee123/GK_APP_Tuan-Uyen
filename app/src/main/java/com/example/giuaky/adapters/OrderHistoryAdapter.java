package com.example.giuaky.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.giuaky.R;
import com.example.giuaky.models.Order;
import com.example.giuaky.models.OrderItem;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onOrderClick(Order order);
        void onCancelOrder(Order order);
    }

    private final Context context;
    private final List<Order> orderList = new ArrayList<>();
    private final OnOrderActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public OrderHistoryAdapter(Context context, OnOrderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Order> newList) {
        orderList.clear();
        if (newList != null) {
            orderList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        // Order code
        String orderId = order.getId() != null ? order.getId() : "";
        String shortCode = orderId.length() > 8 ? orderId.substring(0, 8).toUpperCase() : orderId.toUpperCase();
        holder.tvOrderCode.setText("Đơn hàng #" + shortCode);

        // Status badge
        String status = order.getStatus() != null ? order.getStatus() : "Đang xử lý";
        holder.tvOrderStatus.setText(status);
        applyStatusBadgeStyle(holder.tvOrderStatus, status);

        // Date
        if (order.getCreatedAt() > 0) {
            holder.tvOrderDate.setText(dateFormat.format(new Date(order.getCreatedAt())));
        } else {
            holder.tvOrderDate.setText("");
        }

        // Items preview
        List<OrderItem> items = order.getItems();
        int totalQuantity = 0;
        if (items != null && !items.isEmpty()) {
            OrderItem firstItem = items.get(0);
            holder.tvOrderFirstItemName.setText(firstItem.getProductName());

            Glide.with(context)
                    .load(firstItem.getProductImage())
                    .placeholder(R.drawable.ic_placeholder_tech)
                    .error(R.drawable.ic_placeholder_tech)
                    .into(holder.imgOrderPreview);

            if (items.size() > 1) {
                holder.tvOrderOtherItemsCount.setVisibility(View.VISIBLE);
                holder.tvOrderOtherItemsCount.setText(String.format(Locale.getDefault(), "(và %d sản phẩm khác)", items.size() - 1));
            } else {
                holder.tvOrderOtherItemsCount.setVisibility(View.GONE);
            }

            for (OrderItem oi : items) {
                totalQuantity += oi.getQuantity();
            }
        } else {
            holder.tvOrderFirstItemName.setText("Đơn hàng");
            holder.tvOrderOtherItemsCount.setVisibility(View.GONE);
            holder.imgOrderPreview.setImageResource(R.drawable.ic_placeholder_tech);
        }

        holder.tvOrderTotalQuantity.setText(String.format(Locale.getDefault(), "%d sản phẩm", totalQuantity > 0 ? totalQuantity : 1));
        holder.tvOrderTotalAmount.setText(CurrencyUtils.formatVnd(order.getTotalAmount()));

        // Cancel button visible only when status is "Đang xử lý"
        if ("Đang xử lý".equalsIgnoreCase(status)) {
            holder.btnCancelOrder.setVisibility(View.VISIBLE);
            holder.btnCancelOrder.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelOrder(order);
                }
            });
        } else {
            holder.btnCancelOrder.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    private void applyStatusBadgeStyle(TextView tvStatus, String status) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(10f);

        int textColor;
        int bgColor;
        int strokeColor;

        if ("Đang giao".equalsIgnoreCase(status)) {
            textColor = Color.parseColor("#0284C7");
            bgColor = Color.parseColor("#E0F2FE");
            strokeColor = Color.parseColor("#0284C7");
        } else if ("Đã giao".equalsIgnoreCase(status)) {
            textColor = Color.parseColor("#16A34A");
            bgColor = Color.parseColor("#DCFCE7");
            strokeColor = Color.parseColor("#16A34A");
        } else if ("Đã hủy".equalsIgnoreCase(status)) {
            textColor = Color.parseColor("#DC2626");
            bgColor = Color.parseColor("#FEE2E2");
            strokeColor = Color.parseColor("#DC2626");
        } else {
            // "Đang xử lý"
            textColor = Color.parseColor("#D97706");
            bgColor = Color.parseColor("#FEF3C7");
            strokeColor = Color.parseColor("#F59E0B");
        }

        drawable.setColor(bgColor);
        drawable.setStroke(2, strokeColor);
        tvStatus.setTextColor(textColor);
        tvStatus.setBackground(drawable);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvOrderCode;
        final TextView tvOrderStatus;
        final TextView tvOrderDate;
        final ShapeableImageView imgOrderPreview;
        final TextView tvOrderFirstItemName;
        final TextView tvOrderOtherItemsCount;
        final TextView tvOrderTotalQuantity;
        final TextView tvOrderTotalAmount;
        final MaterialButton btnCancelOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tvOrderCode);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            imgOrderPreview = itemView.findViewById(R.id.imgOrderPreview);
            tvOrderFirstItemName = itemView.findViewById(R.id.tvOrderFirstItemName);
            tvOrderOtherItemsCount = itemView.findViewById(R.id.tvOrderOtherItemsCount);
            tvOrderTotalQuantity = itemView.findViewById(R.id.tvOrderTotalQuantity);
            tvOrderTotalAmount = itemView.findViewById(R.id.tvOrderTotalAmount);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
        }
    }
}
