package com.example.ksheeramitra;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2200;

    private static final String PREFS_NAME =
            "KsheeramitraPrefs";

    private static final String KEY_IS_LOGGED_IN =
            "is_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(
                this::navigateNext,
                SPLASH_DELAY
        );
    }

    private void navigateNext() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        boolean isLoggedIn =
                prefs.getBoolean(
                        KEY_IS_LOGGED_IN,
                        false
                );

        Intent intent;

        // ==================================================
        // USER IS LOGGED IN
        // ==================================================

        if (isLoggedIn) {

            intent = new Intent(
                    SplashActivity.this,
                    DashboardActivity.class
            );

        }

        // ==================================================
        // USER IS NOT LOGGED IN
        // ==================================================

        else {

            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );
        }

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    }
}