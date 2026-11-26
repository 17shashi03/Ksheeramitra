package com.example.ksheeramitra;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Sends the farmer message.
 *
 * SMS channel      -> sent automatically from the phone's SIM (needs SEND_SMS permission).
 *                     If permission is not granted, falls back to opening the SMS app.
 * WhatsApp channel -> opens WhatsApp with the message ready (user taps Send;
 *                     WhatsApp cannot be sent automatically).
 *
 * Message language follows the app language (AppSettings.setMessageLanguage to override).
 *
 * This class also contains the SMS permission check and the direct SIM sender,
 * so SmsHelper.java is no longer needed.
 */
public final class MessageHelper {

    private MessageHelper() {
    }

    // =========================================================
    // PUBLIC: milk collection message
    // =========================================================

    public static void sendCollection(
            Context context,
            String phone,
            String farmerName,
            String date,
            String shift,
            double quantity,
            double fat,
            double snf,
            double amount) {

        Context c = localized(context);

        String message = c.getString(
                R.string.msg_collection,
                farmerName,
                date,
                shift,
                String.format(Locale.US, "%.2f", quantity),
                String.format(Locale.US, "%.2f", fat),
                String.format(Locale.US, "%.2f", snf),
                String.format(Locale.US, "%,.2f", amount),
                AppSettings.getDairyName(context)
        );

        send(context, phone, message);
    }

    // =========================================================
    // PUBLIC: payment message
    // =========================================================

    public static void sendPayment(
            Context context,
            String phone,
            String farmerName,
            String date,
            double total,
            double paid,
            double balance) {

        Context c = localized(context);

        String message = c.getString(
                R.string.msg_payment,
                farmerName,
                date,
                String.format(Locale.US, "%,.2f", total),
                String.format(Locale.US, "%,.2f", paid),
                String.format(Locale.US, "%,.2f", balance),
                AppSettings.getDairyName(context)
        );

        send(context, phone, message);
    }

    // =========================================================
    // PUBLIC: quality test message
    // =========================================================

    public static void sendQuality(
            Context context,
            String phone,
            String farmerName,
            String date,
            String milkType,
            double fat,
            double snf,
            double temperature,
            String qualityStatus) {

        Context c = localized(context);

        String message = c.getString(
                R.string.msg_quality,
                farmerName,
                date,
                milkType,
                String.format(Locale.US, "%.2f", fat),
                String.format(Locale.US, "%.2f", snf),
                String.format(Locale.US, "%.1f", temperature),
                qualityStatus,
                AppSettings.getDairyName(context)
        );

        send(context, phone, message);
    }

    // =========================================================
    // PUBLIC: farmer registered message
    // =========================================================

    public static void sendFarmerRegistered(
            Context context,
            String phone,
            String farmerName,
            String farmerId) {

        Context c = localized(context);

        String message = c.getString(
                R.string.sms_farmer_registered,
                farmerName,
                farmerId,
                AppSettings.getDairyName(context)
        );

        send(context, phone, message);
    }

    // =========================================================
    // PUBLIC: SMS permission helpers
    // =========================================================

    public static boolean hasSmsPermission(Context context) {

        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * True when the SMS channel is selected but the app does not yet have
     * permission. The screen should request SEND_SMS, then call the send method.
     */
    public static boolean needsSmsPermission(Context context) {

        return AppSettings.CHANNEL_SMS.equals(AppSettings.getChannel(context))
                && !hasSmsPermission(context);
    }

    // =========================================================
    // SEND
    // =========================================================

    public static void send(Context context, String phone, String message) {

        String number = normalizePhone(phone);

        if (number.isEmpty()) {

            Toast.makeText(
                    context,
                    R.string.msg_need_phone,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (AppSettings.CHANNEL_SMS.equals(AppSettings.getChannel(context))) {
            sendSmsAuto(context, number, message);
        } else {
            openWhatsApp(context, number, message);
        }
    }

    // =========================================================
    // SMS (AUTOMATIC)
    // =========================================================

    private static void sendSmsAuto(Context context, String number, String message) {

        if (hasSmsPermission(context)) {

            boolean sent = sendSmsDirect(context, number, message);

            if (sent) {

                Toast.makeText(
                        context,
                        R.string.sms_sent,
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        context,
                        R.string.sms_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            // Permission not granted: fall back to the SMS app
            Toast.makeText(
                    context,
                    R.string.sms_permission_denied,
                    Toast.LENGTH_SHORT
            ).show();

            openSms(context, number, message);
        }
    }

    // =========================================================
    // SMS DIRECT (from the phone's SIM)
    // =========================================================

    private static boolean sendSmsDirect(Context context, String number, String message) {

        if (number == null || message == null) {
            return false;
        }

        String digits = number.replaceAll("\\D", "");

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

    // =========================================================
    // WHATSAPP
    // =========================================================

    private static void openWhatsApp(Context context, String number, String message) {

        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + number + "?text=" + Uri.encode(message))
        );

        intent.setPackage("com.whatsapp");

        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {

            context.startActivity(intent);

        } catch (ActivityNotFoundException first) {

            // Try WhatsApp Business, then any app that opens wa.me links.

            intent.setPackage("com.whatsapp.w4b");

            try {

                context.startActivity(intent);

            } catch (ActivityNotFoundException second) {

                intent.setPackage(null);

                try {

                    context.startActivity(intent);

                } catch (ActivityNotFoundException third) {

                    showNoApp(context);
                }
            }
        }
    }

    // =========================================================
    // SMS APP (fallback only)
    // =========================================================

    private static void openSms(Context context, String number, String message) {

        Intent intent = new Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("smsto:+" + number)
        );

        intent.putExtra("sms_body", message);

        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {

            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {

            showNoApp(context);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static void showNoApp(Context context) {

        Toast.makeText(
                context,
                R.string.msg_no_app,
                Toast.LENGTH_LONG
        ).show();
    }

    // Indian numbers: 9876543210 or 09876543210 -> 919876543210
    private static String normalizePhone(String phone) {

        if (phone == null) {
            return "";
        }

        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() == 10) {
            return "91" + digits;
        }

        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }

        return digits;
    }

    // Gives string resources in the chosen MESSAGE language,
    // whatever language the app screen is using.
    private static Context localized(Context context) {

        Configuration config =
                new Configuration(context.getResources().getConfiguration());

        config.setLocale(
                new Locale(AppSettings.getMessageLanguage(context))
        );

        return context.createConfigurationContext(config);
    }
}