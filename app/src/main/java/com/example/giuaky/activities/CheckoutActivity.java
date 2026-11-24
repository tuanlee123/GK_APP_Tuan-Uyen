package com.example.giuaky.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.adapters.AddressSelectAdapter;
import com.example.giuaky.adapters.OrderSummaryAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.fragments.AddressFormBottomSheet;
import com.example.giuaky.models.CartItem;
import com.example.giuaky.models.Order;
import com.example.giuaky.models.OrderItem;
import com.example.giuaky.models.ShippingAddress;
import com.example.giuaky.utils.CurrencyUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {

    private View layoutSelectedAddress;
    private View layoutNoAddress;
    private TextView tvCheckoutReceiverName;
    private TextView tvCheckoutReceiverPhone;
    private TextView tvCheckoutDefaultBadge;
    private TextView tvCheckoutDetailAddress;
    private MaterialButton btnChangeAddress;
    private MaterialButton btnAddNewAddressCheckout;

    private RadioGroup rgPaymentMethod;
    private TextView tvSubtotal, tvCheckoutTotal;
    private CircularProgressIndicator progressBarCheckout;
    private MaterialButton btnPlaceOrder;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private boolean isFromCart = false;
    private final ArrayList<CartItem> checkoutItems = new ArrayList<>();
    private final List<ShippingAddress> userAddresses = new ArrayList<>();
    private ShippingAddress selectedAddress = null;
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

        layoutSelectedAddress = findViewById(R.id.layoutSelectedAddress);
        layoutNoAddress = findViewById(R.id.layoutNoAddress);
        tvCheckoutReceiverName = findViewById(R.id.tvCheckoutReceiverName);
        tvCheckoutReceiverPhone = findViewById(R.id.tvCheckoutReceiverPhone);
        tvCheckoutDefaultBadge = findViewById(R.id.tvCheckoutDefaultBadge);
        tvCheckoutDetailAddress = findViewById(R.id.tvCheckoutDetailAddress);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);
        btnAddNewAddressCheckout = findViewById(R.id.btnAddNewAddressCheckout);

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        progressBarCheckout = findViewById(R.id.progressBarCheckout);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        findViewById(R.id.btnCheckoutBack).setOnClickListener(v -> finish());

        // Address interactions
        btnChangeAddress.setOnClickListener(v -> showAddressSelectionBottomSheet());
        btnAddNewAddressCheckout.setOnClickListener(v -> openAddAddressBottomSheet());

        // Setup RecyclerView Order Items
        RecyclerView rvOrderSummary = findViewById(R.id.rvOrderSummary);
        rvOrderSummary.setLayoutManager(new LinearLayoutManager(this));
        rvOrderSummary.setAdapter(new OrderSummaryAdapter(this, checkoutItems));

        calculateTotal();

        loadUserAddresses();

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

    private void loadUserAddresses() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userAddresses.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ShippingAddress address = doc.toObject(ShippingAddress.class);
                        if (address.getId() == null || address.getId().isEmpty()) {
                            address.setId(doc.getId());
                        }
                        userAddresses.add(address);
                    }

                    // Sort so default is first
                    Collections.sort(userAddresses, (a1, a2) -> Boolean.compare(a2.isDefault(), a1.isDefault()));

                    // If no address is currently selected, pick the default one (or first one)
                    if (selectedAddress == null) {
                        for (ShippingAddress addr : userAddresses) {
                            if (addr.isDefault()) {
                                selectedAddress = addr;
                                break;
                            }
                        }
                        if (selectedAddress == null && !userAddresses.isEmpty()) {
                            selectedAddress = userAddresses.get(0);
                        }
                    } else {
                        // Keep selection in sync with loaded addresses
                        boolean found = false;
                        for (ShippingAddress addr : userAddresses) {
                            if (addr.getId().equals(selectedAddress.getId())) {
                                selectedAddress = addr;
                                found = true;
                                break;
                            }
                        }
                        if (!found && !userAddresses.isEmpty()) {
                            selectedAddress = userAddresses.get(0);
                        } else if (userAddresses.isEmpty()) {
                            selectedAddress = null;
                        }
                    }

                    updateAddressUI();
                });
    }

    private void updateAddressUI() {
        if (selectedAddress != null) {
            layoutSelectedAddress.setVisibility(View.VISIBLE);
            layoutNoAddress.setVisibility(View.GONE);

            tvCheckoutReceiverName.setText(selectedAddress.getReceiverName());
            tvCheckoutReceiverPhone.setText(selectedAddress.getPhone());
            tvCheckoutDetailAddress.setText(selectedAddress.getDetailAddress());
            tvCheckoutDefaultBadge.setVisibility(selectedAddress.isDefault() ? View.VISIBLE : View.GONE);
        } else {
            layoutSelectedAddress.setVisibility(View.GONE);
            layoutNoAddress.setVisibility(View.VISIBLE);
        }
    }

    private void showAddressSelectionBottomSheet() {
        if (userAddresses.isEmpty()) {
            openAddAddressBottomSheet();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_select_address, null);
        dialog.setContentView(bottomSheetView);

        RecyclerView rvSelectableAddresses = bottomSheetView.findViewById(R.id.rvSelectableAddresses);
        MaterialButton btnAddNewAddressFromSelect = bottomSheetView.findViewById(R.id.btnAddNewAddressFromSelect);

        String currentSelectedId = selectedAddress != null ? selectedAddress.getId() : "";
        AddressSelectAdapter adapter = new AddressSelectAdapter(this, currentSelectedId, address -> {
            selectedAddress = address;
            updateAddressUI();
            dialog.dismiss();
        });

        rvSelectableAddresses.setLayoutManager(new LinearLayoutManager(this));
        rvSelectableAddresses.setAdapter(adapter);
        adapter.updateList(userAddresses, currentSelectedId);

        btnAddNewAddressFromSelect.setOnClickListener(v -> {
            dialog.dismiss();
            openAddAddressBottomSheet();
        });

        dialog.show();
    }

    private void openAddAddressBottomSheet() {
        AddressFormBottomSheet sheet = AddressFormBottomSheet.newInstance(null, !userAddresses.isEmpty());
        sheet.setOnAddressSavedListener(newAddress -> {
            selectedAddress = newAddress;
            loadUserAddresses();
        });
        sheet.show(getSupportFragmentManager(), "checkout_add_address");
    }

    private void attemptPlaceOrder() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Vui lòng thêm hoặc chọn địa chỉ nhận hàng", Toast.LENGTH_LONG).show();
            return;
        }

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
                selectedAddress.getReceiverName(),
                selectedAddress.getPhone(),
                selectedAddress.getDetailAddress(),
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
