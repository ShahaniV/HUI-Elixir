package com.example.facecoloranalyzer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.File;

public class ImageUtil {

    private static final String PREFS_NAME = "ImagePrefs";
    private static final String IMAGE_PATH_KEY = "imagePath";

    public static Bitmap getSavedImage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String imagePath = sharedPreferences.getString(IMAGE_PATH_KEY, null);

        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    // Change the background to white
                    bitmap = setWhiteBackground(bitmap);
                }
                return bitmap;
            }
        }

        return null;
    }

    private static Bitmap setWhiteBackground(Bitmap bitmap) {
        if (bitmap == null) return null;

        // Create a new bitmap with a white background
        Bitmap whiteBackgroundBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBackgroundBitmap);
        Paint paint = new Paint();
        paint.setColor(android.graphics.Color.WHITE);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

        // Draw the original bitmap on top of the white background
        canvas.drawBitmap(bitmap, 0, 0, null);

        return whiteBackgroundBitmap;
    }
}
