package com.simon.upload.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 建立浮水印範例
 */
public class CreateWatermark {
    private final File image;

    public CreateWatermark(File image) {
        this.image = image;
    }

    public File insertText(String text, Point position) {
        //範例用,設定使用固定hardcode,PS:同時縮放及旋轉matrix時 必須先執行setXXX 否則setXXX之前執行的setXXX,preXXX,postXXX會被覆蓋掉
        Matrix matrix = new Matrix();
        matrix.setScale(0.5f, 0.5f);
        matrix.preRotate(getRotate());

        //範例用,設定使用固定hardcode
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setTextSize(100);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        paint.setAlpha(500);
        paint.setUnderlineText(false);

        Bitmap bitmapSrc = BitmapFactory.decodeFile(image.toString());
        Bitmap bitmap = Bitmap.createBitmap(bitmapSrc, 0, 0, bitmapSrc.getWidth(), bitmapSrc.getHeight(), matrix, true);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawText(text, position.x, position.y, paint);
        canvas.save();

        try (FileOutputStream fos = new FileOutputStream(image)) {
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    /*取得image旋轉角度*/
    private int getRotate() {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(image);
            int value = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
            switch (value) {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

}
