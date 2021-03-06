package com.hellotracks.account;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.api.API;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.AbstractScreenWithIAB;
import com.hellotracks.base.C;
import com.hellotracks.billing.SKU;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.Payload;
import com.hellotracks.billing.util.Purchase;
import com.hellotracks.billing.util.SkuDetails;
import com.hellotracks.util.PlanUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.Utils;

public class AccountFragment extends Fragment {

    private View mView;
    private TextView mAccountText;
    private Button mLoginOutButton;
    private Button mSubscribeOrCancelButton;
    private View mPlanActiveLayout;
    private TextView mPlanText;
    private Activity mActivity;
    private Purchase mPurchase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.management_account, null);

        mActivity = getActivity();

        mPlanText = (TextView) mView.findViewById(R.id.textPlan);
        mSubscribeOrCancelButton = (Button) mView.findViewById(R.id.buttonSubscribeOrCancel);

        Button logoutButton = (Button) mView.findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onLogout(v);
            }
        });

        return mView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == C.RESULTCODE_CLOSEAPP) {
            getActivity().setResult(resultCode);
            getActivity().finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        String username = Prefs.get(getActivity()).getString(Prefs.USERNAME, "");
        String password = Prefs.get(getActivity()).getString(Prefs.PASSWORD, "");
        ((TextView) mView.findViewById(R.id.textUsername)).setText(username);
        TextView websiteView = (TextView) mView.findViewById(R.id.textWebsiteInfo);
        TextView devInfoView = (TextView) mView.findViewById(R.id.textDeviceInfo);
        TextView passwordView = (TextView) mView.findViewById(R.id.textPassword);

        View deviceInfoLayout = mView.findViewById(R.id.layoutDeviceInfo);
        try {
            if (username.equals(Utils.getDeviceAccountUsername(getActivity()))) {
                String deviceInfo = Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
                devInfoView.setText(deviceInfo);
                websiteView.setText(getResources().getString(R.string.WebsiteInfo, username, password));
                passwordView.setText(password);
            } else {
                deviceInfoLayout.setVisibility(View.GONE);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < password.length(); i++) {
                    sb.append("*");
                }
                websiteView.setText(getResources().getString(R.string.WebsiteInfo, username, sb.toString()));
                passwordView.setText(sb.toString());
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }

        if (mInventory != null) {
            onReady(mInventory);
        }
    }

    private Inventory mInventory;

    public void onReady(Inventory inventory) {
        try {
            if (inventory == null)
                return;

            this.mInventory = inventory;
            if (!isAdded()) {
                return;
            }
            // purchased first            
            Map<SkuDetails, Purchase> purchases = AbstractScreenWithIAB.getPurchases(getActivity(), mInventory);
            if (purchases.size() > 0) {
                Entry<SkuDetails, Purchase> entry = purchases.entrySet().iterator().next();           
                mPurchase = entry.getValue();
                updatePlan(mPurchase, entry.getKey());
                return;
            }
            // canceled or refunded
            Map<SkuDetails, Purchase> canceled = AbstractScreenWithIAB.getCanceledOrRefundedPurchases(getActivity(), mInventory);
            if (purchases.size() > 0) {
                Entry<SkuDetails, Purchase> entry = canceled.entrySet().iterator().next();           
                mPurchase = entry.getValue();
                updatePlan(mPurchase, entry.getKey());
                return;
            }

            // else
            updatePlan(null, null);
        } catch (Exception exc) {
            Logger.e("", exc);
        }
    }

    private void updatePlan(Purchase p, SkuDetails sd) {
        if (p != null && sd != null) {
            if (p.getPurchaseState() == Purchase.STATE_PURCHASED) {
                String title = sd.getTitle();
                int idx = title.indexOf("(");
                if (idx > 0) {
                    title = title.substring(0, idx);
                }
                mPlanText.setText(title);
                updateSubscriptionButton(false, p);
            } else if (p.getPurchaseState() == Purchase.STATE_CANCELED) {
                mPlanText.setText(R.string.PlanCanceled);
                updateSubscriptionButton(true, p);
            } else if (p.getPurchaseState() == Purchase.STATE_REFUNDED) {
                mPlanText.setText(R.string.PlanRefunded);
                updateSubscriptionButton(true, p);
            }
            PlanUtils.savePurchase(getActivity(), p, false);
        } else {
            mPlanText.setText(R.string.NoPlan);
            updateSubscriptionButton(true, p);
        }
    }

    private void updateSubscriptionButton(boolean subscribe, Purchase purchase) {
        if (subscribe) {
            mSubscribeOrCancelButton.setVisibility(View.VISIBLE);
            mSubscribeOrCancelButton.setText(R.string.Subscribe);
            mSubscribeOrCancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ((IUpsell) mActivity).upsell();
                }
            });
        } else {
            if (purchase != null && purchase.getSku().endsWith("monthly")) {
                mSubscribeOrCancelButton.setVisibility(View.VISIBLE);
                mSubscribeOrCancelButton.setText(R.string.Unsubscribe);
                mSubscribeOrCancelButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialog dlg = Ui.createAlertDialogBuilderCompat(mActivity)
                                .setMessage(R.string.PleaseCancelYourSubscriptionInGooglePlay)
                                .setPositiveButton(R.string.OpenGooglePlay, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://")));
                                        } catch (android.content.ActivityNotFoundException anfe) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                                    .parse("http://play.google.com/store/apps")));
                                        }
                                    }
                                }).setNegativeButton(R.string.Close, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).setCancelable(true).create();
                        dlg.setCanceledOnTouchOutside(true);
                        dlg.show();
                    }
                });
            } else {
                mSubscribeOrCancelButton.setVisibility(View.GONE);
            }
        }
    }

    public void onLogout(View view) {
        new AlertDialog.Builder(getActivity()).setTitle(Prefs.get(mActivity).getString(Prefs.USERNAME, ""))
                .setMessage(R.string.logoutText)
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setNeutralButton(R.string.DeleteAccount, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dlg, int arg1) {
                        onDeleteAccount();
                    }
                }).setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        doLogout();
                    }
                }).show();
    }

    private void onDeleteAccount() {
        final AlertDialog.Builder alert1 = new AlertDialog.Builder(getActivity());
        alert1.setMessage(R.string.ReallyDeleteAccount);
        alert1.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage(R.string.DeleteAccount);
                final EditText input = new EditText(getActivity());
                input.setHint(R.string.PleaseGiveUsFeedbackWhyDelete);
                alert.setView(input);
                alert.setPositiveButton(getResources().getString(R.string.DeleteAccount),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String value = input.getText().toString().trim();
                                sendDeactivate(value);
                            }
                        });
                alert.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        });
                alert.show();
            }
        });
        alert1.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert1.show();
    }

    private void openLoginDialog() {
        Intent intent = new Intent(getActivity(), LoginScreen.class);
        startActivityForResult(intent, C.REQUESTCODE_LOGIN);
    }

    private void sendDeactivate(String value) {
        try {
            JSONObject obj = AbstractScreen.prepareObj(getActivity());
            obj.put("msg", value);

            API.doAction(getActivity(), AbstractScreen.ACTION_DEACTIVATE, obj,
                    getResources().getString(R.string.DeleteAccount), new ResultWorker() {

                        @Override
                        public void onResult(final String result, Context context) {
                            doLogout();
                        }

                    });

        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private void doLogout() {
        Prefs.removeAllLogout(getActivity());
        AbstractScreen.stopAllServices(getActivity());
        getActivity().finish();
    }

}
