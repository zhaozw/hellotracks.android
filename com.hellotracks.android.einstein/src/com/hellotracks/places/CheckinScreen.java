package com.hellotracks.places;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.c2dm.LauncherUtils;
import com.hellotracks.map.Actions;
import com.hellotracks.network.PlaceListScreen;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;

public class CheckinScreen extends PlaceListScreen {

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        final LazyAdapter adapter = new LazyAdapter(this, array) {

            @Override
            public View getView(final int index, View convertView, ViewGroup parent) {

                final View vi = convertView != null ? convertView : inflater
                        .inflate(R.layout.list_item_mynetwork, null);

                try {
                    JSONObject node = data.get(index);

                    TextView title = (TextView) vi.findViewById(R.id.nameText);
                    TextView info = (TextView) vi.findViewById(R.id.messageText);
                    TextView time = (TextView) vi.findViewById(R.id.timeText);
                    time.setVisibility(View.VISIBLE);

                    time.setText(node.has("time") ? node.getString("time") : "");
                    title.setText(node.getString("title"));
                    info.setText(node.getString("info"));

                    final ImageView icon = (ImageView) vi.findViewById(R.id.icon);

                    String url = node.getString("url");

                    if (url != null) {
                        icon.setImageDrawable(getResources().getDrawable(R.drawable.button_flat_payment_plan));
                        Picasso.with(activity).load(url).into(icon);
                    } else {
                        icon.setVisibility(View.GONE);
                    }

                    View ignore = vi.findViewById(R.id.ignore);
                    ignore.setVisibility(View.GONE);
                } catch (Exception exc) {
                    Logger.w(exc);
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
        return ACTION_NETWORK;
    }

    @Override
    protected Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(C.type, C.place);
        params.put(C.account, account);
        params.put(C.include, 0);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar(R.string.CheckIn);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                Actions.doCheckIn(CheckinScreen.this, "", adapter.getAccount(pos), System.currentTimeMillis(),
                        new ResultWorker() {
                            @Override
                            public void onResult(String result, Context context) {
                                LauncherUtils.playNotificationSound(context);
                                Toast.makeText(getApplicationContext(), R.string.CheckInOK, Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError() {
                                Toast.makeText(getApplicationContext(), R.string.CheckServerConnection,
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int failure, Context context) {
                                Toast.makeText(getApplicationContext(), R.string.CheckServerConnection,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

}