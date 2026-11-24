package com.example.giuaky.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.giuaky.R;
import com.example.giuaky.constants.FirestoreConstants;
import com.example.giuaky.models.User;
import com.example.giuaky.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilRegisterEmail, tilRegisterPassword, tilConfirmPassword;
    private TextInputEditText edtFullName, edtRegisterEmail, edtRegisterPassword, edtConfirmPassword;
    private MaterialButton btnRegister;
    private CircularProgressIndicator progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilFullName = findViewById(R.id.tilFullName);
        tilRegisterEmail = findViewById(R.id.tilRegisterEmail);
        tilRegisterPassword = findViewById(R.id.tilRegisterPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        edtFullName = findViewById(R.id.edtFullName);
        edtRegisterEmail = findViewById(R.id.edtRegisterEmail);
        edtRegisterPassword = findViewById(R.id.edtRegisterPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBarRegister);

        btnRegister.setOnClickListener(v -> attemptRegister());

        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        tilFullName.setError(null);
        tilRegisterEmail.setError(null);
        tilRegisterPassword.setError(null);
        tilConfirmPassword.setError(null);

        String fullName = edtFullName.getText() != null ? edtFullName.getText().toString().trim() : "";
        String email = edtRegisterEmail.getText() != null ? edtRegisterEmail.getText().toString().trim() : "";
        String password = edtRegisterPassword.getText() != null ? edtRegisterPassword.getText().toString() : "";
        String confirmPassword = edtConfirmPassword.getText() != null ? edtConfirmPassword.getText().toString() : "";

        boolean valid = true;

        if (!ValidationUtils.isNotEmpty(fullName)) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            valid = false;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            tilRegisterEmail.setError("Email không hợp lệ");
            valid = false;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            tilRegisterPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            valid = false;
        }

        if (!ValidationUtils.isPasswordMatching(password, confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            valid = false;
        }

        if (!valid) return;

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, authResult -> {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        Toast.makeText(this, "Lỗi: Không thể lấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    User newUser = new User(uid, fullName, email);

                    db.collection(FirestoreConstants.COLLECTION_USERS)
                            .document(uid)
                            .set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                navigateToMain();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(this, e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("email address is already in use")) {
                        Toast.makeText(this, "Email này đã được đăng ký. Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Đăng ký thất bại: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
