package com.hellotracks.panic;

import android.os.Bundle;
import android.view.View;

import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;

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
