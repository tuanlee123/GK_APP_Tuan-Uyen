package com.example.giuaky.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.giuaky.R;
import com.example.giuaky.adapters.ProductSuggestAdapter;
import com.example.giuaky.adapters.ReviewAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.CartItem;
import com.example.giuaky.models.Product;
import com.example.giuaky.models.Review;
import com.example.giuaky.models.User;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    private ImageView imgDetailProduct;
    private TextView tvDetailName;
    private TextView tvDetailPrice;
    private TextView tvDetailRating;
    private TextView tvDetailSold;
    private TextView tvDetailSpecifications;
    private TextView tvDetailDescription;

    // Reviews section views
    private TextView tvAverageRating;
    private TextView tvReviewCount;
    private RatingBar rbWriteReview;
    private TextInputLayout tilReviewComment;
    private EditText edtReviewComment;
    private MaterialButton btnSubmitReview;
    private TextView tvEmptyReviews;
    private RecyclerView rvReviews;
    private MaterialButton btnViewAllReviews;

    // Suggestions section views
    private MaterialCardView cardSuggestions;
    private RecyclerView rvDetailSuggestions;

    // Bottom action bar
    private MaterialButton btnDetailAddToCart;
    private MaterialButton btnDetailBuyNow;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration reviewsListener;

    private String productId;
    private Product currentProduct;
    private ProductSuggestAdapter suggestAdapter;
    private final List<Product> suggestionList = new ArrayList<>();

    private ReviewAdapter reviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        productId = getIntent().getStringExtra("productId");

        initViews();
        setupEvents();

        if (productId != null && !productId.isEmpty()) {
            loadProductDetails();
            listenToReviews();
        } else {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        imgDetailProduct = findViewById(R.id.imgDetailProduct);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailRating = findViewById(R.id.tvDetailRating);
        tvDetailSold = findViewById(R.id.tvDetailSold);
        tvDetailSpecifications = findViewById(R.id.tvDetailSpecifications);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);

        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        rbWriteReview = findViewById(R.id.rbWriteReview);
        tilReviewComment = findViewById(R.id.tilReviewComment);
        edtReviewComment = findViewById(R.id.edtReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        tvEmptyReviews = findViewById(R.id.tvEmptyReviews);
        rvReviews = findViewById(R.id.rvReviews);
        btnViewAllReviews = findViewById(R.id.btnViewAllReviews);

        cardSuggestions = findViewById(R.id.cardSuggestions);
        rvDetailSuggestions = findViewById(R.id.rvDetailSuggestions);
        btnDetailAddToCart = findViewById(R.id.btnDetailAddToCart);
        btnDetailBuyNow = findViewById(R.id.btnDetailBuyNow);

        // Suggestions setup
        suggestAdapter = new ProductSuggestAdapter(this, suggestionList, product -> {
            Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getId());
            startActivity(intent);
        });
        rvDetailSuggestions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDetailSuggestions.setAdapter(suggestAdapter);

        // Reviews setup - strictly no scroll lag, fixed parent scroll
        reviewAdapter = new ReviewAdapter(this, new ArrayList<>());
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setNestedScrollingEnabled(false);
        rvReviews.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupEvents() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnGoToCart).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selectTab", "cart");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnDetailAddToCart.setOnClickListener(v -> addToCart());
        btnDetailBuyNow.setOnClickListener(v -> buyNow());
        btnSubmitReview.setOnClickListener(v -> submitReview());

        btnViewAllReviews.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailActivity.this, AllReviewsActivity.class);
            intent.putExtra("productId", productId);
            intent.putExtra("productName", currentProduct != null ? currentProduct.getName() : "");
            startActivity(intent);
        });
    }

    private void loadProductDetails() {
        db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentProduct = doc.toObject(Product.class);
                        if (currentProduct != null) {
                            if (currentProduct.getId() == null || currentProduct.getId().isEmpty()) {
                                currentProduct.setId(doc.getId());
                            }
                            displayProduct(currentProduct);
                            loadSuggestions(currentProduct);
                        }
                    } else {
                        Toast.makeText(this, "Sản phẩm không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayProduct(Product product) {
        tvDetailName.setText(product.getName());
        tvDetailPrice.setText(CurrencyUtils.formatVnd(product.getPrice()));

        int revCount = product.getReviewCount();
        String ratingStr = revCount > 0
                ? String.format(Locale.getDefault(), "%.1f / 5.0 (%d đánh giá)", product.getRating(), revCount)
                : String.format(Locale.getDefault(), "%.1f / 5.0", product.getRating());
        tvDetailRating.setText(ratingStr);

        String soldStr = String.format(Locale.getDefault(), "Đã bán %d", product.getSoldCount());
        tvDetailSold.setText(soldStr);

        // Specifications
        if (product.getSpecifications() != null && !product.getSpecifications().trim().isEmpty()) {
            tvDetailSpecifications.setText(product.getSpecifications());
        } else {
            tvDetailSpecifications.setText("Đang cập nhật thông số");
        }

        // Description
        if (product.getDescription() != null && !product.getDescription().trim().isEmpty()) {
            tvDetailDescription.setText(product.getDescription());
        } else {
            tvDetailDescription.setText("Đang cập nhật mô tả");
        }

        // Image
        Glide.with(this)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_tech)
                .error(R.drawable.ic_placeholder_tech)
                .into(imgDetailProduct);
    }

    private void loadSuggestions(Product current) {
        db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                .limit(15)
                .get()
                .addOnSuccessListener(snapshots -> {
                    suggestionList.clear();
                    List<Product> sameCategory = new ArrayList<>();
                    List<Product> otherCategory = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (doc.getId().equals(current.getId())) {
                            continue;
                        }
                        Product p = doc.toObject(Product.class);
                        if (p.getId() == null || p.getId().isEmpty()) {
                            p.setId(doc.getId());
                        }

                        if (current.getCategoryId() != null && current.getCategoryId().equals(p.getCategoryId())) {
                            sameCategory.add(p);
                        } else {
                            otherCategory.add(p);
                        }
                    }

                    suggestionList.addAll(sameCategory);
                    suggestionList.addAll(otherCategory);

                    int maxItems = Math.min(suggestionList.size(), 8);
                    List<Product> displayList = new ArrayList<>(suggestionList.subList(0, maxItems));
                    suggestAdapter.updateList(displayList);

                    if (displayList.isEmpty()) {
                        cardSuggestions.setVisibility(View.GONE);
                    } else {
                        cardSuggestions.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    cardSuggestions.setVisibility(View.GONE);
                });
    }

    private void listenToReviews() {
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

                    List<Review> allReviews = new ArrayList<>();
                    double totalRating = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review.getId() == null || review.getId().isEmpty()) {
                            review.setId(doc.getId());
                        }
                        allReviews.add(review);
                        totalRating += review.getRating();
                    }

                    // Sắp xếp theo thời gian tạo mới nhất lên đầu
                    Collections.sort(allReviews, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));

                    int count = allReviews.size();
                    if (count == 0) {
                        tvEmptyReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                        btnViewAllReviews.setVisibility(View.GONE);

                        double currentRatingVal = currentProduct != null ? currentProduct.getRating() : 5.0;
                        tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f ★", currentRatingVal));
                        tvReviewCount.setText("(0 đánh giá)");
                    } else {
                        tvEmptyReviews.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                        btnViewAllReviews.setVisibility(View.VISIBLE);
                        btnViewAllReviews.setText(String.format(Locale.getDefault(), "Xem tất cả đánh giá (%d)", count));

                        // Hiển thị tối đa 3 bình luận mới nhất trên trang chi tiết
                        List<Review> displayList = new ArrayList<>();
                        int maxDisplay = Math.min(count, 3);
                        for (int i = 0; i < maxDisplay; i++) {
                            displayList.add(allReviews.get(i));
                        }
                        reviewAdapter.updateList(displayList);

                        double avg = Math.round((totalRating / count) * 10.0) / 10.0;
                        tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f ★", avg));
                        tvReviewCount.setText(String.format(Locale.getDefault(), "(%d đánh giá)", count));

                        tvDetailRating.setText(String.format(Locale.getDefault(), "%.1f / 5.0 (%d đánh giá)", avg, count));

                        // Đồng bộ rating và reviewCount về Firestore product document
                        if (currentProduct != null) {
                            currentProduct.setRating(avg);
                            currentProduct.setReviewCount(count);
                            db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                                    .document(productId)
                                    .set(currentProduct);
                        }
                    }
                });
    }

    private void submitReview() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để gửi đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = edtReviewComment.getText() != null ? edtReviewComment.getText().toString().trim() : "";
        if (comment.isEmpty()) {
            tilReviewComment.setError("Vui lòng nhập nhận xét của bạn");
            return;
        }
        tilReviewComment.setError(null);

        float rating = rbWriteReview.getRating();
        if (rating < 1.0f) {
            rating = 1.0f;
        }

        btnSubmitReview.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();
        final float finalRating = rating;

        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String userName = "Khách hàng";
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null && user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
                            userName = user.getFullName().trim();
                        } else if (user != null && user.getEmail() != null) {
                            userName = user.getEmail();
                        }
                    }

                    Review newReview = new Review(
                            uid,
                            userName,
                            finalRating,
                            comment,
                            System.currentTimeMillis()
                    );

                    db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                            .document(productId)
                            .collection(FirestoreConstants.COLLECTION_REVIEWS)
                            .add(newReview)
                            .addOnSuccessListener(docRef -> {
                                btnSubmitReview.setEnabled(true);
                                edtReviewComment.setText("");
                                rbWriteReview.setRating(5.0f);
                                Toast.makeText(ProductDetailActivity.this, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                btnSubmitReview.setEnabled(true);
                                Toast.makeText(ProductDetailActivity.this, "Lỗi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmitReview.setEnabled(true);
                    Toast.makeText(ProductDetailActivity.this, "Lỗi xác thực người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addToCart() {
        if (currentProduct == null) return;
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference itemRef = db.collection(FirestoreConstants.COLLECTION_CARTS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ITEMS)
                .document(currentProduct.getId());

        itemRef.get().addOnSuccessListener(snap -> {
            int currentQty = 0;
            if (snap.exists()) {
                CartItem existing = snap.toObject(CartItem.class);
                if (existing != null) {
                    currentQty = existing.getQuantity();
                }
            }

            CartItem newItem = new CartItem(
                    currentProduct.getId(),
                    currentProduct.getName(),
                    currentProduct.getImageUrl(),
                    currentProduct.getPrice(),
                    currentQty + 1
            );

            itemRef.set(newItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã thêm \"" + currentProduct.getName() + "\" vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi thêm giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void buyNow() {
        if (currentProduct == null) return;
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<CartItem> buyNowList = new ArrayList<>();
        buyNowList.add(new CartItem(
                currentProduct.getId(),
                currentProduct.getName(),
                currentProduct.getImageUrl(),
                currentProduct.getPrice(),
                1
        ));

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("isFromCart", false);
        intent.putExtra("cartItems", buyNowList);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsListener != null) {
            reviewsListener.remove();
        }
    }
}
