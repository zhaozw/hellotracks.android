package com.hellotracks.network;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.places.PlacesAutocompleteActivity;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;

public class PlaceListScreen extends BasicAbstractScreen {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return true;
    }

    public void onEvent(final SearchMap.DirectionsResult result) {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.Search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(R.drawable.ic_action_search);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                startActivityForResult(new Intent(PlaceListScreen.this, AddPlaceScreen.class),
                        C.REQUESTCODE_GOOGLEPLACE);
                return false;
            }
        });

        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == C.REQUESTCODE_GOOGLEPLACE && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

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
        return R.string.PlacesEmptyDesc;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar(R.string.Places);
        registerCloseReceiverOn(C.BROADCAST_ADDTRACKTOMAP, C.BROADCAST_SHOWMAP);

        account = Prefs.get(this).getString(Prefs.USERNAME, "");

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                Intent intent = new Intent(PlaceListScreen.this, NewProfileScreen.class);
                intent.putExtra(C.account, adapter.getAccount(pos));
                intent.putExtra(C.name, adapter.getString(pos, "title"));
                startActivityForResult(intent, C.REQUESTCODE_CONTACT());
            }
        });

        {
            View footer = getLayoutInflater().inflate(R.layout.list_header_banner, null);
            ((ImageView) footer.findViewById(R.id.imageBanner)).setImageResource(R.drawable.buildings);
            list.addFooterView(footer);
        }

        refill();

        try {
            EventBus.getDefault().register(this, SearchMap.DirectionsResult.class);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}