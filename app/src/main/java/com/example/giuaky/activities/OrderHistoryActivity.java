package com.example.giuaky.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.adapters.OrderHistoryAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.Order;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity implements OrderHistoryAdapter.OnOrderActionListener {

    private ChipGroup chipGroupOrderStatus;
    private RecyclerView rvOrderHistory;
    private View layoutOrderEmptyState;
    private TextView tvEmptyOrderMessage;
    private CircularProgressIndicator progressBarOrderHistory;

    private OrderHistoryAdapter orderAdapter;
    private final List<Order> allOrders = new ArrayList<>();
    private String selectedStatusFilter = null; // null = Tất cả

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.btnOrderHistoryBack).setOnClickListener(v -> finish());

        chipGroupOrderStatus = findViewById(R.id.chipGroupOrderStatus);
        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        layoutOrderEmptyState = findViewById(R.id.layoutOrderEmptyState);
        tvEmptyOrderMessage = findViewById(R.id.tvEmptyOrderMessage);
        progressBarOrderHistory = findViewById(R.id.progressBarOrderHistory);

        orderAdapter = new OrderHistoryAdapter(this, this);
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        rvOrderHistory.setAdapter(orderAdapter);

        setupFilterChips();
        listenToOrders();
    }

    private void setupFilterChips() {
        chipGroupOrderStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedStatusFilter = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipStatusProcessing) {
                    selectedStatusFilter = "Đang xử lý";
                } else if (checkedId == R.id.chipStatusShipping) {
                    selectedStatusFilter = "Đang giao";
                } else if (checkedId == R.id.chipStatusDelivered) {
                    selectedStatusFilter = "Đã giao";
                } else if (checkedId == R.id.chipStatusCancelled) {
                    selectedStatusFilter = "Đã hủy";
                } else {
                    selectedStatusFilter = null; // Tất cả
                }
            }
            applyFilters();
        });
    }

    private void listenToOrders() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        progressBarOrderHistory.setVisibility(View.VISIBLE);

        // Query by userId without composite orderBy to avoid missing index errors
        ordersListener = db.collection(FirestoreConstants.COLLECTION_ORDERS)
                .whereEqualTo(FirestoreConstants.FIELD_USER_ID, uid)
                .addSnapshotListener((snapshots, error) -> {
                    progressBarOrderHistory.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải đơn hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allOrders.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Order order = doc.toObject(Order.class);
                            if (order.getId() == null || order.getId().isEmpty()) {
                                order.setId(doc.getId());
                            }
                            allOrders.add(order);
                        }
                    }

                    // Sort orders in memory: newest first
                    Collections.sort(allOrders, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));

                    applyFilters();
                });
    }

    private void applyFilters() {
        List<Order> filtered = new ArrayList<>();
        for (Order order : allOrders) {
            if (selectedStatusFilter == null || selectedStatusFilter.equalsIgnoreCase(order.getStatus())) {
                filtered.add(order);
            }
        }

        orderAdapter.updateList(filtered);

        boolean isEmpty = filtered.isEmpty();
        layoutOrderEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvOrderHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (selectedStatusFilter == null) {
                tvEmptyOrderMessage.setText("Bạn chưa có đơn hàng nào");
            } else {
                tvEmptyOrderMessage.setText("Không có đơn hàng nào ở trạng thái \"" + selectedStatusFilter + "\"");
            }
        }
    }

    @Override
    public void onOrderClick(Order order) {
        if (order == null || order.getId() == null) return;
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("orderId", order.getId());
        startActivity(intent);
    }

    @Override
    public void onCancelOrder(Order order) {
        if (order == null || order.getId() == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    db.collection(FirestoreConstants.COLLECTION_ORDERS)
                            .document(order.getId())
                            .update(FirestoreConstants.FIELD_STATUS, "Đã hủy")
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi hủy đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }
}
