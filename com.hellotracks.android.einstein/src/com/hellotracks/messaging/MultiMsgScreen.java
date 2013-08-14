package com.hellotracks.messaging;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.util.ResultWorker;

public class MultiMsgScreen extends AbstractScreen {

    protected ListView list;

    private String[] receivers;

    private TextView messageText;
    private TextView text;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_multimsg);
        list = (ListView) findViewById(R.id.list);
        receivers = getIntent().getStringArrayExtra("receivers");
        text = (TextView) findViewById(R.id.text);
        messageText = (TextView) findViewById(R.id.messageText);
        String[] names = getIntent().getStringArrayExtra("names");
        StringBuilder sb = new StringBuilder();
        sb.append("An");
        sb.append(":");
        for (int i = 0; i < names.length; i++) {
            sb.append("\n");
            sb.append(names[i]);
        }
        text.setText(sb.toString());

        setupActionBar(R.string.Messages);
    }


    public void onSend(View view) {
        String message = messageText.getText().toString();
        if (message.trim().length() == 0)
            return;
        messageText.setText("");
        sendMessage(receivers, message, new ResultWorker() {
            @Override
            public void onResult(String result, Context context) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBack(null);
                    }
                });
            }
        });

    }

}