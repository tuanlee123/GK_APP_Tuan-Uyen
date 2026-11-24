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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.giuaky.R;
import com.example.giuaky.activities.LoginActivity;
import com.example.giuaky.adapters.ShippingAddressAdapter;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.ShippingAddress;
import com.example.giuaky.models.User;
import com.example.giuaky.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileFragment extends Fragment implements ShippingAddressAdapter.OnAddressActionListener {

    private TextView tvAvatarInitial;
    private TextView tvProfileFullName;
    private TextView tvProfileEmail;
    private TextView tvProfileFullNameDetail;
    private TextView tvProfileEmailDetail;
    private TextView tvEmptyAddresses;
    private RecyclerView rvShippingAddresses;

    private ShippingAddressAdapter addressAdapter;
    private final List<ShippingAddress> addressList = new ArrayList<>();
    private ListenerRegistration addressListener;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserFullName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileFullNameDetail = view.findViewById(R.id.tvProfileFullNameDetail);
        tvProfileEmailDetail = view.findViewById(R.id.tvProfileEmailDetail);
        tvEmptyAddresses = view.findViewById(R.id.tvEmptyAddresses);
        rvShippingAddresses = view.findViewById(R.id.rvShippingAddresses);

        MaterialButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        MaterialButton btnAddNewAddress = view.findViewById(R.id.btnAddNewAddress);
        btnAddNewAddress.setOnClickListener(v -> {
            AddressFormBottomSheet sheet = AddressFormBottomSheet.newInstance(null, !addressList.isEmpty());
            sheet.show(getChildFragmentManager(), "add_new_address");
        });

        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());

        // Setup RecyclerView
        addressAdapter = new ShippingAddressAdapter(requireContext(), this);
        rvShippingAddresses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvShippingAddresses.setAdapter(addressAdapter);

        loadProfile();
        listenToAddresses();
    }

    private void loadProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            currentUserFullName = user.getFullName() != null ? user.getFullName() : "";
                            String email = user.getEmail() != null ? user.getEmail() : "";

                            tvProfileFullName.setText(currentUserFullName);
                            tvProfileFullNameDetail.setText(currentUserFullName);
                            tvProfileEmail.setText(email);
                            tvProfileEmailDetail.setText(email);

                            // Set avatar initial from first letter of full name
                            if (!currentUserFullName.isEmpty()) {
                                tvAvatarInitial.setText(String.valueOf(currentUserFullName.charAt(0)).toUpperCase());
                            } else {
                                tvAvatarInitial.setText("U");
                            }
                        }
                    }
                });
    }

    private void showEditProfileDialog() {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextInputLayout tilEditFullName = dialogView.findViewById(R.id.tilEditFullName);
        TextInputEditText edtEditFullName = dialogView.findViewById(R.id.edtEditFullName);

        edtEditFullName.setText(currentUserFullName);
        if (currentUserFullName != null) {
            edtEditFullName.setSelection(currentUserFullName.length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa thông tin")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(d -> {
            MaterialButton btnPositive = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnPositive.setOnClickListener(v -> {
                tilEditFullName.setError(null);
                String newName = edtEditFullName.getText() != null ? edtEditFullName.getText().toString().trim() : "";
                if (!ValidationUtils.isNotEmpty(newName)) {
                    tilEditFullName.setError("Họ và tên không được để trống");
                    return;
                }

                if (mAuth.getCurrentUser() == null) return;
                String uid = mAuth.getCurrentUser().getUid();

                btnPositive.setEnabled(false);
                db.collection(FirestoreConstants.COLLECTION_USERS)
                        .document(uid)
                        .update(FirestoreConstants.FIELD_FULL_NAME, newName)
                        .addOnSuccessListener(aVoid -> {
                            if (!isAdded()) return;
                            currentUserFullName = newName;
                            tvProfileFullName.setText(newName);
                            tvProfileFullNameDetail.setText(newName);
                            if (!newName.isEmpty()) {
                                tvAvatarInitial.setText(String.valueOf(newName.charAt(0)).toUpperCase());
                            }
                            Toast.makeText(requireContext(), "Cập nhật họ và tên thành công", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            btnPositive.setEnabled(true);
                            Toast.makeText(requireContext(), "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        });

        dialog.show();
    }

    private void listenToAddresses() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        addressListener = db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;

                    addressList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ShippingAddress address = doc.toObject(ShippingAddress.class);
                        if (address.getId() == null || address.getId().isEmpty()) {
                            address.setId(doc.getId());
                        }
                        addressList.add(address);
                    }

                    // Sort so that default address is at the top
                    Collections.sort(addressList, (a1, a2) -> Boolean.compare(a2.isDefault(), a1.isDefault()));

                    addressAdapter.updateList(addressList);

                    boolean isEmpty = addressList.isEmpty();
                    tvEmptyAddresses.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvShippingAddresses.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                });
    }

    @Override
    public void onEdit(ShippingAddress address) {
        AddressFormBottomSheet sheet = AddressFormBottomSheet.newInstance(address, !addressList.isEmpty());
        sheet.show(getChildFragmentManager(), "edit_address");
    }

    @Override
    public void onDelete(ShippingAddress address) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ của \"" + address.getReceiverName() + "\" không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteAddress(address))
                .show();
    }

    private void deleteAddress(ShippingAddress address) {
        if (mAuth.getCurrentUser() == null || address.getId() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection(FirestoreConstants.COLLECTION_USERS)
                .document(uid)
                .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                .document(address.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onSetDefault(ShippingAddress address) {
        if (mAuth.getCurrentUser() == null || address.getId() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        WriteBatch batch = db.batch();
        for (ShippingAddress a : addressList) {
            DocumentReference docRef = db.collection(FirestoreConstants.COLLECTION_USERS)
                    .document(uid)
                    .collection(FirestoreConstants.COLLECTION_ADDRESSES)
                    .document(a.getId());

            if (a.getId().equals(address.getId())) {
                batch.update(docRef, FirestoreConstants.FIELD_IS_DEFAULT, true);
            } else if (a.isDefault()) {
                batch.update(docRef, FirestoreConstants.FIELD_IS_DEFAULT, false);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Đã đặt làm địa chỉ mặc định", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (addressListener != null) {
            addressListener.remove();
        }
    }
}
