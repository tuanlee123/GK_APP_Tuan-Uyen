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
import com.example.giuaky.models.Product;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductActionListener {
        void onProductClick(Product product);
        void onAddToCart(Product product);
        void onBuyNow(Product product);
    }

    private final Context context;
    private final List<Product> productList;
    private final OnProductActionListener listener;

    public ProductAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.productList.clear();
        if (newList != null) {
            this.productList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        if (product == null) {
            return;
        }

        holder.tvProductName.setText(product.getName());
        holder.tvProductPrice.setText(CurrencyUtils.formatVnd(product.getPrice()));

        String stats = String.format(Locale.getDefault(), "⭐ %.1f | Đã bán %d", product.getRating(), product.getSoldCount());
        holder.tvProductStats.setText(stats);

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_tech)
                .error(R.drawable.ic_placeholder_tech)
                .into(holder.imgProduct);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCart(product);
            }
        });

        holder.btnBuyNow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBuyNow(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imgProduct;
        final TextView tvProductName;
        final TextView tvProductPrice;
        final TextView tvProductStats;
        final MaterialButton btnAddToCart;
        final MaterialButton btnBuyNow;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductStats = itemView.findViewById(R.id.tvProductStats);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
        }
    }
}
