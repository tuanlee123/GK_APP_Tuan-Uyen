package com.example.giuaky.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.giuaky.R;
import com.example.giuaky.activities.LoginActivity;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvAvatarInitial;
    private TextView tvProfileFullName;
    private TextView tvProfileEmail;
    private TextView tvProfileFullNameDetail;
    private TextView tvProfileEmailDetail;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());

        loadProfile();
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
                            String fullName = user.getFullName() != null ? user.getFullName() : "";
                            String email = user.getEmail() != null ? user.getEmail() : "";

                            tvProfileFullName.setText(fullName);
                            tvProfileFullNameDetail.setText(fullName);
                            tvProfileEmail.setText(email);
                            tvProfileEmailDetail.setText(email);

                            // Set avatar initial from first letter of full name
                            if (!fullName.isEmpty()) {
                                tvAvatarInitial.setText(String.valueOf(fullName.charAt(0)).toUpperCase());
                            }
                        }
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
}
