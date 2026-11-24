package com.example.giuaky.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.adapters.ReviewAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.Review;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AllReviewsActivity extends AppCompatActivity {

    private static final String TAG = "AllReviewsActivity";

    private TextView tvOverviewProductName;
    private TextView tvOverviewAvgRating;
    private RatingBar rbOverviewStars;
    private TextView tvOverviewTotalCount;

    private ChipGroup chipGroupRating;
    private Chip chipFilterAll;
    private Chip chipFilter5Star;
    private Chip chipFilter4Star;
    private Chip chipFilter3Star;
    private Chip chipFilter2Star;
    private Chip chipFilter1Star;

    private RecyclerView rvAllReviews;
    private TextView tvEmptyFilteredReviews;

    private FirebaseFirestore db;
    private ListenerRegistration reviewsListener;

    private String productId;
    private String productName;

    private ReviewAdapter reviewAdapter;
    private final List<Review> allReviews = new ArrayList<>();
    private int currentSelectedStar = 0; // 0 = Tất cả, 1..5 = số sao

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        db = FirebaseFirestore.getInstance();

        productId = getIntent().getStringExtra("productId");
        productName = getIntent().getStringExtra("productName");

        initViews();
        setupEvents();

        if (productId != null && !productId.isEmpty()) {
            listenToAllReviews();
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvOverviewProductName = findViewById(R.id.tvOverviewProductName);
        tvOverviewAvgRating = findViewById(R.id.tvOverviewAvgRating);
        rbOverviewStars = findViewById(R.id.rbOverviewStars);
        tvOverviewTotalCount = findViewById(R.id.tvOverviewTotalCount);

        chipGroupRating = findViewById(R.id.chipGroupRating);
        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilter5Star = findViewById(R.id.chipFilter5Star);
        chipFilter4Star = findViewById(R.id.chipFilter4Star);
        chipFilter3Star = findViewById(R.id.chipFilter3Star);
        chipFilter2Star = findViewById(R.id.chipFilter2Star);
        chipFilter1Star = findViewById(R.id.chipFilter1Star);

        rvAllReviews = findViewById(R.id.rvAllReviews);
        tvEmptyFilteredReviews = findViewById(R.id.tvEmptyFilteredReviews);

        if (productName != null && !productName.isEmpty()) {
            tvOverviewProductName.setText(productName);
            tvOverviewProductName.setVisibility(View.VISIBLE);
        } else {
            tvOverviewProductName.setVisibility(View.GONE);
        }

        reviewAdapter = new ReviewAdapter(this, new ArrayList<>());
        rvAllReviews.setLayoutManager(new LinearLayoutManager(this));
        rvAllReviews.setAdapter(reviewAdapter);
    }

    private void setupEvents() {
        findViewById(R.id.btnBackAllReviews).setOnClickListener(v -> finish());

        chipGroupRating.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipFilterAll) {
                currentSelectedStar = 0;
            } else if (checkedId == R.id.chipFilter5Star) {
                currentSelectedStar = 5;
            } else if (checkedId == R.id.chipFilter4Star) {
                currentSelectedStar = 4;
            } else if (checkedId == R.id.chipFilter3Star) {
                currentSelectedStar = 3;
            } else if (checkedId == R.id.chipFilter2Star) {
                currentSelectedStar = 2;
            } else if (checkedId == R.id.chipFilter1Star) {
                currentSelectedStar = 1;
            }
            applyStarFilter();
        });
    }

    private void listenToAllReviews() {
        reviewsListener = db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                .document(productId)
                .collection(FirestoreConstants.COLLECTION_REVIEWS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe đánh giá: " + error.getMessage(), error);
                        return;
                    }
                    if (snapshots == null) {
                        return;
                    }

                    allReviews.clear();
                    double totalRating = 0;
                    int count5 = 0;
                    int count4 = 0;
                    int count3 = 0;
                    int count2 = 0;
                    int count1 = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review.getId() == null || review.getId().isEmpty()) {
                            review.setId(doc.getId());
                        }
                        allReviews.add(review);
                        totalRating += review.getRating();

                        long star = Math.round(review.getRating());
                        if (star >= 5) count5++;
                        else if (star == 4) count4++;
                        else if (star == 3) count3++;
                        else if (star == 2) count2++;
                        else count1++;
                    }

                    // Sắp xếp thời gian giảm dần
                    Collections.sort(allReviews, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));

                    int totalCount = allReviews.size();

                    // Cập nhật text số lượng trên các Chip lọc
                    chipFilterAll.setText(String.format(Locale.getDefault(), "Tất cả (%d)", totalCount));
                    chipFilter5Star.setText(String.format(Locale.getDefault(), "5 ★ (%d)", count5));
                    chipFilter4Star.setText(String.format(Locale.getDefault(), "4 ★ (%d)", count4));
                    chipFilter3Star.setText(String.format(Locale.getDefault(), "3 ★ (%d)", count3));
                    chipFilter2Star.setText(String.format(Locale.getDefault(), "2 ★ (%d)", count2));
                    chipFilter1Star.setText(String.format(Locale.getDefault(), "1 ★ (%d)", count1));

                    // Cập nhật Overview Card
                    if (totalCount == 0) {
                        tvOverviewAvgRating.setText("5.0 / 5.0");
                        rbOverviewStars.setRating(5.0f);
                        tvOverviewTotalCount.setText("Chưa có lượt đánh giá");
                    } else {
                        double avg = Math.round((totalRating / totalCount) * 10.0) / 10.0;
                        tvOverviewAvgRating.setText(String.format(Locale.getDefault(), "%.1f / 5.0", avg));
                        rbOverviewStars.setRating((float) avg);
                        tvOverviewTotalCount.setText(String.format(Locale.getDefault(), "%d lượt đánh giá", totalCount));
                    }

                    applyStarFilter();
                });
    }

    private void applyStarFilter() {
        List<Review> filteredList = new ArrayList<>();
        if (currentSelectedStar == 0) {
            filteredList.addAll(allReviews);
        } else {
            for (Review review : allReviews) {
                if (Math.round(review.getRating()) == currentSelectedStar) {
                    filteredList.add(review);
                }
            }
        }

        reviewAdapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            rvAllReviews.setVisibility(View.GONE);
            tvEmptyFilteredReviews.setVisibility(View.VISIBLE);
            if (currentSelectedStar == 0) {
                tvEmptyFilteredReviews.setText("Chưa có đánh giá nào cho sản phẩm này");
            } else {
                tvEmptyFilteredReviews.setText(String.format(Locale.getDefault(), "Chưa có đánh giá %d sao nào", currentSelectedStar));
            }
        } else {
            rvAllReviews.setVisibility(View.VISIBLE);
            tvEmptyFilteredReviews.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
    }
}
