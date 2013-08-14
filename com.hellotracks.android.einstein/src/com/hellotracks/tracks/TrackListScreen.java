package com.hellotracks.tracks;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
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
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.flurry.android.FlurryAgent;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.messaging.ContactsScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.Time;
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
    

    protected long fromTS;
    private LazyAdapter adapter;
    private View mSpinnerView;
    
    @Override
    protected void refill() {
        fromTS = System.currentTimeMillis() * 2;
        super.refill();
        fromTS = Math.min(adapter.getLong(adapter.getCount() - 1, "ts") - 1, fromTS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                    intent.putExtra(C.type, C.person);
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
        setupActionBar(R.string.Map);
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
                long trackId = adapter.getId(pos);
                if (trackId > 0) {
                    FlurryAgent.logEvent("ShowTrack");
                    String url = adapter.getString(pos, AbstractScreen.URL);
                    String comments = adapter.getString(pos, "comments");
                    int labels = adapter.getInt(pos, "labels");
                    int actions = adapter.getInt(pos, "actions");
                    String text = adapter.getString(pos, "info");
                    showTrack(view, text, id, url, comments, labels, actions);
                } else if (trackId == -1) {
                    openMergeTracks(pos);
                }
            }
        });
        list.setOnScrollListener(new OnScrollListener() {

            private long lastTrigger = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int lastVisible = firstVisibleItem + visibleItemCount;

                if (totalItemCount >= 6 && lastVisible >= totalItemCount - 1
                        && (System.currentTimeMillis() - lastTrigger > Time.SEC * 1)) {
                    lastTrigger = System.currentTimeMillis();
                    loadMore();
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

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        if (array.length() > 9 && list.getFooterViewsCount() == 0) {
            mSpinnerView = getLayoutInflater().inflate(R.layout.list_item_spinner, null);
            list.addFooterView(mSpinnerView);
        }
        adapter = new TrackListAdapter(this, array);
        return adapter;
    }

    private void loadMore() {
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
                            JSONArray array = new JSONArray(result);
                            if (array.length() > 0) {
                                adapter.addData(array);
                                adapter.notifyDataSetChanged();
                                if (adapter.getCount() > 0)
                                    fromTS = Math.min(adapter.getLong(adapter.getCount() - 1, "ts") - 1, fromTS);
                            } else {
                                list.removeFooterView(mSpinnerView);
                            }
                        } catch (Exception exc) {
                            Log.e(exc);
                        }
                    }
                });
            }
        });
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
        if (view == null)
            return;

        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (pos == 0) {
                    Intent intent = new Intent(TrackListScreen.this, ContactsScreen.class);
                    intent.putExtra(C.type, C.person);
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

    public void openTrackInfo(final int pos, long trackId) {
        Intent intent = new Intent(TrackListScreen.this, TrackInfoScreen.class);
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
    }

    public void openMergeTracks(final int pos) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(TrackListScreen.this);
        alert.setTitle(R.string.MergeTracks);
        alert.setMessage(R.string.MergeText);
        alert.setPositiveButton(getResources().getString(R.string.MergeNow), new DialogInterface.OnClickListener() {
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
        alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog dlg = alert.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

}