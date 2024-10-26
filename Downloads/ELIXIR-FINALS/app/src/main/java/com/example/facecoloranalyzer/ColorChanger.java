package com.example.facecoloranalyzer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog;
import com.github.dhaval2404.colorpicker.model.ColorShape;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;


public class ColorChanger extends AppCompatActivity {

    private ImageView capturedImageView, tshirtImageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private int defaultColorHair, defaultColorLips, defaultColorBlush, defaultColorEyeshadow, defaultColorShirt;

    private Bitmap savedImage;
    // Define constants for item IDs
    private static final int ITEM_HAIR = R.id.btnHair;
    private static final int ITEM_EYESHADOW = R.id.btnEyeShadow;
    private static final int ITEM_BLUSH = R.id.btnBlush;
    private static final int ITEM_LIPS = R.id.btnLips;

    private static final int ITEM_SHIRT = R.id.btnShirt;
    private ProgressDialog progressDialog;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton btnShirt, backbtn, increaseButton, decreaseButton;

    //private static final String SERVER_IP = "172.105.120.225"; // Online server IP
    private static final String SERVER_IP = "192.168.100.72";
    private static final int SERVER_PORT = 5000;
    //private static final int SERVER_PORT = 22;
    private static final String CLIENT_IDENTIFIER = "Changer";
    private String result;

    private int selectedHairColor = -1;    // Default -1 means no color selected yet
    private int selectedLipsColor = -1;
    private int selectedBlushColor = -1;
    private int selectedEyeshadowColor = -1;

    private static final int SIZE_INCREMENT = 20; // Define the increment size
    private static final int MIN_SIZE = 100; // Minimum size for the T-shirt
    private static final int MAX_SIZE = 800; // Maximum size for the T-shirt

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.color_changer);

        result = getIntent().getStringExtra("SEASON");

        capturedImageView = findViewById(R.id.capturedImageView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);

        btnShirt = findViewById(R.id.btnShirt);
        tshirtImageView = findViewById(R.id.tshirtImageView);
        increaseButton = findViewById(R.id.increaseButton);
        decreaseButton = findViewById(R.id.decreaseButton);

        defaultColorHair = R.color.blackie;
        defaultColorLips = R.color.blackie;
        defaultColorBlush = R.color.blackie;
        defaultColorEyeshadow = R.color.blackie;
        defaultColorShirt = R.color.blackie;
        backbtn = findViewById(R.id.backButton);

        savedImage = ImageUtil.getSavedImage(this); // Get the saved image as a Bitmap
        if (savedImage != null) {
            capturedImageView.setImageBitmap(savedImage); // Set the Bitmap to the ImageView
        } else {
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show(); // Show a message if no image is found
        }

        backbtn.setOnClickListener(view -> {finish();});

        increaseButton.setOnClickListener(v -> adjustTshirtSize(SIZE_INCREMENT));
        decreaseButton.setOnClickListener(v -> adjustTshirtSize(-SIZE_INCREMENT));

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == ITEM_HAIR) {
                    showColorPickerDialog("Hair", ITEM_HAIR);
                } else if (item.getItemId() == ITEM_EYESHADOW) {
                    showColorPickerDialog("Eyeshadow", ITEM_EYESHADOW);
                } else if (item.getItemId() == ITEM_BLUSH) {
                    showColorPickerDialog("Blush", ITEM_BLUSH);
                } else if (item.getItemId() == ITEM_LIPS) {
                    showColorPickerDialog("Lips", ITEM_LIPS);
                } return false;
            }
        });

        btnShirt.setOnClickListener(view -> {
            tshirtImageView.setVisibility(View.VISIBLE);
            showColorPickerDialog("Shirt",ITEM_SHIRT);
            increaseButton.setVisibility(View.VISIBLE);
            decreaseButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Drag the shirt to your desired position.", Toast.LENGTH_LONG).show();
        });

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Applying virtual makeup...");
        progressDialog.setCancelable(false);

        tshirtImageView.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private float lastActionX, lastActionY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        lastActionX = event.getRawX();
                        lastActionY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        v.animate()
                                .x(newX)
                                .y(newY)
                                .setDuration(0)
                                .start();
                        break;
                }
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                // Restrict the scale factor to a minimum of 0.1 and a maximum of 5.0
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
                tshirtImageView.setScaleX(scaleFactor);
                tshirtImageView.setScaleY(scaleFactor);
                return true;
            }
        });
    }

    private void adjustTshirtSize(int delta) {
        int currentWidth = tshirtImageView.getLayoutParams().width;
        int currentHeight = tshirtImageView.getLayoutParams().height;

        int newWidth = Math.max(MIN_SIZE, Math.min(MAX_SIZE, currentWidth + delta));
        int newHeight = Math.max(MIN_SIZE, Math.min(MAX_SIZE, currentHeight + delta));

        tshirtImageView.getLayoutParams().width = newWidth;
        tshirtImageView.getLayoutParams().height = newHeight;
        tshirtImageView.requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void changeTshirtColor(int color) {
        // Get the drawable
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.tshirt);

        if (drawable != null) {
            // Wrap the drawable
            drawable = DrawableCompat.wrap(drawable);
            // Set the color filter
            DrawableCompat.setTint(drawable, color);
            // Set the drawable to the ImageView
            tshirtImageView.setImageDrawable(drawable);
        }
    }
    private void showColorPickerDialog(final String colorType, final int viewID) {
        try {
        String[] colorArray;
        int defaultColor;
        if (result != null) {
            if (result.contains("Autumn")) {
                colorArray = getAutumnColors(colorType);
            } else if (result.contains("Summer")) {
                colorArray = getSummerColors(colorType);
            } else if (result.contains("Spring")) {
                colorArray = getSpringColors(colorType);
            } else if (result.contains("Winter")) {
                colorArray = getWinterColors(colorType);
            } else {
                colorArray = getDefaultColors();
            }
        } else {
            colorArray = getDefaultColors();
        }

            // Retrieve the intent
            Intent intent = getIntent();

// Check if the intent is not null and if the colorType is "Shirt"
            if (intent != null && "Shirt".equalsIgnoreCase(colorType)) {
                // Retrieve the generated colors from the intent
                ArrayList<String> generatedColors = intent.getStringArrayListExtra("GENERATED_COLORS");

                if (generatedColors != null && !generatedColors.isEmpty()) {
                    // Convert the existing colorArray to an ArrayList for easier manipulation
                    ArrayList<String> colorList = new ArrayList<>(Arrays.asList(colorArray));

                    // Append the generated colors to the existing array
                    colorList.addAll(generatedColors);

                    // Use a HashSet to remove duplicates
                    HashSet<String> uniqueColors = new HashSet<>(colorList);

                    // Convert back to array if needed
                    colorArray = uniqueColors.toArray(new String[0]);
                }
            }


            switch (colorType) {
                case "Hair":
                    defaultColor = defaultColorHair;
                    break;
                case "Lips":
                    defaultColor = defaultColorLips;
                    break;
                case "Blush":
                    defaultColor = defaultColorBlush;
                    break;
                case "Eyeshadow":
                    defaultColor = defaultColorEyeshadow;
                    break;
                case "Shirt":
                    defaultColor = defaultColorShirt;
                    break;
                default:
                    Toast.makeText(this, "Color type received: " + colorType, Toast.LENGTH_LONG).show();
                    throw new IllegalArgumentException("Invalid color type");
            }


            new MaterialColorPickerDialog
                .Builder(this)
                .setTitle("Pick " + colorType + " Color")
                .setColorShape(ColorShape.CIRCLE)
                .setColors(colorArray)
                .setDefaultColor(defaultColor)
                .setColorListener(new ColorListener() {
                    @Override
                    public void onColorSelected(int color, String colorHex) {
                        switch (colorType) {
                            case "Hair":
                                defaultColorHair = color;
                                selectedHairColor = color;
                                applyVirtualMakeup();
                                break;
                            case "Lips":
                                defaultColorLips = color;
                                selectedLipsColor = color;
                                applyVirtualMakeup();
                                break;
                            case "Blush":
                                defaultColorBlush = color;
                                selectedBlushColor = color;
                                applyVirtualMakeup();
                                break;
                            case "Eyeshadow":
                                defaultColorEyeshadow = color;
                                selectedEyeshadowColor = color;
                                applyVirtualMakeup();
                                break;
                            case "Shirt":
                                changeTshirtColor(color);
                                break;
                        }
                    }
                })
                .show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(ColorChanger.this, "Error: Invalid color type", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(ColorChanger.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private String[] getAutumnColors(String colorType) {
        switch (colorType) {
            case "Hair":
                return new String[]{"#BD9977", "#B96546", "#B96546", "#A52A2A", "#CF8F52", "#BB5848"};
            case "Lips":
                return new String[]{"#C46C55", "#ED7565", "#E2725D", "#C25A3D", "#C04C3B", "#D54941", "#A82F30", "#75272D", "#7E433E", "#8F3F2F", "#B46152"};
            case "Blush":
                return new String[]{"#F88070", "#A9463F", "#CB7053", "#D67338", "#B63F34", "#B02121"};
            case "Eyeshadow":
                return new String[]{"#EADAC3", "#E3C7A8", "#AC6553", "#654520", "#B12122", "#C35B3E", "#FFA400", "#655E4E", "#7E433E", "#0C5E7A", "#485B86", "#857E47", "#346837", "#193C36", "#56364E"};
            case "Shirt":
                return new String[]{"#F9F5E8", "#F2ECC9", "#F5C7BE", "#FABD14", "#D0A748", "#BF6766", "#EFB39A", "#F47A7F", "#EA7C61", "#B3767A", "#78579A", "#DA2C26", "#6A724C", "#97AD3E", "#8B556F", "#764727", "#CBAD86", "#3C6378"};
            default:
                return getDefaultColors();
        }
    }

    private String[] getSummerColors(String colorType) {
        switch (colorType) {
            case "Hair":
                return new String[]{"#6B574E", "#958D8A", "#824F2A", "#664442", "#9B7A67", "#C7B7A7"};
            case "Lips":
                return new String[]{"#C8074C", "#F8CCDC", "#FFC2C9", "#E097AB", "#D1748F", "#E9728F", "#FE91AA", "#D28FAF", "#AA568C", "#983C6D", "#B14E74"};
            case "Blush":
                return new String[]{"#E39FB8", "#FEA2BF", "#C39FAD", "#DA6F91", "#B4698C", "#AB4571"};
            case "Eyeshadow":
                return new String[]{"#E6E2E1", "#D0C7BD", "#BEAEB1", "#806B70", "#ADACAA", "#68798B", "#C67FAD", "#C7A1C9", "#DCAEC9", "#F2B9DC", "#FFC2C9", "#ECE89C", "#DF5287", "#632C64", "#3873AD", "#24526C", "#187465", "#77A680"};
            case "Shirt":
                return new String[]{"#FFFFFF", "#FCDCDE", "#EC7E99", "#BE656F", "#99596C", "#BB2355", "#F3F2A8", "#EADAEB", "#A0D8F2", "#9896CA", "#6DCCDC", "#1172A9", "#A48E96", "#89978C", "#87C8AD", "#54758E", "#405587", "#4C4D4F"};
            default:
                return getDefaultColors();
        }
    }

    private String[] getSpringColors(String colorType) {
        switch (colorType) {
            case "Hair":
                return new String[]{"#BDA982", "#957646", "#B47E43", "#733F16", "#482A1E", "#4C2715"};
            case "Lips":
                return new String[]{"#F93635", "#FBAB96", "#FF8576", "#EB675A", "#FF745B", "#F58656", "#EE6B58", "#EA5752", "#ED4E48", "#DD343B", "#E04953"};
            case "Blush":
                return new String[]{"#F7947A", "#E89579", "#E89579", "#FA7F6F", "#EA7274", "#E55460"};
            case "Eyeshadow":
                return new String[]{"#FBF2D1", "#C7BEAF", "#746B5E", "#92796D", "#A47064", "#805038", "#E6AB71", "#C8BCA7", "#FEA178", "#FF7064", "#BA7D67", "#B69663", "#E8AE66", "#8666A5", "#2669A0", "#38AB97", "#A7BB52", "#1B8F54"};
            case "Shirt":
                return new String[]{"#FAF3CC", "#F8E842", "#FBB788", "#FBC316", "#F68627", "#ED2224", "#FCDCD1", "#FCCE8B", "#F4846D", "#E58F8A", "#7A88C3", "#A5147A", "#D7C399", "#BFD95A", "#12BBB1", "#2196D3", "#136A5F", "#914B23"};
            default:
                return getDefaultColors();
        }
    }

    private String[] getWinterColors(String colorType) {
        switch (colorType) {
            case "Hair":
                return new String[]{"#716C69", "#591E17", "#754D45", "#0047AB", "#6A0D91", "#e4e4f0"};
            case "Lips":
                return new String[]{"#C180B6", "#FB7CC1", "#DC6DA2", "#C3598D", "#CC4487", "#B83274", "#93306F", "#6C234B", "#8D0749", "#A62250", "#C82252"};
            case "Blush":
                return new String[]{"#EA97BC", "#D54A79", "#A5546D", "#D45F9B", "#AE678A", "#7B3D5C"};
            case "Eyeshadow":
                return new String[]{"#F6F0F0", "#F2D6E9", "#CCE1DA", "#BBBBBB", "#776861", "#2C3147", "#F2E232", "#EDBFDD", "#E10F60", "#44A5F1", "#009B8D", "#794C89", "#554779", "#435186", "#2086B2", "#228E6D", "#1A6346", "#614A8B"};
            case "Shirt":
                return new String[]{"#FFFFFF", "#F8C4DB", "#ED809B", "#EC008C", "#C824BE", "#AC242B", "#C1C8E3", "#6790A2", "#6575B3", "#983F91", "#0772B8", "#114273", "#C6E1D6", "#AFAFA2", "#DBE335", "#0A7A95", "#0B8443", "#080A0B"};
            default:
                return getDefaultColors();
        }
    }

    private String[] getDefaultColors() {
        return new String[]{"#000000", "#000000", "#000000", "#000000", "#000000", "#000000", "#000000", "#000000", "#000000", "#000000"};
    }
    private void removeBackground(File imageFile) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.remove.bg/v1.0/removebg");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("X-Api-Key","MHExXApcwqfQ7JLkbAXxJRGe");
                String boundary = "---------------------------" + System.currentTimeMillis();
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (OutputStream os = conn.getOutputStream();
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"image_file\"; filename=\"selected_image.png\"").append("\r\n");
                    writer.append("Content-Type: application/octet-stream").append("\r\n").append("\r\n");
                    writer.flush();
                    try (InputStream is = new FileInputStream(imageFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    os.flush();
                    writer.append("\r\n");
                    writer.append("--").append(boundary).append("--").append("\r\n");
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    try (InputStream is = conn.getInputStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        runOnUiThread(() -> {
                            if (bitmap != null) {
                                capturedImageView.setImageBitmap(bitmap);
                                capturedImageView.setBackgroundColor(Color.WHITE);
                            } else {
                                Toast.makeText(ColorChanger.this, "Bitmap is null.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ColorChanger.this, "Failed to remove background: " + statusCode, Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e("RemoveBgError", "Failed to remove background", e);
                runOnUiThread(() -> Toast.makeText(ColorChanger.this, "Failed to remove background", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public File convertByteArrayToFile(byte[] byteArray, String filePath) {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(byteArray);
            fos.flush();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File convertBitmapToFile(Bitmap bitmap) {
        // Create a file to write bitmap data
        File file = new File(getCacheDir(), "image.png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Compress the bitmap and write it to the file
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void applyVirtualMakeup() {

        int hairColor = (selectedHairColor != -1) ? selectedHairColor : defaultColorHair;
        int lipsColor = (selectedLipsColor != -1) ? selectedLipsColor : defaultColorLips;
        int blushColor = (selectedBlushColor != -1) ? selectedBlushColor : defaultColorBlush;
        int eyeshadowColor = (selectedEyeshadowColor != -1) ? selectedEyeshadowColor : defaultColorEyeshadow;

        if (savedImage == null) {
            Toast.makeText(this, "No image to process", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert colors to byte arrays
        byte[] hairColorBytes = new byte[] {
                (byte) Color.red(hairColor),
                (byte) Color.green(hairColor),
                (byte) Color.blue(hairColor)
        };

        byte[] lipsColorBytes = new byte[] {
                (byte) Color.red(lipsColor),
                (byte) Color.green(lipsColor),
                (byte) Color.blue(lipsColor)
        };

        byte[] blushColorBytes = new byte[] {
                (byte) Color.red(blushColor),
                (byte) Color.green(blushColor),
                (byte) Color.blue(blushColor)
        };

        byte[] eyeshadowColorBytes = new byte[] {
                (byte) Color.red(eyeshadowColor),
                (byte) Color.green(eyeshadowColor),
                (byte) Color.blue(eyeshadowColor)
        };

        // Convert the image to byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        savedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        progressDialog.show();

        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                // Send the identifier
                byte[] identifierBytes = CLIENT_IDENTIFIER.getBytes();
                dataOutputStream.writeInt(identifierBytes.length);
                dataOutputStream.write(identifierBytes);

                // Send the image data length and image data
                dataOutputStream.writeInt(imageData.length);
                dataOutputStream.write(imageData);

                // Send the hair color data length and hair color data
                dataOutputStream.writeInt(hairColorBytes.length);
                dataOutputStream.write(hairColorBytes);

                // Send the lips color data length and lips color data
                dataOutputStream.writeInt(lipsColorBytes.length);
                dataOutputStream.write(lipsColorBytes);

                // Send the blush color data length and blush color data
                dataOutputStream.writeInt(blushColorBytes.length);
                dataOutputStream.write(blushColorBytes);

                // Send the eyeshadow color data length and eyeshadow color data
                dataOutputStream.writeInt(eyeshadowColorBytes.length);
                dataOutputStream.write(eyeshadowColorBytes);

                // Receive the result image from the server
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                int resultImageLength = dataInputStream.readInt();
                byte[] resultImageData = new byte[resultImageLength];
                dataInputStream.readFully(resultImageData);

                //File resultImage = convertByteArrayToFile(resultImageData, getExternalFilesDir(null) + "/resultImage.png");
                // Convert byte array to Bitmap
                Bitmap resultImage = BitmapFactory.decodeByteArray(resultImageData, 0, resultImageData.length);

                // Update the UI on the main thread
                runOnUiThread(() -> {
                    if (resultImage!=null) {
                        capturedImageView.setImageBitmap(resultImage);
                        Toast.makeText(ColorChanger.this, "Virtual makeup applied successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ColorChanger.this, "Failed to apply virtual makeup", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                });

                socket.close();
            } catch (IOException e) {
                Log.e("ColorChanger", "Error applying virtual makeup", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ColorChanger.this, "Failed to apply virtual makeup", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
