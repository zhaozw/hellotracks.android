package com.hellotracks.base;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.billing.PlanHolder;
import com.hellotracks.billing.SKU;
import com.hellotracks.billing.util.IabHelper;
import com.hellotracks.billing.util.IabHelper.QueryInventoryFinishedListener;
import com.hellotracks.billing.util.IabResult;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.Payload;
import com.hellotracks.billing.util.Purchase;
import com.hellotracks.billing.util.SkuDetails;
import com.hellotracks.util.PlanUtils;
import com.hellotracks.util.Ui;

public abstract class AbstractScreenWithIAB extends AbstractScreen implements QueryInventoryFinishedListener {

    protected Inventory mInventory;
    protected IabHelper mHelper;

    protected PlanHolder mSelectedPlan = null;
    protected Purchase mPurchase = null;

    public Inventory getInventory() {
        return mInventory;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mHelper.handleActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setup() {
        mHelper = new IabHelper(this, Payload.disjunk());
        mHelper.enableDebugLogging(true);
        startIabSetup();
    }

    protected void startIabSetup() {
        try {
            Log.i("start iab setup");
            if (Prefs.get(this).getString(Prefs.USERNAME, "").length() > 0) {
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        if (!result.isSuccess()) {
                            // activity.showMessage("Problem setting up in-app billing: " + result);
                            return;
                        }
                        try {
                            mHelper.queryInventoryAsync(true, SKU.asList(), AbstractScreenWithIAB.this);
                        } catch (Exception exc) {
                            Log.e(exc);
                        }
                    }
                });
            }
        } catch (Exception exc) {
            Log.e(exc);
        }
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        if (result.isFailure()) {
            // handle error
            return;
        }
        mInventory = inv;
        boolean isPremium = getPurchases().size() > 0;
        Prefs.get(this).edit().putBoolean(Prefs.IS_PREMIUM, isPremium).commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // very important:
        Log.d("Destroying helper.");
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }

    public Map<SkuDetails, Purchase> getPurchases() {
        return getPurchases(this, mInventory);
    }

    public static Map<SkuDetails, Purchase> getPurchases(Context context, Inventory mInventory) {
        if (mInventory == null)
            return null;

        Map<SkuDetails, Purchase> map = new HashMap<SkuDetails, Purchase>();
        String[] skus = SKU.names();
        for (int i = 0; i < skus.length; i++) {
            Purchase p = mInventory.getPurchase(skus[i]);
            if (p != null && Payload.verifyPayload(context, p.getDeveloperPayload())) {
                if (p.getPurchaseState() == Purchase.STATE_PURCHASED) {
                    map.put(mInventory.getSkuDetails(skus[i]), p);
                }
            }
        }
        return map;
    }

    public static Map<SkuDetails, Purchase> getCanceledOrRefundedPurchases(Context context, Inventory mInventory) {
        if (mInventory == null)
            return null;

        Map<SkuDetails, Purchase> map = new HashMap<SkuDetails, Purchase>();
        String[] skus = SKU.names();
        for (int i = 0; i < skus.length; i++) {
            Purchase p = mInventory.getPurchase(skus[i]);
            if (p != null && Payload.verifyPayload(context, p.getDeveloperPayload())) {
                if (p.getPurchaseState() != Purchase.STATE_PURCHASED) {
                    map.put(mInventory.getSkuDetails(skus[i]), p);
                }
            }
        }
        return map;
    }

    public void checkout() {
        if (!mHelper.subscriptionsSupported()) {
            //showMessage("Subscriptions not supported on your device yet. Sorry!");
            return;
        }
        SkuDetails sd = mSelectedPlan.paymentPlan;
        SKU sku = SKU.valueOf(sd.getSku());
        if (sku == null) {
            return;
        }

        String payload = Payload.createPayload(this);
        if (sku.isSubscription()) {
            mHelper.launchSubscriptionPurchaseFlow(this, sku.toString(), C.REQUESTCODE_INAPPBILLING,
                    mPurchaseFinishedListener);
        } else {
            mHelper.launchPurchaseFlow(this, mSelectedPlan.paymentPlan.getSku(), IabHelper.ITEM_TYPE_SUBS,
                    C.REQUESTCODE_INAPPBILLING, mPurchaseFinishedListener, payload);
        }
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Ui.showModalMessage(AbstractScreenWithIAB.this, R.string.PurchaseFailed, null);
                PlanUtils.notifyUsAboutPurchase(AbstractScreenWithIAB.this, purchase);
                return;
            } else if (result.isSuccess()) {
                if (purchase != null) {
                    mPurchase = purchase;
                    PlanUtils.savePurchase(AbstractScreenWithIAB.this, purchase, true);
                    PlanUtils.notifyUsAboutPurchase(AbstractScreenWithIAB.this, purchase);
                }
                confirmation();
            }
        }

    };

    public void confirmation() {

    }

}
