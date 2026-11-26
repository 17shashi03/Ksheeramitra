package com.example.ksheeramitra;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FarmerActivity extends AppCompatActivity {

    private static final String TAG = "FarmerActivity";

    private static final String STATE_CAMERA_URI = "state_camera_uri";
    private static final String STATE_PENDING_URI = "state_pending_uri";

    private EditText etFarmerName;
    private EditText etPhone;
    private EditText etVillage;

    private RadioGroup rgGender;
    private RadioButton rbMale;
    private RadioButton rbFemale;

    private Spinner spinnerMemberType;

    private TextView tvFarmerIdPreview;

    private Button btnAddFarmer;

    // ---- PHOTO VIEWS ----
    private ImageView ivFarmerPhoto;
    private ImageView ivFarmerPhotoPlaceholder;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String USERS_COLLECTION = "users";
    private static final String FARMERS_COLLECTION = "farmers";

    // Values saved in Firestore (always English, index 0 = placeholder)
    private static final String[] MEMBER_TYPE_VALUES = {
            "",
            "Regular Member",
            "Associate Member"
    };

    private boolean editMode = false;

    private String editFarmerId = "";

    // ---- SMS ----
    private ActivityResultLauncher<String> smsPermissionLauncher;
    private Runnable pendingSend = null;

    // ---- PHOTO ----
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private Uri cameraOutputUri;             // file the camera writes into
    private Uri pendingPhotoUri = null;      // newly chosen photo, saved with the farmer
    private String existingPhotoBase64 = ""; // photo already stored for this farmer

    private interface PhotoCallback {
        void onResult(String photoBase64);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_farmer);

        mAuth = FirebaseAuth.getInstance();
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

        registerPhotoLaunchers();

        initializeViews();

        setupPhotoClicks();

        setupMemberTypeSpinner();

        restorePhotoState(savedInstanceState);

        checkEditMode();

        btnAddFarmer.setOnClickListener(v -> {

            if (editMode) {
                updateFarmer();
            } else {
                createFarmer();
            }
        });
    }

    // =========================================================
    // KEEP PHOTO IF ANDROID KILLS THE APP WHILE CAMERA IS OPEN
    // =========================================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        if (cameraOutputUri != null) {
            outState.putString(STATE_CAMERA_URI, cameraOutputUri.toString());
        }

        if (pendingPhotoUri != null) {
            outState.putString(STATE_PENDING_URI, pendingPhotoUri.toString());
        }
    }

    private void restorePhotoState(Bundle state) {

        if (state == null) {
            return;
        }

        String cam = state.getString(STATE_CAMERA_URI);

        if (cam != null) {
            cameraOutputUri = Uri.parse(cam);
        }

        String pending = state.getString(STATE_PENDING_URI);

        if (pending != null) {
            showPickedPhoto(Uri.parse(pending));
        }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeViews() {

        etFarmerName =
                findViewById(R.id.etFarmerName);

        etPhone =
                findViewById(R.id.etPhone);

        etVillage =
                findViewById(R.id.etVillage);

        rgGender =
                findViewById(R.id.rgGender);

        rbMale =
                findViewById(R.id.rbMale);

        rbFemale =
                findViewById(R.id.rbFemale);

        // Set from resources so they follow the app language
        rbMale.setText(R.string.gender_male);
        rbFemale.setText(R.string.gender_female);

        spinnerMemberType =
                findViewById(R.id.spinnerMemberType);

        tvFarmerIdPreview =
                findViewById(R.id.tvFarmerIdPreview);

        btnAddFarmer =
                findViewById(R.id.btnAddFarmer);

        ivFarmerPhoto =
                findViewById(R.id.ivFarmerPhoto);

        ivFarmerPhotoPlaceholder =
                findViewById(R.id.ivFarmerPhotoPlaceholder);

        btnAddFarmer.setText(R.string.add_farmer_btn);

        tvFarmerIdPreview.setText(
                R.string.farmer_id_will_be_generated
        );
    }

    // =========================================================
    // SAFE STRING LOOKUP (uses strings.xml when present, else English fallback)
    // =========================================================

    private String str(String name, String fallback) {

        int id = getResources()
                .getIdentifier(name, "string", getPackageName());

        return id == 0 ? fallback : getString(id);
    }

    // =========================================================
    // PHOTO: CAMERA / GALLERY
    // =========================================================

    private void registerPhotoLaunchers() {

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraOutputUri != null) {
                        showPickedPhoto(cameraOutputUri);
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        showPickedPhoto(uri);
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(
                                this,
                                str("camera_permission_denied", "Camera permission denied"),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void setupPhotoClicks() {

        View.OnClickListener open = v -> showPhotoOptions();

        findViewById(R.id.cardFarmerPhoto).setOnClickListener(open);
        findViewById(R.id.cardChangeFarmerPhoto).setOnClickListener(open);
    }

    private boolean hasPhoto() {
        return pendingPhotoUri != null
                || !TextUtils.isEmpty(existingPhotoBase64);
    }

    private void showPhotoOptions() {

        final boolean hasPhoto = hasPhoto();

        String[] items = hasPhoto
                ? new String[]{
                str("photo_take", "Take photo"),
                str("photo_gallery", "Choose from gallery"),
                str("photo_remove", "Remove photo")}
                : new String[]{
                str("photo_take", "Take photo"),
                str("photo_gallery", "Choose from gallery")};

        new MaterialAlertDialogBuilder(this)
                .setTitle(str("photo_title", "Farmer photo"))
                .setItems(items, (dialog, which) -> {

                    if (which == 0) {

                        if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            launchCamera();
                        } else {
                            cameraPermissionLauncher.launch(
                                    Manifest.permission.CAMERA
                            );
                        }

                    } else if (which == 1) {

                        pickImageLauncher.launch("image/*");

                    } else {

                        removePhoto();
                    }
                })
                .show();
    }

    private void launchCamera() {

        try {

            File dir = new File(getCacheDir(), "images");

            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Cannot create cache dir");
            }

            File file = File.createTempFile("farmer_", ".jpg", dir);

            cameraOutputUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            takePictureLauncher.launch(cameraOutputUri);

        } catch (IOException | IllegalArgumentException e) {

            // IllegalArgumentException = file_paths.xml missing or wrong
            Log.e(TAG, "Could not open camera", e);

            Toast.makeText(
                    this,
                    str("camera_open_failed", "Could not open camera"),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showPickedPhoto(Uri uri) {

        pendingPhotoUri = uri;

        Glide.with(this).load(uri).centerCrop().into(ivFarmerPhoto);

        ivFarmerPhoto.setVisibility(View.VISIBLE);
        ivFarmerPhotoPlaceholder.setVisibility(View.GONE);
    }

    private void showExistingPhoto(String base64) {

        byte[] bytes = PhotoUtil.decode(base64);

        if (bytes == null) {
            return;
        }

        Glide.with(this).load(bytes).centerCrop().into(ivFarmerPhoto);

        ivFarmerPhoto.setVisibility(View.VISIBLE);
        ivFarmerPhotoPlaceholder.setVisibility(View.GONE);
    }

    private void removePhoto() {

        pendingPhotoUri = null;
        existingPhotoBase64 = "";

        ivFarmerPhoto.setImageDrawable(null);
        ivFarmerPhoto.setVisibility(View.GONE);
        ivFarmerPhotoPlaceholder.setVisibility(View.VISIBLE);
    }

    /**
     * Shrinks the newly chosen photo (max 400px, under about 150 KB) and gives it
     * back as text, to be saved inside the farmer's Firestore document.
     * When there is no new photo, the existing one is returned.
     * If shrinking fails, the farmer is still saved (with the old photo, if any).
     */
    private void preparePhoto(PhotoCallback callback) {

        if (pendingPhotoUri == null) {
            callback.onResult(existingPhotoBase64);
            return;
        }

        btnAddFarmer.setText(str("uploading_photo", "Saving photo..."));

        PhotoUtil.encode(this, pendingPhotoUri, (base64, error) -> {

            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (base64 == null) {

                Toast.makeText(
                        this,
                        str("photo_upload_failed", "Photo failed, farmer saved without photo")
                                + " (" + error + ")",
                        Toast.LENGTH_LONG
                ).show();

                callback.onResult(existingPhotoBase64);
                return;
            }

            pendingPhotoUri = null;
            existingPhotoBase64 = base64;

            callback.onResult(base64);
        });
    }

    // =========================================================
    // GENDER HELPERS
    // =========================================================

    private String getSelectedGender() {

        int id = rgGender.getCheckedRadioButtonId();

        if (id == R.id.rbMale) {
            return "Male";
        }

        if (id == R.id.rbFemale) {
            return "Female";
        }

        return "";
    }

    private void setGender(String gender) {

        if ("Male".equals(gender)) {

            rgGender.check(R.id.rbMale);

        } else if ("Female".equals(gender)) {

            rgGender.check(R.id.rbFemale);
        }
    }

    // =========================================================
    // SPINNER
    // =========================================================

    private void setupMemberTypeSpinner() {

        // Display text (translated). Same order as MEMBER_TYPE_VALUES.
        String[] memberTypeLabels = {
                getString(R.string.select_member_type),
                getString(R.string.member_regular),
                getString(R.string.member_associate)
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        memberTypeLabels
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMemberType.setAdapter(adapter);
    }

    private String getSelectedMemberType() {

        int position =
                spinnerMemberType.getSelectedItemPosition();

        if (position < 0
                || position >= MEMBER_TYPE_VALUES.length) {
            return "";
        }

        return MEMBER_TYPE_VALUES[position];
    }

    private void setMemberType(String memberType) {

        for (int i = 1; i < MEMBER_TYPE_VALUES.length; i++) {

            if (MEMBER_TYPE_VALUES[i].equals(memberType)) {

                spinnerMemberType.setSelection(i);

                break;
            }
        }
    }

    // =========================================================
    // CHECK EDIT MODE
    // =========================================================

    private void checkEditMode() {

        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        String farmerId =
                intent.getStringExtra("farmer_id");

        if (TextUtils.isEmpty(farmerId)) {
            return;
        }

        editMode = true;

        editFarmerId = farmerId;

        String name =
                intent.getStringExtra("farmer_name");

        String village =
                intent.getStringExtra("farmer_village");

        String phone =
                intent.getStringExtra("farmer_phone");

        etFarmerName.setText(
                name == null ? "" : name
        );

        etVillage.setText(
                village == null ? "" : village
        );

        etPhone.setText(
                phone == null ? "" : phone
        );

        tvFarmerIdPreview.setText(
                getString(R.string.farmer_id_prefix, editFarmerId)
        );

        btnAddFarmer.setText(
                R.string.update_farmer_btn
        );

        // Member type, gender and photo are loaded from Firestore below.
        loadFarmerForEdit();
    }

    // =========================================================
    // LOAD FARMER FOR EDIT
    // =========================================================

    private void loadFarmerForEdit() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .collection(FARMERS_COLLECTION)
                .document(editFarmerId)
                .get()
                .addOnSuccessListener((DocumentSnapshot document) -> {

                    if (!document.exists()) {
                        return;
                    }

                    // Older farmers may not have gender
                    setGender(
                            document.getString("gender")
                    );

                    setMemberType(
                            document.getString("memberType")
                    );

                    // Older farmers may not have a photo
                    String photo =
                            document.getString("photoBase64");

                    if (!TextUtils.isEmpty(photo)
                            && pendingPhotoUri == null
                            && TextUtils.isEmpty(existingPhotoBase64)) {

                        existingPhotoBase64 = photo;

                        showExistingPhoto(photo);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to load farmer", e));
    }

    // =========================================================
    // CREATE FARMER
    // =========================================================

    private void createFarmer() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    R.string.please_login_first,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String name =
                etFarmerName.getText()
                        .toString()
                        .trim();

        String phone =
                normalizePhone(
                        etPhone.getText()
                                .toString()
                                .trim()
                );

        String village =
                etVillage.getText()
                        .toString()
                        .trim();

        String gender =
                getSelectedGender();

        String memberType =
                getSelectedMemberType();

        if (!validate(
                name,
                phone,
                village,
                gender,
                memberType
        )) {
            return;
        }

        btnAddFarmer.setEnabled(false);

        btnAddFarmer.setText(
                R.string.generating_id
        );

        generateFarmerId(
                user.getUid(),
                name,
                phone,
                village,
                gender,
                memberType
        );
    }

    // =========================================================
    // GENERATE FARMER ID
    // =========================================================

    private void generateFarmerId(
            String uid,
            String name,
            String phone,
            String village,
            String gender,
            String memberType
    ) {

        DocumentReference userRef =
                db.collection(USERS_COLLECTION)
                        .document(uid);

        db.runTransaction(transaction -> {

            DocumentSnapshot snapshot =
                    transaction.get(userRef);

            Long counter =
                    snapshot.getLong("farmerCounter");

            long nextNumber =
                    counter == null
                            ? 1
                            : counter + 1;

            Map<String, Object> counterData =
                    new HashMap<>();

            counterData.put(
                    "farmerCounter",
                    nextNumber
            );

            transaction.set(
                    userRef,
                    counterData,
                    SetOptions.merge()
            );

            return nextNumber;

        }).addOnSuccessListener(number -> {

            String farmerId =
                    String.format(
                            "KM-%06d",
                            number
                    );

            // Shrink the photo (if any) first, then save the farmer
            preparePhoto(photoBase64 ->
                    saveFarmer(
                            uid,
                            farmerId,
                            name,
                            phone,
                            village,
                            gender,
                            memberType,
                            photoBase64
                    )
            );

        }).addOnFailureListener(e -> {

            Log.e(TAG, "Unable to generate id", e);

            resetButton();

            Toast.makeText(
                    this,
                    R.string.unable_generate_id,
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    // =========================================================
    // SAVE FARMER
    // =========================================================

    private void saveFarmer(
            String uid,
            String farmerId,
            String name,
            String phone,
            String village,
            String gender,
            String memberType,
            String photoBase64
    ) {

        Map<String, Object> farmer =
                new HashMap<>();

        farmer.put("farmerId", farmerId);
        farmer.put("ownerUid", uid);
        farmer.put("name", name);
        farmer.put("phone", phone);
        farmer.put("village", village);
        farmer.put("gender", gender);
        farmer.put("memberType", memberType);
        farmer.put("photoBase64", photoBase64 == null ? "" : photoBase64);
        farmer.put("status", "Active");
        farmer.put("createdAt", System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(FARMERS_COLLECTION)
                .document(farmerId)
                .set(farmer)
                .addOnSuccessListener(unused -> {

                    tvFarmerIdPreview.setText(
                            getString(R.string.farmer_id_prefix, farmerId)
                    );

                    Toast.makeText(
                            this,
                            R.string.farmer_added,
                            Toast.LENGTH_SHORT
                    ).show();

                    // ---- SEND MESSAGE TO THE FARMER'S NUMBER ----
                    sendFarmerMessage(
                            phone,
                            name,
                            farmerId
                    );

                    clearForm();

                    resetButton();

                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Failed to save farmer", e);

                    resetButton();

                    Toast.makeText(
                            this,
                            R.string.failed_save_farmer,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // MESSAGE (via MessageHelper)
    // =========================================================

    /**
     * Sends the registration message through MessageHelper.
     * Asks for SMS permission first when the SMS channel is selected.
     */
    private void sendFarmerMessage(
            String phone,
            String name,
            String farmerId
    ) {

        Runnable send = () -> MessageHelper.sendFarmerRegistered(
                this,
                phone,
                name,
                farmerId
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
    // UPDATE FARMER
    // =========================================================

    private void updateFarmer() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        String name =
                etFarmerName.getText()
                        .toString()
                        .trim();

        String phone =
                normalizePhone(
                        etPhone.getText()
                                .toString()
                                .trim()
                );

        String village =
                etVillage.getText()
                        .toString()
                        .trim();

        String gender =
                getSelectedGender();

        String memberType =
                getSelectedMemberType();

        if (!validate(
                name,
                phone,
                village,
                gender,
                memberType
        )) {
            return;
        }

        btnAddFarmer.setEnabled(false);

        btnAddFarmer.setText(
                R.string.updating
        );

        // Shrink the new photo (if any), then update the farmer
        preparePhoto(photoBase64 -> {

            Map<String, Object> updates =
                    new HashMap<>();

            updates.put("name", name);
            updates.put("phone", phone);
            updates.put("village", village);
            updates.put("gender", gender);
            updates.put("memberType", memberType);
            updates.put("photoBase64", photoBase64 == null ? "" : photoBase64);
            updates.put("updatedAt", System.currentTimeMillis());

            db.collection(USERS_COLLECTION)
                    .document(user.getUid())
                    .collection(FARMERS_COLLECTION)
                    .document(editFarmerId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {

                        Toast.makeText(
                                this,
                                R.string.farmer_updated,
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();

                    })
                    .addOnFailureListener(e -> {

                        Log.e(TAG, "Failed to update farmer", e);

                        resetButton();

                        Toast.makeText(
                                this,
                                R.string.failed_update_farmer,
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean validate(
            String name,
            String phone,
            String village,
            String gender,
            String memberType
    ) {

        if (TextUtils.isEmpty(name)) {

            etFarmerName.setError(
                    getString(R.string.enter_farmer_name)
            );

            etFarmerName.requestFocus();

            return false;
        }

        if (TextUtils.isEmpty(phone)
                || !phone.matches("[0-9]{10}")) {

            etPhone.setError(
                    getString(R.string.err_valid_phone)
            );

            etPhone.requestFocus();

            return false;
        }

        if (TextUtils.isEmpty(village)) {

            etVillage.setError(
                    getString(R.string.enter_village)
            );

            etVillage.requestFocus();

            return false;
        }

        if (TextUtils.isEmpty(gender)) {

            Toast.makeText(
                    this,
                    R.string.select_gender_error,
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (TextUtils.isEmpty(memberType)) {

            Toast.makeText(
                    this,
                    R.string.select_member_type_error,
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    // =========================================================
    // PHONE NORMALIZATION
    // =========================================================

    private String normalizePhone(
            String phone
    ) {

        phone = phone
                .replace(" ", "")
                .replace("-", "");

        if (phone.startsWith("+91")) {

            phone = phone.substring(3);
        }

        if (phone.startsWith("91")
                && phone.length() == 12) {

            phone = phone.substring(2);
        }

        return phone;
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearForm() {

        etFarmerName.setText("");
        etPhone.setText("");
        etVillage.setText("");

        rgGender.clearCheck();

        spinnerMemberType.setSelection(0);

        removePhoto();

        tvFarmerIdPreview.setText(
                R.string.farmer_id_will_be_generated
        );
    }

    // =========================================================
    // RESET BUTTON
    // =========================================================

    private void resetButton() {

        btnAddFarmer.setEnabled(true);

        btnAddFarmer.setText(
                editMode
                        ? R.string.update_farmer_btn
                        : R.string.add_farmer_btn
        );
    }
}