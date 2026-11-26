package com.example.ksheeramitra;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sends an email through your Google Apps Script (free).
 *
 * Events: "login", "signup", "logout", "profile_created"
 *
 * Usage:
 *   MailNotifier.send(user, "login", null, null);
 *   MailNotifier.send(user, "signup", name, null);
 *   MailNotifier.send(user, "logout", null, () -> { auth.signOut(); ... });
 *
 * 'afterToken' runs (on the main thread) as soon as the login token is read,
 * so for logout you can sign out right after, without waiting for the mail.
 */
public final class MailNotifier {

    private static final String TAG = "MailNotifier";

    private static final String SCRIPT_URL =
            "https://script.google.com/macros/s/AKfycby2WrhpPAwM75moMch2-qy8ZkcDJ_tR2Hg_5eupQtG517KbWwxHJYTFQnjInaj-TPbGKA/exec";

    private MailNotifier() {
    }

    public static void send(FirebaseUser user, String event, String name, Runnable afterToken) {

        if (user == null) {
            if (afterToken != null) afterToken.run();
            return;
        }

        user.getIdToken(false).addOnCompleteListener(task -> {

            if (task.isSuccessful() && task.getResult() != null) {

                final String token = task.getResult().getToken();

                new Thread(() -> post(token, event, name)).start();

            } else {

                Log.e(TAG, "Could not get token", task.getException());
            }

            if (afterToken != null) {
                afterToken.run();
            }
        });
    }

    private static void post(String token, String event, String name) {

        HttpURLConnection conn = null;

        try {

            JSONObject json = new JSONObject();
            json.put("idToken", token);
            json.put("event", event);
            json.put("name", name == null ? "" : name);

            byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) new URL(SCRIPT_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();

            // The script answers "ok" when the mail was sent
            String reply = "";
            try (InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                if (in != null) {
                    java.util.Scanner s = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
                    reply = s.hasNext() ? s.next() : "";
                }
            }

            Log.d(TAG, event + " mail: code " + code + ", reply: " + reply);

        } catch (Exception e) {

            Log.e(TAG, "Mail request failed", e);

        } finally {

            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}