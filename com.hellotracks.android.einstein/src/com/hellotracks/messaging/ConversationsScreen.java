package com.hellotracks.messaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.flurry.android.FlurryAgent;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;

public class ConversationsScreen extends AbstractScreen {

    protected ListView list;

    private String lastData = null;

    private TextView statusLabel;
    
    private MenuItem mItemMultiSend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_conversations);

        setupActionBar(R.string.Map);

        list = (ListView) findViewById(R.id.list);

        statusLabel = (TextView) findViewById(R.id.statusLabel);

        refill();
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, intent.getStringExtra("msg"), Toast.LENGTH_LONG).show();
            refill();
        }
    };

    private BroadcastReceiver tabActivatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refill();
        }
    };

    private void refill() {
        try {
            final String username = Prefs.get(this).getString(Prefs.USERNAME, "");
            String cache = Prefs.get(this).getString("cache_conversations_" + username, null);
            if (cache != null) {
                try {
                    if (!cache.equals(lastData)) {
                        setData(cache);
                    }
                } catch (JSONException exc) {
                    Log.w(exc);
                }
            }

            JSONObject obj = prepareObj();
            doAction(ACTION_CONVERSATIONS, obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    Prefs.get(ConversationsScreen.this).edit().putString("cache_conversations_" + username, result)
                            .commit();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                setData(result);
                            } catch (Exception exc) {
                                Log.w(exc);
                            }
                        }

                    });
                }
            });

        } catch (Exception exc2) {
            Log.w(exc2);
        }
    }

    protected void onResume() {
        registerReceiver(tabActivatedReceiver, new IntentFilter(Prefs.TAB_MESSAGES_INTENT));
        registerReceiver(messageReceiver, new IntentFilter(Prefs.PUSH_INTENT));
        super.onResume();
    };

    @Override
    protected void onPause() {
        unregisterReceiver(tabActivatedReceiver);
        unregisterReceiver(messageReceiver);
        super.onPause();
    }

    private ConversationsAdapter adapter;

    protected void setData(String result) throws Exception {
        adapter = new ConversationsAdapter(this, new JSONArray(result), new Runnable() {

            @Override
            public void run() {
                supportInvalidateOptionsMenu();
            }

        });
        list.setAdapter(adapter);
        list.setOnItemClickListener(new MessageClickListener(adapter, list));
        lastData = result;

        if (result.length() <= 2) {
            statusLabel.setVisibility(View.VISIBLE);
            statusLabel.setText(R.string.NoMessagesDesc);
        } else {
            statusLabel.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == C.REQUESTCODE_CONTACT) {
            if (data != null && data.getStringExtra(C.account) != null) {
                String account = data.getStringExtra(C.account);
                String name = data.getStringExtra(C.name);
                Intent intent = new Intent(getApplicationContext(), ConversationScreen.class);
                intent.putExtra(C.account, account);
                intent.putExtra(C.name, name);
                startActivityForResult(intent, 0);
            }
            return;
        }
        refill();
    }

    public class MessageClickListener implements OnItemClickListener {

        LazyAdapter adapter;
        ListView list;

        public MessageClickListener(LazyAdapter adapter, ListView list) {
            this.adapter = adapter;
            this.list = list;
        }

        @Override
        public void onItemClick(AdapterView<?> ad, View view, final int pos, long id) {
            Intent intent = new Intent(getApplicationContext(), ConversationScreen.class);
            intent.putExtra(C.account, adapter.getAccount(pos));
            intent.putExtra(C.name, adapter.getString(pos, C.name));
            startActivityForResult(intent, 0);
        }
    }

    public void onMultiMsg(View view) {
        FlurryAgent.logEvent("MultiMsg");
        Intent intent = new Intent(ConversationsScreen.this, MultiMsgScreen.class);
        intent.putExtra("receivers", adapter.getSelectedAccounts().toArray(new String[0]));
        intent.putExtra("names", adapter.getSelectedNames().toArray(new String[0]));
        startActivityForResult(intent, C.REQUESTCODE_MSG);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu bar) {
        mItemMultiSend = bar.add(1, Menu.NONE, Menu.NONE, R.string.SendMessage);
        mItemMultiSend.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mItemMultiSend.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                onMultiMsg(null);
                return false;
            }
        });
        mItemMultiSend.setVisible(adapter != null && adapter.getSelectedAccounts().size() > 0);
        return true;
    }
}