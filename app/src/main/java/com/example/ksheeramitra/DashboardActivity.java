package com.example.ksheeramitra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "Dashboard";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration farmerListener;
    private ListenerRegistration milkListener;
    private ListenerRegistration qualityListener;
    private ListenerRegistration paymentListener;

    private TextView txtMilk;
    private TextView txtFarmers;
    private TextView txtQuality;
    private TextView txtPayment;
    private TextView txtFarmersPaid;

    private CardView cardMilk;
    private CardView cardFarmers;
    private CardView cardQuality;
    private CardView cardPayment;
    private CardView cardFarmersPaid;

    private Button btnProfile;
    private Button btnAddFarmer;
    private Button btnFarmerList;
    private Button btnCollectMilk;
    private Button btnQualityTest;
    private Button btnPayments;
    private Button btnReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupNavigation();

        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        listenToDashboardData();
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeViews() {
        txtMilk = findViewById(R.id.txtMilk);
        txtFarmers = findViewById(R.id.txtFarmers);
        txtQuality = findViewById(R.id.txtQuality);
        txtPayment = findViewById(R.id.txtPayment);
        txtFarmersPaid = findViewById(R.id.txtFarmersPaid);

        cardMilk = findViewById(R.id.cardMilk);
        cardFarmers = findViewById(R.id.cardFarmers);
        cardQuality = findViewById(R.id.cardQuality);
        cardPayment = findViewById(R.id.cardPayment);
        cardFarmersPaid = findViewById(R.id.cardFarmersPaid);

        btnProfile = findViewById(R.id.btnProfile);
        btnAddFarmer = findViewById(R.id.btnAddFarmer);
        btnFarmerList = findViewById(R.id.btnFarmerList);
        btnCollectMilk = findViewById(R.id.btnCollectMilk);
        btnQualityTest = findViewById(R.id.btnQualityTest);
        btnPayments = findViewById(R.id.btnPayments);
        btnReports = findViewById(R.id.btnReports);
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private void setupNavigation() {

        btnProfile.setOnClickListener(v -> openActivity(ProfileActivity.class));
        btnAddFarmer.setOnClickListener(v -> openActivity(FarmerActivity.class));
        btnFarmerList.setOnClickListener(v -> openActivity(FarmerListActivity.class));
        btnCollectMilk.setOnClickListener(v -> openActivity(MilkCollectionActivity.class));
        btnQualityTest.setOnClickListener(v -> openActivity(QualityTestingActivity.class));
        btnPayments.setOnClickListener(v -> openActivity(PaymentManagementActivity.class));
        btnReports.setOnClickListener(v -> openActivity(ReportsActivity.class));

        cardMilk.setOnClickListener(v -> openActivity(MilkCollectionListActivity.class));
        cardFarmers.setOnClickListener(v -> openActivity(FarmerListActivity.class));
        cardQuality.setOnClickListener(v -> openActivity(QualityTestListActivity.class));
        cardPayment.setOnClickListener(v -> openActivity(PaymentListActivity.class));
        cardFarmersPaid.setOnClickListener(v -> openActivity(PaymentListActivity.class));
    }

    private void openActivity(Class<?> activityClass) {
        try {
            startActivity(new Intent(DashboardActivity.this, activityClass));
        } catch (Exception e) {
            // Shows the real reason if an Activity is missing/not registered
            Log.e(TAG, "Cannot open " + activityClass.getSimpleName(), e);
            Toast.makeText(
                    this,
                    "Cannot open " + activityClass.getSimpleName()
                            + ": " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // DASHBOARD DATA
    // =========================================================

    private void listenToDashboardData() {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date());

        // ---------------------- FARMERS ----------------------
        farmerListener = db.collection("users")
                .document(uid)
                .collection("farmers")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showListenerError("Farmers", error);
                        return;
                    }

                    txtFarmers.setText(
                            snapshot == null
                                    ? "0"
                                    : String.valueOf(snapshot.size())
                    );
                });

        // ---------------------- TODAY MILK ----------------------
        milkListener = db.collection("milkCollections")
                .whereEqualTo("ownerUid", uid)
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showListenerError("Milk", error);
                        return;
                    }

                    double totalMilk = 0.0;

                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            totalMilk += getDouble(document, "quantity");
                        }
                    }

                    txtMilk.setText(
                            String.format(Locale.US, "%.2f L", totalMilk)
                    );
                });

        // ---------------------- TODAY QUALITY ----------------------
        qualityListener = db.collection("milkQualityTests")
                .whereEqualTo("ownerUid", uid)
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showListenerError("Quality", error);
                        return;
                    }

                    int count = (snapshot == null) ? 0 : snapshot.size();

                    txtQuality.setText(String.valueOf(count));
                });

        // ---------------------- PAYMENTS ----------------------
        paymentListener = db.collection("payments")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        showListenerError("Payments", error);
                        return;
                    }

                    double totalPaid = 0.0;
                    double totalPending = 0.0;

                    if (snapshot != null) {

                        for (DocumentSnapshot document : snapshot.getDocuments()) {

                            double total = getDouble(document, "totalAmount");
                            double paid = getDouble(document, "amountPaid");

                            if (paid < 0) {
                                paid = 0;
                            }

                            if (paid > total) {
                                paid = total;
                            }

                            double pending = total - paid;

                            if (pending < 0) {
                                pending = 0;
                            }

                            totalPaid += paid;
                            totalPending += pending;
                        }
                    }

                    txtPayment.setText("₹" + formatMoney(totalPending));
                    txtFarmersPaid.setText("₹" + formatMoney(totalPaid));
                });
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void showListenerError(String name, Exception error) {
        Log.e(TAG, name + " listener failed", error);
        Toast.makeText(
                this,
                name + ": " + error.getMessage(),
                Toast.LENGTH_LONG
        ).show();
    }

    private double getDouble(DocumentSnapshot document, String field) {

        Object value = document.get(field);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value == null) {
            return 0.0;
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        if (farmerListener != null) {
            farmerListener.remove();
        }

        if (milkListener != null) {
            milkListener.remove();
        }

        if (qualityListener != null) {
            qualityListener.remove();
        }

        if (paymentListener != null) {
            paymentListener.remove();
        }

        super.onDestroy();
    }
}