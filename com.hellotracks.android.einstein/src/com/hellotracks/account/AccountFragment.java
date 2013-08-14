package com.hellotracks.account;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.billing.SKU;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.billing.util.Payload;
import com.hellotracks.billing.util.Purchase;
import com.hellotracks.billing.util.SkuDetails;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.Ui;

public class AccountFragment extends Fragment {

    private View mView;
    private TextView mAccountText;
    private Button mLoginOutButton;
    private Button mSubscribeOrCancelButton;
    private View mPlanActiveLayout;
    private TextView mPlanText;
    private AccountManagementActivity mActivity;
    private Purchase mPurchase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_account, null);

        mActivity = (AccountManagementActivity) getActivity();

        mPlanText = (TextView) mView.findViewById(R.id.textPlan);
        mSubscribeOrCancelButton = (Button) mView.findViewById(R.id.buttonSubscribeOrCancel);

        Button buttonLogInOut = (Button) mView.findViewById(R.id.buttonLogInOut);
        buttonLogInOut.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onChangeUser(v);
            }
        });

        View back = mView.findViewById(R.id.buttonBack);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mActivity.finish();
            }
        });

        return mView;
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
            String s = Secure.getString(getActivity().getContentResolver(), Secure.ANDROID_ID);
            final String u = "+" + s.substring(4, 5) + s.substring(12, 16);
            if (username.equals(u)) {
                String deviceInfo = Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
                devInfoView.setText(deviceInfo);
                websiteView.setText(getResources().getString(R.string.WebsiteInfo, username,
                        Prefs.get(getActivity()).getString(Prefs.PASSWORD, "")));
                passwordView.setText(password);
            } else {
                deviceInfoLayout.setVisibility(View.GONE);
                websiteView.setText(getResources().getString(R.string.WebsiteInfo, username, ""));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < password.length(); i++) {
                    sb.append("*");
                }
                passwordView.setText(sb.toString());
            }
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onReady(Inventory inventory) {
        try {
            for (String sku : SKU.names()) {
                Purchase p = inventory.getPurchase(sku);
                if (p != null && Payload.verifyPayload(getActivity(), p.getDeveloperPayload())) {
                    // already a purchase!
                    mPurchase = p;

                    SkuDetails sd = inventory.getSkuDetails(sku);
                    updatePlan(p, sd);
                    return;
                }
            }
            updatePlan(null, null);
        } catch (Exception exc) {
            Log.e("", exc);
        }
    }

    private void updatePlan(Purchase p, SkuDetails sd) {
        if (p != null) {
            if (p.getPurchaseState() == Purchase.STATE_PURCHASED) {
                mPlanText.setText(sd.getTitle());
                updateSubscriptionButton(false);
            } else if (p.getPurchaseState() == Purchase.STATE_CANCELED) {
                mPlanText.setText(R.string.PlanCanceled);
                updateSubscriptionButton(true);
            } else if (p.getPurchaseState() == Purchase.STATE_REFUNDED) {
                mPlanText.setText(R.string.PlanRefunded);
                updateSubscriptionButton(true);
            }
        } else {
            mPlanText.setText(R.string.None);
            updateSubscriptionButton(true);
        }
    }

    private void updateSubscriptionButton(boolean subscribe) {
        if (subscribe) {
            mSubscribeOrCancelButton.setText(R.string.Subscribe);
            mSubscribeOrCancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    mActivity.upsell();
                }
            });
        } else {
            mSubscribeOrCancelButton.setText(R.string.Unsubscribe);
            mSubscribeOrCancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    AlertDialog dlg = Ui.createAlertDialogBuilderCompat(mActivity)
                            .setMessage(R.string.PleaseCancelYourSubscriptionInGooglePlay)
                            .setPositiveButton(R.string.OpenGooglePlay, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent("android.intent.action.VIEW");
                                    i.setComponent(new ComponentName("com.android.vending",
                                            "com.android.vending.MyDownloadsActivity"));
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(i);
                                    // TODO validate
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
        }
    }

    public void onChangeUser(View view) {
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
        Intent intent = new Intent(getActivity(), ChangeUserScreen.class);
        startActivity(intent);
    }

    private void sendDeactivate(String value) {
        try {
            JSONObject obj = AbstractScreen.prepareObj(getActivity());
            obj.put("msg", value);

            AbstractScreen.doAction(getActivity(), AbstractScreen.ACTION_DEACTIVATE, obj,
                    getResources().getString(R.string.DeleteAccount), new ResultWorker() {

                        @Override
                        public void onResult(final String result, Context context) {
                            doLogout();
                        }

                    });

        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    private void doLogout() {
        Prefs.get(getActivity()).edit().putString(C.account, null).putBoolean(Prefs.STATUS_ONOFF, false)
                .putString(Prefs.USERNAME, "").putString(Prefs.PASSWORD, "").commit();
        getActivity().stopService(new Intent(getActivity(), C.trackingServiceClass));
        Prefs.get(getActivity()).edit().putLong(Prefs.LAST_LOGOUT, System.currentTimeMillis()).commit();
        openLoginDialog();
    }

}
