package com.example.giuaky.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.giuaky.R;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.ShippingAddress;
import com.example.giuaky.models.User;
import com.example.giuaky.utils.ValidationUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class AddressFormBottomSheet extends BottomSheetDialogFragment {

    public interface OnAddressSavedListener {
        void onAddressSaved(ShippingAddress address);
    }

    private static final String ARG_ADDRESS = "arg_address";
    private static final String ARG_HAS_ANY_ADDRESS = "arg_has_any_address";

    private ShippingAddress editingAddress;
    private boolean hasAnyAddresses = true;
    private OnAddressSavedListener listener;

    private TextInputLayout tilReceiverName, tilReceiverPhone, tilDetailAddress;
    private TextInputEditText edtReceiverName, edtReceiverPhone, edtDetailAddress;
    private MaterialSwitch switchDefault;
    private CircularProgressIndicator progressBarSaveAddress;
    private MaterialButton btnSaveAddress;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public static AddressFormBottomSheet newInstance(@Nullable ShippingAddress address, boolean hasAnyAddresses) {
        AddressFormBottomSheet sheet = new AddressFormBottomSheet();
        Bundle args = new Bundle();
        if (address != null) {
            args.putSerializable(ARG_ADDRESS, address);
        }
        args.putBoolean(ARG_HAS_ANY_ADDRESS, hasAnyAddresses);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnAddressSavedListener(OnAddressSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            editingAddress = (ShippingAddress) getArguments().getSerializable(ARG_ADDRESS);
            hasAnyAddresses = getArguments().getBoolean(ARG_HAS_ANY_ADDRESS, true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_address_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        TextView tvFormTitle = view.findViewById(R.id.tvFormTitle);
        tilReceiverName = view.findViewById(R.id.tilReceiverName);
        tilReceiverPhone = view.findViewById(R.id.tilReceiverPhone);
        tilDetailAddress = view.findViewById(R.id.tilDetailAddress);
        edtReceiverName = view.findViewById(R.id.edtReceiverName);
        edtReceiverPhone = view.findViewById(R.id.edtReceiverPhone);
        edtDetailAddress = view.findViewById(R.id.edtDetailAddress);
        switchDefault = view.findViewById(R.id.switchDefault);
        progressBarSaveAddress = view.findViewById(R.id.progressBarSaveAddress);
        btnSaveAddress = view.findViewById(R.id.btnSaveAddress);

        if (editingAddress != null) {
            tvFormTitle.setText("Cập nhật địa chỉ giao hàng");
            edtReceiverName.setText(editingAddress.getReceiverName());
            edtReceiverPhone.setText(editingAddress.getPhone());
            edtDetailAddress.setText(editingAddress.getDetailAddress());
            switchDefault.setChecked(editingAddress.isDefault());
        } else {
            tvFormTitle.setText("Thêm địa chỉ giao hàng");
            if (!hasAnyAddresses) {
                switchDefault.setChecked(true);
            }
            loadCurrentUserFullName();
        }

        btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void loadCurrentUserFullName() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                            if (edtReceiverName.getText() == null || edtReceiverName.getText().toString().trim().isEmpty()) {
                                edtReceiverName.setText(user.getFullName());
                            }
                        }
                    }
                });
    }

    private void saveAddress() {
        tilReceiverName.setError(null);
        tilReceiverPhone.setError(null);
        tilDetailAddress.setError(null);

        String name = edtReceiverName.getText() != null ? edtReceiverName.getText().toString().trim() : "";
        String phone = edtReceiverPhone.getText() != null ? edtReceiverPhone.getText().toString().trim() : "";
        String address = edtDetailAddress.getText() != null ? edtDetailAddress.getText().toString().trim() : "";
        boolean isDefault = switchDefault.isChecked();

        boolean valid = true;
        if (!ValidationUtils.isNotEmpty(name)) {
            tilReceiverName.setError("Vui lòng nhập họ tên người nhận");
            valid = false;
        }

        if (!ValidationUtils.isNotEmpty(phone) || phone.length() < 9) {
            tilReceiverPhone.setError("Vui lòng nhập số điện thoại hợp lệ (từ 9 số)");
            valid = false;
        }

        if (!ValidationUtils.isNotEmpty(address)) {
            tilDetailAddress.setError("Vui lòng nhập địa chỉ chi tiết");
            valid = false;
        }

        if (!valid) return;

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu địa chỉ", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String uid = mAuth.getCurrentUser().getUid();

        DocumentReference targetDocRef;
        String addressId;
        if (editingAddress != null && editingAddress.getId() != null && !editingAddress.getId().isEmpty()) {
            addressId = editingAddress.getId();
            targetDocRef = db.collection(FirestoreConstants.COLLECTION_USERS)
                    .document(uid)
                    .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                    .document(addressId);
        } else {
            targetDocRef = db.collection(FirestoreConstants.COLLECTION_USERS)
                    .document(uid)
                    .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                    .document();
            addressId = targetDocRef.getId();
        }

        ShippingAddress resultAddress = new ShippingAddress(
                addressId,
                name,
                phone,
                address,
                isDefault
        );

        if (isDefault) {
            // Unset isDefault on other addresses via WriteBatch
            db.collection(FirestoreConstants.COLLECTION_USERS)
                    .document(uid)
                    .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (!doc.getId().equals(addressId)) {
                                Boolean docIsDefault = doc.getBoolean(FirestoreConstants.FIELD_IS_DEFAULT);
                                if (docIsDefault != null && docIsDefault) {
                                    batch.update(doc.getReference(), FirestoreConstants.FIELD_IS_DEFAULT, false);
                                }
                            }
                        }
                        batch.set(targetDocRef, resultAddress);
                        batch.commit()
                                .addOnSuccessListener(aVoid -> handleSuccess(resultAddress))
                                .addOnFailureListener(e -> handleError(e.getMessage()));
                    })
                    .addOnFailureListener(e -> handleError(e.getMessage()));
        } else {
            targetDocRef.set(resultAddress)
                    .addOnSuccessListener(aVoid -> handleSuccess(resultAddress))
                    .addOnFailureListener(e -> handleError(e.getMessage()));
        }
    }

    private void handleSuccess(ShippingAddress savedAddress) {
        setLoading(false);
        Toast.makeText(requireContext(), "Lưu địa chỉ thành công", Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onAddressSaved(savedAddress);
        }
        dismiss();
    }

    private void handleError(String message) {
        setLoading(false);
        Toast.makeText(requireContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean loading) {
        btnSaveAddress.setEnabled(!loading);
        progressBarSaveAddress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
