package com.example.giuaky.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.activities.CheckoutActivity;
import com.example.giuaky.activities.ProductDetailActivity;
import com.example.giuaky.adapters.ProductAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.CartItem;
import com.example.giuaky.models.Category;
import com.example.giuaky.models.Product;
import com.example.giuaky.models.User;
import com.example.giuaky.utils.SearchUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private TextView tvGreeting;
    private RecyclerView rvProducts;
    private ChipGroup chipGroupCategories;
    private View layoutEmptyState;

    private ProductAdapter productAdapter;
    private final List<Product> allProducts = new ArrayList<>();

    // "null" means ALL category selected
    private String selectedCategoryId = null;
    private String searchKeyword = "";

    private ListenerRegistration categoryListener;
    private ListenerRegistration productListener;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvGreeting = view.findViewById(R.id.tvGreeting);
        rvProducts = view.findViewById(R.id.rvProducts);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        TextInputEditText edtSearch = view.findViewById(R.id.edtSearch);

        // Setup RecyclerView
        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), new ProductAdapter.OnProductActionListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra("productId", product.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCart(Product product) {
                addProductToCart(product);
            }

            @Override
            public void onBuyNow(Product product) {
                buyProductNow(product);
            }
        });

        rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProducts.setAdapter(productAdapter);
        rvProducts.setHasFixedSize(false);

        // Load user greeting
        loadUserGreeting();

        // Setup search
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchKeyword = s != null ? s.toString().trim() : "";
                applyFilters();
            }
        });

        // Listen to categories
        listenToCategories();

        // Listen to products
        listenToProducts();

        // "Tất cả" chip listener
        Chip chipAll = view.findViewById(R.id.chipAll);
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedCategoryId = null;
                applyFilters();
            }
        });
    }

    private void addProductToCart(Product product) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference itemRef = db.collection(FirestoreConstants.COLLECTION_CARTS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ITEMS)
                .document(product.getId());

        itemRef.get().addOnSuccessListener(snapshot -> {
            int currentQty = 0;
            if (snapshot.exists()) {
                CartItem existing = snapshot.toObject(CartItem.class);
                if (existing != null) {
                    currentQty = existing.getQuantity();
                }
            }

            CartItem newItem = new CartItem(
                    product.getId(),
                    product.getName(),
                    product.getImageUrl(),
                    product.getPrice(),
                    currentQty + 1
            );

            itemRef.set(newItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Đã thêm " + product.getName() + " vào giỏ hàng", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Lỗi thêm giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void buyProductNow(Product product) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<CartItem> buyList = new ArrayList<>();
        buyList.add(new CartItem(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                1
        ));

        Intent intent = new Intent(requireContext(), CheckoutActivity.class);
        intent.putExtra("isFromCart", false);
        intent.putExtra("cartItems", buyList);
        startActivity(intent);
    }

    private void loadUserGreeting() {
        tvGreeting.setText("Shop chào bạn...");
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                            tvGreeting.setText("Shop chào " + user.getFullName());
                        } else {
                            tvGreeting.setText("Shop chào bạn!");
                        }
                    }
                });
    }

    private void listenToCategories() {
        categoryListener = db.collection(FirestoreConstants.COLLECTION_CATEGORIES)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;

                    // Remove all chips except "Tất cả" (chipAll has id=R.id.chipAll)
                    int chipCount = chipGroupCategories.getChildCount();
                    if (chipCount > 1) {
                        chipGroupCategories.removeViews(1, chipCount - 1);
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Category category = doc.toObject(Category.class);
                        // Fallback: if @DocumentId not populated, set manually
                        if (category.getId() == null || category.getId().isEmpty()) {
                            category.setId(doc.getId());
                        }

                        Chip chip = new Chip(requireContext());
                        chip.setText(category.getName());
                        chip.setCheckable(true);
                        chip.setClickable(true);
                        chip.setTag(category.getId());

                        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                selectedCategoryId = (String) buttonView.getTag();
                                applyFilters();
                            }
                        });

                        chipGroupCategories.addView(chip);
                    }
                });
    }

    private void listenToProducts() {
        productListener = db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;

                    allProducts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Product product = doc.toObject(Product.class);
                        // Fallback for @DocumentId
                        if (product.getId() == null || product.getId().isEmpty()) {
                            product.setId(doc.getId());
                        }
                        allProducts.add(product);
                    }

                    applyFilters();
                });
    }

    private void applyFilters() {
        List<Product> filtered = new ArrayList<>();

        for (Product product : allProducts) {
            boolean matchesCategory = (selectedCategoryId == null)
                    || (product.getCategoryId() != null && Objects.equals(product.getCategoryId(), selectedCategoryId));

            boolean matchesSearch = searchKeyword.isEmpty()
                    || SearchUtils.matches(product.getName(), searchKeyword)
                    || SearchUtils.matches(product.getDescription(), searchKeyword);

            if (matchesCategory && matchesSearch) {
                filtered.add(product);
            }
        }

        productAdapter.updateList(filtered);

        boolean isEmpty = filtered.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvProducts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (categoryListener != null) categoryListener.remove();
        if (productListener != null) productListener.remove();
    }
}
