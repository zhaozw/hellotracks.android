package com.hellotracks.util;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

/**
 * This abstract class defines SDK-independent API for communication with Contacts Provider. The actual implementation
 * used by the application depends on the level of API available on the device. If the API level is Cupcake or Donut, we
 * want to use the {@link ContactAccessorSdk3_4} class. If it is Eclair or higher, we want to use
 * {@link ContactAccessorSdk5}.
 */
public abstract class ContactAccessor {

    /**
     * Static singleton instance of {@link ContactAccessor} holding the SDK-specific implementation of the class.
     */
    private static ContactAccessor sInstance;

    public static ContactAccessor getInstance() {
        if (sInstance == null) {
            sInstance = new ContactAccessorSdk5();
        }

        return sInstance;
    }

    /**
     * Returns the {@link Intent#ACTION_PICK} intent configured for the right authority: legacy or current.
     */
    public abstract Intent getPickContactIntent();

    /**
     * Loads contact data for the supplied URI. The actual queries will differ for different APIs used, but the result
     * is the same: the {@link #mDisplayName} and {@link #mPhoneNumber} fields are populated with correct data.
     */
    public abstract ContactInfo loadContact(ContentResolver contentResolver, Uri contactUri);
}