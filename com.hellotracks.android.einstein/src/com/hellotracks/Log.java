package com.hellotracks;

public class Log {
	static final String TAG = "HelloTracks";
	static final boolean LOG = BuildConfig.DEBUG;

	public static void i(String string) {
		if (LOG)
			android.util.Log.i(TAG, string);
	}
 
	public static void e(String string) {
		if (LOG)
			android.util.Log.e(TAG, string);
	}

	public static void e(String string, Throwable exc) {
		if (LOG)
			android.util.Log.e(TAG, string, exc);
	}
	
	public static void e(Throwable exc) {
        if (LOG)
            android.util.Log.e(TAG, "", exc);
    }

	public static void d(String string) {
		if (LOG)
			android.util.Log.d(TAG, string);
	}

	public static void v(String string) {
		if (LOG)
			android.util.Log.v(TAG, string);
	}

	public static void w(String string) {
		if (LOG)
			android.util.Log.w(TAG, string);
	}

	public static void w(Exception exc) {
		if (LOG)
			android.util.Log.w(TAG, exc);
	}

	public static void w(String string, Exception exc) {
		if (LOG)
			android.util.Log.w(TAG, string, exc);
	}
}