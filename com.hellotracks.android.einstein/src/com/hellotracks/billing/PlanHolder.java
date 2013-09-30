package com.hellotracks.billing;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.hellotracks.R;
import com.hellotracks.billing.util.SkuDetails;
import com.hellotracks.util.Ui;

public class PlanHolder {
    public View view;
    public RadioButton radio;
    public SkuDetails paymentPlan;


    public PlanHolder(Activity activity, RadioGroup group, SkuDetails pp) {
        this.paymentPlan = pp;
        this.view = Ui
                .inflateAndReturnInflatedView(activity.getLayoutInflater(), R.layout.include_payment_plan, group);
        this.radio = (RadioButton) view.findViewById(R.id.radioButton);
        
        String title = pp.getTitle();
        
        int idx = title.indexOf("(");
        if (idx > 0) {
            title = title.substring(0, idx);
        }
        ((TextView) this.view.findViewById(R.id.textTitle)).setText(title);
        
        
        String priceDesc = pp.getPrice();
        if (pp.getSku().endsWith("monthly")) {
            priceDesc += activity.getResources().getString(R.string.PerMonth);
        }
        ((TextView) this.view.findViewById(R.id.textPrice)).setText(priceDesc);
        ((TextView) this.view.findViewById(R.id.textDesc)).setText(pp.getDescription());

        this.view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PlanHolder.this.radio.setChecked(true);
            }
        });
    }

    public RadioButton getRadio() {
        return radio;
    }

    public View getView() {
        return view;
    }
}