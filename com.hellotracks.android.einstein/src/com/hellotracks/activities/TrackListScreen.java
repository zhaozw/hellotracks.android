package com.hellotracks.activities;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.flurry.android.FlurryAgent;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.einstein.BasicAbstractScreen;
import com.hellotracks.einstein.C;
import com.hellotracks.einstein.ContactsScreen;
import com.hellotracks.einstein.TrackInfoScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class TrackListScreen extends BasicAbstractScreen {

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            account = Prefs.get(TrackListScreen.this).getString(Prefs.USERNAME, "");
            getSupportActionBar().setTitle(R.string.MyTracks);
            refill();
        }
    };

    private BroadcastReceiver mTrackReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent data) {
            finish();
        }

    };

    protected void onResume() {
        registerReceiver(mIntentReceiver, new IntentFilter(Prefs.TAB_TRACKS_INTENT));
        super.onResume();
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(mTrackReceiver);
        unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }

    protected void setupActionBar() {
        getSupportActionBar().show();
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg));
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setTitle(R.string.MyTracks);
    }

    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return true;
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.clear();

        {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.MyTracks);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    account = Prefs.get(TrackListScreen.this).getString(Prefs.USERNAME, "");
                    getSupportActionBar().setTitle(R.string.MyTracks);
                    refill();
                    return false;
                }
            });
        }

        {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.TracksFromContacts);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    Intent intent = new Intent(TrackListScreen.this, ContactsScreen.class);
                    intent.putExtra(C.type, "person");
                    intent.putExtra(C.account, account);
                    startActivityForResult(intent, C.REQUESTCODE_CONTACT);
                    return false;
                }
            });
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setupActionBar();
        registerReceiver(mTrackReceiver, new IntentFilter(C.BROADCAST_ADDTRACKTOMAP));
        count = 1;
        account = getIntent().getStringExtra(C.account);
        if (account == null) {
            account = Prefs.get(this).getString(Prefs.USERNAME, "");
        }
        super.onCreate(savedInstanceState);

        String name = getIntent().getStringExtra(C.name);

        if (name != null && name.length() > 0)
            getSupportActionBar().setTitle(name);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, final View view, final int pos, long id) {
                FlurryAgent.logEvent("Track");
                Intent intent = new Intent(TrackListScreen.this, TrackInfoScreen.class);
                long trackId = adapter.getId(pos);
                if (trackId > 0) {
                    intent.putExtra("track", trackId);
                    intent.putExtra("labels", adapter.getInt(pos, "labels"));
                    intent.putExtra("url", adapter.getString(pos, "url"));
                    intent.putExtra("text", adapter.getString(pos, "info"));
                    intent.putExtra("link", adapter.getString(pos, "link"));
                    intent.putExtra("actions", adapter.getInt(pos, "actions"));
                    try {
                        intent.putExtra("comments", adapter.getArray(pos, "comments").toString());
                    } catch (Exception exc) {
                    }
                    startActivityForResult(intent, 0);
                } else if (trackId == -1) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(TrackListScreen.this);
                    alert.setTitle(R.string.MergeTracks);
                    alert.setMessage(R.string.MergeText);
                    alert.setPositiveButton(getResources().getString(R.string.MergeNow),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    try {
                                        JSONObject data = prepareObj();
                                        data.put("track1", adapter.getLong(pos, "track1"));
                                        data.put("track2", adapter.getLong(pos, "track2"));
                                        doAction(ACTION_MERGETRACKS, data, new ResultWorker() {
                                            @Override
                                            public void onResult(String result, Context context) {
                                                refill();
                                            }
                                        });
                                        Toast.makeText(TrackListScreen.this, R.string.OK, Toast.LENGTH_SHORT).show();
                                    } catch (Exception exc) {
                                    }
                                }
                            });
                    alert.setNegativeButton(getResources().getString(R.string.Cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                }
                            });
                    AlertDialog dlg = alert.create();
                    dlg.setCanceledOnTouchOutside(true);
                    dlg.show();
                }
            }
        });

        refill();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getStringExtra(C.account) != null) {
            account = data.getStringExtra(C.account);
            String name = data.getStringExtra(C.name);
            if (name != null) {
                getSupportActionBar().setTitle(name);
            }
        }
        if (resultCode == C.RESULTCODE_SHOWTRACK) {
            setResult(C.RESULTCODE_SHOWTRACK, data);
            finish();
        }
        refill();
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.CurrentlyNoTracksAvailable;
    }

    public void onBack(View view) {
        finish();
    }

    public static class Flag {
        public static final int GREEN = 1 << 1;
        public static final int YELLOW = 1 << 2;
        public static final int ORANGE = 1 << 3;
        public static final int RED = 1 << 4;
        public static final int VIOLETT = 1 << 5;
        public static final int BLUE = 1 << 6;
        public static final int NONE = 0;
    }

    private LazyAdapter adapter;

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        if (array.length() > 0 && list.getFooterViewsCount() == 0) {
            View more = getLayoutInflater().inflate(R.layout.list_item_more, null);
            Button button = (Button) more.findViewById(R.id.loadButton);
            button.setOnClickListener(new OnClickListener() {

                protected long fromTS = System.currentTimeMillis() * 2;

                @Override
                public void onClick(final View v) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    if (getParams() != null)
                        map.putAll(getParams());
                    map.put("fromts", fromTS - 1);
                    map.put("count", 10);
                    refill(map, new ResultWorker() {
                        @Override
                        public void onResult(final String result, Context context) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        adapter.addData(new JSONArray(result));
                                        adapter.notifyDataSetChanged();
                                        if (adapter.getCount() > 0)
                                            fromTS = Math.min(adapter.getLong(adapter.getCount() - 1, "ts") - 1, fromTS);
                                    } catch (Exception exc) {
                                    }
                                }
                            });
                        }
                    });

                }
            });
            list.addFooterView(more);
        }
        adapter = new LazyAdapter(this, array) {
            @Override
            protected int getListItemLayoutFor(int index) {
                return R.layout.list_item_track;
            }

            @Override
            public View getView(final int index, View convertView, ViewGroup parent) {
                View view = super.getView(index, convertView, parent);

                View layoutMain = view.findViewById(R.id.layoutMain);
                View layoutLabels = view.findViewById(R.id.layoutLabels);
                View layoutPause = view.findViewById(R.id.layoutPause);
                View message = (View) view.findViewById(R.id.message);

                final long id = getId(index);
                boolean isPause = id == -1;

                layoutLabels.setVisibility(isPause ? View.GONE : View.VISIBLE);
                layoutMain.setVisibility(isPause ? View.GONE : View.VISIBLE);
                message.setVisibility(isPause ? View.GONE : View.VISIBLE);
                layoutPause.setVisibility(isPause ? View.VISIBLE : View.GONE);

                if (isPause) {
                    TextView button = (TextView) view.findViewById(R.id.time);
                    button.setText(getString(index, "text"));
                    return view;
                }

                view.findViewById(R.id.image).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        FlurryAgent.logEvent("ShowTrack");
                        String url = adapter.getString(index, AbstractScreen.URL);
                        String comments = adapter.getString(index, "comments");
                        int labels = adapter.getInt(index, "labels");
                        int actions = adapter.getInt(index, "actions");
                        String text = adapter.getString(index, "info");
                        showTrack(v, text, id, url, comments, labels, actions);
                    }
                });

                JSONArray comments = getArray(index, "comments");
                if (comments != null && comments.length() > 0) {
                    TextView bubble = (TextView) view.findViewById(R.id.bubble);
                    bubble.setVisibility(View.VISIBLE);
                    bubble.setText(" " + comments.length() + " ");
                } else {
                    view.findViewById(R.id.bubble).setVisibility(View.GONE);
                }

                int labels = getInt(index, "labels");
                view.findViewById(R.id.label_green).setVisibility((labels & Flag.GREEN) > 0 ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.label_yellow).setVisibility(
                        (labels & Flag.YELLOW) > 0 ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.label_orange).setVisibility(
                        (labels & Flag.ORANGE) > 0 ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.label_red).setVisibility((labels & Flag.RED) > 0 ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.label_violett).setVisibility(
                        (labels & Flag.VIOLETT) > 0 ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.label_blue).setVisibility((labels & Flag.BLUE) > 0 ? View.VISIBLE : View.GONE);

                return view;

            }

        };
        return adapter;
    }

    @Override
    protected int getContentView() {
        return R.layout.screen_tracks;
    }

    @Override
    protected Map<String, Object> getParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("pauses", true);
        return params;
    }

    @Override
    protected String getAction() {
        return ACTION_TRACKS;
    }

    public void onMenu(View view) {
        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (pos == 0) {
                    Intent intent = new Intent(TrackListScreen.this, ContactsScreen.class);
                    intent.putExtra(C.type, "person");
                    intent.putExtra(C.account, account);
                    startActivityForResult(intent, C.REQUESTCODE_CONTACT);
                } else {
                    account = Prefs.get(TrackListScreen.this).getString(Prefs.USERNAME, "");
                    getSupportActionBar().setTitle(R.string.MyTracks);
                    refill();
                }
            }
        });

        ActionItem item = new ActionItem(this, R.string.Contacts);
        quick.addActionItem(item);
        ActionItem item2 = new ActionItem(this, R.string.MyTracks);
        quick.addActionItem(item2);
        quick.show(view);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            onMenu(findViewById(R.id.button_menu));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}