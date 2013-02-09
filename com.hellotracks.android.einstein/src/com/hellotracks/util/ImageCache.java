package com.hellotracks.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;

import com.hellotracks.Log;

/**
 * The ImageCache class can be used to download images and store them in the
 * cache directory of the device. Multiple requests to the same URL will result
 * in a single download, until the cache timeout has passed.
 * 
 * @author Thomas Vervest
 */
public class ImageCache {

	/**
	 * The ImageCallback interface defines a single method used to pass an image
	 * back to the calling object when it has been loaded.
	 */
	public static interface ImageCallback {
		/**
		 * The onImageLoaded method is called by the ImageCache when an image
		 * has been loaded.
		 * 
		 * @param image
		 *            The requested image in the form of a Drawable object.
		 * @param url
		 *            The originally requested URL
		 */
		void onImageLoaded(Bitmap image, String url);
	}

	private static ImageCache _instance = null;

	/**
	 * Gets the singleton instance of the ImageCache.
	 * 
	 * @return The ImageCache.
	 */
	public synchronized static ImageCache getInstance() {
		if (_instance == null) {
			_instance = new ImageCache();
		}
		return _instance;
	}

	private static final long CACHE_TIMEOUT = 60000 * 60 * 24 * 365;
	private final Object _lock = new Object();
	private HashMap<String, Bitmap> _cache;
	private HashMap<String, List<ImageCallback>> _callbacks;

	private ImageCache() {
		_cache = new HashMap<String, Bitmap>();
		_callbacks = new HashMap<String, List<ImageCallback>>();
	}

	public String getHash(String url) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(url.getBytes());
			return new BigInteger(digest.digest()).toString(16);
		} catch (NoSuchAlgorithmException ex) {
			// this should never happen, but just to make sure return the url
			return url;
		}
	}
	
	public Bitmap loadFromCache(String url) {
		return drawableFromCache(url, getHash(url));
	}

	private Bitmap drawableFromCache(String url, String hash) {
		Bitmap d = null;
		synchronized (_lock) {
			if (_cache.containsKey(hash)) {
				d = _cache.get(hash);
			}
		}
		return d;
	}

	public Bitmap loadSync(String url, String hash, Context context) {
		Bitmap d = null;
		try {
			d = drawableFromCache(url, hash);

			File f = new File(context.getCacheDir(), hash);
			boolean timeout = f.lastModified() + CACHE_TIMEOUT < new Date()
					.getTime();
			if (d == null || timeout) {
				if (timeout)
					f.delete();
				if (!f.exists()) {
					InputStream is = new URL(url).openConnection()
							.getInputStream();
					if (f.createNewFile()) {
						FileOutputStream fo = new FileOutputStream(f);
						byte[] buffer = new byte[256];
						int size;
						while ((size = is.read(buffer)) > 0) {
							fo.write(buffer, 0, size);
						}
						fo.flush();
						fo.close();
					}
				}
				d = BitmapFactory.decodeFile(f.getAbsolutePath());
				synchronized (_lock) {
					_cache.put(hash, d);
				}
			}
		} catch (Exception ex) {
			Log.w(ex);
		}
		return d;
	}

	/**
	 * Loads an image from the passed URL and calls the callback method when the
	 * image is done loading.
	 * 
	 * @param url
	 *            The URL of the target image.
	 * @param callback
	 *            A ImageCallback object to pass the loaded image. If null, the
	 *            image will only be pre-loaded into the cache.
	 * @param context
	 *            The context of the new Drawable image.
	 */
	public void loadAsync(final String url, final ImageCallback callback,
			final Context context) {
		final String hash = getHash(url);

		synchronized (_lock) {
			List<ImageCallback> callbacks = _callbacks.get(hash);
			if (callbacks != null) {
				if (callback != null)
					callbacks.add(callback);
				return;
			}

			callbacks = new ArrayList<ImageCallback>();
			if (callback != null)
				callbacks.add(callback);
			_callbacks.put(hash, callbacks);
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				Bitmap d = loadSync(url, hash, context);
				List<ImageCallback> callbacks;

				synchronized (_lock) {
					callbacks = _callbacks.remove(hash);
				}

				for (ImageCallback iter : callbacks) {
					iter.onImageLoaded(d, url);
				}
			}
		}, "ImageCache loader: " + url).start();
	}
	
	private static Bitmap createFancy(Bitmap originalImage) {
		// The gap we want between the reflection and the original image
		final int reflectionGap = 4;

		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create a Bitmap with the flip matix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0,
				height / 2, width, height / 2, matrix, false);

		// Create a new bitmap with same width but taller to fit reflection
		Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
				(height + height / 2), Config.ARGB_8888);

		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(bitmapWithReflection);
		// Draw in the original image
		canvas.drawBitmap(originalImage, 0, 0, null);
		// Draw in the gap
		Paint deafaultPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);
		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

		// Create a shader that is a linear gradient that covers the reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0,
				originalImage.getHeight(), 0, bitmapWithReflection.getHeight()
						+ reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
		// Set the paint to use this shader (linear gradient)
		paint.setShader(shader);
		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight()
				+ reflectionGap, paint);
		return bitmapWithReflection;
	}
}