package com.squareup.picasso;

import android.graphics.Bitmap;

/**
 * A memory cache for storing the most recently used images.
 * <p/>
 * <em>Note:</em> The {@link #get(String)} method will be invoked on the main thread.
 */
public interface Cache {
    /** Retrieve an image for the specified {@code key} or {@code null}. */
    Bitmap get(String key);

    /** Store an image in the cache for the specified {@code key}. */
    void set(String key, Bitmap bitmap);

    /** Returns the current size of the cache in bytes. */
    int size();

    /** Returns the maximum size in bytes that the cache can hold. */
    int maxSize();

    /** A cache which does not store any values. */
    Cache NONE = new Cache() {
        public Bitmap get(String key) {
            return null;
        }

        public void set(String key, Bitmap bitmap) {
            // Ignore.
        }

        public int size() {
            return 0;
        }

        public int maxSize() {
            return 0;
        }
    };
}
