package com.hellotracks.einstein;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;

public class RemoteActivationInfoScreen extends AbstractScreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_remoteactivation_info);

        TextView text = (TextView) findViewById(R.id.textSignInWithUsernameAndPassword);
        String username = Prefs.get(this).getString(Prefs.USERNAME, "");
        String password = Prefs.get(this).getString(Prefs.PASSWORD, "");

        text.setText(getResources().getString(R.string.RemoteActivationDesc4, username, password));

    }

    public void onOK(View view) {
        finish();
    }

}
