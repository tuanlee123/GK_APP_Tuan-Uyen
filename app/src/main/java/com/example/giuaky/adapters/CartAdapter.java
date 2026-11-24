package com.example.giuaky.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartItemActionListener {
        void onIncreaseQuantity(CartItem item);
        void onDecreaseQuantity(CartItem item);
        void onDeleteItem(CartItem item);
    }

    private final Context context;
    private final List<CartItem> cartItems;
    private final OnCartItemActionListener listener;

    public CartAdapter(Context context, List<CartItem> cartItems, OnCartItemActionListener listener) {
        this.context = context;
        this.cartItems = cartItems != null ? cartItems : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<CartItem> newList) {
        this.cartItems.clear();
        if (newList != null) {
            this.cartItems.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item == null) return;

        holder.tvProductName.setText(item.getProductName());
        holder.tvProductPrice.setText(CurrencyUtils.formatVnd(item.getPrice()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        Glide.with(context)
                .load(item.getProductImage())
                .placeholder(R.drawable.ic_placeholder_tech)
                .error(R.drawable.ic_placeholder_tech)
                .into(holder.imgProduct);

        holder.btnIncrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseQuantity(item);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecreaseQuantity(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteItem(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imgProduct;
        final TextView tvProductName;
        final TextView tvProductPrice;
        final TextView tvQuantity;
        final View btnDecrease;
        final View btnIncrease;
        final ImageButton btnDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgCartProduct);
            tvProductName = itemView.findViewById(R.id.tvCartProductName);
            tvProductPrice = itemView.findViewById(R.id.tvCartProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDelete = itemView.findViewById(R.id.btnDeleteCartItem);
        }
    }
}
