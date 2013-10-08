package com.hellotracks.messaging;

import org.json.JSONArray;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.Ui;
import com.hellotracks.util.lazylist.LazyAdapter;

public class ConversationListScreen extends BasicAbstractScreen {

    private MenuItem mItemMultiSend;
    private ConversationsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar(R.string.Map);
        list.setOnItemClickListener(new ConversationClickListener(mAdapter, list));
    }
    

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        if (list.getFooterViewsCount() == 0) {
            View footer = getLayoutInflater().inflate(R.layout.list_header_text, null);
            TextView text = (TextView) footer.findViewById(R.id.text);
            text.setText(R.string.ConversationsDesc);
            list.addFooterView(footer);
        }
        mAdapter = new ConversationsAdapter(this, array, new Runnable() {

            @Override
            public void run() {
                supportInvalidateOptionsMenu();
            }

        });
        return mAdapter;
    }

    @Override
    protected int getContentView() {
        return R.layout.screen_conversations;
    }

    @Override
    protected String getAction() {
        return ACTION_CONVERSATIONS;
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Ui.makeText(context, intent.getStringExtra("msg"), Toast.LENGTH_LONG).show();
            refill();
        }
    };

    private BroadcastReceiver tabActivatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refill();
        }
    };

    protected void onResume() {
        refill();
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

    @Override
    protected int getEmptyMessage() {
        return R.string.NoMessagesDesc;
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

    public class ConversationClickListener implements OnItemClickListener {

        ListView list;

        public ConversationClickListener(LazyAdapter adapter, ListView list) {
            this.list = list;
        }

        @Override
        public void onItemClick(AdapterView<?> ad, View view, final int pos, long id) {
            try {
                if (mAdapter != null) {
                    String account = mAdapter.getAccount(pos);
                    if (account != null && account.length() > 0) {
                        Intent intent = new Intent(getApplicationContext(), ConversationScreen.class);
                        intent.putExtra(C.account, mAdapter.getAccount(pos));
                        intent.putExtra(C.name, mAdapter.getString(pos, C.name));
                        startActivityForResult(intent, 0);
                    }
                }
            } catch (Exception exc) {
                Log.e(exc);
            }
        }
    }

    public void onMultiMsg(View view) {
        FlurryAgent.logEvent("MultiMsg");
        Intent intent = new Intent(ConversationListScreen.this, MultiMsgScreen.class);
        intent.putExtra("receivers", mAdapter.getSelectedAccounts().toArray(new String[0]));
        intent.putExtra("names", mAdapter.getSelectedNames().toArray(new String[0]));
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
        super.onCreateOptionsMenu(bar);
        mItemMultiSend = bar.add(1, Menu.NONE, Menu.NONE, R.string.SendMessage);
        MenuItemCompat.setShowAsAction(mItemMultiSend, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        mItemMultiSend.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                onMultiMsg(null);
                return false;
            }
        });
        mItemMultiSend.setVisible(adapter != null && mAdapter.getSelectedAccounts().size() > 0);
        return true;
    }
}