package com.example.ksheeramitra;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MilkCollectionActivity extends AppCompatActivity {

    private EditText etFarmerId;
    private EditText etMilkQuantity;
    private EditText etFat;
    private EditText etSnf;
    private EditText etRate;

    private TextView tvFarmerName;
    private TextView tvFarmerVillage;
    private TextView tvFarmerStatus;
    private TextView tvResult;

    private RadioGroup radioSession;

    private Button btnLoadFarmer;
    private Button btnCalculate;
    private Button btnSave;
    private Button btnClear;

    private com.google.android.material.materialswitch.MaterialSwitch switchSendSms;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm:ss";

    // Holds the farmer record once successfully loaded from Firestore.
    // Save is only allowed once this is populated and amount is calculated.
    private String loadedFarmerName = null;
    private String loadedFarmerVillage = null;
    private String loadedFarmerPhone = null;
    private double calculatedAmount = -1;

    // Asks for SMS permission before saving, then continues with the save
    private ActivityResultLauncher<String> smsPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milk_collection);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(
                                this,
                                R.string.sms_permission_denied,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    // Save the collection either way
                    saveMilkCollection();
                }
        );

        initializeViews();

        btnLoadFarmer.setOnClickListener(v -> onLoadFarmerClicked());
        btnCalculate.setOnClickListener(v -> onCalculateClicked());
        btnSave.setOnClickListener(v -> onSaveClicked());
        btnClear.setOnClickListener(v -> clearFields());

        // If opened from another screen with a farmer already selected
        loadFarmerFromIntent();
    }

    private void initializeViews() {

        etFarmerId = findViewById(R.id.etFarmerId);
        etMilkQuantity = findViewById(R.id.etMilkQuantity);
        etFat = findViewById(R.id.etFat);
        etSnf = findViewById(R.id.etSnf);
        etRate = findViewById(R.id.etRate);

        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvFarmerVillage = findViewById(R.id.tvFarmerVillage);
        tvFarmerStatus = findViewById(R.id.tvFarmerStatus);
        tvResult = findViewById(R.id.tvResult);

        radioSession = findViewById(R.id.radioSession);

        btnLoadFarmer = findViewById(R.id.btnLoadFarmer);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnSave = findViewById(R.id.btnSave);
        btnClear = findViewById(R.id.btnClear);

        switchSendSms = findViewById(R.id.switchSendSms);
    }

    // =========================================================
    // LOAD FARMER IF PASSED FROM ANOTHER SCREEN
    // =========================================================

    private void loadFarmerFromIntent() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        String farmerId = intent.getStringExtra("farmer_id");

        if (!TextUtils.isEmpty(farmerId)) {
            etFarmerId.setText(farmerId);
            loadFarmerById(farmerId);
        }
    }

    // =========================================================
    // LOAD FARMER BUTTON
    // =========================================================

    private void onLoadFarmerClicked() {

        String farmerId = etFarmerId.getText().toString().trim();

        if (TextUtils.isEmpty(farmerId)) {
            etFarmerId.setError("Enter Farmer ID");
            etFarmerId.requestFocus();
            return;
        }

        if (!farmerId.matches("KM-\\d{6}")) {
            etFarmerId.setError("Farmer ID must be like KM-000001");
            etFarmerId.requestFocus();
            return;
        }

        loadFarmerById(farmerId);
    }

    // =========================================================
    // FIND FARMER
    // =========================================================

    private void loadFarmerById(String farmerId) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();

        // Reset state until the lookup resolves
        loadedFarmerName = null;
        loadedFarmerVillage = null;
        loadedFarmerPhone = null;
        tvFarmerStatus.setText("Looking up farmer...");

        db.collection("users")
                .document(uid)
                .collection("farmers")
                .whereEqualTo("farmerId", farmerId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {

                        DocumentSnapshot farmer = snapshot.getDocuments().get(0);

                        String name = getString(farmer.getData(), "name");
                        String village = getString(farmer.getData(), "village");
                        String phone = getString(farmer.getData(), "phone");

                        loadedFarmerName = name;
                        loadedFarmerVillage = village;
                        loadedFarmerPhone = phone;

                        tvFarmerName.setText("Farmer: " + name);
                        tvFarmerVillage.setText("Village: " + village);
                        tvFarmerStatus.setText("Farmer loaded. Enter milk details.");

                    } else {

                        tvFarmerName.setText("Farmer: Not loaded");
                        tvFarmerVillage.setText("Village: --");
                        tvFarmerStatus.setText("Farmer not found");

                        Toast.makeText(
                                MilkCollectionActivity.this,
                                "Farmer not found",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {

                    tvFarmerStatus.setText("Unable to load farmer");

                    Toast.makeText(
                            MilkCollectionActivity.this,
                            "Unable to find farmer: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // CALCULATE BUTTON
    // =========================================================

    private void onCalculateClicked() {

        if (loadedFarmerName == null) {

            tvFarmerStatus.setText("Load a farmer before calculating");

            Toast.makeText(
                    this,
                    "Please load a farmer first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String quantityText = etMilkQuantity.getText().toString().trim();
        String fatText = etFat.getText().toString().trim();
        String snfText = etSnf.getText().toString().trim();
        String rateText = etRate.getText().toString().trim();

        if (TextUtils.isEmpty(quantityText)) {
            etMilkQuantity.setError("Enter milk quantity");
            etMilkQuantity.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(fatText)) {
            etFat.setError("Enter fat");
            etFat.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(snfText)) {
            etSnf.setError("Enter SNF");
            etSnf.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(rateText)) {
            etRate.setError("Enter milk rate");
            etRate.requestFocus();
            return;
        }

        double quantity;
        double fat;
        double snf;
        double rate;

        try {

            quantity = Double.parseDouble(quantityText);
            fat = Double.parseDouble(fatText);
            snf = Double.parseDouble(snfText);
            rate = Double.parseDouble(rateText);

        } catch (NumberFormatException e) {

            Toast.makeText(
                    this,
                    "Please enter valid numbers",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (quantity <= 0) {
            etMilkQuantity.setError("Quantity must be greater than 0");
            return;
        }

        if (fat < 0 || fat > 15) {
            etFat.setError("Fat must be between 0 and 15");
            return;
        }

        if (snf < 0 || snf > 15) {
            etSnf.setError("SNF must be between 0 and 15");
            return;
        }

        if (rate <= 0) {
            etRate.setError("Rate must be greater than 0");
            return;
        }

        calculatedAmount = quantity * rate;

        tvResult.setText(
                String.format(
                        Locale.US,
                        "Session: %s\nQuantity: %.2f L | Fat: %.1f%% | SNF: %.1f%%\nAmount payable: \u20B9%.2f",
                        getSelectedSession(),
                        quantity,
                        fat,
                        snf,
                        calculatedAmount
                )
        );

        btnSave.setEnabled(true);
    }

    private String getSelectedSession() {
        int checkedId = radioSession.getCheckedRadioButtonId();
        return checkedId == R.id.radioEvening ? "Evening" : "Morning";
    }

    // =========================================================
    // SAVE BUTTON: ask for SMS permission first (if needed)
    // =========================================================

    private void onSaveClicked() {

        boolean readyToSave = loadedFarmerName != null && calculatedAmount >= 0;

        if (readyToSave
                && switchSendSms.isChecked()
                && MessageHelper.needsSmsPermission(this)) {

            // The save continues in the permission callback
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            return;
        }

        saveMilkCollection();
    }

    // =========================================================
    // SAVE MILK COLLECTION + PAYMENT
    // =========================================================

    private void saveMilkCollection() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (loadedFarmerName == null || calculatedAmount < 0) {

            Toast.makeText(
                    this,
                    "Load a farmer and calculate before saving",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid = user.getUid();

        String farmerId = etFarmerId.getText().toString().trim();

        String quantityText = etMilkQuantity.getText().toString().trim();
        String fatText = etFat.getText().toString().trim();
        String snfText = etSnf.getText().toString().trim();
        String rateText = etRate.getText().toString().trim();

        double quantity;
        double fat;
        double snf;
        double rate;

        try {

            quantity = Double.parseDouble(quantityText);
            fat = Double.parseDouble(fatText);
            snf = Double.parseDouble(snfText);
            rate = Double.parseDouble(rateText);

        } catch (NumberFormatException e) {

            Toast.makeText(
                    this,
                    "Please enter valid numbers",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        createMilkAndPayment(
                uid,
                farmerId,
                loadedFarmerName,
                loadedFarmerVillage,
                loadedFarmerPhone,
                getSelectedSession(),
                quantity,
                fat,
                snf,
                rate
        );
    }

    // =========================================================
    // CREATE BOTH RECORDS IN ONE FIRESTORE BATCH
    // =========================================================

    private void createMilkAndPayment(
            String uid,
            String farmerId,
            String farmerName,
            String village,
            String farmerPhone,
            String session,
            double quantity,
            double fat,
            double snf,
            double rate
    ) {

        double amount = quantity * rate;

        String date = new SimpleDateFormat(
                DATE_FORMAT,
                Locale.US
        ).format(new Date());

        String time = new SimpleDateFormat(
                TIME_FORMAT,
                Locale.US
        ).format(new Date());

        long createdAt = System.currentTimeMillis();

        // Read the switch now, because fields are cleared after saving
        final boolean wantSms = switchSendSms.isChecked();

        // -----------------------------------------------------
        // GENERATE UNIQUE MILK COLLECTION ID
        // -----------------------------------------------------

        DocumentReference milkRef =
                db.collection("milkCollections")
                        .document();

        String milkCollectionId = milkRef.getId();

        // -----------------------------------------------------
        // GENERATE UNIQUE PAYMENT ID
        // -----------------------------------------------------

        DocumentReference paymentRef =
                db.collection("payments")
                        .document();

        String paymentId = paymentRef.getId();

        // -----------------------------------------------------
        // MILK COLLECTION DATA
        // -----------------------------------------------------

        Map<String, Object> milkData = new HashMap<>();

        milkData.put("milkCollectionId", milkCollectionId);
        milkData.put("ownerUid", uid);
        milkData.put("farmerId", farmerId);
        milkData.put("farmerName", farmerName);
        milkData.put("village", village);
        milkData.put("session", session);
        milkData.put("quantity", quantity);
        milkData.put("milkQuantity", quantity);
        milkData.put("fat", fat);
        milkData.put("snf", snf);
        milkData.put("rate", rate);
        milkData.put("pricePerLitre", rate);
        milkData.put("amount", amount);
        milkData.put("date", date);
        milkData.put("time", time);
        milkData.put("createdAt", createdAt);

        // -----------------------------------------------------
        // PAYMENT DATA
        // -----------------------------------------------------

        Map<String, Object> paymentData = new HashMap<>();

        paymentData.put("paymentId", paymentId);
        paymentData.put("ownerUid", uid);
        paymentData.put("farmerId", farmerId);
        paymentData.put("farmerName", farmerName);
        paymentData.put("village", village);
        paymentData.put("session", session);

        // VERY IMPORTANT:
        // Link payment to this exact milk collection
        paymentData.put("milkCollectionId", milkCollectionId);

        paymentData.put("quantity", quantity);
        paymentData.put("milkQuantity", quantity);
        paymentData.put("fat", fat);
        paymentData.put("snf", snf);
        paymentData.put("rate", rate);
        paymentData.put("totalAmount", amount);
        paymentData.put("amount", amount);

        // Nothing has been paid yet
        paymentData.put("amountPaid", 0.0);

        // Entire amount is outstanding
        paymentData.put("remainingAmount", amount);

        // IMPORTANT
        paymentData.put("status", "PENDING");

        paymentData.put("date", date);
        paymentData.put("time", time);
        paymentData.put("createdAt", createdAt);

        // -----------------------------------------------------
        // SAVE BOTH AT THE SAME TIME
        // -----------------------------------------------------

        WriteBatch batch = db.batch();

        batch.set(milkRef, milkData);
        batch.set(paymentRef, paymentData);

        batch.commit()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            MilkCollectionActivity.this,
                            "Milk collected successfully.\n"
                                    + "Payment marked PENDING: \u20B9"
                                    + String.format(
                                    Locale.US,
                                    "%.2f",
                                    amount
                            ),
                            Toast.LENGTH_LONG
                    ).show();

                    // ---- SEND MESSAGE TO THE FARMER'S NUMBER (via MessageHelper) ----
                    // Permission was already requested in onSaveClicked().
                    // If it was refused, MessageHelper opens the SMS app instead.
                    if (wantSms) {

                        MessageHelper.sendCollection(
                                MilkCollectionActivity.this,
                                farmerPhone,
                                farmerName,
                                date,
                                session,
                                quantity,
                                fat,
                                snf,
                                amount
                        );
                    }

                    clearFields();

                    // Return to dashboard
                    Intent intent = new Intent(
                            MilkCollectionActivity.this,
                            DashboardActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            MilkCollectionActivity.this,
                            "Failed to save milk/payment: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // CLEAR
    // =========================================================

    private void clearFields() {

        etFarmerId.setText("");
        etMilkQuantity.setText("");
        etFat.setText("");
        etSnf.setText("");
        etRate.setText("");

        tvFarmerName.setText("Farmer: Not loaded");
        tvFarmerVillage.setText("Village: --");
        tvFarmerStatus.setText("Enter Farmer ID and load farmer");
        tvResult.setText("Enter Farmer ID, load the farmer, then enter milk details.");

        radioSession.check(R.id.radioMorning);

        loadedFarmerName = null;
        loadedFarmerVillage = null;
        loadedFarmerPhone = null;
        calculatedAmount = -1;

        btnSave.setEnabled(false);

        etFarmerId.requestFocus();
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String getString(
            Map<String, Object> data,
            String key
    ) {

        Object value = data.get(key);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }
}