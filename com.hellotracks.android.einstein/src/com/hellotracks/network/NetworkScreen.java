package com.hellotracks.network;

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
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.profile.ProfileSettingsScreen;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

public class NetworkScreen extends BasicAbstractScreen {

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clearAndRefill();
        }
    };

    protected void onResume() {
        registerReceiver(mIntentReceiver, new IntentFilter(Prefs.TAB_TRACKS_INTENT));
        super.onResume();
    };

    @Override
    protected void onPause() {
        unregisterReceiver(mIntentReceiver);
        super.onPause();
    }

    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (type == null || "".equals(type))
                finish();
            else
                clearAndRefill();
            break;
        }
        return true;
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.clear();

        {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.SearchForPeopleOrPlaces);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            item.setIcon(R.drawable.ic_action_search);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    openSearchDialog();
                    return false;
                }
            });
        }

        {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.FindUsersNearbyMe);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            item.setIcon(R.drawable.ic_action_location);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    find();
                    return false;
                }
            });
        }

        {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.InviteNewUser);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            item.setIcon(R.drawable.ic_action_invite);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    openInviteDialog();
                    return false;
                }
            });
        }

        return true;
    }

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        final LazyAdapter adapter = new LazyAdapter(this, array) {

            @Override
            public View getView(final int index, View convertView, ViewGroup parent) {

                final View vi = convertView != null ? convertView : inflater.inflate(R.layout.list_item_mynetwork, null);

                try {
                    JSONObject node = data.get(index);
                    int type = node.has("type") ? node.getInt("type") : 0;

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
                        Picasso.with(activity).load(url).into(icon);
                    } else {
                        icon.setVisibility(View.GONE);
                    }

                    if (type == TYPE_INVITATION) {
                        info.setBackgroundColor(getResources().getColor(R.color.orange));
                        title.setVisibility(View.INVISIBLE);
                    } else if (type == TYPE_RECOMMENDATION) {
                        info.setBackgroundColor(getResources().getColor(R.color.violett));
                        title.setVisibility(View.INVISIBLE);
                    } else {
                        info.setBackgroundDrawable(null);
                        title.setVisibility(View.VISIBLE);
                    }

                    View ignore = vi.findViewById(R.id.ignore);
                    if ((type & TYPE_EXTERNAL) > 0) {
                        time.setVisibility(View.GONE);
                        ignore.setVisibility(View.VISIBLE);
                        ignore.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                try {
                                    JSONObject obj = prepareObj();
                                    long id = getId(index);
                                    Log.w("not interrested in id=" + id);
                                    obj.put("id", id);

                                    doAction(AbstractScreen.ACTION_NOTINTERESTED, obj,
                                            getResources().getString(R.string.SendNow), new ResultWorker() {
                                                @Override
                                                public void onResult(String result, Context context) {
                                                    NetworkScreen.this.adapter.remove(index);
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
                } catch (Exception exc) {
                    Log.w(exc);
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

    private void find() {
        type = C.person;
        action = ACTION_FIND;
        refill();
    }

    @Override
    protected String getAction() {
        return action;
    }

    @Override
    protected Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(C.type, type);
        if (ACTION_MYNETWORK.equals(action)) {
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
    private String type = "";
    private String search;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar(R.string.Map);
        action = getIntent().getStringExtra(C.action);
        type = getIntent().getStringExtra(C.type);
        search = getIntent().getStringExtra(C.search);
        if (action == null)
            action = ACTION_MYNETWORK;
        account = getIntent().getStringExtra(C.account);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                Intent intent = new Intent(NetworkScreen.this, NewProfileScreen.class);
                intent.putExtra(C.account, adapter.getAccount(pos));
                intent.putExtra(C.name, adapter.getString(pos, "title"));
                startActivityForResult(intent, C.REQUESTCODE_CONTACT);
            }
        });
        
        
        {
            final View v = getLayoutInflater().inflate(R.layout.list_item_search, null);
            final TextView searchField = (TextView) v.findViewById(R.id.searchField);
            searchField.setFocusable(false);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
            searchField.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    openSearchDialog();
                }
            });
            list.addFooterView(v);
        }
        {
            View v = getLayoutInflater().inflate(R.layout.list_item_more, null);
            Button button = (Button) v.findViewById(R.id.loadButton);
            button.setText(R.string.FindUsersNearbyMe);
            button.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_action_location), null, null, null);

            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(final View view) {
                    find();
                }
            });
            list.addFooterView(v);
        }
        {
            View v = getLayoutInflater().inflate(R.layout.list_item_more, null);
            Button button = (Button) v.findViewById(R.id.loadButton);
            button.setText(R.string.InviteNewUser);
            button.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_action_invite), null, null, null);
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(final View v) {
                    openInviteDialog();
                }
            });
            list.addFooterView(v);
        }

        refill();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.REQUESTCODE_CONTACT) {
            clearAndRefill();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void openSearchDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.EnterSearch);
        final EditText input = new EditText(this);
        input.setHint(R.string.NameOrPlace);
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.Search), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() >= 2) {
                    action = ACTION_SEARCH;
                    type = "search";
                    search = value;
                    refill();
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

    private void clearAndRefill() {
        type = "";
        action = ACTION_MYNETWORK;
        refill();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void openInviteDialog() {
        if (!isOnline(true)) {
            return;
        }
        String name = Prefs.get(this).getString(Prefs.NAME, "");
        String email = Prefs.get(this).getString(Prefs.EMAIL, "");
        String defName = Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
        if (name.trim().length() == 0 || name.equals(defName) || email.length() == 0) {
            AlertDialog dlg = new AlertDialog.Builder(this).setCancelable(true)
                    .setPositiveButton(R.string.OpenProfile, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface d, int i) {
                            startActivityForResult(new Intent(NetworkScreen.this, ProfileSettingsScreen.class),
                                    C.REQUESTCODE_CONTACT);
                        }
                    }).setMessage(R.string.SetNameAndEmail).create();
            dlg.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(NetworkScreen.this);
        builder.setTitle(R.string.InviteContact);
        Resources r = getResources();
        String[] names = new String[] { r.getString(R.string.InviteContactByEmail),
                r.getString(R.string.InviteContactBySms) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    onInviteContactByEmail(null);
                } else {
                    onInviteContactBySms(null);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}