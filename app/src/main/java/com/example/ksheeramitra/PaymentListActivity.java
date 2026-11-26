package com.example.ksheeramitra;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PaymentListActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration paymentListener;

    private EditText etSearch;
    private TextView tvPaymentCount;
    private TextView tvEmpty;
    private LinearLayout paymentContainer;

    private final List<DocumentSnapshot> paymentList =
            new ArrayList<>();

    private final List<DocumentSnapshot> filteredList =
            new ArrayList<>();

    // =========================================================
    // COLORS
    // =========================================================

    private static final int GREEN = Color.rgb(46, 125, 50);
    private static final int DARK_GREEN = Color.rgb(27, 94, 32);
    private static final int LIGHT_GREEN = Color.rgb(232, 245, 233);

    private static final int TEXT_DARK = Color.rgb(38, 53, 45);
    private static final int TEXT_SECONDARY = Color.rgb(113, 128, 120);
    private static final int BORDER = Color.rgb(225, 233, 227);

    private static final int ORANGE = Color.rgb(245, 124, 0);
    private static final int RED = Color.rgb(211, 47, 47);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etSearch = findViewById(R.id.etSearch);
        tvPaymentCount = findViewById(R.id.tvPaymentCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        paymentContainer = findViewById(R.id.paymentContainer);

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        setupSearch();
        listenToPayments();
    }

    // =========================================================
    // FIRESTORE
    // =========================================================

    private void listenToPayments() {

        String uid = auth.getCurrentUser().getUid();

        tvEmpty.setVisibility(View.GONE);

        paymentListener = db.collection("payments")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {

                        Toast.makeText(
                                this,
                                getString(R.string.unable_load_payments_simple),
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    paymentList.clear();

                    if (snapshot != null) {

                        paymentList.addAll(
                                snapshot.getDocuments()
                        );
                    }

                    sortPayments();
                    filterPayments();
                });
    }

    // =========================================================
    // SORT
    // =========================================================

    private void sortPayments() {

        Collections.sort(
                paymentList,
                (a, b) -> {

                    long timeA =
                            getLong(a, "createdAt");

                    long timeB =
                            getLong(b, "createdAt");

                    return Long.compare(
                            timeB,
                            timeA
                    );
                }
        );
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        etSearch.addTextChangedListener(
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

                        filterPayments();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    private void filterPayments() {

        String search =
                etSearch.getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.getDefault());

        filteredList.clear();

        for (DocumentSnapshot document :
                paymentList) {

            String farmerName =
                    getString(document, "farmerName");

            String farmerId =
                    getString(document, "farmerId");

            String village =
                    getString(document, "village");

            String status =
                    getString(document, "status");

            String date =
                    getString(document, "date");

            String combined =
                    farmerName + " " +
                            farmerId + " " +
                            village + " " +
                            status + " " +
                            date;

            if (search.isEmpty() ||
                    combined
                            .toLowerCase(Locale.getDefault())
                            .contains(search)) {

                filteredList.add(document);
            }
        }

        tvPaymentCount.setText(
                getString(R.string.total_payment_records, filteredList.size())
        );

        displayPayments();
    }

    // =========================================================
    // DISPLAY
    // =========================================================

    private void displayPayments() {

        paymentContainer.removeAllViews();

        if (filteredList.isEmpty()) {

            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        for (DocumentSnapshot document :
                filteredList) {

            paymentContainer.addView(
                    createPaymentCard(document)
            );
        }
    }

    // =========================================================
    // MODERN PAYMENT CARD
    // =========================================================

    private View createPaymentCard(
            DocumentSnapshot document) {

        // -----------------------------------------------------
        // CARD
        // -----------------------------------------------------

        CardView card =
                new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                dp(6),
                0,
                dp(10)
        );

        card.setLayoutParams(cardParams);

        card.setRadius(dp(18));
        card.setCardElevation(dp(3));
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(Color.WHITE);

        // -----------------------------------------------------
        // MAIN CONTAINER
        // -----------------------------------------------------

        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL
        );

        main.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(14)
        );

        // -----------------------------------------------------
        // DATA
        // -----------------------------------------------------

        String farmerName =
                getString(
                        document,
                        "farmerName"
                );

        String farmerId =
                getString(
                        document,
                        "farmerId"
                );

        String village =
                getString(
                        document,
                        "village"
                );

        String collectionId =
                getString(
                        document,
                        "milkCollectionId"
                );

        String status =
                getString(
                        document,
                        "status"
                );

        String date =
                getString(
                        document,
                        "date"
                );

        String time =
                getString(
                        document,
                        "time"
                );

        double quantity =
                getDouble(
                        document,
                        "milkQuantity"
                );

        double rate =
                getDouble(
                        document,
                        "rate"
                );

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
                getDouble(
                        document,
                        "remainingAmount"
                );

        // =====================================================
        // HEADER
        // =====================================================

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        // -----------------------------------------------------
        // FARMER AVATAR
        // -----------------------------------------------------

        TextView avatar =
                new TextView(this);

        avatar.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(46),
                        dp(46)
                )
        );

        avatar.setGravity(
                Gravity.CENTER
        );

        avatar.setText(
                getInitials(farmerName)
        );

        avatar.setTextColor(
                GREEN
        );

        avatar.setTextSize(15);

        avatar.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        avatar.setBackground(
                roundedBackground(
                        LIGHT_GREEN,
                        23
                )
        );

        header.addView(avatar);

        // -----------------------------------------------------
        // FARMER INFORMATION
        // -----------------------------------------------------

        LinearLayout farmerInfo =
                new LinearLayout(this);

        farmerInfo.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams farmerParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        farmerParams.setMargins(
                dp(12),
                0,
                dp(8),
                0
        );

        farmerInfo.setLayoutParams(
                farmerParams
        );

        TextView name =
                createText(
                        farmerName.isEmpty()
                                ? getString(R.string.farmer_fallback)
                                : farmerName,
                        17,
                        TEXT_DARK,
                        true
                );

        farmerInfo.addView(name);

        TextView identity =
                createText(
                        getString(R.string.farmer_card_id, farmerId.isEmpty() ? "—" : farmerId),
                        11,
                        TEXT_SECONDARY,
                        false
                );

        LinearLayout.LayoutParams identityParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        identityParams.setMargins(
                0,
                dp(3),
                0,
                0
        );

        identity.setLayoutParams(
                identityParams
        );

        farmerInfo.addView(identity);

        header.addView(farmerInfo);

        // =====================================================
        // STATUS BADGE
        // =====================================================

        TextView statusBadge =
                createStatusBadge(status);

        header.addView(statusBadge);

        main.addView(header);

        // =====================================================
        // VILLAGE / COLLECTION
        // =====================================================

        LinearLayout locationRow =
                new LinearLayout(this);

        locationRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        locationRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams locationParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        locationParams.setMargins(
                0,
                dp(12),
                0,
                0
        );

        locationRow.setLayoutParams(
                locationParams
        );

        TextView villageView =
                createText(
                        "● " +
                                (village.isEmpty()
                                        ? getString(R.string.village_not_available)
                                        : village),
                        11,
                        TEXT_SECONDARY,
                        false
                );

        locationRow.addView(villageView);

        if (!collectionId.isEmpty()) {

            TextView collectionView =
                    createText(
                            "  •  " + getString(R.string.collection_id_label, collectionId),
                            11,
                            TEXT_SECONDARY,
                            false
                    );

            locationRow.addView(
                    collectionView
            );
        }

        main.addView(locationRow);

        // =====================================================
        // DIVIDER
        // =====================================================

        main.addView(
                createDivider()
        );

        // =====================================================
        // MILK INFO ROW
        // =====================================================

        LinearLayout milkRow =
                new LinearLayout(this);

        milkRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        milkRow.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams milkRowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        milkRowParams.setMargins(
                0,
                dp(2),
                0,
                dp(2)
        );

        milkRow.setLayoutParams(
                milkRowParams
        );

        // Quantity
        milkRow.addView(
                createInfoBox(
                        getString(R.string.milk_quantity_caps),
                        getString(R.string.quantity_litres, formatDouble(quantity))
                )
        );

        // Rate
        milkRow.addView(
                createInfoBox(
                        getString(R.string.rate_caps),
                        getString(R.string.rate_per_litre_value, formatDouble(rate))
                )
        );

        main.addView(milkRow);

        // =====================================================
        // TOTAL AMOUNT
        // =====================================================

        LinearLayout amountSection =
                new LinearLayout(this);

        amountSection.setOrientation(
                LinearLayout.HORIZONTAL
        );

        amountSection.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams amountParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        amountParams.setMargins(
                0,
                dp(10),
                0,
                0
        );

        amountSection.setLayoutParams(
                amountParams
        );

        LinearLayout amountText =
                new LinearLayout(this);

        amountText.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams amountTextParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        amountText.setLayoutParams(
                amountTextParams
        );

        TextView totalLabel =
                createText(
                        getString(R.string.total_payment_caps),
                        10,
                        TEXT_SECONDARY,
                        true
                );

        amountText.addView(totalLabel);

        TextView totalAmount =
                createText(
                        "₹" +
                                formatDouble(total),
                        23,
                        DARK_GREEN,
                        true
                );

        LinearLayout.LayoutParams totalAmountParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        totalAmountParams.setMargins(
                0,
                dp(3),
                0,
                0
        );

        totalAmount.setLayoutParams(
                totalAmountParams
        );

        amountText.addView(totalAmount);

        amountSection.addView(
                amountText
        );

        // -----------------------------------------------------
        // BALANCE
        // -----------------------------------------------------

        LinearLayout balanceBox =
                new LinearLayout(this);

        balanceBox.setOrientation(
                LinearLayout.VERTICAL
        );

        balanceBox.setGravity(
                Gravity.END
        );

        TextView balanceLabel =
                createText(
                        getString(R.string.balance_caps),
                        10,
                        TEXT_SECONDARY,
                        true
                );

        balanceBox.addView(
                balanceLabel
        );

        TextView balanceAmount =
                createText(
                        "₹" +
                                formatDouble(remaining),
                        17,
                        getBalanceColor(
                                status,
                                remaining
                        ),
                        true
                );

        LinearLayout.LayoutParams balanceAmountParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        balanceAmountParams.setMargins(
                0,
                dp(3),
                0,
                0
        );

        balanceAmount.setLayoutParams(
                balanceAmountParams
        );

        balanceBox.addView(
                balanceAmount
        );

        amountSection.addView(
                balanceBox
        );

        main.addView(
                amountSection
        );

        // =====================================================
        // PAYMENT PROGRESS
        // =====================================================

        double progress = 0;

        if (total > 0) {

            progress =
                    (paid / total) * 100;

            if (progress < 0) {
                progress = 0;
            }

            if (progress > 100) {
                progress = 100;
            }
        }

        TextView paymentProgress =
                createText(
                        getString(R.string.paid_percent, formatDouble(paid), (int) Math.round(progress)),
                        11,
                        TEXT_SECONDARY,
                        false
                );

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        progressParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        paymentProgress.setLayoutParams(
                progressParams
        );

        main.addView(
                paymentProgress
        );

        // =====================================================
        // DATE / TIME FOOTER
        // =====================================================

        LinearLayout footer =
                new LinearLayout(this);

        footer.setOrientation(
                LinearLayout.HORIZONTAL
        );

        footer.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        footerParams.setMargins(
                0,
                dp(12),
                0,
                0
        );

        footer.setLayoutParams(
                footerParams
        );

        TextView dateView =
                createText(
                        getString(R.string.date_label, date.isEmpty() ? "—" : date),
                        11,
                        TEXT_SECONDARY,
                        false
                );

        footer.addView(dateView);

        TextView timeView =
                createText(
                        getString(R.string.time_label, time.isEmpty() ? "—" : time),
                        11,
                        TEXT_SECONDARY,
                        false
                );

        LinearLayout.LayoutParams timeParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        timeParams.gravity =
                Gravity.END;

        timeView.setLayoutParams(
                timeParams
        );

        timeView.setGravity(
                Gravity.END
        );

        footer.addView(timeView);

        main.addView(footer);

        card.addView(main);

        return card;
    }

    // =========================================================
    // INFO BOX
    // =========================================================

    private View createInfoBox(
            String label,
            String value) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(
                Gravity.CENTER
        );

        box.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        box.setBackground(
                roundedBackground(
                        Color.rgb(247, 250, 248),
                        12
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        params.setMargins(
                dp(3),
                0,
                dp(3),
                0
        );

        box.setLayoutParams(params);

        TextView labelView =
                createText(
                        label,
                        9,
                        TEXT_SECONDARY,
                        true
                );

        labelView.setGravity(
                Gravity.CENTER
        );

        box.addView(labelView);

        TextView valueView =
                createText(
                        value,
                        14,
                        TEXT_DARK,
                        true
                );

        valueView.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams valueParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        valueParams.setMargins(
                0,
                dp(4),
                0,
                0
        );

        valueView.setLayoutParams(
                valueParams
        );

        box.addView(valueView);

        return box;
    }

    // =========================================================
    // STATUS BADGE
    // =========================================================

    private TextView createStatusBadge(
            String status) {

        TextView badge =
                createText(
                        localizeStatus(status).toUpperCase(Locale.getDefault()),
                        10,
                        getStatusTextColor(status),
                        true
                );

        badge.setGravity(
                Gravity.CENTER
        );

        badge.setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
        );

        badge.setBackground(
                roundedBackground(
                        getStatusBackgroundColor(status),
                        20
                )
        );

        return badge;
    }

    // =========================================================
    // DIVIDER
    // =========================================================

    private View createDivider() {

        View divider =
                new View(this);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        params.setMargins(
                0,
                dp(12),
                0,
                dp(10)
        );

        divider.setLayoutParams(params);

        divider.setBackgroundColor(
                BORDER
        );

        return divider;
    }

    // =========================================================
    // TEXT HELPER
    // =========================================================

    private TextView createText(
            String text,
            float size,
            int color,
            boolean bold) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setIncludeFontPadding(false);

        if (bold) {

            view.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        return view;
    }

    // =========================================================
    // BACKGROUND
    // =========================================================

    private GradientDrawable roundedBackground(
            int color,
            float radius) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        drawable.setCornerRadius(
                dp(radius)
        );

        return drawable;
    }

    // =========================================================
    // STATUS COLORS
    // =========================================================

    private String localizeStatus(String status) {

        if ("PAID".equalsIgnoreCase(status)) {
            return getString(R.string.status_paid);
        }

        if ("PARTIAL".equalsIgnoreCase(status)) {
            return getString(R.string.status_partial);
        }

        return getString(R.string.status_pending);
    }

    private int getStatusBackgroundColor(
            String status) {

        if ("PAID".equalsIgnoreCase(status)) {

            return Color.rgb(
                    232,
                    245,
                    233
            );
        }

        if ("PARTIAL".equalsIgnoreCase(status)) {

            return Color.rgb(
                    255,
                    243,
                    224
            );
        }

        return Color.rgb(
                255,
                235,
                238
        );
    }

    private int getStatusTextColor(
            String status) {

        if ("PAID".equalsIgnoreCase(status)) {

            return GREEN;
        }

        if ("PARTIAL".equalsIgnoreCase(status)) {

            return ORANGE;
        }

        return RED;
    }

    private int getBalanceColor(
            String status,
            double remaining) {

        if ("PAID".equalsIgnoreCase(status)
                || remaining <= 0) {

            return GREEN;
        }

        if ("PARTIAL".equalsIgnoreCase(status)) {

            return ORANGE;
        }

        return RED;
    }

    // =========================================================
    // INITIALS
    // =========================================================

    private String getInitials(
            String name) {

        if (name == null ||
                name.trim().isEmpty()) {

            return "F";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {

            return parts[0]
                    .substring(
                            0,
                            Math.min(
                                    1,
                                    parts[0].length()
                            )
                    )
                    .toUpperCase(
                            Locale.getDefault()
                    );
        }

        return (
                parts[0].substring(0, 1) +
                        parts[parts.length - 1]
                                .substring(0, 1)
        ).toUpperCase(
                Locale.getDefault()
        );
    }

    // =========================================================
    // FIRESTORE HELPERS
    // =========================================================

    private String getString(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private double getDouble(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }

        try {

            return Double.parseDouble(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            return 0.0;
        }
    }

    private long getLong(
            DocumentSnapshot document,
            String field) {

        Object value =
                document.get(field);

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        try {

            return Long.parseLong(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            return 0L;
        }
    }

    private String formatDouble(
            double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    // =========================================================
    // DP HELPER
    // =========================================================

    private int dp(float value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density +
                        0.5f
        );
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        if (paymentListener != null) {

            paymentListener.remove();
        }

        super.onDestroy();
    }
}