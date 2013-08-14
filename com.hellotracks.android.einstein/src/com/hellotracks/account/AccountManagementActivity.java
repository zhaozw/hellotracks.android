package com.hellotracks.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.hellotracks.Log;
import com.hellotracks.base.C;
import com.hellotracks.billing.ConfirmationFragment;
import com.hellotracks.billing.PlanHolder;
import com.hellotracks.billing.SKU;
import com.hellotracks.billing.UpsellFragment;
import com.hellotracks.billing.util.IabHelper;
import com.hellotracks.billing.util.IabResult;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.Payload;
import com.hellotracks.billing.util.Purchase;
import com.hellotracks.billing.util.IabHelper.QueryInventoryFinishedListener;

public class AccountManagementActivity extends FragmentActivity {

    private PlanHolder mSelectedPlan = null;
    private Purchase mPurchase = null;
    private Inventory mInventory;
    private Fragment mFragment;
    private int step = 0;
    private IabHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        if (getLastCustomNonConfigurationInstance() instanceof PlanHolder) {
            mSelectedPlan = (PlanHolder) getLastCustomNonConfigurationInstance();
            step = 2;
        }

        if (fm.findFragmentById(android.R.id.content) == null) {
            account();
        } else {
            mFragment = (Fragment) fm.findFragmentById(android.R.id.content);
        }

        setup();
    }

    public void onBack(View view) {
        finish();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mSelectedPlan;
    }

    public void account() {
        jumpToFragment(1, new AccountFragment());
    }

    public void upsell() {
        jumpToFragment(1, new UpsellFragment());
    }

    public void confirmation() {
        jumpToFragment(3, new ConfirmationFragment());
    }

    private void jumpToFragment(int s, Fragment f) {
        step = s;
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commitAllowingStateLoss();
        mFragment = f;
    }

    public void setSelectedPlan(PlanHolder selectedPlan) {
        this.mSelectedPlan = selectedPlan;
    }

    public PlanHolder getSelectedPlan() {
        return mSelectedPlan;
    }
    
    public Purchase getPurchase() {
        return mPurchase;
    }
    
    public Inventory getInventory() {
        return mInventory;
    }

    public void checkout() {
        if (!mHelper.subscriptionsSupported()) {
            //showMessage("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        String payload = Payload.createPayload(this);
        mHelper.launchPurchaseFlow(this, mSelectedPlan.paymentPlan.getSku(), IabHelper.ITEM_TYPE_SUBS,
                C.REQUESTCODE_INAPPBILLING, mPurchaseFinishedListener, payload);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setup() {
        mHelper = new IabHelper(this, Payload.disjunk());
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // activity.showMessage("Problem setting up in-app billing: " + result);
                    return;
                }

                mHelper.queryInventoryAsync(true, SKU.asList(), new QueryInventoryFinishedListener() {

                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                        if (result.isFailure()) {
                            // handle error
                            return;
                        }
                        mInventory = inv;
                        if (mFragment instanceof AccountFragment) {
                            AccountFragment f = (AccountFragment) mFragment;
                            f.onReady(inv);
                        } else if (mFragment instanceof UpsellFragment) {
                            UpsellFragment f = (UpsellFragment) mFragment;
                            f.onReady(inv);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d("Destroying helper.");
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                // showMessage("Error purchasing: " + result);
                return;
            }
            mPurchase = purchase;
            confirmation();
        }
    };

}
