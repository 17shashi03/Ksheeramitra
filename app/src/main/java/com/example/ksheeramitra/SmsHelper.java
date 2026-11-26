package com.example.ksheeramitra;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

/**
 * Shared SMS sender. Sends from this phone's SIM card.
 * Needs <uses-permission android:name="android.permission.SEND_SMS" /> in AndroidManifest.xml
 */
public class SmsHelper {

    private SmsHelper() {
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Sends the SMS. Returns true if the message was handed to the phone's SMS system.
     * Phone must be a 10 digit Indian number (+91 is added automatically).
     */
    public static boolean send(Context context, String phone, String message) {

        if (phone == null || message == null) {
            return false;
        }

        String digits = phone.replaceAll("\\D", "");

        if (digits.length() < 10) {
            return false;
        }

        digits = digits.substring(digits.length() - 10);

        try {

            SmsManager smsManager =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            ? context.getSystemService(SmsManager.class)
                            : SmsManager.getDefault();

            // divideMessage handles long text and Kannada (Unicode) text
            ArrayList<String> parts = smsManager.divideMessage(message);

            smsManager.sendMultipartTextMessage(
                    "+91" + digits,
                    null,
                    parts,
                    null,
                    null
            );

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}