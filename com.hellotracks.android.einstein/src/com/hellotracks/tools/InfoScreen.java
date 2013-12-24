package com.hellotracks.tools;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;

public class InfoScreen extends AbstractScreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_info);
        
        TextView text = (TextView) findViewById(R.id.text);
        text.setText(Html.fromHtml(getResources().getString(R.string.InfoTextHTML)));
    }
    
    public void onOK(View view) {
        finish();
    }

}
