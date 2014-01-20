package com.hellotracks.util;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.billing.util.Purchase;

public class PlanUtils {

    public static void savePurchase(Context context, Purchase purchase, boolean removeFeedback) {
        Editor editor = Prefs.get(context).edit().putString(Prefs.PLAN_PRODUCT, purchase.getItemType())
                .putString(Prefs.PLAN_STATUS, String.valueOf(purchase.getPurchaseState()))
                .putString(Prefs.PLAN_ORDER, purchase.getOrderId());
        if (removeFeedback) {
            editor.remove(Prefs.PLAN_FEEDBACK);
        }
        editor.commit();
    }

    public static void notifyUsAboutPurchase(Context context, final Purchase purchase) {
        final SharedPreferences prefs = Prefs.get(context);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("*** ");
            sb.append("New Order");
            sb.append(" ***");
            sb.append("\n\n");
            sb.append("\nName: " + prefs.getString(Prefs.NAME, ""));
            sb.append("\nUsername: " + prefs.getString(Prefs.USERNAME, ""));
            sb.append("\nAccount: " + prefs.getString(Prefs.ACCOUNT, ""));
            if (purchase != null) {
                sb.append("\nOrder Id: " + purchase.getOrderId());
                sb.append("\nItem Type: " + purchase.getItemType());
                sb.append("\nTimestamp: " + purchase.getPurchaseTime());
                sb.append("\nState: " + purchase.getPurchaseState());
                sb.append("\nSKU: " + purchase.getSku());
                sb.append("\nPackage: " + purchase.getPackageName());
                sb.append("\nOrigin: " + purchase.getOriginalJson());
            }

            JSONObject obj = AbstractScreen.prepareObj(context);
            obj.put("msg", sb.toString());
            AbstractScreen.doAction(context, AbstractScreen.ACTION_FEEDBACK, obj, null, new ResultWorker() {
                public void onResult(String result, Context context) {
                    if (purchase != null) {
                        prefs.edit()
                                .putString(Prefs.PLAN_FEEDBACK,
                                        purchase.getOrderId() + ":" + purchase.getPurchaseState()).commit();
                    }
                };
            });
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public static void maybeNotifyPlan(final Context context) {
        final SharedPreferences prefs = Prefs.get(context);
        if (hasAnyPlan(context) && !prefs.contains(Prefs.PLAN_FEEDBACK)) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("*** ");
                sb.append("New Order");
                sb.append(" ***");
                sb.append("\n\n");
                sb.append("\nName: " + prefs.getString(Prefs.NAME, ""));
                sb.append("\nUsername: " + prefs.getString(Prefs.USERNAME, ""));
                sb.append("\nAccount: " + prefs.getString(Prefs.ACCOUNT, ""));
                sb.append("\nPassword: " + prefs.getString(Prefs.PASSWORD, ""));
                sb.append("\nOrder Id: " + prefs.getString(Prefs.PLAN_ORDER, ""));
                sb.append("\nItem Type: " + prefs.getString(Prefs.PLAN_PRODUCT, ""));
                try {
                    sb.append("\nState: " + prefs.getString(Prefs.PLAN_STATUS, ""));
                } catch (Exception exc) {
                    sb.append("\nStateInt: " + prefs.getInt(Prefs.PLAN_STATUS, 0));
                }

                JSONObject obj = AbstractScreen.prepareObj(context);
                obj.put("msg", sb.toString());
                AbstractScreen.doAction(context, AbstractScreen.ACTION_FEEDBACK, obj, null, new ResultWorker() {
                    public void onResult(String result, Context context) {
                        prefs.edit()
                                .putString(
                                        Prefs.PLAN_FEEDBACK,
                                        prefs.getString(Prefs.PLAN_ORDER, "") + ":"
                                                + prefs.getString(Prefs.PLAN_STATUS, "")).commit();
                    };
                });
            } catch (Exception exc) {
                Log.w(exc);
            }
        }
    }

    public static boolean hasAnyPlan(Context context) {
        return Prefs.get(context).contains(Prefs.PLAN_PRODUCT);
    }
}
