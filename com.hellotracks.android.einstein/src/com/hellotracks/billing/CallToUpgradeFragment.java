package com.hellotracks.billing;

import java.util.Collection;
import java.util.LinkedList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioGroup;

import com.hellotracks.Logger;
import com.hellotracks.R;
import com.hellotracks.account.IUpsell;
import com.hellotracks.account.ManagementScreen;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.SkuDetails;

public class CallToUpgradeFragment extends Fragment {

    private PlanHolder[] plans = new PlanHolder[0];
    private PlanHolder selectedPlan = null;
    private Button mSubmitButton;
    private RadioGroup mGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final IUpsell activity = (IUpsell) getActivity();

        View v = inflater.inflate(R.layout.fragment_billing_calltoupgrade, container, false);

        if (getActivity() instanceof ManagementScreen) {
            v.findViewById(R.id.layout_header).setVisibility(View.GONE);
        }

        mSubmitButton = (Button) v.findViewById(R.id.buttonSubmit);
        mSubmitButton.setEnabled(false);
        mSubmitButton.setText(R.string.SelectAPlan);
        mSubmitButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (selectedPlan != null) {
                    activity.setSelectedPlan(selectedPlan);
                    activity.checkout();
                }
            }
        });

        View back = v.findViewById(R.id.buttonBack);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });

        mGroup = (RadioGroup) v.findViewById(R.id.radioGroup);

        if (activity.getInventory() != null) {
            onReady(activity.getInventory());
        }
        return v;
    }

    public PlanHolder getSelectedPlan() {
        return selectedPlan;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mInventory != null && plans.length == 0) {
            onReady(mInventory);
        }
    }

    private Inventory mInventory;

    public void onReady(Inventory inv) {
        if (inv == null)
            return;

        this.mInventory = inv;

        if (!isAdded())
            return;

        try {
            Collection<SkuDetails> list = new LinkedList<SkuDetails>();
            for (SKU sku : SKU.values()) {
                SkuDetails sd = inv.getSkuDetails(sku.name());
                if (sd != null) {
                    list.add(sd);
                }
            }
            configurePlans(list.toArray(new SkuDetails[0]));
            if (selectedPlan == null) {
                mSubmitButton.setText(R.string.SelectAPlan);
            }
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    public void configurePlans(final SkuDetails[] paymentPlans) {
        plans = new PlanHolder[paymentPlans.length + 1];
        for (int i = 0; i < paymentPlans.length; i++) {
            plans[i] = new PlanHolder(getActivity(), mGroup, paymentPlans[i]);
            final int index = i;
            plans[i].radio.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (int i = 0; i < plans.length; i++) {
                            if (i == index) {
                                plans[i].view.setBackgroundResource(R.drawable.button_flat_payment_plan_active);
                                plans[i].radio.setChecked(true);
                                selectedPlan = plans[i];
                                mSubmitButton.setEnabled(true);
                                mSubmitButton.setText(R.string.Subscribe);
                            } else {
                                plans[i].view.setBackgroundResource(R.drawable.button_flat_payment_plan);
                                plans[i].radio.setChecked(false);
                            }
                        }
                    }
                }
            });
        }
        plans[plans.length - 1] = new PlanHolder(getActivity(), mGroup, getResources().getString(
                R.string.HellotracksBusiness), getResources().getString(R.string.FreeTrial), getResources().getString(
                R.string.BusinessShortDesc));

        if (plans.length > 0) {
            plans[0].radio.setChecked(true);
        }
    }

}
