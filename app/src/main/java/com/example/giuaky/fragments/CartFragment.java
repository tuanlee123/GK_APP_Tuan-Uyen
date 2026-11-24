package com.example.giuaky.fragments;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.giuaky.activities.MainActivity;
import com.example.giuaky.adapters.CartAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.CartItem;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment {

    private TextView tvCartItemCount;
    private TextView tvCartTotalAmount;
    private RecyclerView rvCartItems;
    private View layoutEmptyCart;
    private View cardCheckoutBar;
    private MaterialButton btnCheckout;

    private CartAdapter cartAdapter;
    private final ArrayList<CartItem> cartList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration cartListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvCartItemCount = view.findViewById(R.id.tvCartItemCount);
        tvCartTotalAmount = view.findViewById(R.id.tvCartTotalAmount);
        rvCartItems = view.findViewById(R.id.rvCartItems);
        layoutEmptyCart = view.findViewById(R.id.layoutEmptyCart);
        cardCheckoutBar = view.findViewById(R.id.cardCheckoutBar);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        cartAdapter = new CartAdapter(requireContext(), new ArrayList<>(), new CartAdapter.OnCartItemActionListener() {
            @Override
            public void onIncreaseQuantity(CartItem item) {
                updateItemQuantity(item, item.getQuantity() + 1);
            }

            @Override
            public void onDecreaseQuantity(CartItem item) {
                if (item.getQuantity() > 1) {
                    updateItemQuantity(item, item.getQuantity() - 1);
                } else {
                    showRemoveConfirmDialog(item);
                }
            }

            @Override
            public void onDeleteItem(CartItem item) {
                showRemoveConfirmDialog(item);
            }
        });

        rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCartItems.setAdapter(cartAdapter);

        view.findViewById(R.id.btnExploreProducts).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
        });

        btnCheckout.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(requireContext(), "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            intent.putExtra("isFromCart", true);
            intent.putExtra("cartItems", cartList);
            startActivity(intent);
        });

        listenToCart();
    }

    private void listenToCart() {
        if (mAuth.getCurrentUser() == null) {
            updateCartUI();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        cartListener = db.collection(FirestoreConstants.COLLECTION_CARTS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ITEMS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;

                    cartList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        CartItem item = doc.toObject(CartItem.class);
                        if (item.getProductId() == null || item.getProductId().isEmpty()) {
                            item.setProductId(doc.getId());
                        }
                        cartList.add(item);
                    }

                    updateCartUI();
                });
    }

    private void updateCartUI() {
        cartAdapter.updateList(cartList);

        double totalAmount = 0;
        int totalItems = 0;
        for (CartItem item : cartList) {
            totalAmount += item.getPrice() * item.getQuantity();
            totalItems += item.getQuantity();
        }

        tvCartTotalAmount.setText(CurrencyUtils.formatVnd(totalAmount));
        tvCartItemCount.setText(String.format(Locale.getDefault(), "%d mặt hàng", totalItems));

        boolean isEmpty = cartList.isEmpty();
        layoutEmptyCart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvCartItems.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        cardCheckoutBar.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateItemQuantity(CartItem item, int newQty) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        item.setQuantity(newQty);

        db.collection(FirestoreConstants.COLLECTION_CARTS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ITEMS)
                .document(item.getProductId())
                .set(item);
    }

    private void showRemoveConfirmDialog(CartItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xoá sản phẩm")
                .setMessage("Bạn có chắc muốn xoá \"" + item.getProductName() + "\" khỏi giỏ hàng?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    if (mAuth.getCurrentUser() == null) return;
                    String uid = mAuth.getCurrentUser().getUid();
                    db.collection(FirestoreConstants.COLLECTION_CARTS)
                            .document(uid)
                            .collection(FirestoreConstants.COLLECTION_ITEMS)
                            .document(item.getProductId())
                            .delete();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cartListener != null) {
            cartListener.remove();
        }
    }
}
