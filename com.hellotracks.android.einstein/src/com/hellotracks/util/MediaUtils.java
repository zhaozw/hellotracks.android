package com.hellotracks.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import com.hellotracks.Log;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.db.Closer;
import com.hellotracks.profile.ProfileSettingsScreen;

public class MediaUtils {
    
    public static final int SELECT_IMAGE = 34;
    public static final int TAKE_PICTURE = 35;
    public static final int IMAGE_MAX_SIZE = 200;
    public static final String PIC_NAME = "temp_pic.jpg";

    
    public static String getPath(Activity activity, Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = null;
        try {
            cursor = activity.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
                // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        } catch (Exception exc) {
            Log.e(exc);
        } finally {
            Closer.close(cursor);
        }
        return null;
    }
    
    
    public static Bitmap decodeFile(File f) {
        Bitmap b = null;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();

            int scale = 1;
            if (o.outHeight > 200 || o.outWidth > MediaUtils.IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(
                        2,
                        (int) Math.round(Math.log(MediaUtils.IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth))
                                / Math.log(0.5)));
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        } catch (IOException e) {
        }
        return b;
    }
    
    public static void post(final Context context, final String account, final String url, final String imagePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream out = null;
                try {
                    HttpClient httpClient = new DefaultHttpClient();

                    HttpPost httpPost = new HttpPost(url);

                    JSONObject dataNode = AbstractScreen.prepareObj(context);
                    if (account != null)
                        dataNode.put(C.account, account);

                    MultipartEntity multiPart = new MultipartEntity();
                    multiPart.addPart("auth", new StringBody(dataNode.toString()));

                    File file = new File(imagePath);
                    int o = 1;
                    try {
                        ExifInterface exif = new ExifInterface(imagePath);
                        String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                        Log.i("orientation: " + orientation);
                        if (orientation != null && orientation.length() > 0) {
                            o = Integer.parseInt(orientation);
                        }
                    } catch (Exception exc) {
                    }

                    Bitmap bitmap = MediaUtils.decodeFile(file);
                    if (o > 1) {
                        Matrix mtx = new Matrix();
                        switch (o) {
                        case 2:
                            mtx.preScale(-1.0f, 1.0f);
                            break;
                        case 3:
                            mtx.postRotate(180);
                            break;
                        case 4:
                            mtx.preScale(-1.0f, 1.0f);
                            mtx.postRotate(180);
                            break;
                        case 5:
                            mtx.postRotate(90);
                            mtx.preScale(-1.0f, 1.0f);
                            break;
                        case 6:
                            mtx.postRotate(90);
                            break;
                        case 7:
                            mtx.postRotate(-90);
                            mtx.preScale(-1.0f, 1.0f);
                            break;
                        case 8:
                            mtx.postRotate(-90);
                            break;
                        }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);

                    }
                    out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    multiPart.addPart("file", new ByteArrayBody(out.toByteArray(), "portrait"));
                    httpPost.setEntity(multiPart);
                    httpClient.execute(httpPost);
                } catch (Exception exc) {
                    Log.w(exc);
                } finally {
                    try {
                        out.close();
                    } catch (Exception exc) {
                    }
                }
            }
        }).start();

    }
}
