package com.hellotracks.billing;

import java.text.SimpleDateFormat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hellotracks.R;
import com.hellotracks.account.AccountManagementActivity;
import com.hellotracks.billing.util.Purchase;

public class ConfirmationFragment extends Fragment {

    public ConfirmationFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMMMMMM dd, yyyy");

        final AccountManagementActivity activity = (AccountManagementActivity) getActivity();

        PlanHolder plan = activity.getSelectedPlan();
        Purchase purchase = activity.getPurchase();

        View v = inflater.inflate(R.layout.fragment_payment_confirmation, container, false);

        TextView textDate = (TextView) v.findViewById(R.id.textDate);
        TextView textPlan = (TextView) v.findViewById(R.id.textPlan);
        TextView textPrice = (TextView) v.findViewById(R.id.textPrice);

        textDate.setText(sdf.format(purchase.getPurchaseTime()));
        textPlan.setText(plan.paymentPlan.getTitle());
        textPrice.setText(plan.paymentPlan.getPrice());

        Button submit = (Button) v.findViewById(R.id.buttonSubmit);
        submit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });

        ImageButton back = (ImageButton) v.findViewById(R.id.buttonBack);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });

        TextView settingsText = (TextView) v.findViewById(R.id.textSettings);
        settingsText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });
        return v;
    }
}
