package com.example.ksheeramitra;

import android.Manifest;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentManagementActivity extends AppCompatActivity {

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // =========================================================
    // XML VIEWS
    // =========================================================

    private Spinner spinnerFarmer;

    private EditText etMilkQuantity;
    private EditText etFat;
    private EditText etSnf;
    private EditText etRate;
    private EditText etAmountPaid;

    private TextView tvTotalAmount;
    private TextView tvRemainingAmount;

    private RadioGroup rgPaymentStatus;
    private RadioButton rbPaid;
    private RadioButton rbPartial;
    private RadioButton rbPending;

    private Button btnSavePayment;

    private com.google.android.material.materialswitch.MaterialSwitch switchSendSms;

    // =========================================================
    // FARMERS
    // =========================================================

    private final List<DocumentSnapshot> farmerList =
            new ArrayList<>();

    private final List<String> farmerNames =
            new ArrayList<>();

    private ArrayAdapter<String> farmerAdapter;

    // =========================================================
    // PAYMENT DATA
    // =========================================================

    private double selectedFarmerTotal = 0.0;
    private double selectedFarmerPaid = 0.0;
    private double selectedFarmerPending = 0.0;

    private double enteredAmountPaid = 0.0;

    private boolean loadingFarmer = false;

    // =========================================================
    // SMS
    // =========================================================

    private ActivityResultLauncher<String> smsPermissionLauncher;
    private Runnable pendingSend = null;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_management);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Must be registered in onCreate
        smsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    Runnable send = pendingSend;
                    pendingSend = null;
                    if (send != null) {
                        // If permission was refused, MessageHelper opens the SMS app instead
                        send.run();
                    }
                }
        );

        initializeViews();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    getString(R.string.please_login_again),
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        setupFarmerSpinner();
        setupAmountPaidListener();
        setupButtons();

        loadFarmers();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        spinnerFarmer = findViewById(R.id.spinnerFarmer);

        etMilkQuantity = findViewById(R.id.etMilkQuantity);
        etFat = findViewById(R.id.etFat);
        etSnf = findViewById(R.id.etSnf);
        etRate = findViewById(R.id.etRate);
        etAmountPaid = findViewById(R.id.etAmountPaid);

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);

        rgPaymentStatus = findViewById(R.id.rgPaymentStatus);

        rbPaid = findViewById(R.id.rbPaid);
        rbPartial = findViewById(R.id.rbPartial);
        rbPending = findViewById(R.id.rbPending);

        btnSavePayment = findViewById(R.id.btnSavePayment);

        switchSendSms = findViewById(R.id.switchSendSms);
    }

    // =========================================================
    // GET CURRENT USER UID
    // =========================================================

    private String getCurrentUid() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return null;
        }

        return user.getUid();
    }

    // =========================================================
    // FARMER SPINNER
    // =========================================================

    private void setupFarmerSpinner() {

        farmerAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        farmerNames
                );

        farmerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerFarmer.setAdapter(farmerAdapter);

        spinnerFarmer.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (loadingFarmer) {
                            return;
                        }

                        if (position < 0 ||
                                position >= farmerList.size()) {
                            return;
                        }

                        loadFarmerPayment(
                                farmerList.get(position)
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    // =========================================================
    // LOAD FARMERS
    // =========================================================

    private void loadFarmers() {

        String uid = getCurrentUid();

        if (uid == null) {
            return;
        }

        loadingFarmer = true;

        db.collection("users")
                .document(uid)
                .collection("farmers")
                .get()
                .addOnSuccessListener(snapshot -> {

                    farmerList.clear();
                    farmerNames.clear();

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        farmerList.add(document);

                        String farmerId =
                                getString(
                                        document,
                                        "farmerId"
                                );

                        String farmerName =
                                getString(
                                        document,
                                        "name"
                                );

                        String displayName;

                        if (!farmerId.isEmpty()) {

                            displayName =
                                    farmerId +
                                            " - " +
                                            farmerName;

                        } else {

                            displayName =
                                    farmerName;
                        }

                        farmerNames.add(displayName);
                    }

                    farmerAdapter.notifyDataSetChanged();

                    loadingFarmer = false;

                    if (!farmerList.isEmpty()) {

                        spinnerFarmer.setSelection(0);

                        loadFarmerPayment(
                                farmerList.get(0)
                        );

                    } else {

                        clearPaymentDisplay();

                        Toast.makeText(
                                this,
                                getString(R.string.no_farmers_found),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {

                    loadingFarmer = false;

                    Toast.makeText(
                            this,
                            getString(R.string.unable_load_farmers_detail, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // LOAD FARMER PAYMENT
    // =========================================================

    private void loadFarmerPayment(
            DocumentSnapshot farmer) {

        String uid = getCurrentUid();

        if (uid == null) {
            return;
        }

        String farmerId =
                getString(
                        farmer,
                        "farmerId"
                );

        String farmerName =
                getString(
                        farmer,
                        "name"
                );

        selectedFarmerTotal = 0.0;
        selectedFarmerPaid = 0.0;
        selectedFarmerPending = 0.0;

        /*
         * IMPORTANT:
         *
         * The Firestore query MUST contain ownerUid.
         *
         * This allows the Firestore security rule to
         * verify that the current user owns these records.
         */

        db.collection("payments")
                .whereEqualTo(
                        "ownerUid",
                        uid
                )
                .whereEqualTo(
                        "farmerId",
                        farmerId
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    double total = 0.0;
                    double paid = 0.0;

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        double documentTotal =
                                getDouble(
                                        document,
                                        "totalAmount"
                                );

                        double documentPaid =
                                getDouble(
                                        document,
                                        "amountPaid"
                                );

                        if (documentTotal < 0) {
                            documentTotal = 0;
                        }

                        if (documentPaid < 0) {
                            documentPaid = 0;
                        }

                        if (documentPaid >
                                documentTotal) {

                            documentPaid =
                                    documentTotal;
                        }

                        total += documentTotal;
                        paid += documentPaid;
                    }

                    double pending =
                            Math.max(
                                    0.0,
                                    total - paid
                            );

                    selectedFarmerTotal = total;
                    selectedFarmerPaid = paid;
                    selectedFarmerPending = pending;

                    tvTotalAmount.setText(
                            getString(R.string.total_amount_value, formatMoney(total))
                    );

                    tvRemainingAmount.setText(
                            getString(R.string.remaining_balance_value, formatMoney(pending))
                    );

                    etAmountPaid.setText("");

                    loadLatestMilkDetails(
                            uid,
                            farmerId
                    );

                    updateStatus(
                            0.0,
                            pending
                    );
                })
                .addOnFailureListener(e -> {

                    tvTotalAmount.setText(
                            getString(R.string.total_amount_default)
                    );

                    tvRemainingAmount.setText(
                            getString(R.string.remaining_balance_default)
                    );

                    Toast.makeText(
                            this,
                            getString(R.string.unable_load_payment_for, farmerName, getFirebaseErrorMessage(e)),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // LOAD LATEST MILK DETAILS
    // =========================================================

    private void loadLatestMilkDetails(
            String uid,
            String farmerId) {

        db.collection("milkCollections")
                .whereEqualTo(
                        "ownerUid",
                        uid
                )
                .whereEqualTo(
                        "farmerId",
                        farmerId
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot == null ||
                            snapshot.isEmpty()) {

                        etMilkQuantity.setText("");
                        etFat.setText("");
                        etSnf.setText("");
                        etRate.setText("");

                        return;
                    }

                    DocumentSnapshot latest = null;

                    long latestTime = 0L;

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        long createdAt =
                                getLong(
                                        document,
                                        "createdAt"
                                );

                        if (latest == null ||
                                createdAt > latestTime) {

                            latest = document;
                            latestTime = createdAt;
                        }
                    }

                    if (latest == null) {
                        return;
                    }

                    etMilkQuantity.setText(
                            formatNumber(
                                    getDouble(
                                            latest,
                                            "quantity"
                                    )
                            )
                    );

                    etFat.setText(
                            formatNumber(
                                    getDouble(
                                            latest,
                                            "fat"
                                    )
                            )
                    );

                    etSnf.setText(
                            formatNumber(
                                    getDouble(
                                            latest,
                                            "snf"
                                    )
                            )
                    );

                    etRate.setText(
                            formatNumber(
                                    getDouble(
                                            latest,
                                            "rate"
                                    )
                            )
                    );
                })
                .addOnFailureListener(e -> {

                    /*
                     * Milk details are informational.
                     * Do not block payment if this fails.
                     */

                    etMilkQuantity.setText("");
                    etFat.setText("");
                    etSnf.setText("");
                    etRate.setText("");
                });
    }

    // =========================================================
    // AMOUNT PAID LISTENER
    // =========================================================

    private void setupAmountPaidListener() {

        etAmountPaid.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        calculateEnteredPayment();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    // =========================================================
    // CALCULATE ENTERED PAYMENT
    // =========================================================

    private void calculateEnteredPayment() {

        String value =
                etAmountPaid.getText()
                        .toString()
                        .trim();

        if (value.isEmpty()) {

            enteredAmountPaid = 0.0;

            tvRemainingAmount.setText(
                    getString(R.string.remaining_balance_value, formatMoney(selectedFarmerPending))
            );

            updateStatus(
                    0.0,
                    selectedFarmerPending
            );

            return;
        }

        double amount;

        try {

            amount =
                    Double.parseDouble(value);

        } catch (Exception e) {

            amount = 0.0;
        }

        enteredAmountPaid =
                Math.max(
                        0.0,
                        amount
                );

        double newRemaining =
                Math.max(
                        0.0,
                        selectedFarmerPending -
                                enteredAmountPaid
                );

        tvRemainingAmount.setText(
                getString(R.string.remaining_balance_value, formatMoney(newRemaining))
        );

        updateStatus(
                enteredAmountPaid,
                newRemaining
        );
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    private void updateStatus(
            double paidNow,
            double remaining) {

        if (selectedFarmerPending <= 0.01) {

            rbPaid.setChecked(true);

            return;
        }

        if (paidNow >=
                selectedFarmerPending -
                        0.01) {

            rbPaid.setChecked(true);

        } else if (paidNow > 0.01) {

            rbPartial.setChecked(true);

        } else {

            rbPending.setChecked(true);
        }
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        btnSavePayment.setOnClickListener(
                v -> savePayment()
        );
    }

    // =========================================================
    // SAVE PAYMENT
    // =========================================================

    private void savePayment() {

        String uid = getCurrentUid();

        if (uid == null) {

            Toast.makeText(
                    this,
                    getString(R.string.please_login_again),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (farmerList.isEmpty()) {

            Toast.makeText(
                    this,
                    getString(R.string.no_farmer_selected),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int position =
                spinnerFarmer.getSelectedItemPosition();

        if (position < 0 ||
                position >= farmerList.size()) {

            Toast.makeText(
                    this,
                    getString(R.string.please_select_farmer),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String amountText =
                etAmountPaid.getText()
                        .toString()
                        .trim();

        if (amountText.isEmpty()) {

            Toast.makeText(
                    this,
                    getString(R.string.enter_amount_paid),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double amountPaid;

        try {

            amountPaid =
                    Double.parseDouble(
                            amountText
                    );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    getString(R.string.enter_valid_payment),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (amountPaid <= 0) {

            Toast.makeText(
                    this,
                    getString(R.string.payment_must_be_positive),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (amountPaid >
                selectedFarmerPending +
                        0.01) {

            Toast.makeText(
                    this,
                    getString(R.string.payment_exceeds_pending),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        DocumentSnapshot farmer =
                farmerList.get(position);

        String farmerId =
                getString(
                        farmer,
                        "farmerId"
                );

        String farmerName =
                getString(
                        farmer,
                        "name"
                );

        // ---- values needed for the SMS (captured before the screen reloads) ----
        final boolean wantSms = switchSendSms.isChecked();
        final String smsPhone = getString(farmer, "phone");
        final double smsTotal = selectedFarmerTotal;
        final double smsPaidTotal = selectedFarmerPaid + amountPaid;
        final double smsBalance =
                Math.max(0.0, selectedFarmerPending - amountPaid);

        btnSavePayment.setEnabled(false);

        /*
         * Get pending payment obligations.
         *
         * ownerUid is REQUIRED in the query.
         */

        db.collection("payments")
                .whereEqualTo(
                        "ownerUid",
                        uid
                )
                .whereEqualTo(
                        "farmerId",
                        farmerId
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<DocumentSnapshot>
                            pendingPayments =
                            new ArrayList<>();

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        double total =
                                getDouble(
                                        document,
                                        "totalAmount"
                                );

                        double paid =
                                getDouble(
                                        document,
                                        "amountPaid"
                                );

                        double remaining =
                                Math.max(
                                        0.0,
                                        total - paid
                                );

                        if (remaining > 0.01) {

                            pendingPayments.add(
                                    document
                            );
                        }
                    }

                    /*
                     * Oldest payment first.
                     * Collections.sort works on all API levels
                     * (List.sort needs API 24+).
                     */

                    Collections.sort(
                            pendingPayments,
                            (a, b) ->
                                    Long.compare(
                                            getLong(
                                                    a,
                                                    "createdAt"
                                            ),
                                            getLong(
                                                    b,
                                                    "createdAt"
                                            )
                                    )
                    );

                    if (pendingPayments.isEmpty()) {

                        btnSavePayment.setEnabled(true);

                        Toast.makeText(
                                this,
                                getString(R.string.no_pending_payment),
                                Toast.LENGTH_LONG
                        ).show();

                        loadFarmerPayment(
                                farmer
                        );

                        return;
                    }

                    WriteBatch batch =
                            db.batch();

                    double amountLeft =
                            amountPaid;

                    Date nowDate =
                            new Date();

                    String date =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.US
                            ).format(nowDate);

                    String time =
                            new SimpleDateFormat(
                                    "HH:mm:ss",
                                    Locale.US
                            ).format(nowDate);

                    long now =
                            System.currentTimeMillis();

                    for (DocumentSnapshot document :
                            pendingPayments) {

                        if (amountLeft <= 0.01) {
                            break;
                        }

                        double total =
                                getDouble(
                                        document,
                                        "totalAmount"
                                );

                        double oldPaid =
                                getDouble(
                                        document,
                                        "amountPaid"
                                );

                        double oldRemaining =
                                Math.max(
                                        0.0,
                                        total - oldPaid
                                );

                        if (oldRemaining <= 0.01) {
                            continue;
                        }

                        double applied =
                                Math.min(
                                        amountLeft,
                                        oldRemaining
                                );

                        double newPaid =
                                oldPaid + applied;

                        double newRemaining =
                                Math.max(
                                        0.0,
                                        total - newPaid
                                );

                        String status;

                        if (newRemaining <= 0.01) {

                            status = "PAID";

                        } else if (newPaid > 0.01) {

                            status = "PARTIAL";

                        } else {

                            status = "PENDING";
                        }

                        /*
                         * UPDATE EXISTING PAYMENT
                         */

                        Map<String, Object>
                                updates =
                                new HashMap<>();

                        updates.put(
                                "ownerUid",
                                uid
                        );

                        updates.put(
                                "amountPaid",
                                newPaid
                        );

                        updates.put(
                                "remainingAmount",
                                newRemaining
                        );

                        updates.put(
                                "status",
                                status
                        );

                        updates.put(
                                "lastPaymentDate",
                                date
                        );

                        updates.put(
                                "lastPaymentTime",
                                time
                        );

                        updates.put(
                                "updatedAt",
                                now
                        );

                        batch.update(
                                document.getReference(),
                                updates
                        );

                        /*
                         * PAYMENT TRANSACTION HISTORY
                         */

                        String transactionId =
                                db.collection(
                                                "paymentTransactions"
                                        )
                                        .document()
                                        .getId();

                        Map<String, Object>
                                transaction =
                                new HashMap<>();

                        transaction.put(
                                "transactionId",
                                transactionId
                        );

                        transaction.put(
                                "paymentId",
                                getString(
                                        document,
                                        "paymentId"
                                )
                        );

                        transaction.put(
                                "milkCollectionId",
                                getString(
                                        document,
                                        "milkCollectionId"
                                )
                        );

                        transaction.put(
                                "ownerUid",
                                uid
                        );

                        transaction.put(
                                "farmerId",
                                farmerId
                        );

                        transaction.put(
                                "farmerName",
                                farmerName
                        );

                        transaction.put(
                                "village",
                                getString(
                                        farmer,
                                        "village"
                                )
                        );

                        transaction.put(
                                "amount",
                                applied
                        );

                        transaction.put(
                                "date",
                                date
                        );

                        transaction.put(
                                "time",
                                time
                        );

                        transaction.put(
                                "createdAt",
                                now
                        );

                        batch.set(
                                db.collection(
                                                "paymentTransactions"
                                        )
                                        .document(
                                                transactionId
                                        ),
                                transaction
                        );

                        amountLeft -= applied;
                    }

                    /*
                     * COMMIT
                     */

                    batch.commit()
                            .addOnSuccessListener(
                                    unused -> {

                                        btnSavePayment
                                                .setEnabled(true);

                                        Toast.makeText(
                                                this,
                                                getString(R.string.payment_saved_for, farmerName),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        // ---- SEND SMS TO THE FARMER'S NUMBER ----
                                        if (wantSms) {

                                            sendPaymentMessage(
                                                    smsPhone,
                                                    farmerName,
                                                    date,
                                                    smsTotal,
                                                    smsPaidTotal,
                                                    smsBalance
                                            );
                                        }

                                        loadFarmerPayment(
                                                farmer
                                        );
                                    }
                            )
                            .addOnFailureListener(
                                    e -> {

                                        btnSavePayment
                                                .setEnabled(true);

                                        Toast.makeText(
                                                this,
                                                getString(R.string.payment_failed, getFirebaseErrorMessage(e)),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                            );
                })
                .addOnFailureListener(e -> {

                    btnSavePayment.setEnabled(true);

                    Toast.makeText(
                            this,
                            getString(R.string.unable_load_payments_detail, getFirebaseErrorMessage(e)),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // SMS
    // =========================================================

    /**
     * Sends the payment message through MessageHelper.
     * Asks for SMS permission first when the SMS channel is selected.
     */
    private void sendPaymentMessage(
            String phone,
            String farmerName,
            String date,
            double total,
            double paid,
            double balance) {

        Runnable send = () -> MessageHelper.sendPayment(
                this,
                phone,
                farmerName,
                date,
                total,
                paid,
                balance
        );

        if (MessageHelper.needsSmsPermission(this)) {

            pendingSend = send;

            smsPermissionLauncher.launch(
                    Manifest.permission.SEND_SMS
            );

        } else {

            send.run();
        }
    }

    // =========================================================
    // GET STRING
    // =========================================================

    private String getString(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    // =========================================================
    // GET DOUBLE
    // =========================================================

    private double getDouble(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }

        if (value == null) {
            return 0.0;
        }

        try {

            return Double.parseDouble(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            return 0.0;
        }
    }

    // =========================================================
    // GET LONG
    // =========================================================

    private long getLong(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        if (value == null) {
            return 0L;
        }

        try {

            return Long.parseLong(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            return 0L;
        }
    }

    // =========================================================
    // FORMAT MONEY
    // =========================================================

    private String formatMoney(
            double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    // =========================================================
    // FORMAT NUMBER
    // =========================================================

    private String formatNumber(
            double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    // =========================================================
    // FIREBASE ERROR
    // =========================================================

    private String getFirebaseErrorMessage(
            Exception e) {

        String message = e.getMessage();

        if (message == null) {
            return getString(R.string.firebase_unknown_error);
        }

        if (message.contains("PERMISSION_DENIED")) {

            return getString(R.string.firebase_permission_denied);
        }

        return message;
    }

    // =========================================================
    // CLEAR DISPLAY
    // =========================================================

    private void clearPaymentDisplay() {

        selectedFarmerTotal = 0.0;
        selectedFarmerPaid = 0.0;
        selectedFarmerPending = 0.0;

        tvTotalAmount.setText(
                getString(R.string.total_amount_default)
        );

        tvRemainingAmount.setText(
                getString(R.string.remaining_balance_default)
        );

        etMilkQuantity.setText("");
        etFat.setText("");
        etSnf.setText("");
        etRate.setText("");
        etAmountPaid.setText("");

        rbPending.setChecked(true);
    }
}