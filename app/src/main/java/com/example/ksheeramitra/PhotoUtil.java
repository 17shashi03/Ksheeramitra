package com.example.ksheeramitra;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shrinks a photo (max 400px, under about 150 KB) and turns it into a Base64
 * string that can be saved directly inside a Firestore document.
 * No Firebase Storage needed.
 */
public final class PhotoUtil {

    private static final String TAG = "PhotoUtil";

    private static final int MAX_SIDE = 400;
    private static final int MAX_BYTES = 150 * 1024;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private PhotoUtil() {
    }

    public interface Callback {
        /** base64 is null when it failed; error then holds the reason. Runs on the main thread. */
        void onDone(String base64, String error);
    }

    public static void encode(Context context, Uri uri, Callback callback) {

        final Context app = context.getApplicationContext();

        EXECUTOR.execute(() -> {

            String result = null;
            String error = null;

            try {

                // Glide respects the camera's rotation (EXIF)
                Bitmap bitmap = Glide.with(app)
                        .asBitmap()
                        .load(uri)
                        .downsample(DownsampleStrategy.AT_MOST)
                        .override(MAX_SIDE, MAX_SIDE)
                        .submit()
                        .get();

                int quality = 80;
                byte[] bytes;

                do {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    bytes = out.toByteArray();
                    quality -= 10;
                } while (bytes.length > MAX_BYTES && quality >= 30);

                result = Base64.encodeToString(bytes, Base64.NO_WRAP);

            } catch (Exception e) {

                Log.e(TAG, "Could not read photo", e);
                error = String.valueOf(e.getMessage());
            }

            final String r = result;
            final String err = error;

            MAIN.post(() -> callback.onDone(r, err));
        });
    }

    /** Returns the image bytes (for Glide), or null if the text is empty / invalid. */
    public static byte[] decode(String base64) {

        if (base64 == null || base64.isEmpty()) {
            return null;
        }

        try {
            return Base64.decode(base64, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
