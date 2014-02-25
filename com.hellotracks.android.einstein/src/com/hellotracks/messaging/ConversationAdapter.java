package com.hellotracks.messaging;

import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.hellotracks.Logger;
import com.hellotracks.R;
import com.hellotracks.events.TemporyMarkerEvent;
import com.hellotracks.map.Actions;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.StaticMap;
import com.hellotracks.util.Time;
import com.hellotracks.util.Ui;
import com.hellotracks.util.UnitUtils;
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
    private MessagesScreen screen;

    public ConversationAdapter(MessagesScreen a, JSONObject dataObject) throws JSONException {
        super(a, dataObject.getJSONArray("conversation"));

        screen = a;

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

            final ImageView icon;
            if (in) {
                icon = (ImageView) vi.findViewById(R.id.iconLeft);
                vi.findViewById(R.id.iconRight).setVisibility(View.GONE);
                vi.findViewById(R.id.layoutBubble).setBackgroundResource(R.drawable.bubble_speach_blue);
                timeField.setTextColor(Color.rgb(230, 230, 230));
                timeField.setText(Time.formatDateTime(node.getLong("ts")));
            } else {
                icon = (ImageView) vi.findViewById(R.id.iconRight);
                vi.findViewById(R.id.iconLeft).setVisibility(View.GONE);
                vi.findViewById(R.id.layoutBubble).setBackgroundResource(R.drawable.bubble_speach_white);
                timeField.setTextColor(Color.rgb(100, 100, 100));
                
                String read = node.getInt("read") > 1 ? screen.getString(R.string.ReadAlready) : screen.getString(R.string.ReadNotYet);
                timeField.setText(Time.formatDateTime(node.getLong("ts")) + " |  ✓ " + read);
            }
            icon.setVisibility(View.VISIBLE);
            
            

            final String msg = node.getString("msg");

            int textIndex = msg.indexOf("text:");
            String text = textIndex > 0 ? msg.substring(textIndex + 5) : msg;
            if (msg.contains("geo:") || msg.contains("navigation:")) {
                vi.findViewById(R.id.layoutLocation).setVisibility(View.VISIBLE);
                ImageView locImage = (ImageView) vi.findViewById(R.id.image);
                try {
                    int idx1 = msg.indexOf("q=") + 2;
                    if (!Character.isDigit(msg.charAt(idx1))) {
                        idx1 += 1;
                    }
                    final String name = extractNameOfMessage(msg);
                    text = name + "\n\n" + text;
                    int end = idx1;
                    while (end < msg.length()) {
                        if (Character.isDigit(msg.charAt(end)) || '-' == msg.charAt(end) || msg.charAt(end) == ','
                                || msg.charAt(end) == '.') {
                            end += 1;
                        } else {
                            break;
                        }
                    }
                    String sub = msg.substring(idx1, end);
                    String[] s = sub.split(",");
                    final double lat = Double.parseDouble(s[0]);
                    final double lng = Double.parseDouble(s[1]);
                    Button b = (Button) vi.findViewById(R.id.buttonDirections);
                    b.setText(UnitUtils.getNiceDistance(screen, new LatLng(lat, lng), screen.getLastLocation()));
                    b.setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            Actions.doOnDirections(screen, screen.getLastLocation(), lat, lng);
                        }
                    });                   
                    
                    URL url = StaticMap.Google.createMap(150, lat, lng);
                    Picasso.with(activity).load(url.toString()).into(locImage);
                    locImage.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (msg.contains("navigation:")) {
                                Actions.doOnDirections(screen, screen.getLastLocation(), lat, lng);
                            } else {
                                EventBus.getDefault().post(new TemporyMarkerEvent(lat, lng, name));
                                activity.finish();
                            }
                        }
                    });
                } catch (Exception exc) {
                    Logger.e(exc);
                    Logger.w(msg);
                }
            } else if (msg.contains("http") || msg.contains("www.")) {
                vi.findViewById(R.id.layoutLocation).setVisibility(View.GONE);
                //vi.findViewById(R.id.pin).setVisibility(View.VISIBLE);
            } else {
                //vi.findViewById(R.id.pin).setVisibility(View.GONE);
                vi.findViewById(R.id.layoutLocation).setVisibility(View.GONE);
            }

            messageField.setText(text);
            messageField.setTextColor(screen.getResources().getColor(in ?  R.color.white : R.color.text));

            String url = in ? otherUrl : myUrl;
            if (url != null) {
                Picasso.with(activity).load(url).into(icon);
            } else {
                icon.setVisibility(View.GONE);
            }
            return vi;
        } catch (Exception exc) {
            Logger.w(exc);
        }

        return new View(inflater.getContext());
    }

    private String extractNameOfMessage(String msg) {
        int idx2 = msg.indexOf("(", 0);
        int idx3 = msg.indexOf(")", idx2) + 1;
        if (idx2 > 0 && idx3 > 0) {
            return msg.substring(idx2 + 1, idx3 - 1);
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