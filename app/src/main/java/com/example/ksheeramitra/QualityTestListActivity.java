package com.example.ksheeramitra;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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

public class QualityTestListActivity extends AppCompatActivity {

    private EditText etSearch;
    private TextView tvQualityCount;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private View emptyCard;
    private LinearLayout qualityContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration qualityListener;

    private final List<DocumentSnapshot> allTests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quality_test_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etSearch = findViewById(R.id.etSearch);
        tvQualityCount = findViewById(R.id.tvQualityCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        emptyCard = findViewById(R.id.emptyCard);
        progressBar = findViewById(R.id.progressBar);
        qualityContainer = findViewById(R.id.qualityContainer);

        setupSearch();
        loadQualityTests();
    }

    private void loadQualityTests() {

        if (auth.getCurrentUser() == null) {
            showEmpty(getString(R.string.please_login_again));
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        emptyCard.setVisibility(View.GONE);

        qualityListener = db.collection("milkQualityTests")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((snapshots, error) -> {

                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        showEmpty(getString(R.string.unable_load_quality_tests));
                        return;
                    }

                    allTests.clear();

                    if (snapshots != null) {
                        allTests.addAll(snapshots.getDocuments());
                    }

                    Collections.sort(allTests, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot d1,
                                           DocumentSnapshot d2) {

                            long t1 = getLong(d1, "createdAt");
                            long t2 = getLong(d2, "createdAt");

                            return Long.compare(t2, t1);
                        }
                    });

                    displayTests(allTests);
                });
    }

    private void setupSearch() {

        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {

                filterTests(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterTests(String searchText) {

        if (searchText.isEmpty()) {
            displayTests(allTests);
            return;
        }

        String query = searchText.toLowerCase(Locale.getDefault());

        List<DocumentSnapshot> filteredTests = new ArrayList<>();

        for (DocumentSnapshot doc : allTests) {

            String farmerId = getString(doc, "farmerId");
            String farmerName = getString(doc, "farmerName");
            String village = getString(doc, "village");
            String milkType = getString(doc, "milkType");
            String status = getString(doc, "qualityStatus");
            String date = getString(doc, "date");
            String qualityTestId = getString(doc, "qualityTestId");

            if (farmerId.toLowerCase(Locale.getDefault()).contains(query)
                    || farmerName.toLowerCase(Locale.getDefault()).contains(query)
                    || village.toLowerCase(Locale.getDefault()).contains(query)
                    || milkType.toLowerCase(Locale.getDefault()).contains(query)
                    || status.toLowerCase(Locale.getDefault()).contains(query)
                    || date.toLowerCase(Locale.getDefault()).contains(query)
                    || qualityTestId.toLowerCase(Locale.getDefault()).contains(query)) {

                filteredTests.add(doc);
            }
        }

        displayTests(filteredTests);
    }

    private void displayTests(List<DocumentSnapshot> tests) {

        qualityContainer.removeAllViews();

        tvQualityCount.setText(getString(R.string.tests_count, tests.size()));

        if (tests.isEmpty()) {
            tvEmpty.setText(R.string.no_quality_tests);
            emptyCard.setVisibility(View.VISIBLE);
            return;
        }

        emptyCard.setVisibility(View.GONE);

        for (DocumentSnapshot doc : tests) {
            addTestCard(doc);
        }
    }

    private void addTestCard(DocumentSnapshot doc) {

        CardView card = new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 20);

        card.setLayoutParams(cardParams);
        card.setRadius(16);
        card.setCardElevation(5);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 20, 24, 20);

        String farmerName = getString(doc, "farmerName");
        String farmerId = getString(doc, "farmerId");
        String village = getString(doc, "village");

        String qualityTestId = getString(doc, "qualityTestId");

        String milkType = getString(doc, "milkType");
        String status = getString(doc, "qualityStatus");

        double quantity = getDouble(doc, "milkQuantity");

        if (quantity == 0) {
            quantity = getDouble(doc, "quantity");
        }

        double fat = getDouble(doc, "fat");
        double snf = getDouble(doc, "snf");
        double temperature = getDouble(doc, "temperature");

        String date = getString(doc, "date");
        String time = getString(doc, "time");

        addText(layout,
                getString(R.string.quality_farmer_label, farmerName),
                18,
                Color.BLACK,
                true);

        addText(layout,
                getString(R.string.quality_farmer_id_label, farmerId),
                14,
                Color.DKGRAY,
                false);

        addText(layout,
                getString(R.string.farmer_card_village, village),
                14,
                Color.DKGRAY,
                false);

        addText(layout,
                getString(R.string.quality_test_id_label, qualityTestId),
                14,
                Color.DKGRAY,
                false);

        addDivider(layout);

        addText(layout,
                getString(R.string.milk_type_label, localizeMilkType(milkType)),
                15,
                Color.DKGRAY,
                false);

        addText(layout,
                getString(R.string.quantity_label, String.format(Locale.US, "%.2f", quantity)),
                15,
                Color.DKGRAY,
                false);

        addText(layout,
                getString(R.string.fat_snf_label, String.format(Locale.US, "%.2f", fat), String.format(Locale.US, "%.2f", snf)),
                15,
                Color.DKGRAY,
                false);

        addText(layout,
                getString(R.string.temperature_label, String.format(Locale.US, "%.2f", temperature)),
                15,
                Color.DKGRAY,
                false);

        TextView statusView = new TextView(this);

        statusView.setText(getString(R.string.quality_status_label, localizeQuality(status).toUpperCase(Locale.getDefault())));
        statusView.setTextSize(15);
        statusView.setTypeface(null, Typeface.BOLD);

        if ("GOOD".equalsIgnoreCase(status)) {
            statusView.setTextColor(Color.rgb(46, 125, 50));
        } else if ("AVERAGE".equalsIgnoreCase(status)) {
            statusView.setTextColor(Color.rgb(239, 108, 0));
        } else if ("POOR".equalsIgnoreCase(status)) {
            statusView.setTextColor(Color.rgb(198, 40, 40));
        } else {
            statusView.setTextColor(Color.DKGRAY);
        }

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(0, 12, 0, 0);

        statusView.setLayoutParams(statusParams);
        layout.addView(statusView);

        addText(layout,
                getString(R.string.date_time_label, date, time),
                13,
                Color.GRAY,
                false);

        card.addView(layout);
        qualityContainer.addView(card);
    }

    private void addText(LinearLayout parent,
                         String text,
                         float size,
                         int color,
                         boolean bold) {

        TextView textView = new TextView(this);

        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);

        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 3, 0, 3);

        textView.setLayoutParams(params);

        parent.addView(textView);
    }

    private void addDivider(LinearLayout parent) {

        View divider = new View(this);

        divider.setBackgroundColor(Color.LTGRAY);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );

        params.setMargins(0, 12, 0, 12);

        divider.setLayoutParams(params);

        parent.addView(divider);
    }

    private void showEmpty(String message) {

        qualityContainer.removeAllViews();

        tvQualityCount.setText(getString(R.string.tests_count, 0));

        tvEmpty.setText(message);
        emptyCard.setVisibility(View.VISIBLE);
    }

    private String localizeQuality(String status) {

        if ("GOOD".equalsIgnoreCase(status)) {
            return getString(R.string.quality_good);
        }

        if ("AVERAGE".equalsIgnoreCase(status)) {
            return getString(R.string.quality_average);
        }

        if ("POOR".equalsIgnoreCase(status)) {
            return getString(R.string.quality_poor);
        }

        return status;
    }

    private String localizeMilkType(String type) {

        if ("Cow".equalsIgnoreCase(type)) {
            return getString(R.string.cow);
        }

        if ("Buffalo".equalsIgnoreCase(type)) {
            return getString(R.string.buffalo);
        }

        return type;
    }

    private String getString(DocumentSnapshot doc, String field) {

        Object value = doc.get(field);

        return value == null ? "" : String.valueOf(value);
    }

    private double getDouble(DocumentSnapshot doc, String field) {

        Object value = doc.get(field);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private long getLong(DocumentSnapshot doc, String field) {

        Object value = doc.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (qualityListener != null) {
            qualityListener.remove();
            qualityListener = null;
        }
    }
}