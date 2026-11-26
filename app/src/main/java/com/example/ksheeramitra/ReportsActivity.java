package com.example.ksheeramitra;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {

    // =========================================================
    // PDF DESIGN CONSTANTS  (change the colours here if you like)
    // =========================================================

    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final float MARGIN = 36f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;
    private static final float CONTENT_BOTTOM = PAGE_H - 52f;

    private static final float HEADER_H = 24f;
    private static final float ROW_H = 20f;

    private static final int COLOR_PRIMARY = 0xFF512DA8;
    private static final int COLOR_PRIMARY_DARK = 0xFF311B92;
    private static final int COLOR_TINT = 0xFFEDE7F6;
    private static final int COLOR_STRIPE = 0xFFF8F6FC;
    private static final int COLOR_BORDER = 0xFFD1C4E9;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_MUTED = 0xFF616161;
    private static final int COLOR_LIGHT_ON_PRIMARY = 0xFFD1C4E9;
    private static final int COLOR_GREEN = 0xFF2E7D32;
    private static final int COLOR_ORANGE = 0xFFEF6C00;
    private static final int COLOR_RED = 0xFFC62828;
    private static final int COLOR_BLUE = 0xFF1E88E5;
    private static final int COLOR_TEAL = 0xFF00897B;

    private static final int ALIGN_LEFT = 0;
    private static final int ALIGN_CENTER = 1;
    private static final int ALIGN_RIGHT = 2;

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration farmerListener;
    private ListenerRegistration milkListener;
    private ListenerRegistration qualityListener;
    private ListenerRegistration paymentListener;

    // =========================================================
    // TEXT VIEWS
    // =========================================================

    private TextView txtTotalMilk;
    private TextView txtTotalFarmers;
    private TextView txtQualityTests;
    private TextView txtTotalPayment;

    // =========================================================
    // BUTTONS
    // =========================================================

    private Button btnToday;
    private Button btnWeekly;
    private Button btnMonthly;
    private Button btnGeneratePdf;
    private Button btnGenerateFarmerPdf;

    // =========================================================
    // GRAPH
    // =========================================================

    private ChartView chartView;

    // =========================================================
    // DATE
    // =========================================================

    private String fromDate;
    private String toDate;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // =========================================================
    // PDF TITLE (changes between overall and farmer report)
    // =========================================================

    private String reportTitle = "Milk Collection Report";

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reports);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupPeriodButtons();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        setTodayPeriod();

        loadReports();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        txtTotalMilk = findViewById(R.id.txtTotalMilk);
        txtTotalFarmers = findViewById(R.id.txtTotalFarmers);
        txtQualityTests = findViewById(R.id.txtQualityTests);
        txtTotalPayment = findViewById(R.id.txtTotalPayment);

        btnToday = findViewById(R.id.btnToday);
        btnWeekly = findViewById(R.id.btnWeekly);
        btnMonthly = findViewById(R.id.btnMonthly);

        chartView = findViewById(R.id.chartView);

        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnGenerateFarmerPdf = findViewById(R.id.btnGenerateFarmerPdf);
    }

    // =========================================================
    // PERIOD BUTTONS
    // =========================================================

    private void setupPeriodButtons() {

        btnToday.setOnClickListener(v -> {
            setTodayPeriod();
            loadReports();
        });

        btnWeekly.setOnClickListener(v -> {
            setWeeklyPeriod();
            loadReports();
        });

        btnMonthly.setOnClickListener(v -> {
            setMonthlyPeriod();
            loadReports();
        });

        btnGeneratePdf.setOnClickListener(v -> generatePdfReport());

        btnGenerateFarmerPdf.setOnClickListener(v -> askFarmerId());
    }

    // =========================================================
    // TODAY
    // =========================================================

    private void setTodayPeriod() {

        String today = dateFormat.format(Calendar.getInstance().getTime());

        fromDate = today;
        toDate = today;
    }

    // =========================================================
    // WEEKLY
    // =========================================================

    private void setWeeklyPeriod() {

        Calendar end = Calendar.getInstance();
        Calendar start = Calendar.getInstance();

        start.add(Calendar.DAY_OF_MONTH, -6);

        fromDate = dateFormat.format(start.getTime());
        toDate = dateFormat.format(end.getTime());
    }

    // =========================================================
    // MONTHLY
    // =========================================================

    private void setMonthlyPeriod() {

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.set(Calendar.DAY_OF_MONTH, 1);

        fromDate = dateFormat.format(start.getTime());
        toDate = dateFormat.format(end.getTime());
    }

    // =========================================================
    // DATE CHECK
    // =========================================================

    private boolean isDateInRange(String date) {

        if (date == null || date.trim().isEmpty()) {
            return false;
        }

        return date.compareTo(fromDate) >= 0
                && date.compareTo(toDate) <= 0;
    }

    // =========================================================
    // PAYMENT DATE (uses createdAt when "date" is missing)
    // =========================================================

    private String paymentDate(DocumentSnapshot document) {

        String date = getString(document, "date");

        if (date.isEmpty()) {

            long createdAt = getLong(document, "createdAt");

            if (createdAt > 0) {
                date = dateFormat.format(new Date(createdAt));
            }
        }

        return date;
    }

    // =========================================================
    // LOAD REPORTS
    // =========================================================

    private void loadReports() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();

        removeListeners();

        resetValues();

        // FARMERS

        farmerListener =
                db.collection("users")
                        .document(uid)
                        .collection("farmers")
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {
                                showFirebaseError("Farmers", error);
                                return;
                            }

                            if (snapshot == null) {
                                return;
                            }

                            txtTotalFarmers.setText(
                                    String.valueOf(snapshot.size())
                            );
                        });

        // MILK

        milkListener =
                db.collection("milkCollections")
                        .whereEqualTo("ownerUid", uid)
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {
                                showFirebaseError("Milk Collections", error);
                                return;
                            }

                            if (snapshot == null) {
                                return;
                            }

                            calculateMilkAndGraph(snapshot.getDocuments());
                        });

        // QUALITY

        qualityListener =
                db.collection("milkQualityTests")
                        .whereEqualTo("ownerUid", uid)
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {
                                showFirebaseError("Quality Tests", error);
                                return;
                            }

                            if (snapshot == null) {
                                return;
                            }

                            int totalTests = 0;

                            for (DocumentSnapshot document : snapshot.getDocuments()) {

                                if (isDateInRange(getString(document, "date"))) {
                                    totalTests++;
                                }
                            }

                            txtQualityTests.setText(String.valueOf(totalTests));
                        });

        // PAYMENTS

        paymentListener =
                db.collection("payments")
                        .whereEqualTo("ownerUid", uid)
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {
                                showFirebaseError("Payments", error);
                                return;
                            }

                            if (snapshot == null) {
                                return;
                            }

                            double totalPayment = 0;

                            for (DocumentSnapshot document : snapshot.getDocuments()) {

                                if (!isDateInRange(paymentDate(document))) {
                                    continue;
                                }

                                totalPayment += getDouble(document, "amountPaid");
                            }

                            txtTotalPayment.setText(formatMoney(totalPayment));
                        });
    }

    // =========================================================
    // MILK + GRAPH
    // =========================================================

    private void calculateMilkAndGraph(List<DocumentSnapshot> documents) {

        double totalMilk = 0;

        Map<String, Float> dailyMilk = new LinkedHashMap<>();

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        try {

            start.setTime(dateFormat.parse(fromDate));
            end.setTime(dateFormat.parse(toDate));

        } catch (Exception e) {

            showGraphNoData();
            return;
        }

        Calendar current = (Calendar) start.clone();

        while (!current.after(end)) {

            dailyMilk.put(dateFormat.format(current.getTime()), 0f);

            current.add(Calendar.DAY_OF_MONTH, 1);
        }

        for (DocumentSnapshot document : documents) {

            String date = getString(document, "date");

            if (!isDateInRange(date)) {
                continue;
            }

            double quantity = getDouble(document, "quantity");

            totalMilk += quantity;

            if (dailyMilk.containsKey(date)) {

                float oldValue = dailyMilk.get(date);

                dailyMilk.put(date, oldValue + (float) quantity);
            }
        }

        txtTotalMilk.setText(String.format(Locale.US, "%.2f L", totalMilk));

        List<Float> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (Map.Entry<String, Float> entry : dailyMilk.entrySet()) {

            values.add(entry.getValue());
            labels.add(makeGraphLabel(entry.getKey()));
        }

        if (values.isEmpty()) {

            showGraphNoData();
            return;
        }

        float[] graphValues = new float[values.size()];
        String[] graphLabels = new String[labels.size()];

        for (int i = 0; i < values.size(); i++) {

            graphValues[i] = values.get(i);
            graphLabels[i] = labels.get(i);
        }

        chartView.setValues(graphValues, graphLabels);
    }

    // =========================================================
    // GRAPH LABEL
    // =========================================================

    private String makeGraphLabel(String dateString) {

        try {

            Date date = dateFormat.parse(dateString);

            Calendar calendar = Calendar.getInstance();

            calendar.setTime(date);

            return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

        } catch (Exception e) {

            return dateString;
        }
    }

    // =========================================================
    // ========== REPORT 1 : OVERALL DAIRY PDF =================
    // =========================================================

    // PDF REPORT  (data loading)

    private void generatePdfReport() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid = user.getUid();

        Toast.makeText(
                this,
                "Preparing PDF report...",
                Toast.LENGTH_SHORT
        ).show();

        Task<?> farmersTask =
                db.collection("users")
                        .document(uid)
                        .collection("farmers")
                        .get();

        Task<?> milkTask =
                db.collection("milkCollections")
                        .whereEqualTo("ownerUid", uid)
                        .get();

        Task<?> qualityTask =
                db.collection("milkQualityTests")
                        .whereEqualTo("ownerUid", uid)
                        .get();

        Task<?> paymentsTask =
                db.collection("payments")
                        .whereEqualTo("ownerUid", uid)
                        .get();

        Tasks.whenAllSuccess(
                        farmersTask,
                        milkTask,
                        qualityTask,
                        paymentsTask
                )
                .addOnSuccessListener(results -> {

                    List<DocumentSnapshot> farmers =
                            ((QuerySnapshot) results.get(0)).getDocuments();

                    List<DocumentSnapshot> milk =
                            ((QuerySnapshot) results.get(1)).getDocuments();

                    List<DocumentSnapshot> quality =
                            ((QuerySnapshot) results.get(2)).getDocuments();

                    List<DocumentSnapshot> payments =
                            ((QuerySnapshot) results.get(3)).getDocuments();

                    createPdf(farmers, milk, quality, payments);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to prepare report: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // CREATE PDF  (data preparation + layout)

    private void createPdf(
            List<DocumentSnapshot> farmers,
            List<DocumentSnapshot> milkDocuments,
            List<DocumentSnapshot> qualityDocuments,
            List<DocumentSnapshot> paymentDocuments) {

        try {

            // MILK: totals, per-day stats, per-farmer stats
            // daily value = {entries, quantity, fat*qty, snf*qty}
            // farmer value = {milk, paid}

            double totalMilk = 0;
            double totalFatQty = 0;
            double totalSnfQty = 0;
            int totalEntries = 0;

            Map<String, double[]> daily = new LinkedHashMap<>();
            Map<String, double[]> farmerStats = new HashMap<>();

            Calendar cur = Calendar.getInstance();
            Calendar endDay = Calendar.getInstance();

            cur.setTime(dateFormat.parse(fromDate));
            endDay.setTime(dateFormat.parse(toDate));

            while (!cur.after(endDay)) {

                daily.put(dateFormat.format(cur.getTime()), new double[4]);

                cur.add(Calendar.DAY_OF_MONTH, 1);
            }

            for (DocumentSnapshot document : milkDocuments) {

                String date = getString(document, "date");

                if (!isDateInRange(date)) {
                    continue;
                }

                double qty = getDouble(document, "quantity");
                double fat = getDouble(document, "fat");
                double snf = getDouble(document, "snf");

                totalMilk += qty;
                totalFatQty += fat * qty;
                totalSnfQty += snf * qty;
                totalEntries++;

                double[] day = daily.get(date);

                if (day != null) {
                    day[0] += 1;
                    day[1] += qty;
                    day[2] += fat * qty;
                    day[3] += snf * qty;
                }

                String farmerId = getString(document, "farmerId");

                if (!farmerId.isEmpty()) {

                    double[] stats = farmerStats.get(farmerId);

                    if (stats == null) {
                        stats = new double[2];
                        farmerStats.put(farmerId, stats);
                    }

                    stats[0] += qty;
                }
            }

            // QUALITY COUNT

            int qualityCount = 0;

            for (DocumentSnapshot document : qualityDocuments) {

                if (isDateInRange(getString(document, "date"))) {
                    qualityCount++;
                }
            }

            // PAYMENTS

            List<DocumentSnapshot> paymentsInRange = new ArrayList<>();

            double totalPayment = 0;
            double totalBilled = 0;
            double totalBalance = 0;

            for (DocumentSnapshot document : paymentDocuments) {

                if (!isDateInRange(paymentDate(document))) {
                    continue;
                }

                paymentsInRange.add(document);

                double billed = getDouble(document, "totalAmount");
                double paid = getDouble(document, "amountPaid");

                totalPayment += paid;
                totalBilled += billed;
                totalBalance += Math.max(0.0, billed - paid);

                String farmerId = getString(document, "farmerId");

                if (!farmerId.isEmpty()) {

                    double[] stats = farmerStats.get(farmerId);

                    if (stats == null) {
                        stats = new double[2];
                        farmerStats.put(farmerId, stats);
                    }

                    stats[1] += paid;
                }
            }

            Collections.sort(paymentsInRange, (a, b) -> {

                int c = paymentDate(a).compareTo(paymentDate(b));

                if (c != 0) {
                    return c;
                }

                return Long.compare(
                        getLong(a, "createdAt"),
                        getLong(b, "createdAt")
                );
            });

            // TABLE 1: DAILY MILK COLLECTION

            String[] dailyHeaders = {
                    "Date", "Collections", "Milk (L)", "Avg Fat %", "Avg SNF %"
            };

            float[] dailyWidths = {150, 85, 100, 94, 94};

            int[] dailyAligns = {
                    ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT, ALIGN_RIGHT, ALIGN_RIGHT
            };

            List<String[]> dailyRows = new ArrayList<>();

            SimpleDateFormat dayFormat =
                    new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);

            for (Map.Entry<String, double[]> entry : daily.entrySet()) {

                double[] day = entry.getValue();

                String label;

                try {
                    label = dayFormat.format(dateFormat.parse(entry.getKey()));
                } catch (Exception e) {
                    label = entry.getKey();
                }

                dailyRows.add(new String[]{
                        label,
                        String.valueOf((int) day[0]),
                        formatAmount(day[1]),
                        averageText(day[2], day[1]),
                        averageText(day[3], day[1])
                });
            }

            String[] dailyTotals = {
                    "TOTAL",
                    String.valueOf(totalEntries),
                    formatAmount(totalMilk),
                    averageText(totalFatQty, totalMilk),
                    averageText(totalSnfQty, totalMilk)
            };

            // TABLE 2: FARMER-WISE SUMMARY

            String[] farmerHeaders = {
                    "#", "Farmer ID", "Farmer Name", "Village", "Milk (L)", "Paid (Rs.)"
            };

            float[] farmerWidths = {28, 72, 140, 98, 80, 105};

            int[] farmerAligns = {
                    ALIGN_CENTER, ALIGN_LEFT, ALIGN_LEFT, ALIGN_LEFT,
                    ALIGN_RIGHT, ALIGN_RIGHT
            };

            List<DocumentSnapshot> sortedFarmers = new ArrayList<>(farmers);

            Collections.sort(sortedFarmers, (a, b) ->
                    getString(a, "farmerId")
                            .compareToIgnoreCase(getString(b, "farmerId"))
            );

            List<String[]> farmerRows = new ArrayList<>();

            double farmerMilkTotal = 0;
            double farmerPaidTotal = 0;

            int serial = 1;

            for (DocumentSnapshot farmer : sortedFarmers) {

                String farmerId = getString(farmer, "farmerId");

                String displayId = farmerId.isEmpty() ? farmer.getId() : farmerId;

                String name = getString(farmer, "name");

                if (name.isEmpty()) {
                    name = "Unknown";
                }

                String village = getString(farmer, "village");

                if (village.isEmpty()) {
                    village = "-";
                }

                double[] stats = farmerStats.get(farmerId);

                double milk = stats == null ? 0 : stats[0];
                double paid = stats == null ? 0 : stats[1];

                farmerMilkTotal += milk;
                farmerPaidTotal += paid;

                farmerRows.add(new String[]{
                        String.valueOf(serial++),
                        displayId,
                        name,
                        village,
                        formatAmount(milk),
                        formatAmount(paid)
                });
            }

            String[] farmerTotals = {
                    "",
                    "TOTAL",
                    farmers.size() + " farmers",
                    "",
                    formatAmount(farmerMilkTotal),
                    formatAmount(farmerPaidTotal)
            };

            // TABLE 3: PAYMENT DETAILS

            String[] paymentHeaders = {
                    "Date", "Farmer", "Total (Rs.)", "Paid (Rs.)",
                    "Balance (Rs.)", "Status"
            };

            float[] paymentWidths = {74, 125, 82, 82, 88, 72};

            int[] paymentAligns = {
                    ALIGN_LEFT, ALIGN_LEFT, ALIGN_RIGHT, ALIGN_RIGHT,
                    ALIGN_RIGHT, ALIGN_CENTER
            };

            List<String[]> paymentRows = buildPaymentRows(paymentsInRange);

            String[] paymentTotals = {
                    "TOTAL",
                    paymentsInRange.size() + " records",
                    formatAmount(totalBilled),
                    formatAmount(totalPayment),
                    formatAmount(totalBalance),
                    ""
            };

            // BUILD THE DOCUMENT

            reportTitle = "Milk Collection Report";

            PdfBuilder pdf = new PdfBuilder();

            pdf.startPage();

            pdf.summaryCards(
                    formatAmount(totalMilk) + " L",
                    String.valueOf(farmers.size()),
                    String.valueOf(qualityCount),
                    formatMoney(totalPayment)
            );

            pdf.sectionTitle("Daily Milk Collection");
            pdf.table(
                    dailyHeaders,
                    dailyWidths,
                    dailyAligns,
                    dailyRows,
                    dailyTotals
            );

            pdf.sectionTitle("Farmer-wise Summary");
            pdf.table(
                    farmerHeaders,
                    farmerWidths,
                    farmerAligns,
                    farmerRows,
                    farmerTotals
            );

            pdf.sectionTitle("Payment Details");
            pdf.table(
                    paymentHeaders,
                    paymentWidths,
                    paymentAligns,
                    paymentRows,
                    paymentTotals
            );

            pdf.signatures();

            PdfDocument pdfDocument = pdf.finish();

            // SAVE

            File directory =
                    new File(
                            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                            "KsheeraMitra"
                    );

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName =
                    "KsheeraMitra_Report_" + System.currentTimeMillis() + ".pdf";

            File file = new File(directory, fileName);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                pdfDocument.writeTo(outputStream);
            }

            pdfDocument.close();

            openPdf(file);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "PDF generation failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // ========== REPORT 2 : SPECIFIC FARMER PDF ===============
    // =========================================================

    // ASK FOR THE FARMER ID

    private void askFarmerId() {

        EditText input = new EditText(this);
        input.setHint("e.g. KM-0001");
        input.setSingleLine(true);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        );

        FrameLayout box = new FrameLayout(this);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Farmer Report")
                .setMessage("Enter the Farmer ID")
                .setView(box)
                .setPositiveButton("Generate", (dialog, which) -> {

                    String id = input.getText().toString().trim();

                    if (id.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Enter a Farmer ID",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        generateFarmerPdfReport(id);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // LOAD DATA FOR ONE FARMER

    private void generateFarmerPdfReport(String enteredId) {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid = user.getUid();

        Toast.makeText(
                this,
                "Preparing farmer report...",
                Toast.LENGTH_SHORT
        ).show();

        Task<?> farmersTask =
                db.collection("users")
                        .document(uid)
                        .collection("farmers")
                        .get();

        Task<?> milkTask =
                db.collection("milkCollections")
                        .whereEqualTo("ownerUid", uid)
                        .get();

        Task<?> qualityTask =
                db.collection("milkQualityTests")
                        .whereEqualTo("ownerUid", uid)
                        .get();

        Task<?> paymentsTask =
                db.collection("payments")
                        .whereEqualTo("ownerUid", uid)
                        .get();

        Tasks.whenAllSuccess(
                        farmersTask,
                        milkTask,
                        qualityTask,
                        paymentsTask
                )
                .addOnSuccessListener(results -> {

                    List<DocumentSnapshot> farmers =
                            ((QuerySnapshot) results.get(0)).getDocuments();

                    DocumentSnapshot farmer = null;

                    for (DocumentSnapshot f : farmers) {

                        if (getString(f, "farmerId").equalsIgnoreCase(enteredId)) {
                            farmer = f;
                            break;
                        }
                    }

                    if (farmer == null) {

                        Toast.makeText(
                                this,
                                "Farmer ID " + enteredId + " not found",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String id = getString(farmer, "farmerId");

                    List<DocumentSnapshot> milk = filterByFarmer(
                            ((QuerySnapshot) results.get(1)).getDocuments(), id);

                    List<DocumentSnapshot> quality = filterByFarmer(
                            ((QuerySnapshot) results.get(2)).getDocuments(), id);

                    List<DocumentSnapshot> payments = filterByFarmer(
                            ((QuerySnapshot) results.get(3)).getDocuments(), id);

                    createFarmerPdf(farmer, milk, quality, payments);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to prepare report: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private List<DocumentSnapshot> filterByFarmer(
            List<DocumentSnapshot> documents,
            String farmerId) {

        List<DocumentSnapshot> out = new ArrayList<>();

        for (DocumentSnapshot d : documents) {

            if (getString(d, "farmerId").equalsIgnoreCase(farmerId)) {
                out.add(d);
            }
        }

        return out;
    }

    // BUILD THE FARMER PDF

    private void createFarmerPdf(
            DocumentSnapshot farmer,
            List<DocumentSnapshot> milkDocs,
            List<DocumentSnapshot> qualityDocs,
            List<DocumentSnapshot> paymentDocs) {

        try {

            String farmerId = getString(farmer, "farmerId");
            String name = getString(farmer, "name");
            String village = getString(farmer, "village");
            String phone = getString(farmer, "phone");

            if (name.isEmpty()) name = "Unknown";
            if (village.isEmpty()) village = "-";
            if (phone.isEmpty()) phone = "-";

            // MILK ENTRIES IN RANGE

            List<DocumentSnapshot> milkInRange = new ArrayList<>();

            double totalMilk = 0;
            double fatQty = 0;
            double snfQty = 0;

            for (DocumentSnapshot d : milkDocs) {

                if (!isDateInRange(getString(d, "date"))) {
                    continue;
                }

                milkInRange.add(d);

                double qty = getDouble(d, "quantity");

                totalMilk += qty;
                fatQty += getDouble(d, "fat") * qty;
                snfQty += getDouble(d, "snf") * qty;
            }

            Collections.sort(milkInRange, (a, b) -> {

                int c = getString(a, "date").compareTo(getString(b, "date"));

                if (c != 0) {
                    return c;
                }

                return Long.compare(
                        getLong(a, "createdAt"),
                        getLong(b, "createdAt")
                );
            });

            // PAYMENTS IN RANGE

            List<DocumentSnapshot> paymentsInRange = new ArrayList<>();

            double totalBilled = 0;
            double totalPaid = 0;
            double totalBalance = 0;

            for (DocumentSnapshot d : paymentDocs) {

                if (!isDateInRange(paymentDate(d))) {
                    continue;
                }

                paymentsInRange.add(d);

                double billed = getDouble(d, "totalAmount");
                double paid = getDouble(d, "amountPaid");

                totalBilled += billed;
                totalPaid += paid;
                totalBalance += Math.max(0.0, billed - paid);
            }

            Collections.sort(paymentsInRange, (a, b) -> {

                int c = paymentDate(a).compareTo(paymentDate(b));

                if (c != 0) {
                    return c;
                }

                return Long.compare(
                        getLong(a, "createdAt"),
                        getLong(b, "createdAt")
                );
            });

            // QUALITY TESTS IN RANGE

            int qualityCount = 0;

            for (DocumentSnapshot d : qualityDocs) {

                if (isDateInRange(getString(d, "date"))) {
                    qualityCount++;
                }
            }

            // TABLE 1: EVERY MILK ENTRY

            String[] entryHeaders = {
                    "Date", "Shift", "Milk (L)", "Fat %", "SNF %"
            };

            float[] entryWidths = {110, 80, 111, 111, 111};

            int[] entryAligns = {
                    ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT, ALIGN_RIGHT, ALIGN_RIGHT
            };

            List<String[]> entryRows = new ArrayList<>();

            for (DocumentSnapshot d : milkInRange) {

                String shift = getString(d, "shift");

                if (shift.isEmpty()) {
                    shift = "-";
                }

                entryRows.add(new String[]{
                        displayDate(getString(d, "date")),
                        shift,
                        formatAmount(getDouble(d, "quantity")),
                        String.format(Locale.US, "%.2f", getDouble(d, "fat")),
                        String.format(Locale.US, "%.2f", getDouble(d, "snf"))
                });
            }

            String[] entryTotals = {
                    "TOTAL",
                    milkInRange.size() + " entries",
                    formatAmount(totalMilk),
                    averageText(fatQty, totalMilk),
                    averageText(snfQty, totalMilk)
            };

            // TABLE 2: PAYMENTS

            String[] paymentHeaders = {
                    "Date", "Farmer", "Total (Rs.)", "Paid (Rs.)",
                    "Balance (Rs.)", "Status"
            };

            float[] paymentWidths = {74, 125, 82, 82, 88, 72};

            int[] paymentAligns = {
                    ALIGN_LEFT, ALIGN_LEFT, ALIGN_RIGHT, ALIGN_RIGHT,
                    ALIGN_RIGHT, ALIGN_CENTER
            };

            String[] paymentTotals = {
                    "TOTAL",
                    paymentsInRange.size() + " records",
                    formatAmount(totalBilled),
                    formatAmount(totalPaid),
                    formatAmount(totalBalance),
                    ""
            };

            // BUILD THE DOCUMENT

            reportTitle = "Farmer Report - " + farmerId;

            PdfBuilder pdf = new PdfBuilder();

            pdf.startPage();

            pdf.farmerInfo(farmerId, name, village, phone);

            pdf.summaryCards(
                    new String[]{
                            "TOTAL MILK", "AVG FAT / SNF",
                            "QUALITY TESTS", "TOTAL PAID"
                    },
                    new String[]{
                            formatAmount(totalMilk) + " L",
                            averageText(fatQty, totalMilk) + " / "
                                    + averageText(snfQty, totalMilk),
                            String.valueOf(qualityCount),
                            formatMoney(totalPaid)
                    },
                    new int[]{
                            COLOR_BLUE, COLOR_PRIMARY, COLOR_TEAL, COLOR_GREEN
                    }
            );

            pdf.sectionTitle("Milk Collection Entries");
            pdf.table(
                    entryHeaders,
                    entryWidths,
                    entryAligns,
                    entryRows,
                    entryTotals
            );

            pdf.sectionTitle("Payment Details");
            pdf.table(
                    paymentHeaders,
                    paymentWidths,
                    paymentAligns,
                    buildPaymentRows(paymentsInRange),
                    paymentTotals
            );

            pdf.signatures();

            PdfDocument pdfDocument = pdf.finish();

            // SAVE

            File directory =
                    new File(
                            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                            "KsheeraMitra"
                    );

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName =
                    "KsheeraMitra_Farmer_" + farmerId + "_"
                            + System.currentTimeMillis() + ".pdf";

            File file = new File(directory, fileName);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                pdfDocument.writeTo(outputStream);
            }

            pdfDocument.close();

            openPdf(file);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "PDF generation failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // PAYMENT ROWS (shared by both reports)
    // =========================================================

    private List<String[]> buildPaymentRows(List<DocumentSnapshot> payments) {

        List<String[]> rows = new ArrayList<>();

        for (DocumentSnapshot payment : payments) {

            String farmerName = getString(payment, "farmerName");

            if (farmerName.isEmpty()) {
                farmerName = getString(payment, "farmerId");
            }

            if (farmerName.isEmpty()) {
                farmerName = "Farmer";
            }

            double billed = getDouble(payment, "totalAmount");
            double paid = getDouble(payment, "amountPaid");
            double balance = Math.max(0.0, billed - paid);

            String status = getString(payment, "status").toUpperCase(Locale.US);

            if (status.isEmpty()) {

                if (balance <= 0.01) {
                    status = "PAID";
                } else if (paid > 0.01) {
                    status = "PARTIAL";
                } else {
                    status = "PENDING";
                }
            }

            rows.add(new String[]{
                    paymentDate(payment),
                    farmerName,
                    formatAmount(billed),
                    formatAmount(paid),
                    formatAmount(balance),
                    status
            });
        }

        return rows;
    }

    // =========================================================
    // PDF BUILDER  (pages, banner, cards, tables, footer)
    // =========================================================

    private class PdfBuilder {

        private final PdfDocument document = new PdfDocument();

        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber = 0;
        private float y = 0;

        private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shape = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final String generatedOn =
                new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                        .format(new Date());

        // ---------------- pages ----------------

        void startPage() {

            if (page != null) {
                finishPage();
            }

            pageNumber++;

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber)
                            .create();

            page = document.startPage(info);
            canvas = page.getCanvas();

            if (pageNumber == 1) {
                drawBanner();
            } else {
                drawSlimHeader();
            }
        }

        private void finishPage() {

            drawFooter();

            document.finishPage(page);

            page = null;
        }

        PdfDocument finish() {

            if (page != null) {
                finishPage();
            }

            return document;
        }

        private void ensureSpace(float height) {

            if (y + height > CONTENT_BOTTOM) {
                startPage();
            }
        }

        // ---------------- text helpers ----------------

        private void font(float size, boolean bold, int color) {

            text.setTextSize(size);

            text.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            bold ? Typeface.BOLD : Typeface.NORMAL
                    )
            );

            text.setColor(color);
        }

        private void drawText(
                String value,
                float x,
                float baseline,
                Paint.Align align) {

            text.setTextAlign(align);

            canvas.drawText(value, x, baseline, text);

            text.setTextAlign(Paint.Align.LEFT);
        }

        private String fit(String value, float maxWidth) {

            return TextUtils.ellipsize(
                    value,
                    text,
                    maxWidth,
                    TextUtils.TruncateAt.END
            ).toString();
        }

        private void fill(int color) {

            shape.setStyle(Paint.Style.FILL);
            shape.setColor(color);
        }

        private void stroke(int color, float width) {

            shape.setStyle(Paint.Style.STROKE);
            shape.setColor(color);
            shape.setStrokeWidth(width);
        }

        // ---------------- page furniture ----------------

        private void drawBanner() {

            fill(COLOR_PRIMARY);
            canvas.drawRect(0, 0, PAGE_W, 98, shape);

            fill(COLOR_PRIMARY_DARK);
            canvas.drawRect(0, 98, PAGE_W, 102, shape);

            font(24, true, COLOR_WHITE);
            drawText("KSHEERAMITRA", MARGIN, 42, Paint.Align.LEFT);

            font(10.5f, false, COLOR_LIGHT_ON_PRIMARY);
            drawText(
                    "Smart Dairy Management System",
                    MARGIN,
                    58,
                    Paint.Align.LEFT
            );

            font(14, true, COLOR_WHITE);
            drawText(reportTitle, MARGIN, 82, Paint.Align.LEFT);

            font(8.5f, true, COLOR_LIGHT_ON_PRIMARY);
            drawText(
                    "REPORT PERIOD",
                    PAGE_W - MARGIN,
                    38,
                    Paint.Align.RIGHT
            );

            font(12, true, COLOR_WHITE);
            drawText(periodText(), PAGE_W - MARGIN, 55, Paint.Align.RIGHT);

            font(8.5f, false, COLOR_LIGHT_ON_PRIMARY);
            drawText(
                    "Generated: " + generatedOn,
                    PAGE_W - MARGIN,
                    80,
                    Paint.Align.RIGHT
            );

            y = 102 + 22;
        }

        private void drawSlimHeader() {

            fill(COLOR_PRIMARY);
            canvas.drawRect(0, 0, PAGE_W, 30, shape);

            font(11, true, COLOR_WHITE);
            drawText("KSHEERAMITRA", MARGIN, 20, Paint.Align.LEFT);

            font(9, false, COLOR_LIGHT_ON_PRIMARY);
            drawText(
                    reportTitle + "  |  " + periodText(),
                    PAGE_W - MARGIN,
                    20,
                    Paint.Align.RIGHT
            );

            y = 30 + 24;
        }

        private void drawFooter() {

            float lineY = PAGE_H - 36f;

            stroke(COLOR_BORDER, 0.8f);
            canvas.drawLine(MARGIN, lineY, PAGE_W - MARGIN, lineY, shape);

            font(8, false, COLOR_MUTED);

            drawText(
                    "KsheeraMitra  |  Smart Dairy Management System",
                    MARGIN,
                    PAGE_H - 22f,
                    Paint.Align.LEFT
            );

            drawText(
                    "Page " + pageNumber,
                    PAGE_W - MARGIN,
                    PAGE_H - 22f,
                    Paint.Align.RIGHT
            );

            fill(COLOR_WHITE);
        }

        // ---------------- farmer details box ----------------

        void farmerInfo(String id, String name, String village, String phone) {

            float height = 50f;

            ensureSpace(height + 20);

            RectF box = new RectF(MARGIN, y, MARGIN + CONTENT_W, y + height);

            fill(COLOR_TINT);
            canvas.drawRoundRect(box, 8, 8, shape);

            stroke(COLOR_BORDER, 0.9f);
            canvas.drawRoundRect(box, 8, 8, shape);

            String[] labels = {"FARMER ID", "NAME", "VILLAGE", "PHONE"};
            String[] values = {id, name, village, phone};

            float colW = CONTENT_W / 4f;

            for (int i = 0; i < 4; i++) {

                float left = MARGIN + i * colW + 12;

                font(8.5f, true, COLOR_MUTED);
                drawText(labels[i], left, y + 19, Paint.Align.LEFT);

                font(11, true, COLOR_PRIMARY_DARK);
                drawText(fit(values[i], colW - 20), left, y + 37, Paint.Align.LEFT);
            }

            fill(COLOR_WHITE);

            y += height + 24;
        }

        // ---------------- summary cards ----------------

        void summaryCards(
                String milk,
                String farmerCount,
                String qualityCount,
                String payment) {

            summaryCards(
                    new String[]{
                            "TOTAL MILK", "FARMERS", "QUALITY TESTS", "TOTAL PAYMENT"
                    },
                    new String[]{milk, farmerCount, qualityCount, payment},
                    new int[]{
                            COLOR_BLUE, COLOR_PRIMARY, COLOR_TEAL, COLOR_GREEN
                    }
            );
        }

        void summaryCards(String[] labels, String[] values, int[] accents) {

            float gap = 10f;
            float width = (CONTENT_W - 3 * gap) / 4f;
            float height = 62f;

            ensureSpace(height + 20);

            for (int i = 0; i < 4; i++) {

                float left = MARGIN + i * (width + gap);

                RectF box = new RectF(left, y, left + width, y + height);

                fill(COLOR_WHITE);
                canvas.drawRoundRect(box, 8, 8, shape);

                stroke(COLOR_BORDER, 0.9f);
                canvas.drawRoundRect(box, 8, 8, shape);

                fill(accents[i]);
                canvas.drawRect(left, y + 9, left + 4, y + height - 9, shape);

                font(8.5f, true, COLOR_MUTED);
                drawText(labels[i], left + 14, y + 22, Paint.Align.LEFT);

                float size = 16f;

                font(size, true, COLOR_TEXT);

                while (text.measureText(values[i]) > width - 24 && size > 9) {
                    size -= 1;
                    text.setTextSize(size);
                }

                drawText(values[i], left + 14, y + 47, Paint.Align.LEFT);
            }

            y += height + 24;
        }

        // ---------------- section title ----------------

        void sectionTitle(String title) {

            ensureSpace(26 + HEADER_H + ROW_H * 2);

            fill(COLOR_PRIMARY);
            canvas.drawRect(MARGIN, y, MARGIN + 4, y + 16, shape);

            font(13, true, COLOR_PRIMARY_DARK);
            drawText(title, MARGIN + 12, y + 12.5f, Paint.Align.LEFT);

            y += 16 + 10;
        }

        // ---------------- table ----------------

        void table(
                String[] headers,
                float[] widths,
                int[] aligns,
                List<String[]> rows,
                String[] totals) {

            int statusCol = -1;

            for (int i = 0; i < headers.length; i++) {
                if ("Status".equals(headers[i])) {
                    statusCol = i;
                }
            }

            ensureSpace(HEADER_H + ROW_H * 2);

            float segmentTop = y;

            headerRow(headers, widths, aligns);

            if (rows.isEmpty()) {

                messageRow("No records found for this period");

                endSegment(widths, segmentTop, false);

                y += 22;

                return;
            }

            for (int n = 0; n < rows.size(); n++) {

                if (y + ROW_H > CONTENT_BOTTOM) {

                    endSegment(widths, segmentTop, true);

                    startPage();

                    segmentTop = y;

                    headerRow(headers, widths, aligns);
                }

                dataRow(
                        rows.get(n),
                        widths,
                        aligns,
                        statusCol,
                        n % 2 == 1,
                        false
                );
            }

            if (totals != null) {

                if (y + ROW_H > CONTENT_BOTTOM) {

                    endSegment(widths, segmentTop, true);

                    startPage();

                    segmentTop = y;

                    headerRow(headers, widths, aligns);
                }

                dataRow(totals, widths, aligns, statusCol, false, true);
            }

            endSegment(widths, segmentTop, true);

            y += 22;
        }

        private void headerRow(
                String[] headers,
                float[] widths,
                int[] aligns) {

            fill(COLOR_PRIMARY);
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + HEADER_H, shape);

            font(9.5f, true, COLOR_WHITE);

            float x = MARGIN;

            for (int i = 0; i < headers.length; i++) {

                cell(headers[i], x, widths[i], aligns[i], y + 15.5f);

                x += widths[i];
            }

            y += HEADER_H;
        }

        private void dataRow(
                String[] cells,
                float[] widths,
                int[] aligns,
                int statusCol,
                boolean stripe,
                boolean totalsRow) {

            if (totalsRow) {
                fill(COLOR_TINT);
            } else if (stripe) {
                fill(COLOR_STRIPE);
            } else {
                fill(COLOR_WHITE);
            }

            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + ROW_H, shape);

            float x = MARGIN;

            for (int i = 0; i < cells.length; i++) {

                int color = COLOR_TEXT;
                boolean bold = totalsRow;

                if (totalsRow) {

                    color = COLOR_PRIMARY_DARK;

                } else if (i == statusCol) {

                    color = statusColor(cells[i]);
                    bold = true;
                }

                font(9.5f, bold, color);

                cell(cells[i], x, widths[i], aligns[i], y + 13.5f);

                x += widths[i];
            }

            if (totalsRow) {

                stroke(COLOR_PRIMARY, 1.2f);
                canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, shape);

            } else {

                stroke(COLOR_BORDER, 0.5f);
                canvas.drawLine(
                        MARGIN,
                        y + ROW_H,
                        MARGIN + CONTENT_W,
                        y + ROW_H,
                        shape
                );
            }

            y += ROW_H;
        }

        private void messageRow(String message) {

            fill(COLOR_WHITE);
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + ROW_H + 6, shape);

            font(9.5f, false, COLOR_MUTED);
            drawText(message, MARGIN + 8, y + 16, Paint.Align.LEFT);

            y += ROW_H + 6;
        }

        private void endSegment(float[] widths, float segmentTop, boolean grid) {

            stroke(COLOR_BORDER, 0.6f);

            if (grid) {

                float x = MARGIN;

                for (int i = 0; i < widths.length - 1; i++) {

                    x += widths[i];

                    canvas.drawLine(x, segmentTop + HEADER_H, x, y, shape);
                }
            }

            stroke(COLOR_BORDER, 1f);
            canvas.drawRect(MARGIN, segmentTop, MARGIN + CONTENT_W, y, shape);

            fill(COLOR_WHITE);
        }

        private void cell(
                String value,
                float left,
                float width,
                int align,
                float baseline) {

            String shown = fit(value == null ? "" : value, width - 12);

            if (align == ALIGN_RIGHT) {

                drawText(shown, left + width - 6, baseline, Paint.Align.RIGHT);

            } else if (align == ALIGN_CENTER) {

                drawText(shown, left + width / 2f, baseline, Paint.Align.CENTER);

            } else {

                drawText(shown, left + 6, baseline, Paint.Align.LEFT);
            }
        }

        private int statusColor(String status) {

            if ("PAID".equalsIgnoreCase(status)) {
                return COLOR_GREEN;
            }

            if ("PARTIAL".equalsIgnoreCase(status)) {
                return COLOR_ORANGE;
            }

            return COLOR_RED;
        }

        // ---------------- signatures ----------------

        void signatures() {

            ensureSpace(80);

            y += 34;

            stroke(COLOR_MUTED, 0.8f);

            canvas.drawLine(MARGIN, y, MARGIN + 150, y, shape);

            canvas.drawLine(
                    PAGE_W - MARGIN - 150,
                    y,
                    PAGE_W - MARGIN,
                    y,
                    shape
            );

            font(9, false, COLOR_MUTED);

            drawText("Prepared by", MARGIN, y + 13, Paint.Align.LEFT);

            drawText(
                    "Authorized Signature",
                    PAGE_W - MARGIN,
                    y + 13,
                    Paint.Align.RIGHT
            );

            fill(COLOR_WHITE);

            y += 20;
        }
    }

    // =========================================================
    // PDF TEXT HELPERS
    // =========================================================

    private String periodText() {

        if (fromDate.equals(toDate)) {
            return displayDate(fromDate);
        }

        return displayDate(fromDate) + "  -  " + displayDate(toDate);
    }

    private String displayDate(String date) {

        try {

            return new SimpleDateFormat("dd MMM yyyy", Locale.US)
                    .format(dateFormat.parse(date));

        } catch (Exception e) {

            return date;
        }
    }

    private String averageText(double weightedSum, double quantity) {

        if (quantity <= 0.0001) {
            return "-";
        }

        return String.format(Locale.US, "%.2f", weightedSum / quantity);
    }

    private String formatAmount(double value) {

        return String.format(Locale.US, "%,.2f", value);
    }

    // =========================================================
    // OPEN PDF
    // =========================================================

    private void openPdf(File file) {

        Uri uri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        file
                );

        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(uri, "application/pdf");

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "No PDF viewer found on this device.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // RESET
    // =========================================================

    private void resetValues() {

        txtTotalMilk.setText("0.00 L");
        txtTotalFarmers.setText("0");
        txtQualityTests.setText("0");
        txtTotalPayment.setText("₹0.00");

        showGraphNoData();
    }

    // =========================================================
    // NO GRAPH DATA
    // =========================================================

    private void showGraphNoData() {

        if (chartView != null) {

            chartView.setValues(
                    new float[]{0},
                    new String[]{"No Data"}
            );
        }
    }

    // =========================================================
    // MONEY
    // =========================================================

    private String formatMoney(double amount) {

        return "₹" + String.format(Locale.US, "%.2f", amount);
    }

    // =========================================================
    // GET STRING
    // =========================================================

    private String getString(DocumentSnapshot document, String field) {

        Object value = document.get(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    // =========================================================
    // GET DOUBLE
    // =========================================================

    private double getDouble(DocumentSnapshot document, String field) {

        Object value = document.get(field);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value == null) {
            return 0;
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    // GET LONG
    // =========================================================

    private long getLong(DocumentSnapshot document, String field) {

        Object value = document.get(field);

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value == null) {
            return 0;
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    // FIREBASE ERROR
    // =========================================================

    private void showFirebaseError(String section, Exception error) {

        String message = error.getMessage();

        if (message == null) {
            message = "Unknown Firebase error";
        }

        Toast.makeText(
                this,
                section + " error:\n" + message,
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // REMOVE LISTENERS
    // =========================================================

    private void removeListeners() {

        if (farmerListener != null) {
            farmerListener.remove();
            farmerListener = null;
        }

        if (milkListener != null) {
            milkListener.remove();
            milkListener = null;
        }

        if (qualityListener != null) {
            qualityListener.remove();
            qualityListener = null;
        }

        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        removeListeners();

        super.onDestroy();
    }
}