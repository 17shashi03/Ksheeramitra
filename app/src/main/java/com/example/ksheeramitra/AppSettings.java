package com.example.ksheeramitra;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Saves and applies the app settings:
 * app language, dark mode, message channel, message language.
 */
public final class AppSettings {

    public static final String LANG_EN = "en";
    public static final String LANG_KN = "kn";

    public static final String CHANNEL_WHATSAPP = "whatsapp";
    public static final String CHANNEL_SMS = "sms";

    private static final String PREFS = "ksheeramitra_settings";

    private static final String KEY_LANG = "app_language";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_CHANNEL = "msg_channel";
    private static final String KEY_MSG_LANG = "msg_language";
    private static final String KEY_DAIRY = "dairy_name";

    private AppSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // =========================================================
    // APPLY (called once from KsheeraMitraApp)
    // =========================================================

    public static void applyAll(Context context) {

        SharedPreferences p = prefs(context);

        if (p.contains(KEY_DARK)) {

            AppCompatDelegate.setDefaultNightMode(
                    p.getBoolean(KEY_DARK, false)
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );

        } else {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            );
        }

        if (p.contains(KEY_LANG)) {

            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(
                            p.getString(KEY_LANG, LANG_EN)
                    )
            );
        }
    }

    // =========================================================
    // APP LANGUAGE
    // =========================================================

    public static String getLanguage(Context context) {

        String saved = prefs(context).getString(KEY_LANG, null);

        if (saved != null) {
            return saved;
        }

        return LANG_KN.equals(Locale.getDefault().getLanguage())
                ? LANG_KN
                : LANG_EN;
    }

    public static void setLanguage(Context context, String language) {

        prefs(context).edit().putString(KEY_LANG, language).apply();

        // This also recreates the open screens in the new language.
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language)
        );
    }

    // =========================================================
    // DARK MODE
    // =========================================================

    public static void setDark(Context context, boolean dark) {

        prefs(context).edit().putBoolean(KEY_DARK, dark).apply();

        AppCompatDelegate.setDefaultNightMode(
                dark
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // =========================================================
    // MESSAGE CHANNEL
    // =========================================================

    public static String getChannel(Context context) {
        return prefs(context).getString(KEY_CHANNEL, CHANNEL_WHATSAPP);
    }

    public static void setChannel(Context context, String channel) {
        prefs(context).edit().putString(KEY_CHANNEL, channel).apply();
    }

    // =========================================================
    // MESSAGE LANGUAGE
    // =========================================================

    public static String getMessageLanguage(Context context) {
        return prefs(context).getString(KEY_MSG_LANG, getLanguage(context));
    }

    public static void setMessageLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_MSG_LANG, language).apply();
    }

    // =========================================================
    // DAIRY / OWNER NAME (signed at the end of messages)
    // =========================================================

    public static String getDairyName(Context context) {

        String name = prefs(context).getString(KEY_DAIRY, "");

        return name == null || name.trim().isEmpty()
                ? "KsheeraMitra"
                : name;
    }

    public static void setDairyName(Context context, String name) {
        prefs(context).edit().putString(KEY_DAIRY, name).apply();
    }
}