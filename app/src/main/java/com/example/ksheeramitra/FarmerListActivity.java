package com.example.ksheeramitra;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FarmerListActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration farmerListener;

    private EditText etSearch;
    private TextView tvFarmerCount;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private LinearLayout farmerContainer;

    private final List<Map<String, Object>> farmerList = new ArrayList<>();
    private final List<Map<String, Object>> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etSearch = findViewById(R.id.etSearch);
        tvFarmerCount = findViewById(R.id.tvFarmerCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        farmerContainer = findViewById(R.id.farmerContainer);

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        setupSearch();
        listenToFarmers();
    }

    private void listenToFarmers() {

        String uid = auth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        farmerListener = db.collection("users")
                .document(uid)
                .collection("farmers")
                .addSnapshotListener((snapshot, error) -> {

                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(
                                FarmerListActivity.this,
                                getString(R.string.unable_load_farmers_simple),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    farmerList.clear();

                    if (snapshot != null) {

                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {

                            Map<String, Object> farmer =
                                    new HashMap<>(document.getData());

                            // Always keep the actual Firestore document ID
                            farmer.put("_documentId", document.getId());

                            if (!farmer.containsKey("farmerId")) {
                                farmer.put("farmerId", document.getId());
                            }

                            if (!farmer.containsKey("status")) {
                                farmer.put("status", "Active");
                            }

                            farmerList.add(farmer);
                        }
                    }

                    sortFarmers();
                    filterFarmers();
                });
    }

    private void sortFarmers() {

        Collections.sort(farmerList, (a, b) -> {

            String nameA = getString(a, "name").toLowerCase(Locale.getDefault());
            String nameB = getString(b, "name").toLowerCase(Locale.getDefault());

            return nameA.compareTo(nameB);
        });
    }

    private void setupSearch() {

        etSearch.addTextChangedListener(new TextWatcher() {

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

                filterFarmers();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterFarmers() {

        String search = etSearch.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.getDefault());

        filteredList.clear();

        for (Map<String, Object> farmer : farmerList) {

            String farmerId = getString(farmer, "farmerId");
            String name = getString(farmer, "name");
            String village = getString(farmer, "village");
            String phone = getString(farmer, "phone");

            String combined =
                    farmerId + " " +
                            name + " " +
                            village + " " +
                            phone;

            if (search.isEmpty() ||
                    combined.toLowerCase(Locale.getDefault())
                            .contains(search)) {

                filteredList.add(farmer);
            }
        }

        tvFarmerCount.setText(
                getString(R.string.total_farmers_count, filteredList.size())
        );

        displayFarmers();
    }

    private void displayFarmers() {

        farmerContainer.removeAllViews();

        if (filteredList.isEmpty()) {

            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        for (Map<String, Object> farmer : filteredList) {
            farmerContainer.addView(createFarmerCard(farmer));
        }
    }

    private View createFarmerCard(Map<String, Object> farmer) {

        CardView card = new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setRadius(18);
        card.setCardElevation(5);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Initial
        TextView initial = new TextView(this);

        String name = getString(farmer, "name");

        String firstLetter = "F";

        if (!name.isEmpty()) {
            firstLetter = name.substring(0, 1).toUpperCase();
        }

        initial.setText(firstLetter);
        initial.setTextSize(22);
        initial.setGravity(Gravity.CENTER);
        initial.setTextColor(0xFFFFFFFF);
        initial.setBackgroundColor(0xFF64B5F6);

        LinearLayout.LayoutParams initialParams =
                new LinearLayout.LayoutParams(55, 55);

        initialParams.setMargins(0, 0, 18, 0);
        initial.setLayoutParams(initialParams);

        // Information layout
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        infoLayout.setLayoutParams(infoParams);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(18);
        tvName.setTextColor(0xFF222222);

        TextView tvId = new TextView(this);
        tvId.setText(
                getString(R.string.farmer_card_id, getString(farmer, "farmerId"))
        );
        tvId.setTextSize(14);
        tvId.setTextColor(0xFF555555);

        TextView tvVillage = new TextView(this);
        tvVillage.setText(
                getString(R.string.farmer_card_village, getString(farmer, "village"))
        );
        tvVillage.setTextSize(14);
        tvVillage.setTextColor(0xFF555555);

        TextView tvPhone = new TextView(this);
        tvPhone.setText(
                getString(R.string.farmer_card_phone, getString(farmer, "phone"))
        );
        tvPhone.setTextSize(14);
        tvPhone.setTextColor(0xFF555555);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(
                getString(R.string.farmer_card_status, localizeStatus(getString(farmer, "status")))
        );
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(0xFF2E7D32);

        infoLayout.addView(tvName);
        infoLayout.addView(tvId);
        infoLayout.addView(tvVillage);
        infoLayout.addView(tvPhone);
        infoLayout.addView(tvStatus);

        // Delete button
        TextView delete = new TextView(this);
        delete.setText(getString(R.string.delete).toUpperCase(Locale.getDefault()));
        delete.setTextSize(12);
        delete.setTextColor(0xFFD32F2F);
        delete.setGravity(Gravity.CENTER);
        delete.setPadding(10, 10, 10, 10);

        // Arrow
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(30);
        arrow.setTextColor(0xFF777777);
        arrow.setGravity(Gravity.CENTER);

        mainLayout.addView(initial);
        mainLayout.addView(infoLayout);
        mainLayout.addView(delete);
        mainLayout.addView(arrow);

        card.addView(mainLayout);

        // Edit farmer
        View.OnClickListener editListener = v -> {

            Intent intent =
                    new Intent(
                            FarmerListActivity.this,
                            FarmerActivity.class
                    );

            intent.putExtra(
                    "edit_mode",
                    true
            );

            intent.putExtra(
                    "farmer_id",
                    getString(farmer, "farmerId")
            );

            intent.putExtra(
                    "farmer_name",
                    getString(farmer, "name")
            );

            intent.putExtra(
                    "farmer_phone",
                    getString(farmer, "phone")
            );

            intent.putExtra(
                    "farmer_village",
                    getString(farmer, "village")
            );

            intent.putExtra(
                    "farmer_status",
                    getString(farmer, "status")
            );

            startActivity(intent);
        };

        card.setOnClickListener(editListener);
        arrow.setOnClickListener(editListener);

        // Delete farmer
        delete.setOnClickListener(v ->
                confirmDelete(farmer)
        );

        return card;
    }

    private void confirmDelete(Map<String, Object> farmer) {

        String farmerId = getString(farmer, "farmerId");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_farmer_title))
                .setMessage(
                        getString(R.string.delete_farmer_confirm, getString(farmer, "name"))
                )
                .setNegativeButton(
                        getString(R.string.cancel).toUpperCase(Locale.getDefault()),
                        null
                )
                .setPositiveButton(
                        getString(R.string.delete).toUpperCase(Locale.getDefault()),
                        (dialog, which) -> deleteFarmer(farmer)
                )
                .show();
    }

    private void deleteFarmer(Map<String, Object> farmer) {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        String documentId =
                getString(farmer, "_documentId");

        if (documentId.isEmpty()) {
            documentId =
                    getString(farmer, "farmerId");
        }

        final String finalDocumentId = documentId;

        db.collection("users")
                .document(uid)
                .collection("farmers")
                .document(finalDocumentId)
                .delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            getString(R.string.farmer_deleted),
                            Toast.LENGTH_SHORT
                    ).show();

                    // Snapshot listener automatically refreshes the list.
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                getString(R.string.delete_failed, e.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private String localizeStatus(String status) {

        if ("Active".equalsIgnoreCase(status)) {
            return getString(R.string.status_active);
        }

        if ("Inactive".equalsIgnoreCase(status)) {
            return getString(R.string.status_inactive);
        }

        return status;
    }

    private String getString(
            Map<String, Object> map,
            String key) {

        Object value = map.get(key);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    @Override
    protected void onDestroy() {

        if (farmerListener != null) {
            farmerListener.remove();
        }

        super.onDestroy();
    }
}