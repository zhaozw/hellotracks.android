package com.hellotracks.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;

public class Ui {
	public static Animation inFromRightAnimation() {
		Animation inFromRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRight.setDuration(600);
		inFromRight.setInterpolator(new OvershootInterpolator());
		return inFromRight;
	}

	public static Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
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

    


}
