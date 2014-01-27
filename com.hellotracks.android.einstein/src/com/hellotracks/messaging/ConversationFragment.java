package com.hellotracks.messaging;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.BasicAbstractFragment;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ConversationFragment extends BasicAbstractFragment {

    private ConversationsAdapter mAdapter;

    private TextView messageText;
    private TextView locationText;

    private String location = null;

    public ConversationFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        messageText = (TextView) mView.findViewById(R.id.messageText);
        locationText = (TextView) mView.findViewById(R.id.textLocation);

        if (getArguments() != null && getArguments().containsKey("account")) {
            this.account = getArguments().getString("account");
        }

        super.onActivityCreated(savedInstanceState);
        refill();
    }

    public void setData(String account, String name) {
        getView().findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
        this.account = account;
        this.location = null;
        if (locationText != null) {
            locationText.setText("");
            locationText.setVisibility(View.GONE);
        }
        refill();
    }

    protected void setData(String result) throws JSONException {
        adapter = new ConversationAdapter((MessagesScreen) getActivity(), new JSONObject(result));
        list.setAdapter(adapter);
        list.setOnItemClickListener(new MessageClickListener(adapter, list));
        if (adapter.getCount() > 0) {
            list.setSelection(adapter.getCount() - 1);
        }
        getView().findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
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
        return R.layout.screen_conversation;
    }

    @Override
    protected String getAction() {
        return AbstractScreen.ACTION_CONVERSATION;
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.NoMessagesDesc;
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
            QuickAction quick = new QuickAction(getActivity());
            ActionItem removeItem = new ActionItem(getActivity(), R.string.DeleteMessage);
            quick.addActionItem(removeItem);

            String msg = adapter.getString(pos, "msg");

            final ArrayList<String> urls = MessagesScreen.extractUrls(msg);
            for (String url : urls) {
                ActionItem urlItem = new ActionItem(getActivity(), url);
                quick.addActionItem(urlItem);
            }
            quick.setOnActionItemClickListener(new OnActionItemClickListener() {

                @Override
                public void onItemClick(QuickAction source, int position, int actionId) {
                    if (position == 0) {
                        ((AbstractScreen) getActivity()).deleteMessage(adapter.getId(pos));
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.ClearAll);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                onTrash(null);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void onSend(View view) {
        final String message = messageText.getText().toString();
        if (location != null) {
            String msg = "@uri geo:0,0?q=";
            msg += location + " text:" + message;
            sendMessageNow(msg);
            return;
        }

        sendMessageNow(message);
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
                    AbstractScreen.doAction(getActivity(), AbstractScreen.ACTION_DELMSG,
                            AbstractScreen.prepareObj(getActivity()).put("ids", ids), null, new ResultWorker() {
                                @Override
                                public void onResult(String result, Context context) {
                                    super.onResult(result, context);
                                    refill();
                                }
                            });
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onAddLocation(final View view) {
        location = null;
        locationText.setVisibility(View.VISIBLE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.AddDirectionToMessage);
        final Resources r = getResources();
        String[] names = new String[] { r.getString(R.string.MyCurrentLocation),
                r.getString(R.string.LocationFromNetwork), r.getString(R.string.EnterDirection) };
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    LocationManager locationManager = (LocationManager) getActivity().getSystemService(
                            Context.LOCATION_SERVICE);
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        location = loc.getLatitude() + "," + loc.getLongitude() + "("
                                + Prefs.get(getActivity()).getString(Prefs.NAME, "") + ")";
                        locationText.setText(r.getString(R.string.LocationAdded,
                                r.getString(R.string.MyCurrentLocation)));
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
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(R.string.EnterDirection);
        final EditText input = new EditText(getActivity());
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() >= 2) {
                    location = value;
                    locationText.setText(getResources().getString(R.string.LocationAdded, value));
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
            JSONObject obj = AbstractScreen.prepareObj(getActivity());
            AbstractScreen.doAction(getActivity(), AbstractScreen.ACTION_MARKERS, obj, null, new ResultWorker() {

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

                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle(R.string.LocationFromNetwork);
                                if (names.length > 0) {
                                    builder.setItems(names, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int item) {
                                            location = latitudes[item] + "," + longitudes[item] + "(" + names[item]
                                                    + ")";
                                            locationText.setText(getResources().getString(R.string.LocationAdded,
                                                    names[item]));
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
            Logger.w(exc);
        }

    }

    private void sendMessageNow(final String message) {
        if (message.trim().length() == 0)
            return;
        messageText.setText("");
        location = null;
        locationText.setVisibility(View.GONE);

        ((AbstractScreen) getActivity()).sendMessage(account, message, new ResultWorker() {
            @Override
            public void onResult(String result, Context context) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refill();
                    }
                });
            }
        });
    }

}