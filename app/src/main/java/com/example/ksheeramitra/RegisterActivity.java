package com.example.ksheeramitra;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName;
    private EditText edtUsername;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtConfirmPassword;

    private Button btnRegister;
    private TextView txtLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String PREF_NAME = "KsheeramitraPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtName = findViewById(R.id.edtName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    RegisterActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {

        String name = edtName.getText().toString().trim();
        String email = edtUsername.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword =
                edtConfirmPassword.getText().toString().trim();

        // NAME
        if (TextUtils.isEmpty(name)) {
            edtName.setError("Enter your full name");
            edtName.requestFocus();
            return;
        }

        // EMAIL
        if (TextUtils.isEmpty(email)) {
            edtUsername.setError("Enter your email address");
            edtUsername.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtUsername.setError("Enter a valid email address");
            edtUsername.requestFocus();
            return;
        }

        // PHONE
        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Enter phone number");
            edtPhone.requestFocus();
            return;
        }

        if (!phone.matches("[0-9]{10}")) {
            edtPhone.setError("Enter a valid 10 digit number");
            edtPhone.requestFocus();
            return;
        }

        // PASSWORD
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Enter password");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError(
                    "Password must contain at least 6 characters"
            );
            edtPassword.requestFocus();
            return;
        }

        // CONFIRM PASSWORD
        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError("Confirm your password");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Passwords do not match");
            edtConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("CREATING ACCOUNT...");

        Toast.makeText(
                this,
                "Creating account...",
                Toast.LENGTH_SHORT
        ).show();

        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        btnRegister.setEnabled(true);
                        btnRegister.setText("REGISTER");

                        Toast.makeText(
                                this,
                                getRegistrationErrorMessage(
                                        task.getException()
                                ),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser user =
                            mAuth.getCurrentUser();

                    if (user == null) {

                        btnRegister.setEnabled(true);
                        btnRegister.setText("REGISTER");

                        Toast.makeText(
                                this,
                                "Account creation failed.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String uid = user.getUid();

                    // SAVE LOCALLY FIRST
                    saveLocalProfile(
                            name,
                            phone,
                            email,
                            uid
                    );

                    // SAVE TO FIRESTORE
                    saveUserProfile(
                            uid,
                            name,
                            phone,
                            email
                    );
                });
    }

    // ==================================================
    // SAVE LOCAL PROFILE
    // ==================================================

    private void saveLocalProfile(
            String name,
            String phone,
            String email,
            String uid
    ) {

        getSharedPreferences(
                PREF_NAME,
                MODE_PRIVATE
        )
                .edit()
                .putString("profile_name", name)
                .putString("profile_phone", phone)
                .putString("profile_email", email)
                .putString("profile_uid", uid)
                .apply();
    }

    // ==================================================
    // SAVE FIRESTORE PROFILE
    // ==================================================

    private void saveUserProfile(
            String uid,
            String name,
            String phone,
            String email
    ) {

        Map<String, Object> profile =
                new HashMap<>();

        profile.put("uid", uid);
        profile.put("name", name);
        profile.put("phone", phone);
        profile.put("email", email);

        db.collection("users")
                .document(uid)
                .set(profile)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            RegisterActivity.this,
                            "Account created successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Sign out after registration
                    mAuth.signOut();

                    Intent intent = new Intent(
                            RegisterActivity.this,
                            LoginActivity.class
                    );

                    intent.putExtra(
                            "registered_email",
                            email
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    /*
                     * IMPORTANT:
                     *
                     * Local profile has already been saved.
                     * Therefore registration is still usable.
                     */

                    Toast.makeText(
                            RegisterActivity.this,
                            "Account created. Profile saved locally.",
                            Toast.LENGTH_LONG
                    ).show();

                    mAuth.signOut();

                    Intent intent = new Intent(
                            RegisterActivity.this,
                            LoginActivity.class
                    );

                    intent.putExtra(
                            "registered_email",
                            email
                    );

                    startActivity(intent);
                    finish();
                });
    }

    // ==================================================
    // FIREBASE ERROR
    // ==================================================

    private String getRegistrationErrorMessage(
            Exception exception
    ) {

        if (exception == null) {
            return "Registration failed. Please try again.";
        }

        String error = exception.getMessage();

        if (error == null) {
            return "Registration failed. Please try again.";
        }

        String lowerError =
                error.toLowerCase();

        if (lowerError.contains(
                "email address is already in use")
                || lowerError.contains("email_exists")
                || lowerError.contains("already in use")) {

            return "This email is already registered.";
        }

        if (lowerError.contains("badly formatted")
                || lowerError.contains("invalid-email")) {

            return "Please enter a valid email address.";
        }

        if (lowerError.contains("weak-password")) {

            return "Password is too weak. Use at least 6 characters.";
        }

        if (lowerError.contains("network")) {

            return "Network error. Check your internet connection.";
        }

        return "Registration failed. Please try again.";
    }
}