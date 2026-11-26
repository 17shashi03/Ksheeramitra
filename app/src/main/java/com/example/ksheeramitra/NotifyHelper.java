package com.example.ksheeramitra; // change to your package

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writing these documents is all the app has to do.
 * The Cloud Functions detect the new document and send the SMS / email.
 */
public class NotifyHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static String now() {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(new Date());
    }

    // Call after FirebaseAuth createUserWithEmailAndPassword succeeds
    public static void onRegistered(String uid, String name, String email, String phone) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("email", email);
        m.put("phone", phone);
        db.collection("users").document(uid).set(m);
    }

    // Call after a successful login (read name/phone from users/{uid} or profile)
    public static void onLogin(String name, String email, String phone) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("email", email);
        m.put("phone", phone);
        m.put("time", now());
        db.collection("loginEvents").add(m);
    }

    // Call when saving a new farmer
    public static void onFarmerAdded(String farmerId, String name, String phone, String village) {
        Map<String, Object> m = new HashMap<>();
        m.put("farmerId", farmerId);
        m.put("name", name);
        m.put("phone", phone);
        m.put("village", village);
        db.collection("farmers").document(farmerId).set(m);
    }

    // Call when saving a milk collection
    public static void onCollectionSaved(String farmerName, String phone, String session,
                                         double qty, double fat, double snf, double amount,
                                         String centreName) {
        Map<String, Object> m = new HashMap<>();
        m.put("farmerName", farmerName);
        m.put("phone", phone);
        m.put("date", new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(new Date()));
        m.put("session", session);
        m.put("quantity", qty);
        m.put("fat", fat);
        m.put("snf", snf);
        m.put("amount", amount);
        m.put("centreName", centreName);
        db.collection("collections").add(m);
    }

    // Call when saving a payment
    public static void onPaymentSaved(String farmerName, String phone,
                                      double total, double paid, double balance,
                                      String centreName) {
        Map<String, Object> m = new HashMap<>();
        m.put("farmerName", farmerName);
        m.put("phone", phone);
        m.put("date", new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(new Date()));
        m.put("total", total);
        m.put("paid", paid);
        m.put("balance", balance);
        m.put("centreName", centreName);
        db.collection("payments").add(m);
    }
}