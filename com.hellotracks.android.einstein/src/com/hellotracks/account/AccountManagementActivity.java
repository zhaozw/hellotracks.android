package com.hellotracks.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.C;
import com.hellotracks.billing.ConfirmationFragment;
import com.hellotracks.billing.PlanHolder;
import com.hellotracks.billing.SKU;
import com.hellotracks.billing.CallToUpgradeFragment;
import com.hellotracks.billing.util.IabHelper;
import com.hellotracks.billing.util.IabResult;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.Payload;
import com.hellotracks.billing.util.Purchase;
import com.hellotracks.billing.util.IabHelper.QueryInventoryFinishedListener;

import de.greenrobot.event.EventBus;

public class AccountManagementActivity extends FragmentActivity {

    private PlanHolder mSelectedPlan = null;
    private Purchase mPurchase = null;
    private Inventory mInventory;
    private Fragment mFragment;
    private int step = 0;
    private IabHelper mHelper;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);

        FragmentManager fm = getSupportFragmentManager();

        if (getLastCustomNonConfigurationInstance() instanceof PlanHolder) {
            mSelectedPlan = (PlanHolder) getLastCustomNonConfigurationInstance();
            step = 2;
        }

        if (fm.findFragmentById(android.R.id.content) == null) {
            if (getIntent() != null && getIntent().hasExtra("upsell")) {
                upsell();
            } else {
                account();
            }
        } else {
            mFragment = (Fragment) fm.findFragmentById(android.R.id.content);
        }

        setup();
        
        EventBus.getDefault().register(this, LoginEvent.class);
    }
    
    public void onEvent(LoginEvent e) {
        finish();
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
        jumpToFragment(1, new CallToUpgradeFragment());
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
            if (resultCode == C.RESULTCODE_CLOSEAPP) {
                setResult(requestCode);
                finish();
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
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
                        } else if (mFragment instanceof CallToUpgradeFragment) {
                            CallToUpgradeFragment f = (CallToUpgradeFragment) mFragment;
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

        EventBus.getDefault().unregister(this);

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
