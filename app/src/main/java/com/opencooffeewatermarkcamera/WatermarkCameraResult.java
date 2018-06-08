package com.opencooffeewatermarkcamera;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opencooffeewatermarkcamera.application.WatermarkCameraApplication;
import com.opencooffeewatermarkcamera.library.CustomTextView;
import com.opencooffeewatermarkcamera.library.ExternalStorage;
import com.opencooffeewatermarkcamera.library.FileCache;
import com.opencooffeewatermarkcamera.utils.BitmapUtils;
import com.opencooffeewatermarkcamera.utils.RuntimePermissionUtils;
import com.opencooffeewatermarkcamera.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WatermarkCameraResult extends AppCompatActivity {

    //private static final String LOG_TAG = WatermarkCameraResult.class.getSimpleName();

    public static final int REQUEST_CODE_GALLERY = 1;
    private static final int SAVED_IMAGE = 2;
    private static final int ERROR_SAVING_IMAGE = 3;
    private static final int CREATED_WATERMARK_IMAGE = 4;
    private static final int ERROR_CREATING_WATERMARK_IMAGE = 5;
    private static final int AN_ERROR_HAS_OCURRED = 6;

    private Context context;

    private FileCache fileCache;

    private ExternalStorage externalStorage;

    private ProgressDialog progressDialog;

    private Toast messageToast;

    private Rect rect;

    private GestureDetector gestureDetector;

    private View decorView;
    private ImageButton backImageButton, cancelImageButton, favouriteWatermarkImageButton, addWatermarkImageButton, saveImageButton, shareImageButton;
    private ImageView previewImageView;
    private CustomTextView toastTextView;

    // Is hide system UI?
    private boolean isHideSystemUI = true;

    private boolean isRequestStoragePermissionForLoadWatermark = false;

    private boolean isRequestStoragePermissionForSaveImage = false;

    private boolean isGoToSettingsForExternalStoragePermissionToLoadWatermark = false;

    private boolean isGoToSettingsForExternalStoragePermissionToSaveImage = false;

    private boolean isImageWithWatermark = false;

    // If the original image is saved in the device's memory, it will be set to true.
    private boolean isImageAlreadySaved = false;

    // If the watermark image is saved in the device's memory, it will be set to true.
    private boolean isWatermarkImageAlreadySaved = false;

    // If the image is saved in cache, it will be set to true.
    private boolean isWatermarkImageAlreadySavedInCache = false;

    // Time that elapses from when is asked to go to the configuration to activate storage permission until it is returned from it.
    private long requestStoragePermissionTimestamp = 0;

    private String path;

    private Bitmap originalImageBitmap, originalImageWithWatermarkBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.watermark_camera_result);

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("path") && extras.getString("path") != null) {

            path = extras.getString("path");

            context = getApplicationContext();

            initFields();
            initClasses();
            initCallbacks();

            setBitmapFromFilePath(path);

        } else {
            finish();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >=23) {

            if (isGoToSettingsForExternalStoragePermissionToLoadWatermark) {

                isGoToSettingsForExternalStoragePermissionToLoadWatermark = false;

                if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                }

            }

            if (isGoToSettingsForExternalStoragePermissionToSaveImage) {

                isGoToSettingsForExternalStoragePermissionToSaveImage = false;

                if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    createImageFileInDeviceStorage();
                }

            }

        }

        Utils.hideSystemUI(decorView);
    }

    @Override
    protected void onPause() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        File file = new File(path);

        if (file.exists()) {
            file.delete();
        }

        File temporalImageWithWatermarkFile = fileCache.getFile(CommonValues.TEMPORAL_IMAGE_WITH_WATERMARK + ".jpg");

        if (temporalImageWithWatermarkFile.exists()) {
            temporalImageWithWatermarkFile.delete();
        }

        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case RuntimePermissionUtils.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:

                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    if (isRequestStoragePermissionForLoadWatermark) {

                        isRequestStoragePermissionForLoadWatermark = false;

                        openGallery();

                    } else if (isRequestStoragePermissionForSaveImage) {

                        isRequestStoragePermissionForSaveImage = false;

                        createImageFileInDeviceStorage();
                    }

                } else {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(WatermarkCameraResult.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        if ((Utils.getDeviceCurrentTimestamp() - requestStoragePermissionTimestamp) <= CommonValues.RESPONSE_TIMEOUT) {

                            requestStoragePermissionTimestamp = 0;

                            RuntimePermissionUtils.showMessage(
                                    WatermarkCameraResult.this,
                                    getResources().getString(R.string.storage_runtime_permission_text),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (which == -1) {

                                                if (isRequestStoragePermissionForLoadWatermark) {
                                                    isGoToSettingsForExternalStoragePermissionToLoadWatermark = true;
                                                } else if (isRequestStoragePermissionForSaveImage) {
                                                    isGoToSettingsForExternalStoragePermissionToSaveImage = true;
                                                }

                                                RuntimePermissionUtils.goToSettings(WatermarkCameraResult.this);
                                            }

                                        }

                                    }
                            );

                        }

                    }

                }

                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_GALLERY && data != null && data.getData() != null) {

            InputStream is = null;

            try {

                is = getContentResolver().openInputStream(data.getData());

                Bitmap watermarkBitmap = BitmapUtils.decodeInputStream(is);

                if (watermarkBitmap != null) {
                    isImageWithWatermark = createOriginalImageWithWatermarkBitmap(watermarkBitmap);
                    createFavouriteWatermarkFile(watermarkBitmap);
                } else {
                    toastTextView.setText(getResources().getString(R.string.error_getting_the_selected_image));
                    messageToast.show();
                }

            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } finally {

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }

            }

        }

    }

    private void initFields() {

        decorView = getWindow().getDecorView();

        previewImageView = findViewById(R.id.preview); // ImageView

        backImageButton = findViewById(R.id.back); // ImageButton

        cancelImageButton = findViewById(R.id.cancel); // ImageButton

        favouriteWatermarkImageButton = findViewById(R.id.favourite_watermark); // ImageButton

        addWatermarkImageButton = findViewById(R.id.add_watermark); // ImageButton

        saveImageButton = findViewById(R.id.save); // ImageButton

        shareImageButton = findViewById(R.id.share); // ImageButton

        // Customize the toast to show.
        LayoutInflater layoutInflater = getLayoutInflater();

        View toastLayout = layoutInflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_linear_layout));

        toastTextView = toastLayout.findViewById(R.id.toast); // CustomTextView

        messageToast = new Toast(this);
        messageToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        messageToast.setDuration(Toast.LENGTH_SHORT);
        messageToast.setView(toastLayout);
    }

    private void initClasses() {
        fileCache = new FileCache(context);
        externalStorage = new ExternalStorage(CommonValues.ALBUM_NAME);
    }

    private void initCallbacks() {

        gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onDown(MotionEvent motionEvent) {

                if (isHideSystemUI) {
                    Utils.showSystemUI(decorView);
                    isHideSystemUI = false;
                } else {
                    Utils.hideSystemUI(decorView);
                    isHideSystemUI = true;
                }

                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {}

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent1, MotionEvent motionEvent2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {}

            @Override
            public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float velocityX, float velocityY) {
                return false;
            }

        });

        backImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Build a Rect of the limits of view.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(backImageButton);

                        int width = backImageButton.getWidth();
                        int height = backImageButton.getHeight();

                        width += 12;
                        height += 12;

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

                        int backImageButtonLayoutMarginDp = 36;

                        int backImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, backImageButtonLayoutMarginDp);

                        params.setMargins(backImageButtonLayoutMarginPx - 6, backImageButtonLayoutMarginPx - 6, 0, 0);

                        backImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseBackImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseBackImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            if (isImageWithWatermark) {

                                if (originalImageBitmap != null) {

                                    previewImageView.setImageBitmap(originalImageBitmap);

                                    originalImageWithWatermarkBitmap.recycle();

                                    originalImageWithWatermarkBitmap = null;

                                    isImageWithWatermark = false;

                                    File temporalImageWithWatermarkFile = fileCache.getFile(CommonValues.TEMPORAL_IMAGE_WITH_WATERMARK + ".jpg");

                                    if (temporalImageWithWatermarkFile.exists()) {
                                        temporalImageWithWatermarkFile.delete();
                                    }

                                    isWatermarkImageAlreadySavedInCache = false;
                                }

                            } else {
                                finish();
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseCancelImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;

            }

        });

        cancelImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Build a Rect of the limits of view.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(cancelImageButton);

                        int width = cancelImageButton.getWidth();
                        int height = cancelImageButton.getHeight();

                        width += 12;
                        height += 12;

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

                        int cancelImageButtonLayoutMarginDp = 36;

                        int cancelImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, cancelImageButtonLayoutMarginDp);

                        params.setMargins(0, cancelImageButtonLayoutMarginPx - 6, cancelImageButtonLayoutMarginPx - 6, 0);

                        cancelImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseCancelImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseCancelImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            Intent returnIntent = new Intent();

                            returnIntent.putExtra(CommonValues.FINISH, CommonValues.FINISH);

                            setResult(RESULT_OK, returnIntent);

                            finish();
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseCancelImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

        favouriteWatermarkImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Build a Rect of the limits of view.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(favouriteWatermarkImageButton);

                        int height = favouriteWatermarkImageButton.getHeight();

                        height += 12;

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height);

                        params.weight = 1;
                        params.gravity = Gravity.CENTER;

                        favouriteWatermarkImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseFavouriteWatermarkImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseFavouriteWatermarkImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            if (!isImageWithWatermark) {

                                File favouriteWatermarkFile = fileCache.getFile(CommonValues.FAVOURITE_WATERMARK);

                                if (favouriteWatermarkFile != null && favouriteWatermarkFile.exists()) {

                                    // Get Bitmap from cache memory.
                                    Bitmap watermarkBitmap = BitmapFactory.decodeFile(favouriteWatermarkFile.getAbsolutePath());

                                    if (watermarkBitmap != null) {
                                        isImageWithWatermark = createOriginalImageWithWatermarkBitmap(watermarkBitmap);
                                    }

                                } else {
                                    toastTextView.setText(getResources().getString(R.string.there_is_no_watermark_as_favorite));
                                    messageToast.show();
                                }

                            } else {
                                toastTextView.setText(getResources().getString(R.string.there_is_currently_a_watermark_inserted));
                                messageToast.show();
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseFavouriteWatermarkImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

        addWatermarkImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Build a Rect of the limits of view.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(addWatermarkImageButton);

                        int height = addWatermarkImageButton.getHeight();

                        height += 12;

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height);

                        params.weight = 1;
                        params.gravity = Gravity.CENTER;

                        addWatermarkImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseAddWatermarkImageButton();;
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseAddWatermarkImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            if (!isImageWithWatermark) {

                                if (Build.VERSION.SDK_INT >= 23) {

                                    if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                                        isRequestStoragePermissionForLoadWatermark = true;

                                        requestStoragePermissionTimestamp = Utils.getDeviceCurrentTimestamp();

                                        requestPermissions(
                                                new String[]{
                                                        RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE
                                                },
                                                RuntimePermissionUtils.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
                                        );

                                    } else {
                                        openGallery();
                                    }

                                } else {
                                    openGallery();
                                }

                            } else {
                                toastTextView.setText(getResources().getString(R.string.there_is_currently_a_watermark_inserted));
                                messageToast.show();
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseAddWatermarkImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

        saveImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Build a Rect of the limits of view.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(saveImageButton);

                        int height = saveImageButton.getHeight();

                        height += 12;

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height);

                        params.weight = 1;
                        params.gravity = Gravity.CENTER;

                        saveImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseSaveImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseSaveImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            if (Build.VERSION.SDK_INT >= 23) {

                                if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                                    isRequestStoragePermissionForSaveImage = true;

                                    requestStoragePermissionTimestamp = Utils.getDeviceCurrentTimestamp();

                                    requestPermissions(
                                            new String[]{
                                                    RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE
                                            },
                                            RuntimePermissionUtils.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
                                    );

                                } else {
                                    createImageFileInDeviceStorage();
                                }

                            } else {
                                createImageFileInDeviceStorage();
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseSaveImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

        shareImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Build a Rect of the limits of view.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(shareImageButton);

                        int height = shareImageButton.getHeight();

                        height += 12;

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height);

                        params.weight = 1;
                        params.gravity = Gravity.CENTER;

                        shareImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseShareImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseShareImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            if (isImageWithWatermark) {

                                if (isWatermarkImageAlreadySavedInCache) {
                                    shareFile(fileCache.getFile(CommonValues.TEMPORAL_IMAGE_WITH_WATERMARK + ".jpg"));
                                } else {
                                    CreateTemporalImageWithWatermarkFile createTemporalImageWithWatermarkFile = new CreateTemporalImageWithWatermarkFile();
                                    createTemporalImageWithWatermarkFile.execute();
                                }

                            } else {
                                shareFile(new File(path));
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseShareImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

    }

    private void decreaseBackImageButton() {

        int backImageButtonLayoutWidthDp = 40;

        int backImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, backImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(backImageButtonLayoutWidthPx, backImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        int backImageButtonLayoutMarginDp = 36;

        int backImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, backImageButtonLayoutMarginDp);

        params.setMargins(backImageButtonLayoutMarginPx, backImageButtonLayoutMarginPx, 0, 0);

        backImageButton.setLayoutParams(params);
    }

    private void decreaseCancelImageButton() {

        int cancelImageButtonLayoutWidthDp = 40;

        int cancelImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, cancelImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cancelImageButtonLayoutWidthPx, cancelImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        int cancelImageButtonLayoutMarginDp = 36;

        int cancelImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, cancelImageButtonLayoutMarginDp);

        params.setMargins(0, cancelImageButtonLayoutMarginPx, cancelImageButtonLayoutMarginPx, 0);

        cancelImageButton.setLayoutParams(params);
    }

    private void decreaseFavouriteWatermarkImageButton() {

        int favouriteWatermarkImageButtonLayoutWidthDp = 40;

        int favouriteWatermarkImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, favouriteWatermarkImageButtonLayoutWidthDp);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, favouriteWatermarkImageButtonLayoutWidthPx);

        params.weight = 1;
        params.gravity = Gravity.CENTER;

        favouriteWatermarkImageButton.setLayoutParams(params);
    }

    private void decreaseAddWatermarkImageButton() {

        int addWatermarkImageButtonLayoutWidthDp = 40;

        int addWatermarkImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, addWatermarkImageButtonLayoutWidthDp);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, addWatermarkImageButtonLayoutWidthPx);

        params.weight = 1;
        params.gravity = Gravity.CENTER;

        addWatermarkImageButton.setLayoutParams(params);
    }

    private void decreaseSaveImageButton() {

        int saveImageButtonLayoutWidthDp = 40;

        int saveImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, saveImageButtonLayoutWidthDp);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, saveImageButtonLayoutWidthPx);

        params.weight = 1;
        params.gravity = Gravity.CENTER;

        saveImageButton.setLayoutParams(params);
    }

    private void decreaseShareImageButton() {

        int shareImageButtonLayoutWidthDp = 40;

        int shareImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, shareImageButtonLayoutWidthDp);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, shareImageButtonLayoutWidthPx);

        params.weight = 1;
        params.gravity = Gravity.CENTER;

        shareImageButton.setLayoutParams(params);
    }

    /**
     * Get Bitmap from a path.
     * Do not modify the algorithm to obtain the Bitmap.
     *
     * @param path
     * Path of the image File.
     */
    private void setBitmapFromFilePath(String path) {

        try {

            originalImageBitmap = BitmapFactory.decodeFile(path);

            if (originalImageBitmap != null) {

                RelativeLayout.LayoutParams params;

                if (WatermarkCameraApplication.getIsCameraRectangle()) {

                    params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

                    previewImageView.setScaleType(ImageView.ScaleType.FIT_XY);

                } else {

                    params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

                    int marginTop = WatermarkCameraApplication.getDeviceStatusBarHeight() + (WatermarkCameraApplication.getDeviceScreenHeight() - WatermarkCameraApplication.getDeviceScreenWidth()) / 2;

                    params.setMargins(0, marginTop, 0, 0);
                }

                previewImageView.setLayoutParams(params);

                previewImageView.setImageBitmap(originalImageBitmap);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    private boolean createOriginalImageWithWatermarkBitmap(Bitmap watermarkBitmap) {

        try {

            originalImageWithWatermarkBitmap = originalImageBitmap.copy(CommonValues.BITMAP_CONFIG, true);

            Canvas canvas = new Canvas(originalImageWithWatermarkBitmap);

            Paint paint = new Paint();

            // Helper for setFlags(), setting or clearing the ANTI_ALIAS_FLAG bit AntiAliasing smooths out the edges of what is being drawn, but is has no impact on the interior of the shape.
            // See setDither() and setFilterBitmap() to affect how colors are treated.
            paint.setAntiAlias(true);

            // Helper for setFlags(), setting or clearing the FILTER_BITMAP_FLAG bit.
            // Filtering affects the sampling of bitmaps when they are transformed.
            // Filtering does not affect how the colors in the bitmap are converted into device pixels.
            // That is dependent on dithering and xfermodes.
            paint.setFilterBitmap(true);

            // Helper for setFlags(), setting or clearing the DITHER_FLAG bit Dithering affects how colors that are higher precision than the device are down-sampled.
            // No dithering is generally faster, but higher precision colors are just truncated down (e.g. 8888 -> 565).
            // Dithering tries to distribute the error inherent in this process, to reduce the visual artifacts.
            paint.setDither(true);

            int originalImageWithWatermarkBitmapWidth = originalImageWithWatermarkBitmap.getWidth();
            int originalImageWithWatermarkBitmapHeight = originalImageWithWatermarkBitmap.getHeight();

            int watermarkBitmapWidth = watermarkBitmap.getWidth();
            int watermarkBitmapHeight = watermarkBitmap.getHeight();

            float watermarkPadding;

            if (!WatermarkCameraApplication.getIsCameraRectangle()) {

                if (Math.round(originalImageWithWatermarkBitmapWidth / 5) < watermarkBitmapWidth || Math.round(originalImageWithWatermarkBitmapHeight / 5) < watermarkBitmapHeight) {

                    float coefficient;
                    float widthCoefficient = (originalImageWithWatermarkBitmapWidth / 5.0f) / watermarkBitmapWidth;
                    float heightCoefficient = (originalImageWithWatermarkBitmapHeight / 5.0f) / watermarkBitmapHeight;

                    if (widthCoefficient > heightCoefficient) {
                        coefficient = widthCoefficient;
                    } else {
                        coefficient = heightCoefficient;
                    }

                    watermarkBitmap = BitmapUtils.scaleBitmap(watermarkBitmap, watermarkBitmapWidth * coefficient, watermarkBitmapHeight * coefficient);
                }

                watermarkPadding = watermarkBitmap.getHeight() / 10;

                // Draw the specified bitmap, with its top/left corner at (x,y), using the specified paint, transformed by the current matrix.
                canvas.drawBitmap(
                        watermarkBitmap,
                        originalImageWithWatermarkBitmap.getWidth() - watermarkBitmap.getWidth() - watermarkPadding,
                        originalImageWithWatermarkBitmap.getHeight() - watermarkBitmap.getHeight() - watermarkPadding,
                        paint
                );

            } else {

                if (Math.round(originalImageWithWatermarkBitmapWidth / 8) < watermarkBitmapWidth || Math.round(originalImageWithWatermarkBitmapHeight / 8) < watermarkBitmapHeight) {

                    float coefficient;
                    float widthCoefficient = (originalImageWithWatermarkBitmapWidth / 8.0f) / watermarkBitmapWidth;
                    float heightCoefficient = (originalImageWithWatermarkBitmapHeight / 8.0f) / watermarkBitmapHeight;

                    if (widthCoefficient > heightCoefficient) {
                        coefficient = widthCoefficient;
                    } else {
                        coefficient = heightCoefficient;
                    }

                    watermarkBitmap = BitmapUtils.scaleBitmap(watermarkBitmap, watermarkBitmapWidth * coefficient, watermarkBitmapHeight * coefficient);

                    watermarkBitmap = BitmapUtils.rotateBitmap(watermarkBitmap, 90.0f);
                }

                watermarkPadding = watermarkBitmap.getHeight() / 10;

                // Draw the specified bitmap, with its top/left corner at (x,y), using the specified paint, transformed by the current matrix.
                canvas.drawBitmap(
                        watermarkBitmap,
                        watermarkPadding,
                        originalImageWithWatermarkBitmapHeight - watermarkBitmap.getHeight() - watermarkPadding,
                        paint
                );

            }

            previewImageView.setImageBitmap(originalImageWithWatermarkBitmap);

            return true;

        } catch (Exception e) {
            return false;
        }

    }

    private void createFavouriteWatermarkFile(Bitmap watermarkBitmap) {

        fileCache.deleteFileByName(CommonValues.FAVOURITE_WATERMARK);

        File favouriteWatermarkFile = fileCache.getFile(CommonValues.FAVOURITE_WATERMARK);

        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        FileOutputStream fos = null;

        try {

            baos = new ByteArrayOutputStream();

            watermarkBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

            baos.flush();

            fos = new FileOutputStream(favouriteWatermarkFile);

            byte[] byteArray = baos.toByteArray();

            bais = new ByteArrayInputStream(byteArray);

            Utils.copyStream(bais, fos);

        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {

            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }

            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }

        }

    }

    private void createImageFileInDeviceStorage() {

        if (!isImageAlreadySaved || !isWatermarkImageAlreadySaved) {
            CreateImageFileInDevicePublicFolder createImageFileInDevicePublicFolder = new CreateImageFileInDevicePublicFolder();
            createImageFileInDevicePublicFolder.execute();
        } else {
            toastTextView.setText(getResources().getString(R.string.image_already_saved));
            messageToast.show();
        }

    }

    private void openGallery() {

        Intent imagePickerIntent = new Intent(Intent.ACTION_PICK);

        imagePickerIntent.setType("image/*");

        startActivityForResult(imagePickerIntent, REQUEST_CODE_GALLERY);
    }

    private class CreateImageFileInDevicePublicFolder extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(WatermarkCameraResult.this, R.style.WatermarkCameraProgressDialogStyle);

            progressDialog.setMessage(getResources().getString(R.string.saving_image));
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {

            while (!isCancelled()) {

                String imageFileName = Utils.getImageFileNameByCurrentTime();

                File destinationFile = externalStorage.getPublicAlbumFile(imageFileName);

                if (isImageWithWatermark) {

                    if (BitmapUtils.createImageFileInExternalStorage(originalImageWithWatermarkBitmap, destinationFile)) {

                        isWatermarkImageAlreadySaved = true;

                        return SAVED_IMAGE;

                    } else {
                        return ERROR_SAVING_IMAGE;
                    }

                } else if (originalImageBitmap != null) {

                    if (BitmapUtils.createImageFileInExternalStorage(originalImageBitmap, destinationFile)) {

                        isImageAlreadySaved = true;

                        return SAVED_IMAGE;

                    } else {
                        return ERROR_SAVING_IMAGE;
                    }

                } else {
                    return AN_ERROR_HAS_OCURRED;
                }

            }

            return AN_ERROR_HAS_OCURRED;
        }

        @Override
        protected void onPostExecute(Integer i) {

            switch (i) {

                case SAVED_IMAGE:

                    toastTextView.setText(getResources().getString(R.string.saved_image));

                    break;

                case ERROR_SAVING_IMAGE:

                    toastTextView.setText(getResources().getString(R.string.error_saving_image));

                    break;

                case AN_ERROR_HAS_OCURRED:

                    toastTextView.setText(getResources().getString(R.string.an_error_has_ocurred));

                    break;

            }

            messageToast.show();

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }

        }

    }

    private class CreateTemporalImageWithWatermarkFile extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(WatermarkCameraResult.this, R.style.WatermarkCameraProgressDialogStyle);

            progressDialog.setMessage(getResources().getString(R.string.processing_image));
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {

            while (!isCancelled()) {

                fileCache.deleteFileByName(CommonValues.TEMPORAL_IMAGE_WITH_WATERMARK);

                File temporalImageWithWatermarkFile = fileCache.getFile(CommonValues.TEMPORAL_IMAGE_WITH_WATERMARK + ".jpg");

                ByteArrayOutputStream baos = null;
                ByteArrayInputStream bais = null;
                FileOutputStream fos = null;

                try {

                    baos = new ByteArrayOutputStream();

                    originalImageWithWatermarkBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

                    baos.flush();

                    fos = new FileOutputStream(temporalImageWithWatermarkFile);

                    byte[] byteArray = baos.toByteArray();

                    bais = new ByteArrayInputStream(byteArray);

                    Utils.copyStream(bais, fos);

                    isWatermarkImageAlreadySavedInCache = true;

                    return CREATED_WATERMARK_IMAGE;

                } catch (IOException e1) {

                    e1.printStackTrace();

                    return ERROR_CREATING_WATERMARK_IMAGE;

                } finally {

                    if (baos != null) {
                        try {
                            baos.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }

                    if (bais != null) {
                        try {
                            bais.close();
                        } catch (IOException e3) {
                            e3.printStackTrace();
                        }
                    }

                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }

                }

            }

            return AN_ERROR_HAS_OCURRED;
        }

        @Override
        protected void onPostExecute(Integer i) {

            switch (i) {

                case CREATED_WATERMARK_IMAGE:

                    File temporalImageWithWatermarkFile = fileCache.getFile(CommonValues.TEMPORAL_IMAGE_WITH_WATERMARK + ".jpg");

                    shareFile(temporalImageWithWatermarkFile);

                    break;

                case ERROR_CREATING_WATERMARK_IMAGE:

                    toastTextView.setText(getResources().getString(R.string.error_creating_watermark_image));

                    messageToast.show();

                    break;

                case AN_ERROR_HAS_OCURRED:

                    toastTextView.setText(getResources().getString(R.string.an_error_has_ocurred));

                    messageToast.show();

                    break;

            }

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }

        }

    }

    private void shareFile(File file) {

        if (file != null) {

            if (Build.VERSION.SDK_INT >= 24) {

                Uri shareFileUri = FileProvider.getUriForFile(context, CommonValues.PROVIDER, file);

                Intent shareIntent = ShareCompat.IntentBuilder.from(WatermarkCameraResult.this).setStream(shareFileUri).getIntent();

                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                shareIntent.setDataAndType(shareFileUri, getContentResolver().getType(shareFileUri));

                // Give permissions individually to all apps on the device.
                List<ResolveInfo> resolvedInfoActivities = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo ri : resolvedInfoActivities) {
                    context.grantUriPermission(ri.activityInfo.packageName, shareFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivity(Intent.createChooser(shareIntent, CommonValues.APP_NAME));

            } else {

                Intent shareIntent = new Intent(Intent.ACTION_SEND);

                shareIntent.setType("image/*");

                Uri uri = Uri.fromFile(file);

                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                startActivity(Intent.createChooser(shareIntent, CommonValues.APP_NAME));
            }

        }

    }

}