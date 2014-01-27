package com.hellotracks.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.View;
import android.view.WindowManager;

import com.hellotracks.Logger;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
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
import com.hellotracks.util.PlanUtils;

import de.greenrobot.event.EventBus;

public class ManagementScreen extends AbstractScreenWithIAB implements IUpsell {

    private SlidingPaneLayout mSlidingLayout;
    private Fragment mFragmentMenu;
    private AccountFragment mFragmentAccount;
    private ProfileFragment mFragmentProfile;
    private SettingsFragment mFragmentSettings;
    private CallToUpgradeFragment mFragmentCallToUpgrade;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        setContentView(R.layout.management_sliding_pane);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mFragmentMenu = new ManagementMenuFragment();

        mFragmentAccount = new AccountFragment();
        mFragmentProfile = new ProfileFragment();
        mFragmentSettings = new SettingsFragment();
        mFragmentCallToUpgrade = new CallToUpgradeFragment();

        getSupportFragmentManager().beginTransaction().add(R.id.left_pane, mFragmentMenu, "pane1").commit();

        mSlidingLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane_layout);

        supportInvalidateOptionsMenu();
        setupActionBar(R.string.Settings);

        FragmentManager fm = getSupportFragmentManager();

        if (getLastCustomNonConfigurationInstance() instanceof PlanHolder) {
            mSelectedPlan = (PlanHolder) getLastCustomNonConfigurationInstance();
        }

        if (fm.findFragmentById(android.R.id.content) == null) {
            if (getIntent() != null && getIntent().hasExtra("upsell")) {
                upsell();
            } else if (getIntent() != null && getIntent().hasExtra("profile")) {
                profile();
            } else if (getIntent() != null && getIntent().hasExtra("settings")) {
                settings();
            } else {
                account();
            }
        }

        mSlidingLayout.openPane();

        EventBus.getDefault().register(this, LoginEvent.class);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        /*
         * The action bar up action should open the slider if it is currently closed,
         * as the left pane contains content one level up in the navigation hierarchy.
         */
        if (item.getItemId() == android.R.id.home && !mSlidingLayout.isOpen()) {        
            mSlidingLayout.smoothSlideOpen();
            return true;
        }

        
        return super.onOptionsItemSelected(item);
    }

    public void onEvent(LoginEvent e) {
        finish();
    }

    public void onProfile(View view) {
        profile();
    }

    public void onSettings(View view) {
        settings();
    }

    public void onAccount(View view) {
        account();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mSelectedPlan;
    }

    public void account() {
        jumpToFragment(mFragmentAccount);
        getSupportActionBar().setTitle(R.string.Account);
    }

    public void upsell() {
        jumpToFragment(mFragmentCallToUpgrade);
    }

    public void confirmation() {
        jumpToFragment(new ConfirmationFragment());
    }

    public void profile() {
        jumpToFragment(mFragmentProfile);
        getSupportActionBar().setTitle(R.string.Profile);
    }

    public void settings() {
        jumpToFragment(mFragmentSettings);
        getSupportActionBar().setTitle(R.string.Settings);
    }

    private void jumpToFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_pane, f).commitAllowingStateLoss();
        mSlidingLayout.closePane();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == C.RESULTCODE_CLOSEAPP) {
            setResult(resultCode);
            finish();
        }
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        super.onQueryInventoryFinished(result, inv);
        mFragmentAccount.onReady(inv);
        mFragmentCallToUpgrade.onReady(inv);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
