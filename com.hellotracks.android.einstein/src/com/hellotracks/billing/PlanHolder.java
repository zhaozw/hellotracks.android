package com.hellotracks.billing;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.billing.util.SkuDetails;
import com.hellotracks.util.Ui;

public class PlanHolder {
    public View view;
    public RadioButton radio;
    public SkuDetails paymentPlan;


    public PlanHolder(Activity activity, RadioGroup group, SkuDetails pp) {
        this.paymentPlan = pp;
        
        String title = pp.getTitle();
        int idx = title.indexOf("(");
        if (idx > 0) {
            title = title.substring(0, idx);
        }    
        
        String priceDesc = pp.getPrice();
        if (pp.getSku().endsWith("monthly")) {
            priceDesc += activity.getResources().getString(R.string.PerMonth);
        }
        
        init(activity, group, title, priceDesc, pp.getDescription());


        this.view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PlanHolder.this.radio.setChecked(true);
            }
        });
    }
    
    public PlanHolder(final Activity activity, RadioGroup group, String title, String priceDesc, String desc) {
        init(activity, group, title, priceDesc, desc);
        
        this.view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String titleText =  activity.getString(R.string.HellotracksBusiness);
                String username = Prefs.get(activity).getString(Prefs.USERNAME, "");
                String name = Prefs.get(activity).getString(Prefs.NAME, "");
                String bodyText = activity.getResources().getString(R.string.BusinessEmail, name, username);
                Intent send = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:business@hellotracks.com?subject=" + Uri.encode(titleText) + "&body=" + Uri.encode(bodyText);
                Uri uri = Uri.parse(uriText);
                send.setData(uri);
                activity.startActivity(Intent.createChooser(send, activity.getString(R.string.HellotracksBusiness)));
            }
        });
    }
    
    private void init(Activity activity, RadioGroup group, String title, String priceDesc, String desc) {
        this.view = Ui
                .inflateAndReturnInflatedView(activity.getLayoutInflater(), R.layout.include_payment_plan, group);
        this.radio = (RadioButton) view.findViewById(R.id.radioButton);    
   
        ((TextView) this.view.findViewById(R.id.textTitle)).setText(title);
        ((TextView) this.view.findViewById(R.id.textPrice)).setText(priceDesc);
        ((TextView) this.view.findViewById(R.id.textDesc)).setText(desc);
    }

    public RadioButton getRadio() {
        return radio;
    }

    public View getView() {
        return view;
    }
}