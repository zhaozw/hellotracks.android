package com.hellotracks.messaging;

import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.C;
import com.hellotracks.network.NewPlaceScreen;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.StaticMap;
import com.hellotracks.util.Ui;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;

public class ConversationAdapter extends LazyAdapter {

    private String myUrl = null;
    private String myName = null;
    private String myAccount = null;
    private String otherUrl = null;
    private String otherName = null;
    private String otherAccount = null;
    private MessagesScreen messagesScreen;

    public ConversationAdapter(MessagesScreen a, JSONObject dataObject) throws JSONException {
        super(a, dataObject.getJSONArray("conversation"));

        messagesScreen = a;

        JSONObject me = dataObject.getJSONObject("me");
        myUrl = me.getString("url");
        myName = me.getString("name");
        myAccount = me.getString("account");

        JSONObject other = dataObject.getJSONObject("other");
        otherUrl = other.getString("url");
        otherName = other.getString("name");
        otherAccount = other.getString("account");
    }

    public View getView(int index, View convertView, ViewGroup parent) {

        try {
            JSONObject node = data.get(index);

            boolean in = node.getBoolean("in");

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_message, null);
            }

            final View vi = convertView;

            TextView timeField = (TextView) vi.findViewById(R.id.title);
            TextView messageField = (TextView) vi.findViewById(R.id.info);

            final ImageView icon = (ImageView) vi.findViewById(in ? R.id.iconLeft : R.id.iconRight);
            icon.setVisibility(View.VISIBLE);

            vi.findViewById(in ? R.id.iconRight : R.id.iconLeft).setVisibility(View.GONE);

            timeField.setText(node.getString("time"));

            View bubble = vi.findViewById(R.id.layoutBubble);
            bubble.setBackgroundResource(in ? R.drawable.bubbleblue : R.drawable.bubble_green);
            ((LinearLayout.LayoutParams) bubble.getLayoutParams()).gravity = in ? Gravity.LEFT : Gravity.RIGHT;

            String msg = node.getString("msg");

            int textIndex = msg.indexOf("text:");
            String text = textIndex > 0 ? msg.substring(textIndex + 5) : msg;

            ImageView image = (ImageView) vi.findViewById(R.id.image);
            if (msg.contains("geo:") || msg.contains("navigation:")) {
                try {
                    int idx1 = msg.indexOf("q=") + 2;
                    if (!Character.isDigit(msg.charAt(idx1))) {
                        idx1 += 1;
                    }
                    final String name = extractNameOfMessage(msg);
                    text = name + "\n\n" + text;
                    int end = idx1;
                    while(end < msg.length()) {
                        if (Character.isDigit(msg.charAt(end)) || '-' == msg.charAt(end) || msg.charAt(end) == ',' || msg.charAt(end) == '.') {
                            end += 1;
                        } else {
                            break;
                        }
                    }
                    String sub = msg.substring(idx1, end);
                    String[] s = sub.split(",");
                    final double lat = Double.parseDouble(s[0]);
                    final double lng = Double.parseDouble(s[1]);
                    URL url = StaticMap.Google.createMap(250, lat, lng);
                    Picasso.with(activity).load(url.toString()).into(image);
                    image.setVisibility(View.VISIBLE);
                    image.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            com.hellotracks.types.LatLng origin = new com.hellotracks.types.LatLng(
                                    ((MessagesScreen) activity).getLastLocation());
                            com.hellotracks.types.LatLng destination = new com.hellotracks.types.LatLng(lat, lng);
                            SearchMap.asyncGetDirections(activity, origin, destination,
                                    new SearchMap.Callback<SearchMap.DirectionsResult>() {

                                        @Override
                                        public void onResult(boolean success, SearchMap.DirectionsResult result) {
                                            if (success) {
                                                EventBus.getDefault().post(result);
                                                activity.finish();
                                            } else {
                                                Ui.makeText(activity, R.string.NoEntries, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        }
                    });
                } catch (Exception exc) {
                    Log.e(exc);
                    Log.w(msg);
                }
            } else if (msg.contains("http") || msg.contains("www.")) {
                image.setVisibility(View.GONE);
                //vi.findViewById(R.id.pin).setVisibility(View.VISIBLE);
            } else {
                //vi.findViewById(R.id.pin).setVisibility(View.GONE);
                image.setVisibility(View.GONE);
            }

            messageField.setText(text);

            String url = in ? otherUrl : myUrl;
            if (url != null) {
                Picasso.with(activity).load(url).into(icon);
            } else {
                icon.setVisibility(View.GONE);
            }
            return vi;
        } catch (Exception exc) {
            Log.w(exc);
        }

        return new View(inflater.getContext());
    }
    
    private String extractNameOfMessage(String msg) {
        int idx2 = msg.indexOf("(", 0);
        int idx3 = msg.indexOf(")", idx2) + 1;
        if (idx2 > 0 && idx3 > 0) {
            return msg.substring(idx2 +1, idx3 - 1);
        }
        return "";
    }

    public void removeAll() {
        data.clear();
    }

    @Override
    protected int getListItemLayoutFor(int index) {
        return 0;
    }

}