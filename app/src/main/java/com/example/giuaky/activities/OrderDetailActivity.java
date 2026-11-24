package com.example.giuaky.activities;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.adapters.OrderSummaryAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.CartItem;
import com.example.giuaky.models.Order;
import com.example.giuaky.models.OrderItem;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private CircularProgressIndicator progressBarOrderDetail;
    private View scrollOrderDetail;
    private TextView tvDetailStatus;
    private TextView tvDetailOrderId;
    private TextView tvDetailCreatedAt;
    private TextView tvDetailReceiverNamePhone;
    private TextView tvDetailAddress;
    private RecyclerView rvOrderDetailItems;
    private TextView tvDetailPaymentMethod;
    private TextView tvDetailSubtotal;
    private TextView tvDetailTotalAmount;
    private View cardBottomCancelBar;
    private MaterialButton btnCancelOrderDetail;

    private FirebaseFirestore db;
    private ListenerRegistration orderListener;
    private String orderId;
    private Order currentOrder;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnOrderDetailBack).setOnClickListener(v -> finish());

        progressBarOrderDetail = findViewById(R.id.progressBarOrderDetail);
        scrollOrderDetail = findViewById(R.id.scrollOrderDetail);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailOrderId = findViewById(R.id.tvDetailOrderId);
        tvDetailCreatedAt = findViewById(R.id.tvDetailCreatedAt);
        tvDetailReceiverNamePhone = findViewById(R.id.tvDetailReceiverNamePhone);
        tvDetailAddress = findViewById(R.id.tvDetailAddress);
        rvOrderDetailItems = findViewById(R.id.rvOrderDetailItems);
        tvDetailPaymentMethod = findViewById(R.id.tvDetailPaymentMethod);
        tvDetailSubtotal = findViewById(R.id.tvDetailSubtotal);
        tvDetailTotalAmount = findViewById(R.id.tvDetailTotalAmount);
        cardBottomCancelBar = findViewById(R.id.cardBottomCancelBar);
        btnCancelOrderDetail = findViewById(R.id.btnCancelOrderDetail);

        rvOrderDetailItems.setLayoutManager(new LinearLayoutManager(this));

        btnCancelOrderDetail.setOnClickListener(v -> showCancelConfirmDialog());

        listenToOrder();
    }

    private void listenToOrder() {
        progressBarOrderDetail.setVisibility(View.VISIBLE);
        scrollOrderDetail.setVisibility(View.GONE);

        orderListener = db.collection(FirestoreConstants.COLLECTION_ORDERS)
                .document(orderId)
                .addSnapshotListener((snapshot, error) -> {
                    progressBarOrderDetail.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải đơn hàng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Đơn hàng không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentOrder = snapshot.toObject(Order.class);
                    if (currentOrder != null) {
                        if (currentOrder.getId() == null || currentOrder.getId().isEmpty()) {
                            currentOrder.setId(snapshot.getId());
                        }
                        displayOrderDetails(currentOrder);
                    }
                });
    }

    private void displayOrderDetails(Order order) {
        scrollOrderDetail.setVisibility(View.VISIBLE);

        // Status badge
        String status = order.getStatus() != null ? order.getStatus() : "Đang xử lý";
        tvDetailStatus.setText(status);
        applyStatusBadgeStyle(tvDetailStatus, status);

        // Order ID and Date
        tvDetailOrderId.setText("Mã đơn: #" + order.getId());
        if (order.getCreatedAt() > 0) {
            tvDetailCreatedAt.setText("Thời gian đặt: " + dateFormat.format(new Date(order.getCreatedAt())));
        } else {
            tvDetailCreatedAt.setText("");
        }

        // Delivery info
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "";
        String phone = order.getPhone() != null ? order.getPhone() : "";
        tvDetailReceiverNamePhone.setText(customerName + " • " + phone);
        tvDetailAddress.setText(order.getAddress() != null ? order.getAddress() : "");

        // Product items - reused OrderSummaryAdapter
        List<CartItem> cartItems = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                cartItems.add(new CartItem(
                        oi.getProductId(),
                        oi.getProductName(),
                        oi.getProductImage(),
                        oi.getPrice(),
                        oi.getQuantity()
                ));
            }
        }
        rvOrderDetailItems.setAdapter(new OrderSummaryAdapter(this, cartItems));

        // Payment info
        tvDetailPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod() : "Thanh toán khi nhận hàng (COD)");
        tvDetailSubtotal.setText(CurrencyUtils.formatVnd(order.getTotalAmount()));
        tvDetailTotalAmount.setText(CurrencyUtils.formatVnd(order.getTotalAmount()));

        // Cancel bar
        boolean isProcessing = "Đang xử lý".equalsIgnoreCase(status);
        cardBottomCancelBar.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
    }

    private void showCancelConfirmDialog() {
        if (currentOrder == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    db.collection(FirestoreConstants.COLLECTION_ORDERS)
                            .document(orderId)
                            .update(FirestoreConstants.FIELD_STATUS, "Đã hủy")
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi hủy đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    private void applyStatusBadgeStyle(TextView tvStatus, String status) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(12f);

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
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}
