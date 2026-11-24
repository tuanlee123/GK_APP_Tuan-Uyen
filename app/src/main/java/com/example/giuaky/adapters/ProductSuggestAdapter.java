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
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSuggestAdapter extends RecyclerView.Adapter<ProductSuggestAdapter.SuggestViewHolder> {

    public interface OnSuggestClickListener {
        void onSuggestClick(Product product);
    }

    private final Context context;
    private final List<Product> list;
    private final OnSuggestClickListener listener;

    public ProductSuggestAdapter(Context context, List<Product> list, OnSuggestClickListener listener) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.list.clear();
        if (newList != null) {
            this.list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_suggest, parent, false);
        return new SuggestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestViewHolder holder, int position) {
        Product product = list.get(position);
        if (product == null) return;

        holder.tvSuggestName.setText(product.getName());
        holder.tvSuggestPrice.setText(CurrencyUtils.formatVnd(product.getPrice()));

        String stats = String.format(Locale.getDefault(), "⭐ %.1f | Đã bán %d", product.getRating(), product.getSoldCount());
        holder.tvSuggestStats.setText(stats);

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_tech)
                .error(R.drawable.ic_placeholder_tech)
                .into(holder.imgSuggest);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class SuggestViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imgSuggest;
        final TextView tvSuggestName;
        final TextView tvSuggestPrice;
        final TextView tvSuggestStats;

        public SuggestViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSuggest = itemView.findViewById(R.id.imgSuggest);
            tvSuggestName = itemView.findViewById(R.id.tvSuggestName);
            tvSuggestPrice = itemView.findViewById(R.id.tvSuggestPrice);
            tvSuggestStats = itemView.findViewById(R.id.tvSuggestStats);
        }
    }
}
