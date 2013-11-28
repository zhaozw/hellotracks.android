package com.hellotracks.util;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Build;

public final class CompatibilityUtils {
    public static final int ALERT_DIALOG_THEME = AlertDialog.THEME_HOLO_LIGHT;

    @TargetApi(11)
    public static final <Params> void executeInParallelCompat(AsyncTask<Params, ?, ?> t, Params... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            t.execute(params);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static final AlertDialog.Builder createAlertDialogBuilderCompat(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(context, ALERT_DIALOG_THEME);
        } else {
            return new AlertDialog.Builder(context);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static final void applyPrefsCompat(Editor editor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private CompatibilityUtils() {
    }
}
