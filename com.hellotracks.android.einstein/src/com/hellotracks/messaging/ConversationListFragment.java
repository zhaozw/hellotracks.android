package com.hellotracks.messaging;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.BasicAbstractFragment;
import com.hellotracks.base.C;
import com.hellotracks.util.FlurryAgent;
import com.hellotracks.util.lazylist.LazyAdapter;

public class ConversationListFragment extends BasicAbstractFragment {

    private MenuItem mItemMultiSend;
    private ConversationsAdapter mAdapter;

    public ConversationListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        list.setOnItemClickListener(new ConversationClickListener(mAdapter, list));
        refill();
    }

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        if (list.getFooterViewsCount() == 0) {
            View footer = getActivity().getLayoutInflater().inflate(R.layout.list_header_text, null);
            TextView text = (TextView) footer.findViewById(R.id.text);
            text.setText(R.string.ConversationsDesc);
            list.addFooterView(footer);
        }
        mAdapter = new ConversationsAdapter(getActivity(), array, new Runnable() {

            @Override
            public void run() {
                getActivity().supportInvalidateOptionsMenu();
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
        return AbstractScreen.ACTION_CONVERSATIONS;
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.NoMessagesDesc;
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
                        String name = mAdapter.getString(pos, C.name);
                        ((MessagesScreen) getActivity()).selectConversationWithAccount(account, name);
                    }
                }
            } catch (Exception exc) {
                Log.e(exc);
            }
        }
    }

    public int count() {
        return (adapter != null) ? mAdapter.getSelectedAccounts().size() : 0;
    }

    public void onMultiMsg(View view) {
        FlurryAgent.logEvent("MultiMsg");
        Intent intent = new Intent(getActivity(), MultiMsgScreen.class);
        intent.putExtra("receivers", mAdapter.getSelectedAccounts().toArray(new String[0]));
        intent.putExtra("names", mAdapter.getSelectedNames().toArray(new String[0]));
        startActivityForResult(intent, C.REQUESTCODE_MSG);
    }

    //    @Override
    //    public boolean onCreateOptionsMenu(Menu bar) {
    //        super.onCreateOptionsMenu(bar);
    //        mItemMultiSend = bar.add(1, Menu.NONE, Menu.NONE, R.string.SendMessage);
    //        mItemMultiSend.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    //        mItemMultiSend.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    //
    //            public boolean onMenuItemClick(MenuItem item) {
    //                onMultiMsg(null);
    //                return false;
    //            }
    //        });
    //        mItemMultiSend.setVisible(adapter != null && mAdapter.getSelectedAccounts().size() > 0);
    //        return true;
    //    }
}