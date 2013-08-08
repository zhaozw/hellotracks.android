package com.hellotracks.ui;

import org.json.JSONObject;

import android.app.AlertDialog;
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
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.ChangeUserScreen;
import com.hellotracks.einstein.AccountSettingsScreen;
import com.hellotracks.einstein.C;
import com.hellotracks.model.ResultWorker;


public class SettingsFragment extends Fragment {

    private View mView;
    private TextView mAccountText;
    private Button mLoginOutButton;
    private Button mSubscribeOrCancelButton;
    private View mPlanActiveLayout;
    private TextView mPlanText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_settings, null);
        
        Button buttonLogInOut = (Button) mView.findViewById(R.id.buttonLogInOut);
        buttonLogInOut.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onChangeUser(v);
            }
        });
        
        return mView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        String username = Prefs.get(getActivity()).getString(Prefs.USERNAME, "");
        ((TextView) mView.findViewById(R.id.textUsername)).setText(username);
        TextView websiteView = (TextView) mView.findViewById(R.id.textWebsiteInfo);
        TextView devInfoView = (TextView) mView.findViewById(R.id.textDeviceInfo);
        try {
            String s = Secure
                    .getString(getActivity().getContentResolver(), Secure.ANDROID_ID);
            final String u = "+" + s.substring(4, 5) + s.substring(12, 16);
            if (username.equals(u)) {
                String deviceInfo = Build.MANUFACTURER.toUpperCase() + " "
                        + Build.MODEL;
                devInfoView.setText(deviceInfo);
                websiteView.setText(getResources().getString(
                        R.string.WebsiteInfo, username,
                        Prefs.get(getActivity()).getString(Prefs.PASSWORD, "")));
            } else {
                devInfoView.setText("");
                websiteView.setText(getResources().getString(
                        R.string.WebsiteInfo, username, ""));
            }
        } catch (Exception exc) {
            Log.w(exc);
        }
    }



    public void onChangeUser(View view) {
        Intent intent = new Intent(getActivity(), ChangeUserScreen.class);
        startActivity(intent);
    }

    public void onDeleteAccount(View view) {
        final AlertDialog.Builder alert1 = new AlertDialog.Builder(getActivity());
        alert1.setMessage(R.string.ReallyDeleteAccount);
        alert1.setPositiveButton(R.string.Yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(
                                getActivity());
                        alert.setMessage(R.string.DeleteAccount);
                        final EditText input = new EditText(getActivity());
                        input.setHint(R.string.PleaseGiveUsFeedbackWhyDelete);
                        alert.setView(input);
                        alert.setPositiveButton(
                                getResources()
                                        .getString(R.string.DeleteAccount),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        String value = input.getText()
                                                .toString().trim();
                                        // sendDeactivate(value);
                                    }
                                });
                        alert.setNegativeButton(
                                getResources().getString(R.string.Cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        dialog.cancel();
                                    }
                                });
                        alert.show();
                    }
                });
        alert1.setNegativeButton(getResources().getString(R.string.Cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        alert1.show();
    }

    

}
