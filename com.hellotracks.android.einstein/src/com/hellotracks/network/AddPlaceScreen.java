package com.hellotracks.network;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.R;
import com.hellotracks.base.BasicAbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.places.Places;
import com.hellotracks.places.Places.Result;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

public class AddPlaceScreen extends BasicAbstractScreen {

    protected int getContentView() {
        return R.layout.screen_addplace;
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
        return ACTION_FIND;
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.JustASecond;
    }

    @Override
    protected Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(C.type, C.place);
        params.put("cnt", 30);
        return params;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionBar(R.string.AddPlace);

        final AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        Location l = getLastLocation();
        autoCompView.setAdapter(new Places.PlacesAutoCompleteAdapter(this, R.layout.list_item_places_automcomplete, l
                .getLatitude(), l.getLongitude()));
        autoCompView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View v, int position, long id) {
                Result result = (Result) ad.getItemAtPosition(position);
                Toast.makeText(AddPlaceScreen.this, result.description, Toast.LENGTH_SHORT).show();
                Intent data = new Intent();
                data.putExtra("reference", result.reference);

                String text = result.description;
                int idx = text.indexOf(",");
                if (idx > 0) {
                    data.putExtra("description", text.substring(0, idx));
                } else {
                    data.putExtra("description", result.description);
                }

                setResult(RESULT_OK, data);
                finish();
            }
        });

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                Intent intent = new Intent(AddPlaceScreen.this, NewProfileScreen.class);
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

}