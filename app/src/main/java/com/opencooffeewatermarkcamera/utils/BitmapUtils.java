package com.opencooffeewatermarkcamera.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.opencooffeewatermarkcamera.CommonValues;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    public static Bitmap decodeInputStream(InputStream inputStream) {

        BitmapFactory.Options options = new BitmapFactory.Options();

        // If set to true, the decoder will return null (no bitmap), but the out... fields will still be set, allowing the caller to query the bitmap without having to allocate the memory for its pixels.
        options.inJustDecodeBounds = true;

        // When this flag is set, if inDensity and inTargetDensity are not 0, the bitmap will be scaled to match inTargetDensity when loaded, rather than relying on the graphics system scaling it each time it is drawn to a Canvas.
        // BitmapRegionDecoder ignores this flag, and will not scale output based on density. (though inSampleSize is supported)
        // This flag is turned on by default and should be turned off if you need a non-scaled version of the bitmap.
        // Nine-patch bitmaps ignore this flag and are always scaled.
        // If inPremultiplied is set to false, and the image has alpha, setting this flag to true may result in incorrect colors.
        options.inScaled = false;

        // This field was deprecated in API level 24.
        // As of N, this is ignored. In M and below, if dither is true, the decoder will attempt to dither the decoded image.
        options.inDither = false;

        // If this is non-null, the decoder will try to decode into this internal configuration.
        // If it is null, or the request cannot be met, the decoder will try to pick the best matching config based on the system's screen depth,
        // and characteristics of the original image such as if it has per-pixel alpha (requiring a config that also does).
        // Image are loaded with the ARGB_8888 config by default.
        options.inPreferredConfig = CommonValues.BITMAP_CONFIG;

        // This field was deprecated in API level 24.
        // As of N, this is ignored. The output will always be high quality.
        // In M and below, if inPreferQualityOverSpeed is set to true, the decoder will try to decode the reconstructed image to a higher quality even at the expense of the decoding speed.
        // Currently the field only affects JPEG decode, in the case of which a more accurate, but slightly slower, IDCT method will be used instead.
        options.inPreferQualityOverSpeed = true;

        // If set, decode methods will always return a mutable Bitmap instead of an immutable one.
        // This can be used for instance to programmatically apply effects to a Bitmap loaded through BitmapFactory.
        // Can not be set simultaneously with inPreferredConfig = HARDWARE, because hardware bitmaps are always immutable.
        options.inMutable = false;

        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeStream(inputStream, null, options);
    }

    public static Bitmap cropBitmapToSquareBitmap(Bitmap bitmap, Matrix matrix) {

        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();

        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width)? height - ( height - width) : height;

        int cropW = (width - height) / 2;

        cropW = (cropW < 0) ? 0: cropW;

        int cropH = (height - width) / 2;

        cropH = (cropH < 0)? 0: cropH;

        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight, matrix, true);
    }

    /**
     * Scales the provided bitmap to have the height and width provided.
     * (Alternative method for scaling bitmaps since Bitmap.createScaledBitmap(...) produces bad (blocky) quality bitmaps.)
     *
     * @param bitmap
     * The bitmap to scale.
     *
     * @param newWidth
     * The desired width of the scaled bitmap.
     *
     * @param newHeight
     * The desired height of the scaled bitmap.
     *
     * @return the scaled bitmap.
     */
    @SuppressLint("WrongConstant")
    public static Bitmap scaleBitmap(Bitmap bitmap, float newWidth, float newHeight) {

        float scaleX = newWidth / bitmap.getWidth();
        float scaleY = newHeight / bitmap.getHeight();
        float pivotX = 0;
        float pivotY = 0;

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

        Bitmap newBitmap = Bitmap.createBitmap(Math.round(newWidth), Math.round(newHeight), CommonValues.BITMAP_CONFIG);

        Canvas canvas = new Canvas(newBitmap);

        Matrix matrix = new Matrix();

        matrix.setScale(scaleX, scaleY, pivotX, pivotY);

        canvas.setMatrix(matrix);

        canvas.drawBitmap(bitmap, 0, 0, paint);

        return newBitmap;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, float angle) {

        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Create image file in external storage.
     *
     * @param bitmap
     * @param destinationFile
     *
     * @return boolean value
     */
    public static boolean createImageFileInExternalStorage(Bitmap bitmap, File destinationFile) {

        boolean isSaved = false;

        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        FileOutputStream fos = null;

        try {

            baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

            baos.flush();

            fos = new FileOutputStream(destinationFile);

            byte[] byteArray = baos.toByteArray();

            bais = new ByteArrayInputStream(byteArray);

            Utils.copyStream(bais, fos);

            isSaved = true;

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

            return isSaved;
        }

    }

}