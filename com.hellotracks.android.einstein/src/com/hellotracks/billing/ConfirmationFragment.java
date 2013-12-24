package com.hellotracks.billing;

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
import com.hellotracks.account.ManagementScreen;

public class ConfirmationFragment extends Fragment {

    public ConfirmationFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final AccountManagementActivity activity = (AccountManagementActivity) getActivity();

        PlanHolder plan = activity.getSelectedPlan();

        View v = inflater.inflate(R.layout.fragment_billing_finish, container, false);

        if (getActivity() instanceof ManagementScreen) {
            v.findViewById(R.id.layout_header).setVisibility(View.GONE);
        }
        TextView textPlan = (TextView) v.findViewById(R.id.textPlan);
        TextView textPrice = (TextView) v.findViewById(R.id.textPrice);

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
        return v;
    }
}
