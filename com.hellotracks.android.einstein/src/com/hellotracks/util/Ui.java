package com.hellotracks.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.R;

public class Ui {
    public static Animation inFromRightAnimation() {
        Animation inFromRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.0f);
        inFromRight.setDuration(600);
        inFromRight.setInterpolator(new OvershootInterpolator());
        return inFromRight;
    }

    public static Animation outToLeftAnimation() {
        Animation outtoLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoLeft.setDuration(600);
        outtoLeft.setInterpolator(new AccelerateInterpolator());
        return outtoLeft;
    }

    public static final int getScreenWidth(Activity a) {
        DisplayMetrics metrics = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public static float convertPixelsToDp(float px, Context context) {
        if (context == null)
            return 0;
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public static int convertDpToPixel(float dp, Context context) {
        if (context == null)
            return (int) dp;
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    public static final View inflateAndReturnInflatedView(LayoutInflater inflater, int resource, ViewGroup root) {
        View v = inflater.inflate(resource, root, false);
        root.addView(v);
        return v;
    }

    private static final int ALERT_DIALOG_THEME = AlertDialog.THEME_HOLO_LIGHT;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static final AlertDialog.Builder createAlertDialogBuilderCompat(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(context, ALERT_DIALOG_THEME);
        } else {
            return new AlertDialog.Builder(context);
        }
    }

    public static interface OkHandler {
        public void onOK();
    }

    public static final AlertDialog showModalMessage(Context context, int msg, final OkHandler okHandler) {
        AlertDialog.Builder b = CompatibilityUtils.createAlertDialogBuilderCompat(context);
        b.setMessage(msg);
        b.setCancelable(false);

        b.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (okHandler != null) {
                    okHandler.onOK();
                }
            }
        });
        AlertDialog dlg = b.create();
        dlg.show();
        return dlg;
    }

    public static String fromProgressToText(int p) {
        int meter = fromProgressToMeter(p);
        int feet = (int) (3.2808399 * (double) meter);
        return meter + " m / " + feet + " feet";
    }

    public static int fromMeterToProgress(int x) {
        return (int) Math.sqrt(x - 100);
    }

    public static int fromProgressToMeter(int p) {
        return p * p + 100;
    }

    public static final void showText(Activity context, int msg) {
        makeText(context, context.getString(msg), Toast.LENGTH_LONG).show();
    }

    public static final void showText(Activity context, String msg) {
        makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static final Toast makeText(Context context, int msg, int duration) {
        return makeText(context, context.getString(msg), duration);
    }

    public static final Toast makeText(Context context, String msg, int duration) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            View layout = activity.getLayoutInflater().inflate(R.layout.layout_toast, null);
            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText(msg);
            Toast toast = new Toast(context);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(duration);
            toast.setView(layout);
            return toast;
        } else {
            return Toast.makeText(context, msg, duration);
        }
    }

    public static final Toast makeText(LayoutInflater inflater, String msg, int duration) {
        View layout = inflater.inflate(R.layout.layout_toast, null);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(msg);
        Toast toast = new Toast(inflater.getContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(duration);
        toast.setView(layout);
        return toast;
    }

    public static final Toast makeText(LayoutInflater inflater, String msg, OnClickListener clickListener) {
        View layout = inflater.inflate(R.layout.layout_toast, null);
        layout.setClickable(true);
        layout.setOnClickListener(clickListener);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(msg);
        Toast toast = new Toast(inflater.getContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        return toast;
    }

}
