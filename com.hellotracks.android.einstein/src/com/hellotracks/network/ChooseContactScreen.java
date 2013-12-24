package com.hellotracks.network;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;

public class ChooseContactScreen extends BasicAbstractScreen {

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        final LazyAdapter adapter = new LazyAdapter(this, array) {
            @Override
            public View getView(final int index, View convertView, ViewGroup parent) {
                View vi = super.getView(index, convertView, parent);
                int type = getInt(index, "type");

                TextView title = (TextView) vi.findViewById(R.id.title);
                TextView info = (TextView) vi.findViewById(R.id.info);
                if (type == TYPE_INVITATION || type == TYPE_RECOMMENDATION) {
                    info.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_button_insta_one));
                    title.setVisibility(View.GONE);
                } else {
                    info.setBackgroundDrawable(null);
                    title.setVisibility(View.VISIBLE);
                }

                View ignore = vi.findViewById(R.id.ignore);
                if ((type & TYPE_EXTERNAL) > 0) {
                    ignore.setVisibility(View.VISIBLE);
                    ignore.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            try {
                                JSONObject obj = prepareObj();
                                obj.put("id", getId(index));

                                doAction(AbstractScreen.ACTION_NOTINTERESTED, obj,
                                        getResources().getString(R.string.SendNow), new ResultWorker() {
                                            @Override
                                            public void onResult(String result, Context context) {
                                                ChooseContactScreen.this.adapter.remove(index);
                                            }
                                        });

                            } catch (Exception exc) {
                                Log.w(exc);
                            }
                        }
                    });
                } else {
                    ignore.setVisibility(View.GONE);
                }
                return vi;
            }

            @Override
            protected int getListItemLayoutFor(int index) {
                return R.layout.list_item_network;
            }
        };
        return adapter;
    }

    @Override
    protected String getAction() {
        return action;
    }

    @Override
    protected Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(C.type, type);
        if (ACTION_NETWORK.equals(action)) {
            String uid = account == null ? Prefs.get(this).getString(Prefs.USERNAME, "") : account;
            params.put(C.account, uid);
            params.put("include", TYPE_INVITATION | TYPE_RECOMMENDATION);
        } else {
            params.put("cnt", 25);
        }
        if (ACTION_SEARCH.equals(action)) {
            params.put("search", search);
        }
        return params;
    }

    @Override
    protected int getContentView() {
        return R.layout.screen_network;
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.NoEntries;
    }

    private String action;
    private String type;
    private String search;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        action = getIntent().getStringExtra(C.action);
        if (action == null)
            action = ACTION_NETWORK;
        type = getIntent().getStringExtra(C.type);
        account = getIntent().getStringExtra(C.account);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                Intent intent = new Intent();
                intent.putExtra(C.account, adapter.getAccount(pos));
                intent.putExtra(C.name, adapter.getString(pos, "title"));
                setResult(1, intent);
                finish();
            }
        });

        refill();
    }

    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.REQUESTCODE_CONTACT()) {
            setResult(resultCode, data);
            finish();
        }
    }




    

}