package com.example.lectortickets;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static final int CAMERA_REQUEST_CODE = 200;
    public static final int STORAGE_REQUEST_CODE = 400;
    public static final int IMAGE_PICK_GALLERY_CODE = 1000;
    public static final int IMAGE_PICK_CAMERA_CODE = 1001;


    private String[] cameraPermission;
    private Uri image_uri;
    private Double montoTotal;
    private Double montoPorCuota;

    @BindView(R.id.imageview_demo)
    ImageView imageviewDemo;
    @BindView(R.id.editText_total)
    EditText editTextTotal;
    @BindView(R.id.editText_cuotas)
    EditText editTextCuotas;
    @BindView(R.id.constraint)
    ConstraintLayout constraint;
    @BindView(R.id.editText_Cantidad_cuotas)
    EditText editTextCantidadCuotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        editTextTotal.setOnLongClickListener(view -> {
            editTextTotal.setEnabled(true);
            return true;
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted)
                        pickCamera();
                } else {
                    Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show();
                }
            }
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {

                        pickGallery();
                    } else
                        Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case IMAGE_PICK_CAMERA_CODE:

                    CropImage.activity(image_uri)
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .start(this);
                    break;
                case IMAGE_PICK_GALLERY_CODE:
                    CropImage.activity(data.getData())
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .start(this);
                    break;
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert result != null;
                Uri resultUri = result.getUri();
                imageviewDemo.setImageURI(resultUri);
                BitmapDrawable bitmapDrawable = (BitmapDrawable) imageviewDemo.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                if (!recognizer.isOperational()) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < items.size(); i++) {
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }
                    String cuotas = sb.toString();
                    cuotas = cuotas.substring(cuotas.length() - 2);
                    cuotas = cuotas.replaceAll("\\D+", "");
                    int totalCuotas = Integer.valueOf(cuotas);
                    switch (totalCuotas) {
                        case 7:
                            totalCuotas = 12;
                            break;
                        case 8:
                            totalCuotas = 18;
                            break;
                    }

                    editTextCantidadCuotas.setText(String.valueOf(totalCuotas));

                    String monto = sb.toString();
                    monto = monto.replaceAll("\\D+", "");
                    BigDecimal bigDecimal = new BigDecimal(monto);
                    NumberFormat ArsFormat = NumberFormat.getCurrencyInstance(Locale.US);
                    MathContext m = new MathContext(6);
                    BigDecimal totalBigDecimal = bigDecimal.round(m);
                    ArsFormat.setMinimumFractionDigits(1);
                    ArsFormat.setMaximumFractionDigits(2);
                    editTextTotal.setText(ArsFormat.format(totalBigDecimal));
                    if (monto.contains("CUOTAS")) {
                        Snackbar.make(constraint, "Ocurrio un error al procesar la imagen", Snackbar.LENGTH_SHORT).show();
                    } else {
                        try {
                            montoTotal = Double.valueOf(monto);
                            montoPorCuota = montoTotal / totalCuotas;
                            editTextCuotas.setText(String.valueOf(montoPorCuota));
                        } catch (NumberFormatException e) {
                            Log.e("MainActivity", e.toString());
                        }
                    }


                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                assert result != null;
                Exception error = result.getError();
                Toast.makeText(this, "" + error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean reult1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && reult1;
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, STORAGE_REQUEST_CODE);
    }

    private void pickCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "NewPic");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image to text");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent cameraInntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraInntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraInntent, IMAGE_PICK_CAMERA_CODE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera:
                if (checkCameraPermission())
                    pickCamera();
                else requestCameraPermission();
                break;
            case R.id.gallery:
                if (checkStoragePermission())
                    pickGallery();
                else requestStoragePermission();
        }
        return true;
    }

    private void pickGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }
}

