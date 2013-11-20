package com.hellotracks.messaging;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.google.analytics.tracking.android.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.Ui;

public class MessagesScreen extends AbstractScreen {
    private SlidingPaneLayout mSlidingLayout;
    private ConversationListFragment mConversationListFragment;
    private ConversationFragment mConversationFragment;
    private MenuItem mItemClearAll;
    private MenuItem mItemMultiSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sliding_pane_layout);

        mSlidingLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane_layout);

        mConversationListFragment = new ConversationListFragment();
        mConversationFragment = new ConversationFragment();
        if (getIntent() != null && getIntent().getExtras() != null) {
            mConversationFragment.setArguments(getIntent().getExtras());
        }
        getSupportFragmentManager().beginTransaction().add(R.id.left_pane, mConversationListFragment, "pane1").commit();
        getSupportFragmentManager().beginTransaction().add(R.id.content_pane, mConversationFragment, "pane2").commit();

        supportInvalidateOptionsMenu();
        setupActionBar(R.string.Messages);

        registerReceiver(messageReceiver, new IntentFilter(Prefs.PUSH_INTENT));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Ui.makeText(context, intent.getStringExtra("msg"), Toast.LENGTH_LONG).show();
            refill();
        }
    };

    private void refill() {
        mConversationFragment.refill();
        mConversationListFragment.refill();
    }

    public void selectConversationWithAccount(String account, String name) {
        mConversationFragment.setData(account, name);
        mSlidingLayout.closePane();
        getSupportActionBar().setTitle(name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mItemClearAll = menu.add(1, Menu.NONE, Menu.NONE, R.string.ClearAll);
        mItemClearAll.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        mItemClearAll.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                mConversationFragment.onTrash(null);
                return false;
            }
        });

        mItemMultiSend = menu.add(1, Menu.NONE, Menu.NONE, R.string.SendMessage);
        mItemMultiSend.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mItemMultiSend.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                mConversationListFragment.onMultiMsg(null);
                return false;
            }
        });

        mSlidingLayout.setPanelSlideListener(new SliderListener());
        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(C.account)) {
            mSlidingLayout.closePane();
        } else {
            mSlidingLayout.openPane();
        }

        setMenuItemsVisiblity();
        return true;
    }

    private void setMenuItemsVisiblity() {
        mItemMultiSend.setVisible(mConversationListFragment.count() > 0 && mSlidingLayout.isOpen());
        mItemClearAll.setVisible(!mSlidingLayout.isOpen());
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        /*
         * The action bar up action should open the slider if it is currently closed,
         * as the left pane contains content one level up in the navigation hierarchy.
         */
        if (item.getItemId() == android.R.id.home && !mSlidingLayout.isOpen()) {
            mSlidingLayout.smoothSlideOpen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This panel slide listener updates the action bar accordingly for each panel state.
     */
    private class SliderListener extends SlidingPaneLayout.SimplePanelSlideListener {
        @Override
        public void onPanelOpened(View panel) {
            getSupportActionBar().setTitle(R.string.Messages);
            Log.i("panel opened");
            mConversationListFragment.refill();
            setMenuItemsVisiblity();
        }

        @Override
        public void onPanelClosed(View panel) {
            setMenuItemsVisiblity();
        }
    }

    public void onSend(View view) {
        mConversationFragment.onSend(view);
    }

    public void onAddLocation(View view) {
        mConversationFragment.onAddLocation(view);
    }

    public static ArrayList<String> extractUrls(String msg) {
        ArrayList<String> list = new ArrayList<String>();
        int s = 0;

        int geo = msg.indexOf("geo:");
        if (geo > 0) {
            int text = msg.indexOf("text:");
            if (text < 0)
                text = msg.length();
            list.add(msg.substring(geo, text));
        }
        int nav = msg.indexOf("google.navigation:");
        if (nav > 0) {
            int text = msg.indexOf("text:");
            if (text < 0)
                text = msg.length();
            list.add(msg.substring(nav, text));
        }
        while (s < msg.length() - 1) {
            int i = msg.indexOf("http", s);
            int j = msg.indexOf("www.", s);
            int a = -1;
            if (i >= 0)
                a = i;
            if (j >= 0 && (i < 0 || j < i))
                a = j;
            if (a < 0)
                break;

            int b1 = msg.indexOf(" ", a);
            int b2 = msg.indexOf("\n", a);
            int b;
            if (b1 > 0 && b2 > 0) {
                b = Math.min(b1, b2);
            } else if (b1 > 0) {
                b = b1;
            } else if (b2 > 0) {
                b = b2;
            } else {
                b = msg.length();
            }

            String url = msg.substring(a, b);
            if (!url.startsWith("http"))
                url = "http://" + url;
            list.add(url);
            s = b;
        }
        return list;
    }
}
