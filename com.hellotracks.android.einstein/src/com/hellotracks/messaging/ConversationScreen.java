package com.hellotracks.messaging;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.tracks.TrackListScreen;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ConversationScreen extends AbstractScreen {

    protected ListView list;

    private String account;

    private TextView messageText;
    private TextView locationText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_conversation);
        list = (ListView) findViewById(R.id.list);
        account = getIntent().getStringExtra(C.account);
        messageText = (TextView) findViewById(R.id.messageText);
        locationText = (TextView) findViewById(R.id.textLocation);
        refill();
        setupActionBar(R.string.Messages);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.ClearAll);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                onTrash(null);
                return false;
            }
        });
        return true;
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, intent.getStringExtra("msg"), Toast.LENGTH_LONG).show();
            refill();
        }
    };

    private void refill() {
        try {
            JSONObject obj = prepareObj();
            obj.put(C.account, account);
            doAction("conversation", obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
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
        registerReceiver(messageReceiver, new IntentFilter(Prefs.PUSH_INTENT));
        super.onResume();
        location = null;
    };

    @Override
    protected void onPause() {
        unregisterReceiver(messageReceiver);
        super.onPause();
    }

    private ConversationAdapter adapter;

    protected void setData(String result) throws Exception {

        adapter = new ConversationAdapter(this, new JSONObject(result));
        list.setAdapter(adapter);
        list.setOnItemClickListener(new MessageClickListener(adapter, list));
        if (adapter.getCount() > 0)
            list.setSelection(adapter.getCount() - 1);
    }

    public void onSend(View view) {
        final String message = messageText.getText().toString();

        if (location != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.SendDirection);
            Resources r = getResources();
            String[] names = new String[] { r.getString(R.string.ForMaps), r.getString(R.string.ForGoogleNavigation) };
            builder.setItems(names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String msg = item == 0 ? "@uri geo:0,0?q=" : "@uri google.navigation:q=";
                    msg += location + " text:" + message;
                    sendMessageNow(msg);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
            return;
        }

        sendMessageNow(message);

    }

    private void sendMessageNow(final String message) {
        if (message.trim().length() == 0)
            return;
        messageText.setText("");
        location = null;
        locationText.setVisibility(View.GONE);

        sendMessage(account, message, new ResultWorker() {
            @Override
            public void onResult(String result, Context context) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refill();
                    }
                });
            }
        });
    }

    public void onTrash(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(list.getContext());
        builder.setTitle(R.string.DeleteWholeConversation);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(R.string.Delete, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    JSONArray ids = new JSONArray();
                    for (long id : adapter.getAllIds()) {
                        ids.put(id);
                    }
                    doAction(ACTION_DELMSG, prepareObj().put("ids", ids), new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            super.onResult(result, context);
                            refill();
                        }
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
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

    public class MessageClickListener implements OnItemClickListener {

        LazyAdapter adapter;
        ListView list;

        public MessageClickListener(LazyAdapter adapter, ListView list) {
            this.adapter = adapter;
            this.list = list;
        }

        @Override
        public void onItemClick(AdapterView<?> ad, View view, final int pos, long id) {
            QuickAction quick = new QuickAction(ConversationScreen.this);
            ActionItem removeItem = new ActionItem(ConversationScreen.this, R.string.DeleteMessage);
            quick.addActionItem(removeItem);

            String msg = adapter.getString(pos, "msg");

            final ArrayList<String> urls = extractUrls(msg);
            for (String url : urls) {
                ActionItem urlItem = new ActionItem(ConversationScreen.this, url);
                quick.addActionItem(urlItem);
            }
            quick.setOnActionItemClickListener(new OnActionItemClickListener() {

                @Override
                public void onItemClick(QuickAction source, int position, int actionId) {
                    if (position == 0) {
                        deleteMessage(adapter.getId(pos));
                        adapter.remove(pos);
                    } else {
                        String url = urls.get(position - 1);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                }
            });

            quick.show(view);
        }
    }

    private String location = null;

    public void onAddLocation(final View view) {
        location = null;
        locationText.setVisibility(View.VISIBLE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.AddDirection);
        Resources r = getResources();
        String[] names = new String[] { r.getString(R.string.MyCurrentLocation),
                r.getString(R.string.LocationFromNetwork), r.getString(R.string.EnterDirection) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        location = loc.getLatitude() + "," + loc.getLongitude() + "("
                                + Prefs.get(ConversationScreen.this).getString(Prefs.NAME, "") + ")";
                        locationText.setText(location);
                    }
                } else if (item == 1)
                    openNetworkDialog(view);
                else if (item == 2)
                    openDirectionDialog(view);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    protected void openDirectionDialog(final View view) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.EnterDirection);
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() >= 2) {
                    location = value;
                    locationText.setText(value);
                }
            }
        });
        alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    private void openNetworkDialog(final View view) {
        try {
            JSONObject obj = prepareObj();
            doAction(ACTION_MARKERS, obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    try {
                        JSONArray array = new JSONArray(result);
                        final String[] names = new String[array.length()];
                        final double[] latitudes = new double[array.length()];
                        final double[] longitudes = new double[array.length()];

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            double lat = obj.getDouble("lat");
                            double lng = obj.getDouble("lng");
                            String name = obj.getString("name");

                            names[i] = name;
                            latitudes[i] = lat;
                            longitudes[i] = lng;
                        }

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                AlertDialog.Builder builder = new AlertDialog.Builder(ConversationScreen.this);
                                builder.setTitle(R.string.LocationFromNetwork);
                                if (names.length > 0) {
                                    builder.setItems(names, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int item) {
                                            location = latitudes[item] + "," + longitudes[item] + "(" + names[item]
                                                    + ")";
                                            locationText.setText(location);
                                        }
                                    });
                                    AlertDialog dialog = builder.create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            }

                        });
                    } catch (Exception exc) {
                    }

                }
            });
        } catch (Exception exc) {
            Log.w(exc);
        }

    }

}