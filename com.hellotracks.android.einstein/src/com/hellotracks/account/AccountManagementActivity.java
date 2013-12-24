package com.hellotracks.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreenWithIAB;
import com.hellotracks.base.C;
import com.hellotracks.billing.CallToUpgradeFragment;
import com.hellotracks.billing.ConfirmationFragment;
import com.hellotracks.billing.PlanHolder;
import com.hellotracks.billing.SKU;
import com.hellotracks.billing.util.IabHelper;
import com.hellotracks.billing.util.IabHelper.QueryInventoryFinishedListener;
import com.hellotracks.billing.util.IabResult;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.Payload;
import com.hellotracks.billing.util.Purchase;

import de.greenrobot.event.EventBus;

public class AccountManagementActivity extends AbstractScreenWithIAB implements IUpsell {

   
    private Fragment mFragment;

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
        jumpToFragment(new AccountFragment());
    }

    public void upsell() {
        jumpToFragment(new CallToUpgradeFragment());
    }

    public void confirmation() {
        jumpToFragment(new ConfirmationFragment());
    }

    private void jumpToFragment(Fragment f) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            if (resultCode == C.RESULTCODE_CLOSEAPP) {
                setResult(resultCode);
                finish();
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
    
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        super.onQueryInventoryFinished(result, inv);
        if (mFragment instanceof AccountFragment) {
            AccountFragment f = (AccountFragment) mFragment;
            f.onReady(mInventory);
        } else if (mFragment instanceof CallToUpgradeFragment) {
            CallToUpgradeFragment f = (CallToUpgradeFragment) mFragment;
            f.onReady(mInventory);
        }
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

}
