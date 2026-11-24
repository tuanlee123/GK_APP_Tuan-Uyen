package com.example.giuaky.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.example.giuaky.models.User;
import com.example.giuaky.utils.CurrencyUtils;
import com.example.giuaky.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {

    private TextInputLayout tilCustomerName, tilCustomerPhone, tilCustomerAddress;
    private TextInputEditText edtCustomerName, edtCustomerPhone, edtCustomerAddress;
    private RadioGroup rgPaymentMethod;
    private TextView tvSubtotal, tvCheckoutTotal;
    private CircularProgressIndicator progressBarCheckout;
    private MaterialButton btnPlaceOrder;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private boolean isFromCart = false;
    private ArrayList<CartItem> checkoutItems = new ArrayList<>();
    private double totalAmount = 0;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        isFromCart = getIntent().getBooleanExtra("isFromCart", false);
        ArrayList<CartItem> receivedList = (ArrayList<CartItem>) getIntent().getSerializableExtra("cartItems");
        if (receivedList != null) {
            checkoutItems.addAll(receivedList);
        }

        if (checkoutItems.isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tilCustomerName = findViewById(R.id.tilCustomerName);
        tilCustomerPhone = findViewById(R.id.tilCustomerPhone);
        tilCustomerAddress = findViewById(R.id.tilCustomerAddress);
        edtCustomerName = findViewById(R.id.edtCustomerName);
        edtCustomerPhone = findViewById(R.id.edtCustomerPhone);
        edtCustomerAddress = findViewById(R.id.edtCustomerAddress);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        progressBarCheckout = findViewById(R.id.progressBarCheckout);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        findViewById(R.id.btnCheckoutBack).setOnClickListener(v -> finish());

        // Setup RecyclerView
        RecyclerView rvOrderSummary = findViewById(R.id.rvOrderSummary);
        rvOrderSummary.setLayoutManager(new LinearLayoutManager(this));
        rvOrderSummary.setAdapter(new OrderSummaryAdapter(this, checkoutItems));

        calculateTotal();

        loadUserInfo();

        btnPlaceOrder.setOnClickListener(v -> attemptPlaceOrder());
    }

    private void calculateTotal() {
        totalAmount = 0;
        for (CartItem item : checkoutItems) {
            totalAmount += item.getPrice() * item.getQuantity();
        }
        tvSubtotal.setText(CurrencyUtils.formatVnd(totalAmount));
        tvCheckoutTotal.setText(CurrencyUtils.formatVnd(totalAmount));
    }

    private void loadUserInfo() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                            edtCustomerName.setText(user.getFullName());
                        }
                    }
                });
    }

    private void attemptPlaceOrder() {
        tilCustomerName.setError(null);
        tilCustomerPhone.setError(null);
        tilCustomerAddress.setError(null);

        String name = edtCustomerName.getText() != null ? edtCustomerName.getText().toString().trim() : "";
        String phone = edtCustomerPhone.getText() != null ? edtCustomerPhone.getText().toString().trim() : "";
        String address = edtCustomerAddress.getText() != null ? edtCustomerAddress.getText().toString().trim() : "";

        boolean valid = true;

        if (!ValidationUtils.isNotEmpty(name)) {
            tilCustomerName.setError("Vui lòng nhập họ và tên người nhận");
            valid = false;
        }

        if (!ValidationUtils.isNotEmpty(phone) || phone.length() < 9) {
            tilCustomerPhone.setError("Vui lòng nhập số điện thoại hợp lệ (từ 9 số)");
            valid = false;
        }

        if (!ValidationUtils.isNotEmpty(address)) {
            tilCustomerAddress.setError("Vui lòng nhập địa chỉ giao hàng");
            valid = false;
        }

        if (!valid) return;

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod = "Thanh toán khi nhận hàng (COD)";
        int selectedRadioId = rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton rbSelected = findViewById(selectedRadioId);
        if (rbSelected != null) {
            paymentMethod = rbSelected.getText().toString();
        }

        setLoading(true);

        String uid = mAuth.getCurrentUser().getUid();
        String orderId = db.collection(FirestoreConstants.COLLECTION_ORDERS).document().getId();

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : checkoutItems) {
            orderItems.add(new OrderItem(
                    ci.getProductId(),
                    ci.getProductName(),
                    ci.getProductImage(),
                    ci.getPrice(),
                    ci.getQuantity()
            ));
        }

        Order newOrder = new Order(
                orderId,
                uid,
                name,
                phone,
                address,
                paymentMethod,
                orderItems,
                totalAmount,
                "Đang xử lý",
                System.currentTimeMillis()
        );

        db.collection(FirestoreConstants.COLLECTION_ORDERS)
                .document(orderId)
                .set(newOrder)
                .addOnSuccessListener(aVoid -> {
                    // 1. Increment soldCount for each product
                    for (OrderItem item : orderItems) {
                        if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                            db.collection(FirestoreConstants.COLLECTION_PRODUCTS)
                                    .document(item.getProductId())
                                    .update(FirestoreConstants.FIELD_SOLD_COUNT, FieldValue.increment(item.getQuantity()));
                        }
                    }

                    // 2. Clear cart items if purchased from cart
                    if (isFromCart) {
                        for (CartItem ci : checkoutItems) {
                            if (ci.getProductId() != null) {
                                db.collection(FirestoreConstants.COLLECTION_CARTS)
                                        .document(uid)
                                        .collection(FirestoreConstants.COLLECTION_ITEMS)
                                        .document(ci.getProductId())
                                        .delete();
                            }
                        }
                    }

                    setLoading(false);
                    showOrderSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi đặt hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        btnPlaceOrder.setEnabled(!loading);
        progressBarCheckout.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showOrderSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đặt hàng thành công!")
                .setMessage("Cảm ơn bạn đã mua sắm tại TechStore. Đơn hàng của bạn đã được ghi nhận và đang xử lý.")
                .setCancelable(false)
                .setPositiveButton("Về trang chủ", (dialog, which) -> {
                    Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
