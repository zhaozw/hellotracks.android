package com.hellotracks.einstein;

import android.os.Bundle;
import android.view.View;

import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;

public class PanicInfoScreen extends AbstractScreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_panic_info);

    }

    public void onOK(View view) {
        finish();
    }

}
