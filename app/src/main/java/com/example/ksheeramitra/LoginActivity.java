package com.example.ksheeramitra;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;

    private Button btnLogin;
    private Button btnGoogleLogin;
    private Button btnShowPassword;

    private TextView txtForgotPassword;
    private TextView txtRegister;

    private FirebaseAuth mAuth;

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // -------------------------------------------------
        // FIREBASE
        // -------------------------------------------------

        mAuth = FirebaseAuth.getInstance();

        // -------------------------------------------------
        // FIND VIEWS
        // -------------------------------------------------

        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnShowPassword = findViewById(R.id.btnShowPassword);

        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtRegister = findViewById(R.id.txtRegister);

        // -------------------------------------------------
        // REGISTER
        // -------------------------------------------------

        txtRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });

        // -------------------------------------------------
        // FORGOT PASSWORD
        // -------------------------------------------------

        txtForgotPassword.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    ForgotPasswordActivity.class
            );

            startActivity(intent);
        });

        // -------------------------------------------------
        // SHOW / HIDE PASSWORD
        // -------------------------------------------------

        btnShowPassword.setOnClickListener(v -> {

            if (passwordVisible) {

                editPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );

                btnShowPassword.setText("SHOW");

                passwordVisible = false;

            } else {

                editPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                btnShowPassword.setText("HIDE");

                passwordVisible = true;
            }

            editPassword.setSelection(editPassword.length());
        });

        // -------------------------------------------------
        // LOGIN
        // -------------------------------------------------

        btnLogin.setOnClickListener(v -> loginUser());

        // -------------------------------------------------
        // GOOGLE LOGIN
        // -------------------------------------------------

        // Google authentication can be configured later.
        // This does not falsely report a successful login.

        btnGoogleLogin.setOnClickListener(v -> {

            Toast.makeText(
                    LoginActivity.this,
                    "Google Login is not configured yet.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // -------------------------------------------------
        // REGISTERED EMAIL
        // -------------------------------------------------

        String registeredEmail =
                getIntent().getStringExtra("registered_email");

        if (registeredEmail != null
                && !registeredEmail.trim().isEmpty()) {

            editUsername.setText(registeredEmail);

            editPassword.requestFocus();
        }
    }

    // =====================================================
    // LOGIN USER
    // =====================================================

    private void loginUser() {

        String email =
                editUsername.getText()
                        .toString()
                        .trim();

        String password =
                editPassword.getText()
                        .toString();

        // -------------------------------------------------
        // EMAIL VALIDATION
        // -------------------------------------------------

        if (TextUtils.isEmpty(email)) {

            editUsername.setError(
                    "Enter your email address"
            );

            editUsername.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            editUsername.setError(
                    "Enter a valid email address"
            );

            editUsername.requestFocus();

            return;
        }

        // -------------------------------------------------
        // PASSWORD VALIDATION
        // -------------------------------------------------

        if (TextUtils.isEmpty(password)) {

            editPassword.setError(
                    "Enter your password"
            );

            editPassword.requestFocus();

            return;
        }

        // -------------------------------------------------
        // DISABLE BUTTON
        // -------------------------------------------------

        btnLogin.setEnabled(false);

        btnLogin.setText("LOGGING IN...");

        // -------------------------------------------------
        // FIREBASE LOGIN
        // -------------------------------------------------

        mAuth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser user =
                                        mAuth.getCurrentUser();

                                if (user == null) {

                                    loginFailed(
                                            "Unable to load your account."
                                    );

                                    return;
                                }

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Login successful",
                                        Toast.LENGTH_SHORT
                                ).show();

                                openDashboard();

                            } else {

                                String message =
                                        getLoginErrorMessage(
                                                task.getException()
                                        );

                                loginFailed(message);
                            }
                        }
                );
    }

    // =====================================================
    // OPEN DASHBOARD
    // =====================================================

    private void openDashboard() {

        Intent intent = new Intent(
                LoginActivity.this,
                DashboardActivity.class
        );

        // Remove LoginActivity from the back stack.
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =====================================================
    // LOGIN FAILED
    // =====================================================

    private void loginFailed(String message) {

        btnLogin.setEnabled(true);

        btnLogin.setText("LOGIN");

        Toast.makeText(
                LoginActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // =====================================================
    // FIREBASE ERROR MESSAGE
    // =====================================================

    private String getLoginErrorMessage(Exception exception) {

        if (exception == null) {

            return "Login failed. Please try again.";
        }

        String error = exception.getMessage();

        if (error == null) {

            return "Login failed. Please try again.";
        }

        String lowerError = error.toLowerCase();

        if (lowerError.contains("invalid credential")
                || lowerError.contains("invalid-credential")) {

            return "Invalid email or password.";
        }

        if (lowerError.contains("password is invalid")
                || lowerError.contains("wrong-password")) {

            return "Incorrect password.";
        }

        if (lowerError.contains("user not found")
                || lowerError.contains("user-not-found")) {

            return "No account found with this email.";
        }

        if (lowerError.contains("badly formatted")
                || lowerError.contains("invalid-email")) {

            return "Please enter a valid email address.";
        }

        if (lowerError.contains("network")) {

            return "Network error. Check your internet connection.";
        }

        if (lowerError.contains("too many requests")) {

            return "Too many login attempts. Please try again later.";
        }

        return "Login failed. Please check your email and password.";
    }

    // =====================================================
    // IF ALREADY LOGGED IN
    // =====================================================

    @Override
    protected void onStart() {

        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            openDashboard();
        }
    }
}
