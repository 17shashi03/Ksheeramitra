package com.example.ksheeramitra;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QualityTestingActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etFarmerId;
    private EditText etMilkQuantity;
    private EditText etFat;
    private EditText etSnf;
    private EditText etTemperature;

    private RadioGroup radioMilkType;
    private RadioButton rbCow;
    private RadioButton rbBuffalo;

    private Button btnLoadFarmer;
    private Button btnCheckQuality;
    private Button btnSaveQuality;
    private Button btnClear;

    private TextView tvFarmerName;
    private TextView tvFarmerVillage;
    private TextView tvFarmerStatus;
    private TextView tvQualityResult;
    private TextView tvQualityMessage;

    private com.google.android.material.materialswitch.MaterialSwitch switchSendSms;

    private String loadedFarmerId = "";
    private String loadedFarmerName = "";
    private String loadedFarmerVillage = "";
    private String loadedFarmerPhone = "";

    private String calculatedQualityStatus = "";

    // ---- SMS ----
    private ActivityResultLauncher<String> smsPermissionLauncher;
    private String pendingSmsPhone = null;
    private String pendingSmsMessage = null;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_quality_testing
        );

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Must be registered in onCreate
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (pendingSmsPhone == null || pendingSmsMessage == null) {
                        return;
                    }
                    if (granted) {
                        deliverSms(pendingSmsPhone, pendingSmsMessage);
                    } else {
                        Toast.makeText(
                                this,
                                R.string.sms_permission_denied,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    pendingSmsPhone = null;
                    pendingSmsMessage = null;
                }
        );

        initializeViews();
        setupButtons();

        btnCheckQuality.setEnabled(false);
        btnSaveQuality.setEnabled(false);

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_LONG
            ).show();

            finish();
        }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeViews() {

        etFarmerId =
                findViewById(R.id.etFarmerId);

        etMilkQuantity =
                findViewById(R.id.etMilkQuantity);

        etFat =
                findViewById(R.id.etFat);

        etSnf =
                findViewById(R.id.etSnf);

        etTemperature =
                findViewById(R.id.etTemperature);

        radioMilkType =
                findViewById(R.id.radioMilkType);

        rbCow =
                findViewById(R.id.rbCow);

        rbBuffalo =
                findViewById(R.id.rbBuffalo);

        btnLoadFarmer =
                findViewById(R.id.btnLoadFarmer);

        btnCheckQuality =
                findViewById(R.id.btnCheckQuality);

        btnSaveQuality =
                findViewById(R.id.btnSaveQuality);

        btnClear =
                findViewById(R.id.btnClear);

        tvFarmerName =
                findViewById(R.id.tvFarmerName);

        tvFarmerVillage =
                findViewById(R.id.tvFarmerVillage);

        tvFarmerStatus =
                findViewById(R.id.tvFarmerStatus);

        tvQualityResult =
                findViewById(R.id.tvQualityResult);

        tvQualityMessage =
                findViewById(R.id.tvQualityMessage);

        switchSendSms =
                findViewById(R.id.switchSendSms);
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        btnLoadFarmer.setOnClickListener(
                v -> loadFarmer()
        );

        btnCheckQuality.setOnClickListener(
                v -> checkQuality()
        );

        btnSaveQuality.setOnClickListener(
                v -> saveQuality()
        );

        btnClear.setOnClickListener(
                v -> clearForm()
        );
    }

    // =========================================================
    // LOAD FARMER
    // =========================================================

    private void loadFarmer() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String farmerId =
                etFarmerId
                        .getText()
                        .toString()
                        .trim()
                        .toUpperCase(
                                Locale.getDefault()
                        );

        if (!farmerId.matches(
                "KM-\\d{6}"
        )) {

            etFarmerId.setError(
                    "Example: KM-000001"
            );

            return;
        }

        String uid =
                user.getUid();

        btnLoadFarmer.setEnabled(false);

        /*
         * Same farmer location used by
         * FarmerActivity:
         *
         * users/{uid}/farmers/{farmerId}
         */

        db.collection("users")
                .document(uid)
                .collection("farmers")
                .document(farmerId)
                .get()
                .addOnSuccessListener(document -> {

                    btnLoadFarmer.setEnabled(true);

                    if (!document.exists()) {

                        clearLoadedFarmer();

                        Toast.makeText(
                                this,
                                "Farmer not found",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    loadedFarmerId =
                            farmerId;

                    loadedFarmerName =
                            getString(
                                    document.getData(),
                                    "name"
                            );

                    loadedFarmerVillage =
                            getString(
                                    document.getData(),
                                    "village"
                            );

                    loadedFarmerPhone =
                            getString(
                                    document.getData(),
                                    "phone"
                            );

                    String status =
                            getString(
                                    document.getData(),
                                    "status"
                            );

                    tvFarmerName.setText(
                            "Name: " +
                                    loadedFarmerName
                    );

                    tvFarmerVillage.setText(
                            "Village: " +
                                    loadedFarmerVillage
                    );

                    tvFarmerStatus.setText(
                            "Status: " +
                                    status
                    );

                    btnCheckQuality.setEnabled(
                            true
                    );

                    Toast.makeText(
                            this,
                            "Farmer loaded successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    btnLoadFarmer.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Unable to load farmer:\n" +
                                    getFirebaseErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // CHECK QUALITY
    // =========================================================

    private void checkQuality() {

        if (loadedFarmerId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Load farmer first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Double quantity =
                getDouble(etMilkQuantity);

        Double fat =
                getDouble(etFat);

        Double snf =
                getDouble(etSnf);

        Double temperature =
                getDouble(etTemperature);

        if (quantity == null ||
                quantity <= 0) {

            etMilkQuantity.setError(
                    "Enter valid milk quantity"
            );

            return;
        }

        if (fat == null ||
                fat < 0 ||
                fat > 15) {

            etFat.setError(
                    "Enter fat between 0 and 15"
            );

            return;
        }

        if (snf == null ||
                snf < 0 ||
                snf > 15) {

            etSnf.setError(
                    "Enter SNF between 0 and 15"
            );

            return;
        }

        if (temperature == null ||
                temperature < 0 ||
                temperature > 60) {

            etTemperature.setError(
                    "Enter temperature between 0 and 60"
            );

            return;
        }

        String milkType =
                getMilkType();

        if (milkType.isEmpty()) {

            Toast.makeText(
                    this,
                    "Select Cow or Buffalo",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * QUALITY CLASSIFICATION
         */

        if (fat >= 4.0 &&
                snf >= 8.5 &&
                temperature <= 35.0) {

            calculatedQualityStatus =
                    "GOOD";

        } else if (fat >= 3.0 &&
                snf >= 8.0 &&
                temperature <= 38.0) {

            calculatedQualityStatus =
                    "AVERAGE";

        } else {

            calculatedQualityStatus =
                    "POOR";
        }

        String message;

        if (calculatedQualityStatus.equals(
                "GOOD")) {

            message =
                    "Milk quality is good.";

        } else if (calculatedQualityStatus.equals(
                "AVERAGE")) {

            message =
                    "Milk quality is acceptable but can be improved.";

        } else {

            message =
                    "Milk quality is below the required range.";
        }

        tvQualityResult.setText(
                "QUALITY: " +
                        calculatedQualityStatus
        );

        tvQualityMessage.setText(
                "Farmer: " +
                        loadedFarmerName +
                        "\nMilk Type: " +
                        milkType +
                        "\nQuantity: " +
                        String.format(
                                Locale.US,
                                "%.2f L",
                                quantity
                        ) +
                        "\nFat: " +
                        String.format(
                                Locale.US,
                                "%.2f",
                                fat
                        ) +
                        "\nSNF: " +
                        String.format(
                                Locale.US,
                                "%.2f",
                                snf
                        ) +
                        "\nTemperature: " +
                        String.format(
                                Locale.US,
                                "%.2f °C",
                                temperature
                        ) +
                        "\n\n" +
                        message
        );

        btnSaveQuality.setEnabled(true);
    }

    // =========================================================
    // SAVE QUALITY
    // =========================================================

    private void saveQuality() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (loadedFarmerId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Load farmer first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (calculatedQualityStatus.isEmpty()) {

            Toast.makeText(
                    this,
                    "Check quality first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Double quantity =
                getDouble(etMilkQuantity);

        Double fat =
                getDouble(etFat);

        Double snf =
                getDouble(etSnf);

        Double temperature =
                getDouble(etTemperature);

        if (quantity == null ||
                quantity <= 0 ||
                fat == null ||
                snf == null ||
                temperature == null) {

            Toast.makeText(
                    this,
                    "Enter valid quality values",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String milkType =
                getMilkType();

        if (milkType.isEmpty()) {

            Toast.makeText(
                    this,
                    "Select Cow or Buffalo",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid =
                user.getUid();

        Date currentDate =
                new Date();

        String date =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                ).format(currentDate);

        String time =
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.US
                ).format(currentDate);

        long createdAt =
                System.currentTimeMillis();

        /*
         * Generate unique document ID.
         */

        String qualityTestId =
                db.collection(
                                "milkQualityTests"
                        )
                        .document()
                        .getId();

        Map<String, Object>
                qualityData =
                new HashMap<>();

        /*
         * IMPORTANT:
         *
         * ownerUid MUST be stored.
         *
         * Firestore rules use this field
         * to verify document ownership.
         */

        qualityData.put(
                "qualityTestId",
                qualityTestId
        );

        qualityData.put(
                "ownerUid",
                uid
        );

        qualityData.put(
                "farmerId",
                loadedFarmerId
        );

        qualityData.put(
                "farmerName",
                loadedFarmerName
        );

        qualityData.put(
                "village",
                loadedFarmerVillage
        );

        qualityData.put(
                "milkQuantity",
                quantity
        );

        qualityData.put(
                "quantity",
                quantity
        );

        qualityData.put(
                "milkType",
                milkType
        );

        qualityData.put(
                "fat",
                fat
        );

        qualityData.put(
                "snf",
                snf
        );

        qualityData.put(
                "temperature",
                temperature
        );

        qualityData.put(
                "qualityStatus",
                calculatedQualityStatus
        );

        qualityData.put(
                "date",
                date
        );

        qualityData.put(
                "time",
                time
        );

        qualityData.put(
                "createdAt",
                createdAt
        );

        // ---- SMS text (prepared now, because the form is cleared after saving) ----
        final boolean wantSms = switchSendSms.isChecked();
        final String smsPhone = loadedFarmerPhone;
        final String smsText = getString(
                R.string.msg_quality,
                loadedFarmerName,
                date,
                milkType,
                String.format(Locale.US, "%.2f", fat),
                String.format(Locale.US, "%.2f", snf),
                String.format(Locale.US, "%.1f", temperature),
                calculatedQualityStatus,
                getString(R.string.app_name)
        );

        btnSaveQuality.setEnabled(false);

        db.collection(
                        "milkQualityTests"
                )
                .document(qualityTestId)
                .set(qualityData)
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "Quality test saved successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // ---- SEND SMS TO THE FARMER'S NUMBER ----
                            if (wantSms) {

                                if (TextUtils.isEmpty(smsPhone)) {

                                    Toast.makeText(
                                            this,
                                            R.string.sms_no_phone,
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {

                                    sendSmsToFarmer(
                                            smsPhone,
                                            smsText
                                    );
                                }
                            }

                            clearForm();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            btnSaveQuality
                                    .setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Save failed:\n" +
                                            getFirebaseErrorMessage(e),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // =========================================================
    // SMS
    // =========================================================

    /** Sends the SMS if permission is granted, otherwise asks for it first. */
    private void sendSmsToFarmer(String phone, String message) {

        if (SmsHelper.hasPermission(this)) {

            deliverSms(phone, message);

        } else {

            pendingSmsPhone = phone;
            pendingSmsMessage = message;

            smsPermissionLauncher.launch(
                    Manifest.permission.SEND_SMS
            );
        }
    }

    private void deliverSms(String phone, String message) {

        boolean sent = SmsHelper.send(this, phone, message);

        Toast.makeText(
                this,
                sent ? R.string.sms_sent : R.string.sms_failed,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // MILK TYPE
    // =========================================================

    private String getMilkType() {

        int selectedId =
                radioMilkType
                        .getCheckedRadioButtonId();

        if (selectedId == R.id.rbCow) {
            return "Cow";
        }

        if (selectedId == R.id.rbBuffalo) {
            return "Buffalo";
        }

        return "";
    }

    // =========================================================
    // GET DOUBLE
    // =========================================================

    private Double getDouble(
            EditText editText) {

        String value =
                editText
                        .getText()
                        .toString()
                        .trim();

        if (value.isEmpty()) {
            return null;
        }

        try {

            return Double.parseDouble(
                    value
            );

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // GET STRING
    // =========================================================

    private String getString(
            Map<String, Object> data,
            String key) {

        if (data == null) {
            return "";
        }

        Object value =
                data.get(key);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    // =========================================================
    // FIREBASE ERROR
    // =========================================================

    private String getFirebaseErrorMessage(
            Exception e) {

        String message =
                e.getMessage();

        if (message == null) {
            return "Unknown Firebase error";
        }

        if (message.contains(
                "PERMISSION_DENIED")) {

            return "Permission denied.\n\n" +
                    "Check Firestore Rules and make sure " +
                    "you are logged in.";
        }

        return message;
    }

    // =========================================================
    // CLEAR FARMER
    // =========================================================

    private void clearLoadedFarmer() {

        loadedFarmerId = "";
        loadedFarmerName = "";
        loadedFarmerVillage = "";
        loadedFarmerPhone = "";

        tvFarmerName.setText(
                "Name: -"
        );

        tvFarmerVillage.setText(
                "Village: -"
        );

        tvFarmerStatus.setText(
                "Status: -"
        );

        btnCheckQuality.setEnabled(false);
        btnSaveQuality.setEnabled(false);
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearForm() {

        etFarmerId.setText("");
        etMilkQuantity.setText("");
        etFat.setText("");
        etSnf.setText("");
        etTemperature.setText("");

        radioMilkType.clearCheck();

        tvQualityResult.setText("");
        tvQualityMessage.setText("");

        calculatedQualityStatus = "";

        clearLoadedFarmer();
    }
}