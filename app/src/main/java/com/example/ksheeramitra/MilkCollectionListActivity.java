package com.example.ksheeramitra;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MilkCollectionListActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration milkListener;

    private EditText etSearch;
    private TextView tvCollectionCount;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private LinearLayout collectionContainer;

    private final List<DocumentSnapshot> allCollections =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milk_collection_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        String missing = missingViews();

        if (!missing.isEmpty()) {
            android.widget.Toast.makeText(
                    this,
                    "Layout is missing: " + missing,
                    android.widget.Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        setupSearch();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        loadMilkCollections();
    }

    private void initializeViews() {

        etSearch =
                findViewById(R.id.etSearch);

        tvCollectionCount =
                findViewById(R.id.tvCollectionCount);

        progressBar =
                findViewById(R.id.progressBar);

        tvEmpty =
                findViewById(R.id.tvEmpty);

        collectionContainer =
                findViewById(R.id.collectionContainer);
    }

    private String missingViews() {

        StringBuilder sb = new StringBuilder();

        if (etSearch == null) sb.append("etSearch ");
        if (tvCollectionCount == null) sb.append("tvCollectionCount ");
        if (tvEmpty == null) sb.append("tvEmpty ");
        if (collectionContainer == null) sb.append("collectionContainer ");

        return sb.toString().trim();
    }

    private void setLoading(boolean loading) {

        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

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

                        displayCollections(
                                s.toString().trim()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    private void loadMilkCollections() {

        String uid =
                auth.getCurrentUser().getUid();

        setLoading(true);

        milkListener =
                db.collection("milkCollections")
                        .whereEqualTo(
                                "ownerUid",
                                uid
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    setLoading(false);

                                    if (error != null) {

                                        tvCollectionCount.setText(
                                                getString(R.string.collections_count_default)
                                        );

                                        showEmpty(
                                                getString(R.string.unable_load_milk_collections)
                                        );

                                        return;
                                    }

                                    allCollections.clear();

                                    if (snapshot != null) {

                                        allCollections.addAll(
                                                snapshot.getDocuments()
                                        );
                                    }

                                    Collections.sort(
                                            allCollections,
                                            new Comparator<DocumentSnapshot>() {

                                                @Override
                                                public int compare(
                                                        DocumentSnapshot d1,
                                                        DocumentSnapshot d2) {

                                                    long time1 =
                                                            getLong(
                                                                    d1,
                                                                    "createdAt"
                                                            );

                                                    long time2 =
                                                            getLong(
                                                                    d2,
                                                                    "createdAt"
                                                            );

                                                    return Long.compare(
                                                            time2,
                                                            time1
                                                    );
                                                }
                                            }
                                    );

                                    displayCollections(
                                            etSearch.getText()
                                                    .toString()
                                                    .trim()
                                    );
                                }
                        );
    }

    private void displayCollections(
            String searchText) {

        collectionContainer.removeAllViews();

        String query =
                searchText.toLowerCase(
                        Locale.US
                );

        int displayedCount = 0;

        for (
                DocumentSnapshot document :
                allCollections
        ) {

            String farmerId =
                    getString(
                            document,
                            "farmerId"
                    );

            String farmerName =
                    getString(
                            document,
                            "farmerName"
                    );

            String village =
                    getString(
                            document,
                            "village"
                    );

            String session =
                    getString(
                            document,
                            "session"
                    );

            String date =
                    getString(
                            document,
                            "date"
                    );

            String collectionId =
                    getString(
                            document,
                            "milkCollectionId"
                    );

            String searchableText =
                    (
                            farmerId + " " +
                                    farmerName + " " +
                                    village + " " +
                                    session + " " +
                                    date + " " +
                                    collectionId
                    ).toLowerCase(
                            Locale.US
                    );

            if (!query.isEmpty() &&
                    !searchableText.contains(query)) {

                continue;
            }

            addCollectionCard(
                    document
            );

            displayedCount++;
        }

        tvCollectionCount.setText(
                getString(displayedCount == 1 ? R.string.collection_count_one : R.string.collection_count_many, displayedCount)
        );

        if (displayedCount == 0) {

            showEmpty(
                    getString(R.string.no_milk_collections)
            );

        } else {

            tvEmpty.setVisibility(
                    View.GONE
            );
        }
    }

    private void addCollectionCard(
            DocumentSnapshot document) {

        CardView card =
                new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                14
        );

        card.setLayoutParams(
                cardParams
        );

        card.setRadius(18);
        card.setCardElevation(3);

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                18,
                16,
                18,
                16
        );

        /*
         * Farmer name
         */

        TextView farmerName =
                createText(
                        getString(
                                document,
                                "farmerName"
                        ),
                        18,
                        0xFF263238,
                        true
                );

        layout.addView(
                farmerName
        );

        /*
         * Farmer ID + village
         */

        String farmerInfo =
                getString(R.string.milk_card_id_village, getString(document, "farmerId"), getString(document, "village"));

        layout.addView(
                createText(
                        farmerInfo,
                        13,
                        0xFF78909C,
                        false
                )
        );

        /*
         * Collection ID
         */

        String collectionId =
                getString(R.string.collection_id_full, getString(document, "milkCollectionId"));

        layout.addView(
                createText(
                        collectionId,
                        13,
                        0xFF78909C,
                        false
                )
        );

        /*
         * Milk quantity
         */

        double quantity =
                getDouble(
                        document,
                        "quantity"
                );

        String quantityText =
                getString(R.string.milk_quantity_line, String.format(Locale.US, "%.2f", quantity));

        layout.addView(
                createText(
                        quantityText,
                        17,
                        0xFF1976D2,
                        true
                )
        );

        /*
         * Fat + SNF
         */

        double fat =
                getDouble(
                        document,
                        "fat"
                );

        double snf =
                getDouble(
                        document,
                        "snf"
                );

        String qualityText =
                getString(R.string.fat_snf_label, String.format(Locale.US, "%.2f", fat), String.format(Locale.US, "%.2f", snf));

        layout.addView(
                createText(
                        qualityText,
                        14,
                        0xFF455A64,
                        false
                )
        );

        /*
         * Session + date + time
         */

        String session =
                getString(
                        document,
                        "session"
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

        String dateText =
                getString(R.string.session_date_line, localizeSession(session), date, time);

        layout.addView(
                createText(
                        dateText,
                        13,
                        0xFF78909C,
                        false
                )
        );

        /*
         * Rate
         */

        double rate =
                getDouble(
                        document,
                        "pricePerLitre"
                );

        if (rate == 0) {

            rate =
                    getDouble(
                            document,
                            "rate"
                    );
        }

        /*
         * Amount
         */

        double amount =
                getDouble(
                        document,
                        "amount"
                );

        String amountText =
                getString(R.string.rate_amount_line, String.format(Locale.US, "%.2f", rate), String.format(Locale.US, "%.2f", amount));

        layout.addView(
                createText(
                        amountText,
                        15,
                        0xFF2E7D32,
                        true
                )
        );

        card.addView(
                layout
        );

        collectionContainer.addView(
                card
        );
    }

    private void showEmpty(
            String message) {

        collectionContainer.removeAllViews();

        tvEmpty.setText(
                message
        );

        tvEmpty.setVisibility(
                View.VISIBLE
        );
    }

    private String localizeSession(String session) {

        if ("Morning".equalsIgnoreCase(session)) {
            return getString(R.string.morning);
        }

        if ("Evening".equalsIgnoreCase(session)) {
            return getString(R.string.evening);
        }

        return session;
    }

    private TextView createText(
            String text,
            float size,
            int color,
            boolean bold) {

        TextView textView =
                new TextView(this);

        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);

        if (bold) {

            textView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                4,
                0,
                4
        );

        textView.setLayoutParams(
                params
        );

        return textView;
    }

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

    @Override
    protected void onDestroy() {

        if (milkListener != null) {

            milkListener.remove();
            milkListener = null;
        }

        super.onDestroy();
    }
}