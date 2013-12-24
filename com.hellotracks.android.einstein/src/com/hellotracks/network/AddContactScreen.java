package com.hellotracks.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.RequestFuture;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.account.ManagementScreen;
import com.hellotracks.api.StringRequest;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

public class AddContactScreen extends BasicAbstractScreen {

    protected int getContentView() {
        return R.layout.screen_addcontact;
    };

    @Override
    protected LazyAdapter createAdapter(JSONArray array) {
        final LazyAdapter adapter = new LazyAdapter(this, array) {

            @Override
            public View getView(final int index, View convertView, ViewGroup parent) {

                final View vi = convertView != null ? convertView : inflater.inflate(R.layout.list_item_addcontact,
                        null);

                try {
                    JSONObject node = data.get(index);
                    int type = node.has("type") ? node.getInt("type") : 0;

                    TextView title = (TextView) vi.findViewById(R.id.textTitle);
                    TextView info = (TextView) vi.findViewById(R.id.textDescription);

                    title.setText(node.getString("title"));
                    info.setText(node.getString("info"));

                    final ImageView icon = (ImageView) vi.findViewById(R.id.imageView);

                    String url = node.getString("url");

                    if (url != null) {
                        icon.setImageDrawable(getResources().getDrawable(R.drawable.button_flat_payment_plan));
                        Picasso.with(activity).load(url).into(icon);
                    } else {
                        icon.setVisibility(View.GONE);
                    }

                    if (type == TYPE_INVITATION) {
                        vi.setBackgroundResource(R.color.lightSelection);
                        title.setVisibility(View.INVISIBLE);
                    } else if (type == TYPE_RECOMMENDATION) {
                        vi.setBackgroundResource(R.color.lightSelection);
                        title.setVisibility(View.INVISIBLE);
                    } else {
                        vi.setBackgroundResource(R.color.transparent);
                        title.setVisibility(View.VISIBLE);
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

    @Override
    protected String getAction() {
        return ACTION_FIND;
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.JustASecond;
    }

    @Override
    protected Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(C.type, C.person);
        return params;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionBar(R.string.AddContact);

        final AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoCompView.setAdapter(new SearchAutoCompleteAdapter(this, R.layout.list_item_places_automcomplete));
        autoCompView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View v, int pos, long id) {
                try {
                    Entry e = resultList.get(pos);
                    Intent intent = new Intent(AddContactScreen.this, NewProfileScreen.class);
                    intent.putExtra(C.account, e.json.getString("account"));
                    intent.putExtra(C.name, e.json.getString("title"));
                    startActivityForResult(intent, C.REQUESTCODE_CONTACT());
                } catch (Exception exc) {
                    Log.e(exc);
                }
            }
        });

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                Intent intent = new Intent(AddContactScreen.this, NewProfileScreen.class);
                intent.putExtra(C.account, adapter.getAccount(pos));
                intent.putExtra(C.name, adapter.getString(pos, "title"));
                startActivityForResult(intent, C.REQUESTCODE_CONTACT());
            }
        });

        refill();

        registerCloseReceiverOn(C.BROADCAST_ADDTRACKTOMAP, C.BROADCAST_SHOWMAP);
    };

    public void onEvent(final SearchMap.DirectionsResult result) {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class Entry {
        JSONObject json;

        public Entry(JSONObject obj) {
            this.json = obj;
        }

        @Override
        public String toString() {
            try {
                return json.getString("title");
            } catch (JSONException e) {
                Log.e(e);
            }
            return "";
        }
    }

    private ArrayList<Entry> autocomplete(String input) {
        ArrayList<Entry> resultList = new ArrayList<Entry>();

        try {
            JSONObject data = prepareObj();
            data.put("type", "search");
            data.put("search", input);
            RequestFuture<String> future = RequestFuture.newFuture();

            JSONObject body = new JSONObject();
            body.put(FIELD_VERSION, CURRENT_VERSION);
            body.put(FIELD_DATA, data);

            String url = Prefs.CONNECTOR_BASE_URL + ACTION_SEARCH;
            StringRequest request = new StringRequest(url, body, future);
            request.setShouldCache(false);
            getRequestQueue().add(request);
            String response = future.get(); // this will block
            JSONArray array = new JSONArray(response);
            for (int i = 0; i < array.length(); i++) {
                resultList.add(new Entry(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.w(e);
        }
        return resultList;
    }

    private ArrayList<Entry> resultList;

    private class SearchAutoCompleteAdapter extends ArrayAdapter<Entry> implements Filterable {

        public SearchAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public View getView(int index, View convertView, ViewGroup parent) {
            final View vi = convertView == null ? getLayoutInflater().inflate(R.layout.list_item_places_automcomplete,
                    null) : convertView;

            try {
                Entry entry = resultList.get(index);

                TextView title = (TextView) vi.findViewById(R.id.textTitle);
                title.setText(entry.json.getString("title"));

                TextView description = (TextView) vi.findViewById(R.id.textDescription);
                description.setText(entry.json.getString("info"));

                ImageView image = (ImageView) vi.findViewById(R.id.imageView);
                if (entry.json.has("url")) {
                    image.setImageResource(R.drawable.ic_action_profile);
                    Picasso.with(AddContactScreen.this).load(entry.json.getString("url")).into(image);
                    image.setVisibility(View.VISIBLE);
                } else {
                    image.setVisibility(View.GONE);
                }
            } catch (Exception exc) {
                Log.e(exc);
            }
            return vi;
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public Entry getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    public void onInvite(View view) {
        openInviteDialog();
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
                            Intent intent = new Intent(AddContactScreen.this, ManagementScreen.class);
                            intent.putExtra("profile", true);
                            startActivityForResult(intent, C.REQUESTCODE_CONTACT());
                        }
                    }).setMessage(R.string.SetNameAndEmail).create();
            dlg.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(AddContactScreen.this);
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