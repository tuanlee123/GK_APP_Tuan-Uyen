package com.example.giuaky.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.models.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Review> reviewList = new ArrayList<>();
    private final SimpleDateFormat dateFormat;

    public ReviewAdapter(Context context, List<Review> initialList) {
        this.context = context;
        if (initialList != null) {
            this.reviewList.addAll(initialList);
        }
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    public void updateList(List<Review> newList) {
        if (newList == this.reviewList) {
            notifyDataSetChanged();
            return;
        }
        this.reviewList.clear();
        if (newList != null) {
            this.reviewList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        if (review == null) {
            return;
        }

        String userName = review.getUserName();
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Khách hàng";
        }
        holder.tvReviewUserName.setText(userName);

        holder.rbItemRating.setRating((float) review.getRating());

        if (review.getCreatedAt() > 0) {
            holder.tvReviewDate.setText(dateFormat.format(new Date(review.getCreatedAt())));
        } else {
            holder.tvReviewDate.setText("");
        }

        holder.tvReviewComment.setText(review.getComment() != null ? review.getComment() : "");
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView tvReviewUserName;
        final RatingBar rbItemRating;
        final TextView tvReviewDate;
        final TextView tvReviewComment;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewUserName = itemView.findViewById(R.id.tvReviewUserName);
            rbItemRating = itemView.findViewById(R.id.rbItemRating);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
        }
    }
}
