package com.example.ksheeramitra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.LocaleListCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private static final String PREFS = "ksheeramitra_prefs";
    private static final String KEY_THEME_DARK = "theme_dark";
    private static final String KEY_LANG = "app_lang"; // "en" or "kn"

    private static final String STATE_CAMERA_URI = "state_camera_uri";
    private static final String STATE_PENDING_URI = "state_pending_uri";

    // Views
    private ImageView ivProfilePhoto;
    private TextView tvAvatarLetter;
    private TextInputEditText etName, etPhone, etEmail;
    private RadioGroup rgGender;
    private MaterialButtonToggleGroup toggleLanguage, toggleTheme;
    private MaterialButton btnSaveProfile, btnLogout;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Photo state
    private Uri cameraOutputUri;      // file the camera writes into
    private Uri pendingPhotoUri;      // newly chosen photo, saved when you tap Save
    private String savedPhotoBase64;  // photo already saved in Firestore
    private boolean profileExisted = false; // true once a profile name is saved (for the 'profile created' mail)

    // ---------- Activity result launchers (must be created before onStart) ----------

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraOutputUri != null) {
                    showPhoto(cameraOutputUri);
                }
            });

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    showPhoto(uri);
                }
            });

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    // ---------- Lifecycle ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupPhotoClicks();
        setupSettingsToggles();
        loadProfile();

        // Restore photo state if Android killed the app while the camera was open
        if (savedInstanceState != null) {
            String cam = savedInstanceState.getString(STATE_CAMERA_URI);
            if (cam != null) {
                cameraOutputUri = Uri.parse(cam);
            }
            String pending = savedInstanceState.getString(STATE_PENDING_URI);
            if (pending != null) {
                showPhoto(Uri.parse(pending));
            }
        }

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

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

    private void bindViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        rgGender = findViewById(R.id.rgGender);
        toggleLanguage = findViewById(R.id.toggleLanguage);
        toggleTheme = findViewById(R.id.toggleTheme);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }

    // ---------- Photo: camera / gallery ----------

    private void setupPhotoClicks() {
        View.OnClickListener open = v -> showPhotoOptions();
        findViewById(R.id.cardAvatar).setOnClickListener(open);
        findViewById(R.id.cardChangePhoto).setOnClickListener(open);
    }

    private void showPhotoOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Profile photo")
                .setItems(new String[]{"Take photo", "Choose from gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            launchCamera();
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        pickImage.launch("image/*");
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
            File file = File.createTempFile("profile_", ".jpg", dir);
            cameraOutputUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);
            takePicture.launch(cameraOutputUri);
        } catch (IOException | IllegalArgumentException e) {
            // IllegalArgumentException = file_paths.xml missing or wrong
            Log.e(TAG, "Could not open camera", e);
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhoto(Uri uri) {
        pendingPhotoUri = uri;
        Glide.with(this).load(uri).centerCrop().into(ivProfilePhoto);
        ivProfilePhoto.setVisibility(View.VISIBLE);
        tvAvatarLetter.setVisibility(View.GONE);
    }

    private void showSavedPhoto(String base64) {
        byte[] bytes = PhotoUtil.decode(base64);
        if (bytes == null) {
            return;
        }
        Glide.with(this).load(bytes).centerCrop().into(ivProfilePhoto);
        ivProfilePhoto.setVisibility(View.VISIBLE);
        tvAvatarLetter.setVisibility(View.GONE);
    }

    // ---------- Load profile ----------

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        // Prefill from auth while Firestore loads
        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        return;
                    }
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String email = doc.getString("email");
                    String gender = doc.getString("gender");
                    String photo = doc.getString("photoBase64");

                    if (name != null) {
                        etName.setText(name);
                        if (!name.isEmpty()) {
                            tvAvatarLetter.setText(name.substring(0, 1).toUpperCase());
                            profileExisted = true;
                        }
                    }
                    if (phone != null) etPhone.setText(phone);
                    if (email != null) etEmail.setText(email);

                    if ("Male".equalsIgnoreCase(gender)) {
                        rgGender.check(R.id.rbMale);
                    } else if ("Female".equalsIgnoreCase(gender)) {
                        rgGender.check(R.id.rbFemale);
                    }

                    if (photo != null && !photo.isEmpty()) {
                        savedPhotoBase64 = photo;
                        // Do not overwrite a photo the user just picked
                        if (pendingPhotoUri == null) {
                            showSavedPhoto(photo);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load profile", e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    // ---------- Save profile ----------

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = textOf(etName);
        String phone = textOf(etPhone);
        String email = textOf(etEmail);

        if (name.isEmpty()) {
            etName.setError("Enter your name");
            etName.requestFocus();
            return;
        }
        if (phone.length() < 10) {
            etPhone.setError("Enter a valid phone number");
            etPhone.requestFocus();
            return;
        }

        setSaving(true);

        if (pendingPhotoUri != null) {

            // Shrink the new photo, then save everything together
            PhotoUtil.encode(this, pendingPhotoUri, (base64, error) -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (base64 == null) {
                    setSaving(false);
                    Toast.makeText(this,
                            "Could not read the photo: " + error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                savedPhotoBase64 = base64;
                pendingPhotoUri = null;
                writeProfile(user, name, phone, email, savedPhotoBase64);
            });

        } else {
            writeProfile(user, name, phone, email, savedPhotoBase64);
        }
    }

    private void writeProfile(FirebaseUser user, String name, String phone,
                              String email, String photoBase64) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("email", email);
        data.put("gender", selectedGender());
        if (photoBase64 != null) {
            data.put("photoBase64", photoBase64);
        }

        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setSaving(false);
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();

                    // First time the profile is saved -> send the "profile created" mail
                    if (!profileExisted) {
                        profileExisted = true;
                        MailNotifier.send(user, "profile_created", name, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save profile", e);
                    setSaving(false);
                    Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setSaving(boolean saving) {
        btnSaveProfile.setEnabled(!saving);
        btnSaveProfile.setText(saving ? "Saving..." : getString(R.string.btn_save_profile));
    }

    private String selectedGender() {
        int id = rgGender.getCheckedRadioButtonId();
        if (id == R.id.rbMale) return "Male";
        if (id == R.id.rbFemale) return "Female";
        return "";
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // ---------- Language + Theme ----------

    private void setupSettingsToggles() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Set initial state BEFORE adding listeners so they don't fire on setup
        boolean dark = prefs.getBoolean(KEY_THEME_DARK, false);
        toggleTheme.check(dark ? R.id.btnThemeDark : R.id.btnThemeLight);

        String lang = prefs.getString(KEY_LANG, "en");
        toggleLanguage.check("kn".equals(lang) ? R.id.btnLangKannada : R.id.btnLangEnglish);

        toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean wantDark = checkedId == R.id.btnThemeDark;
            if (wantDark == prefs.getBoolean(KEY_THEME_DARK, false)) return;

            prefs.edit().putBoolean(KEY_THEME_DARK, wantDark).apply();
            AppCompatDelegate.setDefaultNightMode(wantDark
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        toggleLanguage.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String wantLang = checkedId == R.id.btnLangKannada ? "kn" : "en";
            if (wantLang.equals(prefs.getString(KEY_LANG, "en"))) return;

            prefs.edit().putString(KEY_LANG, wantLang).apply();
            // Requires androidx.appcompat 1.6.0 or newer
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(wantLang));
        });
    }

    // ---------- Logout ----------

    private void logout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Do you want to logout?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout", (dialog, which) -> {

                    Runnable finishLogout = () -> {
                        auth.signOut();
                        Intent i = new Intent(this, LoginActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    };

                    // The mail must be requested before signOut()
                    MailNotifier.send(auth.getCurrentUser(), "logout", textOf(etName), finishLogout);
                })
                .show();
    }
}